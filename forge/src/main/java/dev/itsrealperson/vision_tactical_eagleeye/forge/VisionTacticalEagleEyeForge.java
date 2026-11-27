package dev.itsrealperson.vision_tactical_eagleeye.forge;

import dev.architectury.platform.forge.EventBuses;
import dev.itsrealperson.vision_tactical_eagleeye.VisionTacticalEagleEye;
import dev.itsrealperson.vision_tactical_eagleeye.client.VisionTacticalEagleEyeClient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(VisionTacticalEagleEye.MOD_ID)
public final class VisionTacticalEagleEyeForge {
    public VisionTacticalEagleEyeForge() {
        EventBuses.registerModEventBus(VisionTacticalEagleEye.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
        VisionTacticalEagleEye.init();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> VisionTacticalEagleEyeClient::init);
    }
}
