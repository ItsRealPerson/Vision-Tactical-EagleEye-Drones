package dev.itsrealperson.vision_tactical_eagleeye.mixin.client;

import dev.itsrealperson.vision_tactical_eagleeye.client.DroneFpvClientController;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Prevents mouse wheel scrolling from changing the active hotbar slot while piloting a drone in FPV.
 */
@Mixin(Inventory.class)
public abstract class FpvInventoryMixin {

    @Inject(method = "swapPaint", at = @At("HEAD"), cancellable = true)
    private void vision_tactical_eagleeye$lockHotbarScrollInFpv(double direction, CallbackInfo ci) {
        if (DroneFpvClientController.isFpvActive()) {
            ci.cancel();
        }
    }
}
