package dev.itsrealperson.vision_tactical_eagleeye.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.itsrealperson.vision_tactical_eagleeye.client.DroneEoShaderManager;
import dev.itsrealperson.vision_tactical_eagleeye.client.DroneFpvClientController;
import dev.itsrealperson.vision_tactical_eagleeye.entity.TacticalDroneEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hides the player's first-person hand and held item (including TaCZ weapons) while piloting a drone in FPV,
 * and applies the dedicated military EO/UAV GLSL PostChain right after world rendering.
 */
@Mixin(GameRenderer.class)
public abstract class FpvGameRendererMixin {

    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    private void vision_tactical_eagleeye$hideHandInFpv(PoseStack poseStack, Camera camera, float partialTicks, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (DroneFpvClientController.isFpvActive() || mc.getCameraEntity() instanceof TacticalDroneEntity) {
            ci.cancel();
        }
    }

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/GameRenderer;renderLevel(FJLcom/mojang/blaze3d/vertex/PoseStack;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void vision_tactical_eagleeye$applyDroneEoPostChain(float partialTicks, long nanoTime, boolean renderLevel, CallbackInfo ci) {
        DroneEoShaderManager.processIfActive(partialTicks);
    }
}
