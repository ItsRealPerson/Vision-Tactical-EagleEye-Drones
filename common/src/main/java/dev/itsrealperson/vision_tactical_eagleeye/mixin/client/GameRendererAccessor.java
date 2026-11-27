package dev.itsrealperson.vision_tactical_eagleeye.mixin.client;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GameRenderer.class)
public interface GameRendererAccessor {

    @Invoker("getFov")
    double vision_tactical_eagleeye$getFov(Camera camera, float partialTicks, boolean useFOVSetting);
}
