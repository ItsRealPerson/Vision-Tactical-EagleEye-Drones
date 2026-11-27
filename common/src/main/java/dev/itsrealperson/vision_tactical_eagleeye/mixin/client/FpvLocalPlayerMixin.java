package dev.itsrealperson.vision_tactical_eagleeye.mixin.client;

import dev.itsrealperson.vision_tactical_eagleeye.client.DroneFpvClientController;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps the operator's physical player body completely still (no walking, sprinting, sneaking,
 * jumping, or head/body twisting) while piloting a drone in FPV.
 */
@Mixin(LocalPlayer.class)
public abstract class FpvLocalPlayerMixin {

    @Shadow
    public Input input;

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void vision_tactical_eagleeye$freezeOperatorInFpv(CallbackInfo ci) {
        if (!DroneFpvClientController.isFpvActive()) {
            return;
        }
        LocalPlayer self = (LocalPlayer) (Object) this;
        self.setSprinting(false);
        self.setShiftKeyDown(false);
        if (this.input != null) {
            this.input.forwardImpulse = 0.0f;
            this.input.leftImpulse = 0.0f;
            this.input.up = false;
            this.input.down = false;
            this.input.left = false;
            this.input.right = false;
            this.input.jumping = false;
            this.input.shiftKeyDown = false;
        }
        Vec3 vel = self.getDeltaMovement();
        self.setDeltaMovement(0.0, vel.y, 0.0);
        self.yHeadRot = self.yBodyRot;
    }
}
