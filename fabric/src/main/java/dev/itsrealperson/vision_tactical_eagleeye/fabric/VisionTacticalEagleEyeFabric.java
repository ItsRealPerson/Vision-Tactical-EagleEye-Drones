package dev.itsrealperson.vision_tactical_eagleeye.fabric;

import dev.itsrealperson.vision_tactical_eagleeye.VisionTacticalEagleEye;
import net.fabricmc.api.ModInitializer;

public final class VisionTacticalEagleEyeFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        VisionTacticalEagleEye.init();
    }
}
