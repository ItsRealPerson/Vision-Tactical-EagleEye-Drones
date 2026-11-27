package dev.itsrealperson.vision_tactical_eagleeye.mixin.client;

import dev.itsrealperson.vision_tactical_eagleeye.client.DroneFpvClientController;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps the camera locked to the linked FPV drone in first-person while FPV mode is active.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftCameraMixin {

    @Shadow
    @Final
    public Options options;

    @Inject(method = "setCameraEntity", at = @At("HEAD"), cancellable = true)
    private void vision_tactical_eagleeye$preventUnexpectedCameraReset(Entity entity, CallbackInfo ci) {
        Minecraft mc = (Minecraft) (Object) this;
        if (DroneFpvClientController.isFpvActive()
                && mc.player != null
                && entity == mc.player
                && !DroneFpvClientController.isAllowingCameraRestore()) {
            ci.cancel();
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void vision_tactical_eagleeye$enforceFirstPersonInFpv(CallbackInfo ci) {
        if (DroneFpvClientController.isFpvActive() && this.options.getCameraType() != CameraType.FIRST_PERSON) {
            this.options.setCameraType(CameraType.FIRST_PERSON);
        }
    }
}
