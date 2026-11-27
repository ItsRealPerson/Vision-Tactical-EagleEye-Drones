package dev.itsrealperson.vision_tactical_eagleeye.mixin.client;

import dev.itsrealperson.vision_tactical_eagleeye.client.DroneFpvClientController;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Prevents the local player's physical body from walking, strafing, jumping, or sneaking
 * while piloting a Tactical Drone in FPV mode.
 */
@Mixin(KeyboardInput.class)
public abstract class KeyboardInputFpvMixin extends Input {

    @Inject(method = "tick", at = @At("TAIL"))
    private void vision_tactical_eagleeye$suppressPlayerMovementInFpv(boolean isMovingSlowly, float sneakingSpeedMultiplier, CallbackInfo ci) {
        if (DroneFpvClientController.isFpvActive()) {
            this.forwardImpulse = 0.0f;
            this.leftImpulse = 0.0f;
            this.up = false;
            this.down = false;
            this.left = false;
            this.right = false;
            this.jumping = false;
            this.shiftKeyDown = false;
        }
    }
}
