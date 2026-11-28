package dev.itsrealperson.vision_tactical_eagleeye.item;

import dev.itsrealperson.vision_tactical_eagleeye.entity.TacticalDroneEntity;
import dev.itsrealperson.vision_tactical_eagleeye.network.ModNetworking;
import dev.itsrealperson.vision_tactical_eagleeye.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Handheld Anduril Lattice OS FPV controller for linking/cycling deployed drones
 * and toggling remote Auto-Hover. Uses persistent entity UUIDs so links survive world reloads.
 */
public class LatticeControllerItem extends Item {
    public static final String TAG_LINKED_UUID = "LinkedDroneUUID";
    public static final String TAG_LINKED_X = "LinkedX";
    public static final String TAG_LINKED_Y = "LinkedY";
    public static final String TAG_LINKED_Z = "LinkedZ";

    public LatticeControllerItem(Properties properties) {
        super(properties);
    }

    public static String formatShortId(@Nullable UUID uuid) {
        if (uuid == null) {
            return "----";
        }
        return uuid.toString().substring(0, 4).toUpperCase(Locale.ROOT);
    }

    public static boolean hasLinkedDrone(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.hasTag() && stack.getTag().hasUUID(TAG_LINKED_UUID);
    }

    @Nullable
    public static UUID getLinkedDroneUUID(ItemStack stack) {
        return hasLinkedDrone(stack) ? stack.getTag().getUUID(TAG_LINKED_UUID) : null;
    }

    @Nullable
    public static UUID getPlayerLinkedDroneUUID(@Nullable Player player) {
        if (player == null) {
            return null;
        }
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof LatticeControllerItem && hasLinkedDrone(main)) {
            return getLinkedDroneUUID(main);
        }
        ItemStack off = player.getOffhandItem();
        if (off.getItem() instanceof LatticeControllerItem && hasLinkedDrone(off)) {
            return getLinkedDroneUUID(off);
        }
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() instanceof LatticeControllerItem && hasLinkedDrone(s)) {
                return getLinkedDroneUUID(s);
            }
        }
        return null;
    }

    @Nullable
    public static TacticalDroneEntity resolveLinkedDrone(@Nullable Level level, @Nullable UUID uuid, @Nullable Player owner) {
        if (level == null || uuid == null) {
            return null;
        }
        Entity entity = null;
        if (level instanceof ServerLevel serverLevel) {
            entity = serverLevel.getEntity(uuid);
        } else if (level.isClientSide) {
            entity = dev.itsrealperson.vision_tactical_eagleeye.client.DroneFpvClientController.findDroneByUuid(uuid);
        }
        if (entity instanceof TacticalDroneEntity drone && drone.isAlive()
                && (owner == null || drone.isOwnedBy(owner))) {
            return drone;
        }
        return null;
    }

    public static void setLinkedDrone(ItemStack stack, TacticalDroneEntity drone) {
        if (stack == null || stack.isEmpty() || drone == null) {
            return;
        }
        CompoundTag tag = stack.getOrCreateTag();
        tag.putUUID(TAG_LINKED_UUID, drone.getUUID());
        tag.putInt(TAG_LINKED_X, (int) Math.floor(drone.getX()));
        tag.putInt(TAG_LINKED_Y, (int) Math.floor(drone.getY()));
        tag.putInt(TAG_LINKED_Z, (int) Math.floor(drone.getZ()));
    }

    public static void clearLinkedDrone(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTag()) {
            return;
        }
        CompoundTag tag = stack.getTag();
        tag.remove(TAG_LINKED_UUID);
        tag.remove("LinkedDroneId");
        tag.remove(TAG_LINKED_X);
        tag.remove(TAG_LINKED_Y);
        tag.remove(TAG_LINKED_Z);
    }

    public static void clearLinkedDroneInInventory(@Nullable Player player, @Nullable UUID droneUuid) {
        if (player == null || droneUuid == null) {
            return;
        }
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() instanceof LatticeControllerItem && droneUuid.equals(getLinkedDroneUUID(s))) {
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

        UUID linkedUuid = getLinkedDroneUUID(stack);
        if (linkedUuid == null) {
            serverPlayer.displayClientMessage(Component.translatable("message.vision_tactical_eagleeye.no_drone_linked")
                    .withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }

        TacticalDroneEntity target = resolveLinkedDrone(level, linkedUuid, player);
        if (target == null || target.distanceTo(player) > TacticalDroneEntity.MAX_RANGE) {
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
                    formatShortId(getLinkedDroneUUID(stack)),
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
