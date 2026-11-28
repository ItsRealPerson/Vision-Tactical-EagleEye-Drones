package dev.itsrealperson.vision_tactical_eagleeye.item;

import dev.itsrealperson.vision_goggles.api.VisionGogglesAPI;
import dev.itsrealperson.vision_tactical_eagleeye.entity.TacticalDroneEntity;
import dev.itsrealperson.vision_tactical_eagleeye.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Vision Goggles modular chip enabling the Anduril EagleEye & Lattice OS AR Data-Link HUD.
 */
public class EagleEyeModuleItem extends Item {
    public static final String MODULE_ID = "vision_tactical_eagleeye:eagleeye_datalink";

    public EagleEyeModuleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            ItemStack goggles = VisionGogglesAPI.getEquippedGoggles(player);
            if (!goggles.isEmpty() && VisionGogglesAPI.installModule(goggles, MODULE_ID)) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        ModSounds.LATTICE_LINK.get(), SoundSource.PLAYERS, 0.85f, 1.25f);
                player.displayClientMessage(Component.translatable("message.vision_tactical_eagleeye.module_installed")
                        .withStyle(ChatFormatting.GREEN), true);
                return InteractionResultHolder.success(stack);
            }
        }
        return InteractionResultHolder.pass(stack);
    }

    /**
     * Returns true ONLY if the player has Vision Goggles equipped &amp; switched ON,
     * has the {@code eagleeye_datalink_module} installed in those goggles, AND has
     * explicitly linked a live owned drone (battery &gt; 0, within range) on their controller.
     */
    public static boolean hasEagleEyeActive(Player player) {
        if (player == null) {
            return false;
        }
        if (!VisionGogglesAPI.isVisionActive(player)) {
            return false;
        }
        if (!VisionGogglesAPI.hasModule(player, MODULE_ID)) {
            return false;
        }
        java.util.UUID linkedUuid = LatticeControllerItem.getPlayerLinkedDroneUUID(player);
        if (linkedUuid == null) {
            return false;
        }
        TacticalDroneEntity drone = LatticeControllerItem.resolveLinkedDrone(player.level(), linkedUuid, player);
        return drone != null
                && drone.getBattery() > 0.0f
                && drone.distanceTo(player) <= TacticalDroneEntity.MAX_RANGE;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.vision_tactical_eagleeye.eagleeye_datalink_module.desc")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.vision_tactical_eagleeye.eagleeye_datalink_module.desc2")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
