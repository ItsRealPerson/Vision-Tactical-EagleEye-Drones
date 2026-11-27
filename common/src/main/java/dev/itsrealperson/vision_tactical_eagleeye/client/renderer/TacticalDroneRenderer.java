package dev.itsrealperson.vision_tactical_eagleeye.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.itsrealperson.vision_tactical_eagleeye.client.DroneFpvClientController;
import dev.itsrealperson.vision_tactical_eagleeye.entity.TacticalDroneEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * GeckoLib 4 entity renderer for {@link TacticalDroneEntity}.
 * Suppresses rendering the drone body when the local camera is attached to it in first-person FPV.
 */
@Environment(EnvType.CLIENT)
public class TacticalDroneRenderer extends GeoEntityRenderer<TacticalDroneEntity> {

    public TacticalDroneRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new TacticalDroneModel());
        this.shadowRadius = 0.25f;
    }

    @Override
    public void render(TacticalDroneEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        Minecraft mc = Minecraft.getInstance();
        if (DroneFpvClientController.isFpvActive()
                && mc.getCameraEntity() == entity
                && mc.options.getCameraType().isFirstPerson()) {
            return;
        }
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }
}
