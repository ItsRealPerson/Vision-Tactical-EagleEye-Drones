package dev.itsrealperson.vision_tactical_eagleeye.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import dev.itsrealperson.vision_tactical_eagleeye.VisionTacticalEagleEye;
import dev.itsrealperson.vision_tactical_eagleeye.entity.TacticalDroneEntity;
import io.netty.buffer.Unpooled;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;

/**
 * Architectury packet channels for FPV quadcopter control and Lattice OS AR telemetry.
 */
public final class ModNetworking {
    public static final ResourceLocation C2S_DRONE_INPUT =
            new ResourceLocation(VisionTacticalEagleEye.MOD_ID, "drone_input");
    public static final ResourceLocation S2C_DRONE_LINK =
            new ResourceLocation(VisionTacticalEagleEye.MOD_ID, "drone_link");
    public static final ResourceLocation S2C_LATTICE_TRACKS =
            new ResourceLocation(VisionTacticalEagleEye.MOD_ID, "lattice_tracks");

    public static final int FLAG_TOGGLE_HOVER = 1;
    public static final int FLAG_TRIGGER_PAYLOAD = 2;
    public static final int FLAG_DISCONNECT_FPV = 4;

    /** Special droneEntityId sentinel meaning "EMP glare flash only, do not alter FPV link state". */
    public static final int EMP_FLASH_ONLY_ID = -2;

    private ModNetworking() {
    }

    public static void register() {
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, C2S_DRONE_INPUT, (buf, ctx) -> {
            int droneId = buf.readVarInt();
            float forward = buf.readFloat();
            float strafe = buf.readFloat();
            float vertical = buf.readFloat();
            float yaw = buf.readFloat();
            float pitch = buf.readFloat();
            int actionFlags = buf.readVarInt();

            ctx.queue(() -> {
                if (!(ctx.getPlayer() instanceof ServerPlayer player)) {
                    return;
                }
                Entity entity = player.level().getEntity(droneId);
                if (!(entity instanceof TacticalDroneEntity drone) || !drone.isAlive() || !drone.isOwnedBy(player)) {
                    if ((actionFlags & FLAG_DISCONNECT_FPV) != 0) {
                        sendFpvLinkPacket(player, -1, false, 0.0f);
                    }
                    return;
                }

                if ((actionFlags & FLAG_DISCONNECT_FPV) != 0) {
                    drone.setFpvLinked(false);
                    sendFpvLinkPacket(player, drone.getId(), false, 0.0f);
                    player.displayClientMessage(Component.translatable("message.vision_tactical_eagleeye.fpv_disconnected")
                            .withStyle(ChatFormatting.YELLOW), true);
                    return;
                }

                if ((actionFlags & FLAG_TOGGLE_HOVER) != 0) {
                    boolean nextHover = !drone.isAutoHover();
                    drone.setAutoHover(nextHover);
                    player.displayClientMessage(Component.translatable(nextHover
                                    ? "message.vision_tactical_eagleeye.hover_enabled"
                                    : "message.vision_tactical_eagleeye.hover_disabled")
                            .withStyle(nextHover ? ChatFormatting.AQUA : ChatFormatting.YELLOW), true);
                }

                if ((actionFlags & FLAG_TRIGGER_PAYLOAD) != 0) {
                    drone.triggerPayload(player);
                    if (!drone.isAlive()) {
                        return;
                    }
                }

                drone.applyFpvInput(forward, strafe, vertical, yaw, pitch);
            });
        });

