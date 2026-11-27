package dev.itsrealperson.vision_tactical_eagleeye.client;

import dev.itsrealperson.vision_goggles.api.VisionGogglesAPI;
import dev.itsrealperson.vision_tactical_eagleeye.VisionTacticalEagleEye;
import dev.itsrealperson.vision_tactical_eagleeye.entity.TacticalDroneEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Exposes the {@code eagleeye} global Lua table to Vision Goggles scripts
 * using standard Java types via {@link VisionGogglesAPI#registerLuaGlobalTable(String, Map)}.
 */
@Environment(EnvType.CLIENT)
public final class EagleEyeLuaBridge {
    private static boolean initialized = false;

    private EagleEyeLuaBridge() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        Map<String, Object> eagleeye = new LinkedHashMap<>();
        eagleeye.put("isFpvActive", (Supplier<Object>) DroneFpvClientController::isFpvActive);
        eagleeye.put("getActiveDroneCount", (Supplier<Object>) DroneFpvClientController::getActiveDroneCount);
        eagleeye.put("getBatteryPercent", (Supplier<Object>) () ->
                Math.round(DroneFpvClientController.getPrimaryBatteryPct() * 10.0) / 10.0);
        eagleeye.put("getSignalQuality", (Supplier<Object>) () ->
                Math.round(DroneFpvClientController.getPrimarySignal() * 1000.0) / 1000.0);
        eagleeye.put("getTrackedTargetCount", (Supplier<Object>) () ->
                DroneFpvClientController.getTracks().size());
        eagleeye.put("isAutoHover", (Supplier<Object>) DroneFpvClientController::isPrimaryAutoHover);
        eagleeye.put("getPayloadName", (Supplier<Object>) () -> switch (DroneFpvClientController.getPrimaryPayload()) {
            case TacticalDroneEntity.PAYLOAD_EMP -> "EMP_CHARGE";
            case TacticalDroneEntity.PAYLOAD_HE -> "HE_WARHEAD";
            default -> "RECON_PULSE";
        });

        try {
            VisionGogglesAPI.registerLuaGlobalTable("eagleeye", eagleeye);
        } catch (Throwable t) {
            VisionTacticalEagleEye.LOGGER.warn("[EagleEye] Failed to register Lua global table 'eagleeye': {}", t.toString());
        }
    }
}
