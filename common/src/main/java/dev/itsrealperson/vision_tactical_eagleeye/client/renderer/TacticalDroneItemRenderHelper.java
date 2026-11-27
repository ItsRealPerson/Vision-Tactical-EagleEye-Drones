package dev.itsrealperson.vision_tactical_eagleeye.client.renderer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import software.bernie.geckolib.animatable.client.RenderProvider;

import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public final class TacticalDroneItemRenderHelper {
    private TacticalDroneItemRenderHelper() {
    }

    public static void createRenderer(Consumer<Object> consumer) {
        consumer.accept(new RenderProvider() {
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
