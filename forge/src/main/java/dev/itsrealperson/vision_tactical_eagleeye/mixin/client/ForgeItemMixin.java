package dev.itsrealperson.vision_tactical_eagleeye.mixin.client;

import dev.itsrealperson.vision_tactical_eagleeye.client.renderer.TacticalDroneItemRenderer;
import dev.itsrealperson.vision_tactical_eagleeye.item.TacticalDroneItem;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.Item;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

/**
 * Registers {@link TacticalDroneItemRenderer} via Forge's {@link IClientItemExtensions}
 * so the 3D GeckoLib drone model renders in GUI slots, hands, and item frames on Forge.
 */
@Mixin(Item.class)
public abstract class ForgeItemMixin {

    @Inject(method = "initializeClient", at = @At("HEAD"), remap = false)
    private void vision_tactical_eagleeye$initializeClient(Consumer<IClientItemExtensions> consumer, CallbackInfo ci) {
        if ((Object) this instanceof TacticalDroneItem) {
            consumer.accept(new IClientItemExtensions() {
                private TacticalDroneItemRenderer renderer;

                @Override
                public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                    if (this.renderer == null) {
                        this.renderer = new TacticalDroneItemRenderer();
                    }
                    return this.renderer;
                }
            });
        }
    }
}
