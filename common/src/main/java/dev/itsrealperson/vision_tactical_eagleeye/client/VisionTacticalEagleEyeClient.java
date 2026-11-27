package dev.itsrealperson.vision_tactical_eagleeye.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import dev.itsrealperson.vision_tactical_eagleeye.client.renderer.TacticalDroneRenderer;
import dev.itsrealperson.vision_tactical_eagleeye.network.ClientPacketHandlerBridge;
import dev.itsrealperson.vision_tactical_eagleeye.registry.ModEntities;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.KeyMapping;

/**
 * Common client entrypoint for Vision Tactical: EagleEye & Drones.
 * Registers keybindings, GeckoLib entity renderers, FPV controller, AR HUD overlay,
 * Lua global bridge, and networking callbacks.
 */
@Environment(EnvType.CLIENT)
public final class VisionTacticalEagleEyeClient {
    public static final String KEY_CATEGORY = "key.categories.vision_tactical_eagleeye";

    public static final KeyMapping KEY_FPV_TOGGLE = new KeyMapping(
            "key.vision_tactical_eagleeye.fpv_toggle",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_V,
            KEY_CATEGORY
    );

    public static final KeyMapping KEY_DRONE_HOVER = new KeyMapping(
            "key.vision_tactical_eagleeye.drone_hover",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_H,
            KEY_CATEGORY
    );

    public static final KeyMapping KEY_DRONE_PAYLOAD = new KeyMapping(
            "key.vision_tactical_eagleeye.drone_payload",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_G,
            KEY_CATEGORY
    );

    private static boolean initialized = false;

    private VisionTacticalEagleEyeClient() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        KeyMappingRegistry.register(KEY_FPV_TOGGLE);
        KeyMappingRegistry.register(KEY_DRONE_HOVER);
        KeyMappingRegistry.register(KEY_DRONE_PAYLOAD);

        EntityRendererRegistry.register(ModEntities.TACTICAL_DRONE, TacticalDroneRenderer::new);

        ClientPacketHandlerBridge.registerHandlers(
                DroneFpvClientController::onDroneLinkPacket,
                DroneFpvClientController::onLatticeTracksPacket
        );

        DroneFpvClientController.init();
        EagleEyeHudOverlay.init();
        EagleEyeLuaBridge.init();
    }
}
