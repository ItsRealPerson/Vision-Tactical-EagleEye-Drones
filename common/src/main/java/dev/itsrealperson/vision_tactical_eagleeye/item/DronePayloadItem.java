package dev.itsrealperson.vision_tactical_eagleeye.item;

import dev.itsrealperson.vision_tactical_eagleeye.entity.TacticalDroneEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Modular tactical payload canister (EMP or HE) attachable to a deployed {@link TacticalDroneEntity}.
 */
public class DronePayloadItem extends Item {
    private final int payloadType;

    public DronePayloadItem(int payloadType, Properties properties) {
        super(properties);
        this.payloadType = payloadType;
    }

    public int getPayloadType() {
        return this.payloadType;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        String key = this.payloadType == TacticalDroneEntity.PAYLOAD_EMP
                ? "item.vision_tactical_eagleeye.drone_payload_emp.desc"
                : "item.vision_tactical_eagleeye.drone_payload_he.desc";
        String key2 = this.payloadType == TacticalDroneEntity.PAYLOAD_EMP
                ? "item.vision_tactical_eagleeye.drone_payload_emp.desc2"
                : "item.vision_tactical_eagleeye.drone_payload_he.desc2";
        tooltip.add(Component.translatable(key).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(key2).withStyle(ChatFormatting.DARK_GRAY));
    }
}
