package dev.itsrealperson.vision_tactical_eagleeye.item;

import dev.itsrealperson.vision_tactical_eagleeye.entity.TacticalDroneEntity;
import dev.itsrealperson.vision_tactical_eagleeye.registry.ModEntities;
import dev.itsrealperson.vision_tactical_eagleeye.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Deployable tactical FPV quadcopter item storing battery charge and payload type in NBT.
 * Implements {@link GeoItem} to render the full 3D {@code tactical_drone.geo.json} model in GUI, hand, and world.
 */
public class TacticalDroneItem extends Item implements GeoItem {
    public static final String TAG_BATTERY = "Battery";
    public static final String TAG_PAYLOAD = "PayloadType";

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Supplier<Object> renderProvider = new Supplier<Object>() {
        private Object geoRenderProvider;

        @Override
        public Object get() {
            if (this.geoRenderProvider == null) {
                createRenderer(provider -> this.geoRenderProvider = provider);
            }
            return this.geoRenderProvider;
        }
    };

    public TacticalDroneItem(Properties properties) {
        super(properties);
    }

    public static float getBattery(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag() || !stack.getTag().contains(TAG_BATTERY)) {
            return TacticalDroneEntity.MAX_BATTERY;
        }
        return Mth.clamp(stack.getTag().getFloat(TAG_BATTERY), 0.0f, TacticalDroneEntity.MAX_BATTERY);
    }

    public static void setBattery(ItemStack stack, float battery) {
        stack.getOrCreateTag().putFloat(TAG_BATTERY, Mth.clamp(battery, 0.0f, TacticalDroneEntity.MAX_BATTERY));
    }

    public static int getPayloadType(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag()) {
            return TacticalDroneEntity.PAYLOAD_RECON;
        }
        return Mth.clamp(stack.getTag().getInt(TAG_PAYLOAD), TacticalDroneEntity.PAYLOAD_RECON, TacticalDroneEntity.PAYLOAD_HE);
    }

    public static void setPayloadType(ItemStack stack, int payloadType) {
        stack.getOrCreateTag().putInt(TAG_PAYLOAD, Mth.clamp(payloadType, TacticalDroneEntity.PAYLOAD_RECON, TacticalDroneEntity.PAYLOAD_HE));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }

        BlockPos pos = context.getClickedPos();
        Direction face = context.getClickedFace();
        Vec3 spawnPos = Vec3.atBottomCenterOf(pos.relative(face)).add(0.0, 0.25, 0.0);
        deployDrone(level, player, context.getItemInHand(), spawnPos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Vec3 spawnPos = player.getEyePosition().add(player.getLookAngle().scale(1.8));
        deployDrone(level, player, stack, spawnPos);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private void deployDrone(Level level, Player player, ItemStack stack, Vec3 spawnPos) {
        if (level.isClientSide) {
            return;
        }
        TacticalDroneEntity drone = ModEntities.TACTICAL_DRONE.get().create(level);
        if (drone == null) {
            return;
        }
        drone.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, player.getYRot(), 0.0f);
        drone.setYHeadRot(player.getYRot());
        drone.yBodyRot = player.getYRot();
        drone.setOwnerUUID(player.getUUID());
        drone.setBattery(getBattery(stack));
        drone.setPayloadType(getPayloadType(stack));
        drone.setAutoHover(true);
        level.addFreshEntity(drone);

        level.playSound(null, spawnPos.x, spawnPos.y, spawnPos.z,
                ModSounds.DRONE_DEPLOY.get(), SoundSource.PLAYERS, 0.85f, 1.1f);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0f * (getBattery(stack) / TacticalDroneEntity.MAX_BATTERY));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        float ratio = getBattery(stack) / TacticalDroneEntity.MAX_BATTERY;
        if (ratio > 0.5f) {
            return 0x00E5FF;
        }
        if (ratio > 0.2f) {
            return 0xFFBB00;
        }
        return 0xFF3333;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        int pct = Math.round((getBattery(stack) / TacticalDroneEntity.MAX_BATTERY) * 100.0f);
        String payloadName = switch (getPayloadType(stack)) {
            case TacticalDroneEntity.PAYLOAD_EMP -> "EMP";
            case TacticalDroneEntity.PAYLOAD_HE -> "HE";
            default -> "RECON";
        };
        tooltip.add(Component.translatable("item.vision_tactical_eagleeye.tactical_drone.desc")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.vision_tactical_eagleeye.tactical_drone.desc2")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("item.vision_tactical_eagleeye.tactical_drone.battery", pct)
                .withStyle(pct > 25 ? ChatFormatting.AQUA : ChatFormatting.RED));
        tooltip.add(Component.translatable("item.vision_tactical_eagleeye.tactical_drone.payload", payloadName)
                .withStyle(ChatFormatting.GOLD));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public void createRenderer(Consumer<Object> consumer) {
        dev.itsrealperson.vision_tactical_eagleeye.client.renderer.TacticalDroneItemRenderHelper.createRenderer(consumer);
    }

    @Override
    public Supplier<Object> getRenderProvider() {
        return this.renderProvider;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
