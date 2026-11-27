package dev.itsrealperson.vision_tactical_eagleeye.mixin.client;

import dev.itsrealperson.vision_goggles.client.ModKeyMappings;
import dev.itsrealperson.vision_goggles.client.VisionRenderer;
import dev.itsrealperson.vision_tactical_eagleeye.client.DroneFpvClientController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Suppresses Vision Goggles' HUD, NVG lightmap, and keybinds (N toggle, radial menu, zoom, mode switch)
 * while the player is piloting a drone in FPV.
 */
@Mixin(value = VisionRenderer.class, remap = false)
public abstract class FpvVisionRendererSuppressMixin {

    @Inject(method = "renderHUD", at = @At("HEAD"), cancellable = true, remap = false)
    private static void vision_tactical_eagleeye$suppressVisionRendererHudInFpv(GuiGraphics guiGraphics, float partialTicks, CallbackInfo ci) {
        if (DroneFpvClientController.isFpvActive()) {
            ci.cancel();
        }
    }

    @Inject(method = "shouldApplyNightVisionLight", at = @At("HEAD"), cancellable = true, remap = false)
    private static void vision_tactical_eagleeye$enableClearEoNightLightInFpv(CallbackInfoReturnable<Boolean> cir) {
        if (DroneFpvClientController.isFpvActive()) {
            // Enable clean fullbright night lightmap for the drone's EO/UAV camera (green NVG shader is suppressed separately)
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "onClientTick", at = @At("HEAD"), cancellable = true, remap = false)
    private static void vision_tactical_eagleeye$blockGogglesKeysInFpv(Minecraft mc, CallbackInfo ci) {
        if (!DroneFpvClientController.isFpvActive()) {
            return;
        }
        try {
            if (ModKeyMappings.toggleGrayscaleKey != null) {
                while (ModKeyMappings.toggleGrayscaleKey.consumeClick()) {
                }
            }
            if (ModKeyMappings.zoomKey != null) {
                while (ModKeyMappings.zoomKey.consumeClick()) {
                }
            }
            if (ModKeyMappings.toggleFlashlightKey != null) {
                while (ModKeyMappings.toggleFlashlightKey.consumeClick()) {
                }
            }
            if (ModKeyMappings.cycleFlashlightModeKey != null) {
                while (ModKeyMappings.cycleFlashlightModeKey.consumeClick()) {
                }
            }
            if (ModKeyMappings.markTargetKey != null) {
                while (ModKeyMappings.markTargetKey.consumeClick()) {
                }
            }
        } catch (Throwable ignored) {
        }
        ci.cancel();
    }
}
