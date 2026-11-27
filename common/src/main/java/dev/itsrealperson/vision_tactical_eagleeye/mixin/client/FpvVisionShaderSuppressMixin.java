package dev.itsrealperson.vision_tactical_eagleeye.mixin.client;

import dev.itsrealperson.vision_goggles.client.VisionShaderManager;
import dev.itsrealperson.vision_tactical_eagleeye.client.DroneFpvClientController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Suppresses Vision Goggles' post-process shader (e.g. green NVG phosphor) while viewing
 * through the drone's own EO/UAV FPV camera feed.
 */
@Mixin(value = VisionShaderManager.class, remap = false)
public abstract class FpvVisionShaderSuppressMixin {

    @Inject(method = "processShader", at = @At("HEAD"), cancellable = true, remap = false)
    private static void vision_tactical_eagleeye$suppressGogglesShaderInFpv(float partialTick, CallbackInfo ci) {
        if (DroneFpvClientController.isFpvActive()) {
            ci.cancel();
        }
    }
}
