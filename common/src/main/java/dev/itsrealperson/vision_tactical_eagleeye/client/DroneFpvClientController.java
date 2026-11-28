package dev.itsrealperson.vision_tactical_eagleeye.client;

import dev.architectury.event.events.client.ClientTickEvent;
import dev.itsrealperson.vision_goggles.api.VisionGogglesClientAPI;
import dev.itsrealperson.vision_tactical_eagleeye.VisionTacticalEagleEye;
import dev.itsrealperson.vision_tactical_eagleeye.entity.TacticalDroneEntity;
import dev.itsrealperson.vision_tactical_eagleeye.network.LatticeTelemetrySnapshot;
import dev.itsrealperson.vision_tactical_eagleeye.network.LatticeTrackEntry;
import dev.itsrealperson.vision_tactical_eagleeye.network.ModNetworking;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Client-side controller for zero-FPS-loss FPV drone camera linking ({@code mc.setCameraEntity}),
 * smooth inertial flight input capture, Vision Goggles IR spotlight/thermal integration,
 * and Lattice OS telemetry state.
 */
@Environment(EnvType.CLIENT)
public final class DroneFpvClientController {
    private static int activeDroneId = -1;
    private static boolean linkedInFpv = false;
    private static CameraType previousCameraType = null;
    private static boolean allowingCameraRestore = false;

    private static float fpvYaw = 0.0f;
    private static float fpvPitch = 0.0f;

    private static volatile LatticeTelemetrySnapshot latestTelemetry = LatticeTelemetrySnapshot.EMPTY;
    private static final Map<Integer, LatticeTrackEntry> TRACK_MAP = new ConcurrentHashMap<>();

    private static boolean initialized = false;

