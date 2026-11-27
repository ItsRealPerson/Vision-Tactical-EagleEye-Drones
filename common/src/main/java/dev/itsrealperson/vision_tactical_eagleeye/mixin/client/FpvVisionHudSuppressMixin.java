package dev.itsrealperson.vision_tactical_eagleeye.mixin.client;

import dev.itsrealperson.vision_goggles.client.VisionHUDOverlay;
import dev.itsrealperson.vision_tactical_eagleeye.client.DroneFpvClientController;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Suppresses Vision Goggles' HUD overlay (battery, biometric, Lua HUD) while piloting a drone in FPV.
 */
@Mixin(value = VisionHUDOverlay.class, remap = false)
public abstract class FpvVisionHudSuppressMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true, remap = false)
    private static void vision_tactical_eagleeye$suppressGogglesHudInFpv(GuiGraphics gui, float partialTicks, CallbackInfo ci) {
        if (DroneFpvClientController.isFpvActive()) {
            ci.cancel();
        }
    }
}
