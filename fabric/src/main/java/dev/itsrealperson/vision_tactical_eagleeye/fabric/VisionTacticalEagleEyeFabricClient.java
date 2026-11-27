package dev.itsrealperson.vision_tactical_eagleeye.fabric;

import dev.itsrealperson.vision_tactical_eagleeye.client.VisionTacticalEagleEyeClient;
import net.fabricmc.api.ClientModInitializer;

public final class VisionTacticalEagleEyeFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        VisionTacticalEagleEyeClient.init();
    }
}
