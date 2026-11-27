package dev.itsrealperson.vision_tactical_eagleeye.client.renderer;

import dev.itsrealperson.vision_tactical_eagleeye.VisionTacticalEagleEye;
import dev.itsrealperson.vision_tactical_eagleeye.entity.TacticalDroneEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

/**
 * GeckoLib 4 model for {@link TacticalDroneEntity} with procedural rotor spin
 * and inertial pitch/roll chassis tilt.
 */
@Environment(EnvType.CLIENT)
public class TacticalDroneModel extends GeoModel<TacticalDroneEntity> {
    private static final ResourceLocation MODEL =
            new ResourceLocation(VisionTacticalEagleEye.MOD_ID, "geo/tactical_drone.geo.json");
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(VisionTacticalEagleEye.MOD_ID, "textures/entity/tactical_drone.png");
    private static final ResourceLocation ANIMATION =
            new ResourceLocation(VisionTacticalEagleEye.MOD_ID, "animations/tactical_drone.animation.json");

    @Override
    public ResourceLocation getModelResource(TacticalDroneEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(TacticalDroneEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(TacticalDroneEntity animatable) {
        return ANIMATION;
    }

    @Override
    public void setCustomAnimations(TacticalDroneEntity animatable, long instanceId, AnimationState<TacticalDroneEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        CoreGeoBone body = this.getAnimationProcessor().getBone("body");
        if (body != null) {
            body.setRotX(-animatable.clientTiltPitch * Mth.DEG_TO_RAD);
            body.setRotZ(animatable.clientTiltRoll * Mth.DEG_TO_RAD);
        }

        float rotorRad = animatable.rotorAngle * Mth.DEG_TO_RAD;
        setRotorYaw("rotor_fl", rotorRad);
        setRotorYaw("rotor_fr", -rotorRad);
        setRotorYaw("rotor_bl", -rotorRad);
        setRotorYaw("rotor_br", rotorRad);
    }

    private void setRotorYaw(String boneName, float radians) {
        CoreGeoBone bone = this.getAnimationProcessor().getBone(boneName);
        if (bone != null) {
            bone.setRotY(radians);
        }
    }
}
