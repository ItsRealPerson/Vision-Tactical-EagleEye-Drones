package dev.itsrealperson.vision_tactical_eagleeye.mixin.client;

import dev.itsrealperson.vision_tactical_eagleeye.client.DroneFpvClientController;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Redirects mouse look input from the local player to the linked FPV drone camera
 * so drone rotation is frame-accurate without spinning the operator's body on the ground.
 */
@Mixin(Entity.class)
public abstract class LocalPlayerTurnMixin {

    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void vision_tactical_eagleeye$redirectMouseTurnToDrone(double yaw, double pitch, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && (Object) this == mc.player && DroneFpvClientController.isFpvActive()) {
            DroneFpvClientController.handleMouseTurn(yaw, pitch);
            ci.cancel();
        }
    }
}
