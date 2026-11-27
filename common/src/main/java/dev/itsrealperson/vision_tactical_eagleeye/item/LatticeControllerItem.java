package dev.itsrealperson.vision_tactical_eagleeye.item;

import dev.itsrealperson.vision_tactical_eagleeye.entity.TacticalDroneEntity;
import dev.itsrealperson.vision_tactical_eagleeye.network.ModNetworking;
import dev.itsrealperson.vision_tactical_eagleeye.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

/**
 * Handheld Anduril Lattice OS FPV controller for linking/cycling deployed drones
 * and toggling remote Auto-Hover.
 */
public class LatticeControllerItem extends Item {
    public static final String TAG_LINKED_ID = "LinkedDroneId";
    public static final String TAG_LINKED_X = "LinkedX";
    public static final String TAG_LINKED_Y = "LinkedY";
    public static final String TAG_LINKED_Z = "LinkedZ";

    public LatticeControllerItem(Properties properties) {
        super(properties);
    }

    public static boolean hasLinkedDrone(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.hasTag() && stack.getTag().contains(TAG_LINKED_ID);
    }

    public static int getLinkedDroneId(ItemStack stack) {
        return hasLinkedDrone(stack) ? stack.getTag().getInt(TAG_LINKED_ID) : -1;
    }

    public static int getPlayerLinkedDroneId(@Nullable Player player) {
        if (player == null) {
            return -1;
        }
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof LatticeControllerItem && hasLinkedDrone(main)) {
            return getLinkedDroneId(main);
        }
        ItemStack off = player.getOffhandItem();
        if (off.getItem() instanceof LatticeControllerItem && hasLinkedDrone(off)) {
            return getLinkedDroneId(off);
        }
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() instanceof LatticeControllerItem && hasLinkedDrone(s)) {
                return getLinkedDroneId(s);
            }
        }
        return -1;
    }

    public static void setLinkedDrone(ItemStack stack, TacticalDroneEntity drone) {
        if (stack == null || stack.isEmpty() || drone == null) {
            return;
        }
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt(TAG_LINKED_ID, drone.getId());
        tag.putInt(TAG_LINKED_X, (int) Math.floor(drone.getX()));
        tag.putInt(TAG_LINKED_Y, (int) Math.floor(drone.getY()));
        tag.putInt(TAG_LINKED_Z, (int) Math.floor(drone.getZ()));
    }

    public static void clearLinkedDrone(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTag()) {
            return;
        }
        CompoundTag tag = stack.getTag();
        tag.remove(TAG_LINKED_ID);
        tag.remove(TAG_LINKED_X);
        tag.remove(TAG_LINKED_Y);
        tag.remove(TAG_LINKED_Z);
    }

    public static void linkControllerInInventory(Player player, TacticalDroneEntity drone) {
        if (player == null || drone == null) {
            return;
        }
        Inventory inv = player.getInventory();
        boolean linkedAny = false;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() instanceof LatticeControllerItem) {
                setLinkedDrone(s, drone);
                linkedAny = true;
            }
        }
        if (linkedAny) {
            player.displayClientMessage(
                    Component.translatable("message.vision_tactical_eagleeye.drone_linked", drone.getId())
                            .withStyle(ChatFormatting.GREEN),
                    true
            );
        }
    }

    public static void clearLinkedDroneInInventory(@Nullable Player player, int droneId) {
        if (player == null) {
            return;
        }
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() instanceof LatticeControllerItem && hasLinkedDrone(s)
                    && s.getTag().getInt(TAG_LINKED_ID) == droneId) {
                clearLinkedDrone(s);
            }
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return hasLinkedDrone(stack) || super.isFoil(stack);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        int linkedId = getLinkedDroneId(stack);
        if (linkedId < 0) {
            serverPlayer.displayClientMessage(Component.translatable("message.vision_tactical_eagleeye.no_drone_linked")
                    .withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }

        net.minecraft.world.entity.Entity entity = level.getEntity(linkedId);
        if (!(entity instanceof TacticalDroneEntity target)
                || !target.isAlive()
                || !target.isOwnedBy(player)
                || target.distanceTo(player) > TacticalDroneEntity.MAX_RANGE) {
            clearLinkedDrone(stack);
            serverPlayer.displayClientMessage(Component.translatable("message.vision_tactical_eagleeye.no_drone_in_range")
                    .withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }

        if (player.isShiftKeyDown()) {
            boolean nextState = !target.isAutoHover();
            target.setAutoHover(nextState);
            setLinkedDrone(stack, target);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.LATTICE_LINK.get(), SoundSource.PLAYERS, 0.7f, nextState ? 1.2f : 0.85f);
            serverPlayer.displayClientMessage(Component.translatable(nextState
                            ? "message.vision_tactical_eagleeye.hover_enabled"
                            : "message.vision_tactical_eagleeye.hover_disabled")
                    .withStyle(nextState ? ChatFormatting.AQUA : ChatFormatting.YELLOW), true);
            return InteractionResultHolder.success(stack);
        }

        if (target.getBattery() <= 0.0f) {
            serverPlayer.displayClientMessage(Component.translatable("message.vision_tactical_eagleeye.no_drone_in_range")
                    .withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }

        setLinkedDrone(stack, target);
        target.setFpvLinked(true);
        ModNetworking.sendFpvLinkPacket(serverPlayer, target.getId(), true, 0.0f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.LATTICE_LINK.get(), SoundSource.PLAYERS, 0.8f, 1.15f);
        serverPlayer.displayClientMessage(Component.translatable("message.vision_tactical_eagleeye.fpv_linked")
                .withStyle(ChatFormatting.AQUA), true);

        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (hasLinkedDrone(stack)) {
            CompoundTag tag = stack.getTag();
            tooltip.add(Component.translatable(
                    "item.vision_tactical_eagleeye.lattice_fpv_controller.linked",
                    tag.getInt(TAG_LINKED_ID),
                    tag.getInt(TAG_LINKED_X),
                    tag.getInt(TAG_LINKED_Y),
                    tag.getInt(TAG_LINKED_Z)
            ).withStyle(ChatFormatting.GREEN));
        } else {
            tooltip.add(Component.translatable("item.vision_tactical_eagleeye.lattice_fpv_controller.unlinked")
                    .withStyle(ChatFormatting.RED));
        }
        tooltip.add(Component.translatable("item.vision_tactical_eagleeye.lattice_fpv_controller.desc")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.vision_tactical_eagleeye.lattice_fpv_controller.desc2")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("item.vision_tactical_eagleeye.lattice_fpv_controller.controls")
                .withStyle(ChatFormatting.AQUA));
    }
}
