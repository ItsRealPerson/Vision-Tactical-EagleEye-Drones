package dev.itsrealperson.vision_tactical_eagleeye.mixin.client;

import dev.itsrealperson.vision_tactical_eagleeye.client.DroneFpvClientController;
import dev.itsrealperson.vision_tactical_eagleeye.entity.TacticalDroneEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hides vanilla hotbar, health/armor/hunger, XP bar, and selected item name while in drone FPV mode.
 */
@Mixin(Gui.class)
public abstract class FpvGuiMixin {

    @Unique
    private static boolean vision_tactical_eagleeye$isInFpv() {
        Minecraft mc = Minecraft.getInstance();
        return DroneFpvClientController.isFpvActive() || mc.getCameraEntity() instanceof TacticalDroneEntity;
    }

    @Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true)
    private void vision_tactical_eagleeye$hideHotbar(float partialTick, GuiGraphics guiGraphics, CallbackInfo ci) {
        if (vision_tactical_eagleeye$isInFpv()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderPlayerHealth", at = @At("HEAD"), cancellable = true)
    private void vision_tactical_eagleeye$hidePlayerHealth(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (vision_tactical_eagleeye$isInFpv()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderExperienceBar", at = @At("HEAD"), cancellable = true)
    private void vision_tactical_eagleeye$hideExperienceBar(GuiGraphics guiGraphics, int x, CallbackInfo ci) {
        if (vision_tactical_eagleeye$isInFpv()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderSelectedItemName", at = @At("HEAD"), cancellable = true)
    private void vision_tactical_eagleeye$hideSelectedItemName(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (vision_tactical_eagleeye$isInFpv()) {
            ci.cancel();
        }
    }
}