        EnvExecutor.runInEnv(Env.CLIENT, () -> () -> {
            NetworkManager.registerReceiver(NetworkManager.Side.S2C, S2C_DRONE_LINK, (buf, ctx) -> {
                int droneId = buf.readVarInt();
                boolean linked = buf.readBoolean();
                float empFlash = buf.readFloat();
                ctx.queue(() -> ClientPacketHandlerBridge.dispatchDroneLink(droneId, linked, empFlash));
            });

            NetworkManager.registerReceiver(NetworkManager.Side.S2C, S2C_LATTICE_TRACKS, (buf, ctx) -> {
                int activeDroneCount = buf.readVarInt();
                int primaryDroneId = buf.readVarInt();
                float primaryBatteryPct = buf.readFloat();
                float primarySignal = buf.readFloat();
                boolean primaryAutoHover = buf.readBoolean();
                int primaryPayload = buf.readVarInt();

                int count = buf.readVarInt();
                List<LatticeTrackEntry> tracks = new ArrayList<>(Math.max(0, Math.min(count, 128)));
                for (int i = 0; i < count; i++) {
                    int entityId = buf.readVarInt();
                    double x = buf.readDouble();
                    double y = buf.readDouble();
                    double z = buf.readDouble();
                    float vx = buf.readFloat();
                    float vy = buf.readFloat();
                    float vz = buf.readFloat();
                    int threatLevel = buf.readVarInt();
                    int droneId = buf.readVarInt();
                    float healthPct = buf.readFloat();
                    String label = buf.readUtf(48);
                    tracks.add(new LatticeTrackEntry(entityId, x, y, z, vx, vy, vz, threatLevel, droneId, healthPct, label));
                }

                LatticeTelemetrySnapshot snapshot = new LatticeTelemetrySnapshot(
                        tracks, activeDroneCount, primaryDroneId, primaryBatteryPct,
                        primarySignal, primaryAutoHover, primaryPayload
                );
                ctx.queue(() -> ClientPacketHandlerBridge.dispatchLatticeTracks(snapshot));
            });
        });
    }

    public static void sendDroneInputC2S(int droneEntityId, float forward, float strafe, float vertical,
                                         float yaw, float pitch, int actionFlags) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeVarInt(droneEntityId);
        buf.writeFloat(forward);
        buf.writeFloat(strafe);
        buf.writeFloat(vertical);
        buf.writeFloat(yaw);
        buf.writeFloat(pitch);
        buf.writeVarInt(actionFlags);
        NetworkManager.sendToServer(C2S_DRONE_INPUT, buf);
    }

    public static void sendFpvLinkPacket(ServerPlayer player, int droneEntityId, boolean linked) {
        sendFpvLinkPacket(player, droneEntityId, linked, 0.0f);
    }

    public static void sendFpvLinkPacket(ServerPlayer player, int droneEntityId, boolean linked, float empFlashIntensity) {
        if (player == null) return;
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeVarInt(droneEntityId);
        buf.writeBoolean(linked);
        buf.writeFloat(empFlashIntensity);
        NetworkManager.sendToPlayer(player, S2C_DRONE_LINK, buf);
    }

    public static void sendEmpFlash(ServerPlayer player, float intensity) {
        sendFpvLinkPacket(player, EMP_FLASH_ONLY_ID, false, intensity);
    }

    public static void sendLatticeTracksPacket(ServerPlayer player, LatticeTelemetrySnapshot snapshot) {
        if (player == null || snapshot == null) return;
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeVarInt(snapshot.activeDroneCount());
        buf.writeVarInt(snapshot.primaryDroneId());
        buf.writeFloat(snapshot.primaryBatteryPct());
        buf.writeFloat(snapshot.primarySignal());
        buf.writeBoolean(snapshot.primaryAutoHover());
        buf.writeVarInt(snapshot.primaryPayload());

        List<LatticeTrackEntry> tracks = snapshot.tracks();
        int limit = Math.min(tracks.size(), 96);
        buf.writeVarInt(limit);
        for (int i = 0; i < limit; i++) {
            LatticeTrackEntry e = tracks.get(i);
            buf.writeVarInt(e.entityId());
            buf.writeDouble(e.x());
            buf.writeDouble(e.y());
            buf.writeDouble(e.z());
            buf.writeFloat(e.vx());
            buf.writeFloat(e.vy());
            buf.writeFloat(e.vz());
            buf.writeVarInt(e.threatLevel());
            buf.writeVarInt(e.droneId());
            buf.writeFloat(e.healthPct());
            buf.writeUtf(e.label() != null ? e.label() : "", 48);
        }
        NetworkManager.sendToPlayer(player, S2C_LATTICE_TRACKS, buf);
    }
}
