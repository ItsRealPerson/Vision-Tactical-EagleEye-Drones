package dev.itsrealperson.vision_tactical_eagleeye.mixin.client;

import dev.itsrealperson.vision_tactical_eagleeye.client.DroneFpvClientController;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Keeps the camera locked to the linked FPV drone in first-person while FPV mode is active
 * and blocks player inventory, hotbar switching, item dropping, and weapon/item use.
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

    @Inject(method = "handleKeybinds", at = @At("HEAD"), cancellable = true)
    private void vision_tactical_eagleeye$blockGameplayKeybindsInFpv(CallbackInfo ci) {
        if (!DroneFpvClientController.isFpvActive()) {
            return;
        }
        // Drain blocked gameplay keybinds so inputs do not queue up while flying the drone
        for (KeyMapping hotbarKey : this.options.keyHotbarSlots) {
            while (hotbarKey.consumeClick()) {
            }
        }
        while (this.options.keyInventory.consumeClick()) {
        }
        while (this.options.keyDrop.consumeClick()) {
        }
        while (this.options.keySwapOffhand.consumeClick()) {
        }
        while (this.options.keyAttack.consumeClick()) {
        }
        while (this.options.keyUse.consumeClick()) {
        }
        while (this.options.keyPickItem.consumeClick()) {
        }
        ci.cancel();
    }

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void vision_tactical_eagleeye$blockAttackInFpv(CallbackInfoReturnable<Boolean> cir) {
        if (DroneFpvClientController.isFpvActive()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void vision_tactical_eagleeye$blockContinueAttackInFpv(boolean leftClick, CallbackInfo ci) {
        if (DroneFpvClientController.isFpvActive()) {
            ci.cancel();
        }
    }

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void vision_tactical_eagleeye$blockUseItemInFpv(CallbackInfo ci) {
        if (DroneFpvClientController.isFpvActive()) {
            ci.cancel();
        }
    }
}