    private DroneFpvClientController() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        ClientTickEvent.CLIENT_POST.register(DroneFpvClientController::onClientTick);
        registerVisionGogglesHooks();
    }

    private static void registerVisionGogglesHooks() {
        try {
            VisionGogglesClientAPI.registerThermalEntityColorOverride(
                    DroneFpvClientController::shouldOverrideThermalColor,
                    DroneFpvClientController::getThermalOverrideColor
            );
        } catch (LinkageError e) {
            try {
                Function<LivingEntity, Integer> fallback = entity ->
                        shouldOverrideThermalColor(entity) ? getThermalOverrideColor(entity) : null;
                VisionGogglesClientAPI.registerThermalEntityColorOverride(fallback);
            } catch (Throwable t) {
                VisionTacticalEagleEye.LOGGER.warn("[EagleEye] Could not register thermal color override: {}", t.toString());
            }
        } catch (Throwable t) {
            VisionTacticalEagleEye.LOGGER.warn("[EagleEye] Thermal color override registration skipped: {}", t.toString());
        }
    }

    private static boolean shouldOverrideThermalColor(LivingEntity entity) {
        if (entity == null || !entity.isAlive()) {
            return false;
        }
        if (entity instanceof TacticalDroneEntity) {
            return true;
        }
        LatticeTrackEntry track = TRACK_MAP.get(entity.getId());
        return track != null && track.threatLevel() == LatticeTrackEntry.THREAT_HOSTILE;
    }

    private static int getThermalOverrideColor(LivingEntity entity) {
        if (entity instanceof TacticalDroneEntity) {
            return 0x00E5FF; // Tactical cyan signature
        }
        return 0xFF5500; // High-priority hostile signature
    }

    public static void onDroneLinkPacket(int droneEntityId, boolean linked, float empFlashIntensity) {
        if (empFlashIntensity > 0.0f) {
            try {
                VisionGogglesClientAPI.triggerNvgGlare(empFlashIntensity);
            } catch (Throwable ignored) {
            }
        }
        if (droneEntityId == ModNetworking.EMP_FLASH_ONLY_ID) {
            return;
        }

        if (linked) {
            activateFpv(droneEntityId);
        } else {
            disconnectFpv(false);
        }
    }

    public static void onLatticeTracksPacket(LatticeTelemetrySnapshot snapshot) {
        latestTelemetry = snapshot != null ? snapshot : LatticeTelemetrySnapshot.EMPTY;
        TRACK_MAP.clear();
        for (LatticeTrackEntry entry : latestTelemetry.tracks()) {
            TRACK_MAP.put(entry.entityId(), entry);
        }
    }

    public static void activateFpv(int droneEntityId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        Entity entity = mc.level.getEntity(droneEntityId);
        if (!(entity instanceof TacticalDroneEntity drone) || !drone.isAlive()) {
            return;
        }

        if (!linkedInFpv) {
            previousCameraType = mc.options.getCameraType();
        }
        activeDroneId = droneEntityId;
        linkedInFpv = true;
        fpvYaw = drone.getYRot();
        fpvPitch = Mth.clamp(drone.getXRot(), -85.0f, 85.0f);

        mc.options.setCameraType(CameraType.FIRST_PERSON);
        mc.setCameraEntity(drone);
        setVisionGogglesThermalOverride(true);
    }

    public static void disconnectFpv(boolean notifyServer) {
        int prevId = activeDroneId;
        linkedInFpv = false;
        activeDroneId = -1;

        setVisionGogglesThermalOverride(false);

        if (notifyServer && prevId >= 0) {
            ModNetworking.sendDroneInputC2S(
                    prevId, 0.0f, 0.0f, 0.0f, fpvYaw, fpvPitch, ModNetworking.FLAG_DISCONNECT_FPV
            );
        }

        Minecraft mc = Minecraft.getInstance();
        allowingCameraRestore = true;
        try {
            if (mc.player != null && mc.getCameraEntity() != mc.player) {
                mc.setCameraEntity(mc.player);
            }
            if (previousCameraType != null) {
                mc.options.setCameraType(previousCameraType);
                previousCameraType = null;
            }
        } finally {
            allowingCameraRestore = false;
        }
    }

    private static void setVisionGogglesThermalOverride(boolean enable) {
        try {
            java.lang.reflect.Method m = VisionGogglesClientAPI.class.getMethod(
                    "setExternalVisionOverride",
                    dev.itsrealperson.vision_goggles.util.VisionMode.class
            );
            m.invoke(null, enable ? dev.itsrealperson.vision_goggles.util.VisionMode.THERMAL : null);
        } catch (Throwable ignored) {
        }
    }

    public static void handleMouseTurn(double yaw, double pitch) {
        if (!isFpvActive()) {
            return;
        }
        fpvYaw += (float) yaw * 0.15f;
        fpvPitch = Mth.clamp(fpvPitch + (float) pitch * 0.15f, -85.0f, 85.0f);

        Minecraft mc = Minecraft.getInstance();
        TacticalDroneEntity drone = getActiveDrone(mc);
        if (drone != null) {
            drone.setYRot(fpvYaw);
            drone.setXRot(fpvPitch);
            drone.yRotO = fpvYaw;
            drone.xRotO = fpvPitch;
            drone.setYHeadRot(fpvYaw);
            drone.yHeadRotO = fpvYaw;
            drone.yBodyRot = fpvYaw;
            drone.yBodyRotO = fpvYaw;
        }
    }

    public static float getFpvYaw() {
        return fpvYaw;
    }

    public static float getFpvPitch() {
        return fpvPitch;
    }

    private static void onClientTick(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            if (linkedInFpv) {
                disconnectFpv(false);
            }
            return;
        }

        boolean toggleFpvPressed = VisionTacticalEagleEyeClient.KEY_FPV_TOGGLE.consumeClick();
        boolean toggleHoverPressed = VisionTacticalEagleEyeClient.KEY_DRONE_HOVER.consumeClick();
        boolean triggerPayloadPressed = VisionTacticalEagleEyeClient.KEY_DRONE_PAYLOAD.consumeClick();

        if (toggleFpvPressed && isFpvActive()) {
            disconnectFpv(true);
            return;
        }

        if (!isFpvActive()) {
            int targetId = latestTelemetry.primaryDroneId();
            if (targetId >= 0) {
                int flags = 0;
                if (toggleHoverPressed) {
                    flags |= ModNetworking.FLAG_TOGGLE_HOVER;
                }
                if (triggerPayloadPressed) {
                    flags |= ModNetworking.FLAG_TRIGGER_PAYLOAD;
                }
                if (flags != 0) {
                    ModNetworking.sendDroneInputC2S(targetId, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, flags);
                }
            }
            return;
        }

        TacticalDroneEntity drone = getActiveDrone(mc);
        if (drone == null
                || !drone.isAlive()
                || drone.getBattery() <= 0.0f
                || drone.getSignalQuality() < 0.02f
                || drone.distanceTo(mc.player) > TacticalDroneEntity.MAX_RANGE) {
            disconnectFpv(true);
            return;
        }

        if (mc.getCameraEntity() != drone) {
            mc.setCameraEntity(drone);
        }

        float forward = 0.0f;
        float strafe = 0.0f;
        float vertical = 0.0f;

        if (mc.screen == null) {
            if (mc.options.keyUp.isDown()) forward += 1.0f;
            if (mc.options.keyDown.isDown()) forward -= 1.0f;
            if (mc.options.keyLeft.isDown()) strafe += 1.0f;
            if (mc.options.keyRight.isDown()) strafe -= 1.0f;
            if (mc.options.keyJump.isDown()) vertical += 1.0f;
            if (mc.options.keySprint.isDown() || mc.options.keyShift.isDown()) vertical -= 1.0f;
        }

        int flags = 0;
        if (toggleHoverPressed) {
            flags |= ModNetworking.FLAG_TOGGLE_HOVER;
        }
        if (triggerPayloadPressed) {
            flags |= ModNetworking.FLAG_TRIGGER_PAYLOAD;
        }

        ModNetworking.sendDroneInputC2S(
                drone.getId(),
                forward,
                strafe,
                vertical,
                fpvYaw,
                fpvPitch,
                flags
        );
    }

    @Nullable
    public static TacticalDroneEntity getActiveDrone(Minecraft mc) {
        if (mc == null || mc.level == null || activeDroneId < 0) {
            return null;
        }
        Entity entity = mc.level.getEntity(activeDroneId);
        return entity instanceof TacticalDroneEntity drone ? drone : null;
    }

    @Nullable
    public static TacticalDroneEntity findDroneByUuid(@Nullable java.util.UUID uuid) {
        if (uuid == null) {
            return null;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return null;
        }
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof TacticalDroneEntity drone && uuid.equals(drone.getUUID())) {
                return drone;
            }
        }
        return null;
    }

    public static boolean isFpvActive() {
        return linkedInFpv && activeDroneId >= 0;
    }

    public static boolean isAllowingCameraRestore() {
        return allowingCameraRestore;
    }

    public static int getActiveDroneId() {
        return activeDroneId;
    }

    public static LatticeTelemetrySnapshot getLatestTelemetry() {
        return latestTelemetry;
    }

    public static List<LatticeTrackEntry> getTracks() {
        return latestTelemetry != null ? latestTelemetry.tracks() : Collections.emptyList();
    }

    public static int getActiveDroneCount() {
        return latestTelemetry != null ? latestTelemetry.activeDroneCount() : 0;
    }

    public static float getPrimaryBatteryPct() {
        Minecraft mc = Minecraft.getInstance();
        TacticalDroneEntity drone = getActiveDrone(mc);
        if (drone != null) {
            return drone.getBatteryPct();
        }
        return latestTelemetry != null ? latestTelemetry.primaryBatteryPct() : 0.0f;
    }

    public static float getPrimarySignal() {
        Minecraft mc = Minecraft.getInstance();
        TacticalDroneEntity drone = getActiveDrone(mc);
        if (drone != null) {
            return drone.getSignalQuality();
        }
        return latestTelemetry != null ? latestTelemetry.primarySignal() : 0.0f;
    }

    public static boolean isPrimaryAutoHover() {
        Minecraft mc = Minecraft.getInstance();
        TacticalDroneEntity drone = getActiveDrone(mc);
        if (drone != null) {
            return drone.isAutoHover();
        }
        return latestTelemetry != null && latestTelemetry.primaryAutoHover();
    }

    public static int getPrimaryPayload() {
        Minecraft mc = Minecraft.getInstance();
        TacticalDroneEntity drone = getActiveDrone(mc);
        if (drone != null) {
            return drone.getPayloadType();
        }
        return latestTelemetry != null ? latestTelemetry.primaryPayload() : 0;
    }
}
