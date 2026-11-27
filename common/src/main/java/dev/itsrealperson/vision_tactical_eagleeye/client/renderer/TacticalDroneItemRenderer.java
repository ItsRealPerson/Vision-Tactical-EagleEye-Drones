package dev.itsrealperson.vision_tactical_eagleeye.client.renderer;

import dev.itsrealperson.vision_tactical_eagleeye.item.TacticalDroneItem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import software.bernie.geckolib.renderer.GeoItemRenderer;

@Environment(EnvType.CLIENT)
public class TacticalDroneItemRenderer extends GeoItemRenderer<TacticalDroneItem> {
    public TacticalDroneItemRenderer() {
        super(new TacticalDroneItemModel());
    }
}
