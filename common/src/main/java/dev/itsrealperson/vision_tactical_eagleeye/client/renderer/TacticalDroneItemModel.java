package dev.itsrealperson.vision_tactical_eagleeye.client.renderer;

import dev.itsrealperson.vision_tactical_eagleeye.VisionTacticalEagleEye;
import dev.itsrealperson.vision_tactical_eagleeye.item.TacticalDroneItem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

@Environment(EnvType.CLIENT)
public class TacticalDroneItemModel extends GeoModel<TacticalDroneItem> {
    private static final ResourceLocation MODEL =
            new ResourceLocation(VisionTacticalEagleEye.MOD_ID, "geo/tactical_drone.geo.json");
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(VisionTacticalEagleEye.MOD_ID, "textures/entity/tactical_drone.png");
    private static final ResourceLocation ANIMATION =
            new ResourceLocation(VisionTacticalEagleEye.MOD_ID, "animations/tactical_drone.animation.json");

    @Override
    public ResourceLocation getModelResource(TacticalDroneItem animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(TacticalDroneItem animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(TacticalDroneItem animatable) {
        return ANIMATION;
    }
}
