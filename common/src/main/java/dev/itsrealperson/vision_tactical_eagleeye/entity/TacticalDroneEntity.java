package dev.itsrealperson.vision_tactical_eagleeye.entity;

import dev.itsrealperson.vision_tactical_eagleeye.item.DronePayloadItem;
import dev.itsrealperson.vision_tactical_eagleeye.item.LatticeControllerItem;
import dev.itsrealperson.vision_tactical_eagleeye.item.TacticalDroneItem;
import dev.itsrealperson.vision_tactical_eagleeye.network.LatticeTrackManager;
import dev.itsrealperson.vision_tactical_eagleeye.network.ModNetworking;
import dev.itsrealperson.vision_tactical_eagleeye.registry.ModItems;
import dev.itsrealperson.vision_tactical_eagleeye.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Autonomous FPV tactical quadcopter drone integrated with Anduril Lattice OS.
 */
public class TacticalDroneEntity extends PathfinderMob implements GeoEntity {
    public static final float MAX_BATTERY = 1000.0f;
    public static final double MAX_RANGE = 128.0;
    public static final double SCAN_RANGE = 48.0;
    public static final double RECON_PULSE_RANGE = 64.0;
    public static final double EMP_RADIUS = 10.0;
    public static final float MAX_FLIGHT_SPEED = 0.55f;

    public static final int PAYLOAD_RECON = 0;
    public static final int PAYLOAD_EMP = 1;
    public static final int PAYLOAD_HE = 2;

    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID =
            SynchedEntityData.defineId(TacticalDroneEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Float> BATTERY =
            SynchedEntityData.defineId(TacticalDroneEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> AUTO_HOVER =
            SynchedEntityData.defineId(TacticalDroneEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> PAYLOAD_TYPE =
            SynchedEntityData.defineId(TacticalDroneEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SIGNAL_QUALITY =
            SynchedEntityData.defineId(TacticalDroneEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> FPV_LINKED =
            SynchedEntityData.defineId(TacticalDroneEntity.class, EntityDataSerializers.BOOLEAN);

    private static final RawAnimation ANIM_FLY = RawAnimation.begin().thenLoop("fly");
    private static final RawAnimation ANIM_HOVER = RawAnimation.begin().thenLoop("hover");
    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("idle");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // Server-side FPV input state
    public float inputForward;
    public float inputStrafe;
    public float inputVertical;
    public float targetYaw;
    public float targetPitch;
    private int fpvInputTimeout;

    // Client-side smoothed inertial animation state
    public float clientTiltPitch;
    public float clientTiltRoll;
    public float rotorAngle;

    public TacticalDroneEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.noCulling = true;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.4)
                .add(Attributes.FLYING_SPEED, 0.55)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWNER_UUID, Optional.empty());
        this.entityData.define(BATTERY, MAX_BATTERY);
        this.entityData.define(AUTO_HOVER, true);
        this.entityData.define(PAYLOAD_TYPE, PAYLOAD_RECON);
        this.entityData.define(SIGNAL_QUALITY, 1.0f);
        this.entityData.define(FPV_LINKED, false);
    }

    @Override
    protected void registerGoals() {
        // Controlled purely by Lattice OS Auto-Hover & FPV inertial flight physics.
    }

    @Nullable
    public UUID getOwnerUUID() {
        return this.entityData.get(OWNER_UUID).orElse(null);
    }

    public void setOwnerUUID(@Nullable UUID uuid) {
        this.entityData.set(OWNER_UUID, Optional.ofNullable(uuid));
    }

    @Nullable
    public Player getOwnerPlayer() {
        UUID id = getOwnerUUID();
        return id == null ? null : this.level().getPlayerByUUID(id);
    }

    public boolean isOwnedBy(Player player) {
        if (player == null) return false;
        UUID owner = getOwnerUUID();
        return owner == null || owner.equals(player.getUUID());
    }

    public float getBattery() {
        return this.entityData.get(BATTERY);
    }

    public void setBattery(float battery) {
        this.entityData.set(BATTERY, Mth.clamp(battery, 0.0f, MAX_BATTERY));
    }

    public float getBatteryPct() {
        return Mth.clamp(getBattery() / MAX_BATTERY, 0.0f, 1.0f);
    }

    public boolean isAutoHover() {
        return this.entityData.get(AUTO_HOVER);
    }

    public void setAutoHover(boolean autoHover) {
        this.entityData.set(AUTO_HOVER, autoHover);
    }

    public int getPayloadType() {
        return this.entityData.get(PAYLOAD_TYPE);
    }

    public void setPayloadType(int payloadType) {
        this.entityData.set(PAYLOAD_TYPE, Mth.clamp(payloadType, PAYLOAD_RECON, PAYLOAD_HE));
    }

    public float getSignalQuality() {
        return this.entityData.get(SIGNAL_QUALITY);
    }

    public void setSignalQuality(float signal) {
        this.entityData.set(SIGNAL_QUALITY, Mth.clamp(signal, 0.0f, 1.0f));
    }

    public boolean isFpvLinked() {
        return this.entityData.get(FPV_LINKED);
    }

    public void setFpvLinked(boolean linked) {
        this.entityData.set(FPV_LINKED, linked);
        if (!linked) {
            this.inputForward = 0.0f;
            this.inputStrafe = 0.0f;
            this.inputVertical = 0.0f;
            this.fpvInputTimeout = 0;
        }
    }

    /**
     * Applies operator FPV flight input received on the server.
     */
    public void applyFpvInput(float forward, float strafe, float vertical, float yaw, float pitch) {
        this.inputForward = Mth.clamp(forward, -1.0f, 1.0f);
        this.inputStrafe = Mth.clamp(strafe, -1.0f, 1.0f);
        this.inputVertical = Mth.clamp(vertical, -1.0f, 1.0f);
        this.targetYaw = yaw;
        this.targetPitch = Mth.clamp(pitch, -80.0f, 80.0f);
        this.fpvInputTimeout = 10;
        if (!isFpvLinked()) {
            setFpvLinked(true);
        }
    }

    @Override
    public void tick() {
        super.tick();
        this.setNoGravity(true);
        this.fallDistance = 0.0f;

        if (this.level().isClientSide) {
            updateClientVisuals();
            return;
        }

        if (this.fpvInputTimeout > 0) {
            this.fpvInputTimeout--;
            if (this.fpvInputTimeout == 0) {
                this.inputForward = 0.0f;
                this.inputStrafe = 0.0f;
                this.inputVertical = 0.0f;
            }
        }

        float battery = getBattery();
        if (battery > 0.0f) {
            boolean activelyPiloted = isFpvLinked() && this.fpvInputTimeout > 0;
            if (activelyPiloted) {
                this.setYRot(this.targetYaw);
                this.setXRot(this.targetPitch);
                this.yRotO = this.targetYaw;
                this.xRotO = this.targetPitch;
                this.setYHeadRot(this.targetYaw);
                this.yBodyRot = this.targetYaw;
            }

            Vec3 vel = this.getDeltaMovement();
            if (activelyPiloted) {
                double yawRad = Math.toRadians(this.getYRot());
                double fwdX = -Math.sin(yawRad);
                double fwdZ = Math.cos(yawRad);
                double leftX = Math.cos(yawRad);
                double leftZ = Math.sin(yawRad);

                double accelH = 0.095;
                double accelV = 0.085;
                double vx = vel.x * 0.84 + (fwdX * inputForward + leftX * inputStrafe) * accelH;
                double vz = vel.z * 0.84 + (fwdZ * inputForward + leftZ * inputStrafe) * accelH;
                double vy = vel.y * 0.80 + inputVertical * accelV;

                double hSpeed = Math.hypot(vx, vz);
                if (hSpeed > MAX_FLIGHT_SPEED) {
                    double scale = MAX_FLIGHT_SPEED / hSpeed;
                    vx *= scale;
                    vz *= scale;
                }
                vy = Mth.clamp(vy, -MAX_FLIGHT_SPEED * 0.85, MAX_FLIGHT_SPEED * 0.85);
                this.setDeltaMovement(vx, vy, vz);
            } else if (isAutoHover()) {
                double hoverOsc = Math.sin(this.tickCount * 0.12) * 0.008;
                this.setDeltaMovement(vel.x * 0.84, vel.y * 0.80 + hoverOsc, vel.z * 0.84);
            } else {
                this.setDeltaMovement(vel.x * 0.84, Math.max(-0.18, vel.y * 0.80 - 0.04), vel.z * 0.84);
            }

            boolean moving = activelyPiloted || this.getDeltaMovement().horizontalDistanceSqr() > 0.0025;
            float drain = moving ? 0.12f : 0.05f;
            setBattery(battery - drain);

            if (this.tickCount % 40 == 0) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        ModSounds.DRONE_HUM.get(), SoundSource.NEUTRAL, 0.22f, moving ? 1.15f : 0.95f);
            }
        } else {
            // Battery depleted: gentle controlled descent
            Vec3 vel = this.getDeltaMovement();
            this.setDeltaMovement(vel.x * 0.84, Math.max(-0.35, vel.y * 0.85 - 0.08), vel.z * 0.84);
            if (isFpvLinked()) {
                setFpvLinked(false);
                if (getOwnerPlayer() instanceof ServerPlayer sp) {
                    ModNetworking.sendFpvLinkPacket(sp, this.getId(), false, 0.0f);
                }
            }
        }

        if (this.tickCount % 5 == 0) {
            updateSignalQuality();
        }

        if (this.tickCount % 5 == 0 && getBattery() > 0.0f && (isAutoHover() || isFpvLinked())) {
            performLatticeScan();
        }
    }

    private void updateClientVisuals() {
        float battery = getBattery();
        Vec3 vel = this.getDeltaMovement();
        float speed = (float) vel.horizontalDistance();

        if (battery > 0.0f) {
            float spinRate = (isFpvLinked() || speed > 0.04f) ? 54.0f : (isAutoHover() ? 36.0f : 18.0f);
            this.rotorAngle = (this.rotorAngle + spinRate) % 360.0f;
        }

        double yawRad = Math.toRadians(this.getYRot());
        double fwdX = -Math.sin(yawRad);
        double fwdZ = Math.cos(yawRad);
        double leftX = Math.cos(yawRad);
        double leftZ = Math.sin(yawRad);

        float localFwd = (float) (vel.x * fwdX + vel.z * fwdZ);
        float localLeft = (float) (vel.x * leftX + vel.z * leftZ);

        float desiredPitch = Mth.clamp(localFwd * 48.0f, -24.0f, 24.0f);
        float desiredRoll = Mth.clamp(-localLeft * 48.0f, -24.0f, 24.0f);
        this.clientTiltPitch = Mth.lerp(0.22f, this.clientTiltPitch, desiredPitch);
        this.clientTiltRoll = Mth.lerp(0.22f, this.clientTiltRoll, desiredRoll);
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int lerpSteps, boolean teleport) {
        if (this.level().isClientSide
                && dev.itsrealperson.vision_tactical_eagleeye.client.DroneFpvClientController.isFpvActive()
                && dev.itsrealperson.vision_tactical_eagleeye.client.DroneFpvClientController.getActiveDroneId() == this.getId()) {
            // Keep FPV camera rotation client-authoritative so server sync packets never fight mouse look
            float yaw = dev.itsrealperson.vision_tactical_eagleeye.client.DroneFpvClientController.getFpvYaw();
            float pitch = dev.itsrealperson.vision_tactical_eagleeye.client.DroneFpvClientController.getFpvPitch();
            super.lerpTo(x, y, z, yaw, pitch, lerpSteps, teleport);
            this.setYRot(yaw);
            this.setXRot(pitch);
            this.yRotO = yaw;
            this.xRotO = pitch;
            return;
        }
        super.lerpTo(x, y, z, yRot, xRot, lerpSteps, teleport);
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (this.isEffectiveAi() || this.isControlledByLocalInstance()) {
            this.move(MoverType.SELF, this.getDeltaMovement());
        }
    }

    private void updateSignalQuality() {
        Player owner = getOwnerPlayer();
        if (owner == null || owner.level() != this.level()) {
            setSignalQuality(0.0f);
            if (isFpvLinked()) {
                setFpvLinked(false);
            }
            return;
        }

        double dist = this.distanceTo(owner);
        if (dist >= MAX_RANGE) {
            setSignalQuality(0.0f);
            if (isFpvLinked() && owner instanceof ServerPlayer sp) {
                setFpvLinked(false);
                ModNetworking.sendFpvLinkPacket(sp, this.getId(), false, 0.0f);
                sp.displayClientMessage(Component.translatable("message.vision_tactical_eagleeye.signal_lost")
                        .withStyle(ChatFormatting.RED), true);
            }
            return;
        }

        float baseSignal = (float) (1.0 - Math.pow(dist / MAX_RANGE, 1.35));
        HitResult clip = this.level().clip(new ClipContext(
                owner.getEyePosition(),
                this.getEyePosition(),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                this
        ));
        if (clip.getType() == HitResult.Type.BLOCK) {
            baseSignal *= 0.62f;
        }
        setSignalQuality(Mth.clamp(baseSignal, 0.02f, 1.0f));
    }

    private void performLatticeScan() {
        Player owner = getOwnerPlayer();
        if (!(owner instanceof ServerPlayer serverOwner) || owner.level() != this.level()) {
            return;
        }

        AABB box = this.getBoundingBox().inflate(SCAN_RANGE);
        List<LivingEntity> candidates = this.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && e != this && !e.getUUID().equals(serverOwner.getUUID()));

        List<LivingEntity> detected = new ArrayList<>();
        Vec3 droneEye = this.getEyePosition();
        Vec3 droneLook = this.getViewVector(1.0f).normalize();
        for (LivingEntity target : candidates) {
            Vec3 targetCenter = target.getBoundingBox().getCenter();
            Vec3 toTarget = targetCenter.subtract(droneEye);
            if (toTarget.lengthSqr() < 0.25) {
                continue;
            }
            // Require target to be strictly inside the drone's frontal camera cone
            if (droneLook.dot(toTarget.normalize()) <= 0.55) {
                continue;
            }
            // Strict visual block occlusion raycast (no X-Ray through walls/hills)
            HitResult los = this.level().clip(new ClipContext(
                    droneEye,
                    targetCenter,
                    ClipContext.Block.VISUAL,
                    ClipContext.Fluid.NONE,
                    this
            ));
            if (los.getType() == HitResult.Type.MISS) {
                detected.add(target);
            }
        }

        LatticeTrackManager.updateDroneTracks(serverOwner, this, detected);
    }

    /**
     * Detonates or activates the currently installed payload module on the server.
     */
    public void triggerPayload(@Nullable ServerPlayer operator) {
        if (this.level().isClientSide || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (getBattery() <= 0.0f) {
            return;
        }

        int payload = getPayloadType();
        if (payload == PAYLOAD_RECON) {
            AABB scanBox = this.getBoundingBox().inflate(RECON_PULSE_RANGE);
            List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, scanBox,
                    e -> e.isAlive() && e != this && (operator == null || !e.getUUID().equals(operator.getUUID())));

            for (LivingEntity target : targets) {
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 300, 0, false, false));
            }
            ServerPlayer owner = operator != null ? operator
                    : (getOwnerPlayer() instanceof ServerPlayer sp ? sp : null);
            if (owner != null) {
                LatticeTrackManager.updateDroneTracks(owner, this, targets, 300);
                owner.displayClientMessage(Component.translatable("message.vision_tactical_eagleeye.payload_triggered")
                        .withStyle(ChatFormatting.AQUA), true);
            }
            serverLevel.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.2, getZ(), 32, 2.5, 1.0, 2.5, 0.08);
            serverLevel.playSound(null, getX(), getY(), getZ(), ModSounds.LATTICE_SCAN.get(), SoundSource.NEUTRAL, 1.2f, 1.15f);
            setBattery(getBattery() - 35.0f);
        } else if (payload == PAYLOAD_EMP) {
            AABB empBox = this.getBoundingBox().inflate(EMP_RADIUS);
            List<LivingEntity> affected = serverLevel.getEntitiesOfClass(LivingEntity.class, empBox,
                    e -> e.isAlive() && e != this);

            for (LivingEntity entity : affected) {
                if (entity instanceof Enemy || (entity instanceof Player p && !isOwnedBy(p))) {
                    entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 3));
                    entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1));
                    entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 160, 0));
                }
                if (entity instanceof ServerPlayer nearbyPlayer) {
                    ModNetworking.sendEmpFlash(nearbyPlayer, 1.5f);
                }
            }

            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 0.2, getZ(), 64, 3.0, 1.8, 3.0, 0.25);
            serverLevel.playSound(null, getX(), getY(), getZ(), ModSounds.EMP_BLAST.get(), SoundSource.NEUTRAL, 1.5f, 0.9f);
            setBattery(getBattery() - 150.0f);
            setPayloadType(PAYLOAD_RECON);
            if (operator != null) {
                operator.displayClientMessage(Component.translatable("message.vision_tactical_eagleeye.payload_triggered")
                        .withStyle(ChatFormatting.AQUA), true);
            }
        } else if (payload == PAYLOAD_HE) {
            if (isFpvLinked() && getOwnerPlayer() instanceof ServerPlayer sp) {
                setFpvLinked(false);
                ModNetworking.sendFpvLinkPacket(sp, this.getId(), false, 0.0f);
            }
            serverLevel.explode(this, getX(), getY(), getZ(), 2.8f, Level.ExplosionInteraction.NONE);
            this.discard();
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!isOwnedBy(player)) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            if (!this.level().isClientSide) {
                if (isFpvLinked() && player instanceof ServerPlayer sp) {
                    setFpvLinked(false);
                    ModNetworking.sendFpvLinkPacket(sp, this.getId(), false, 0.0f);
                }
                LatticeControllerItem.clearLinkedDroneInInventory(player, this.getId());
                ItemStack packed = new ItemStack(ModItems.TACTICAL_DRONE.get());
                TacticalDroneItem.setBattery(packed, getBattery());
                TacticalDroneItem.setPayloadType(packed, getPayloadType());
                if (!player.addItem(packed)) {
                    player.drop(packed, false);
                }
                this.level().playSound(null, getX(), getY(), getZ(),
                        SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.7f, 1.1f);
                this.discard();
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        if (TacticalDroneItem.isVisionBattery(held) && getBattery() < MAX_BATTERY) {
            if (!this.level().isClientSide) {
                setBattery(MAX_BATTERY);
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 0.3, getZ(), 14, 0.3, 0.2, 0.3, 0.05);
                }
                this.level().playSound(null, getX(), getY(), getZ(),
                        SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8f, 1.35f);
                player.displayClientMessage(Component.translatable("message.vision_tactical_eagleeye.battery_recharged")
                        .withStyle(ChatFormatting.AQUA), true);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        if (held.getItem() instanceof LatticeControllerItem) {
            if (!this.level().isClientSide) {
                LatticeControllerItem.setLinkedDrone(held, this);
                this.level().playSound(null, getX(), getY(), getZ(),
                        ModSounds.LATTICE_LINK.get(), SoundSource.PLAYERS, 0.85f, 1.2f);
                player.displayClientMessage(Component.translatable("message.vision_tactical_eagleeye.drone_linked", this.getId())
                        .withStyle(ChatFormatting.GREEN), true);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        if (held.getItem() instanceof DronePayloadItem payloadItem) {
            if (getPayloadType() == PAYLOAD_RECON) {
                if (!this.level().isClientSide) {
                    setPayloadType(payloadItem.getPayloadType());
                    if (!player.getAbilities().instabuild) {
                        held.shrink(1);
                    }
                    this.level().playSound(null, getX(), getY(), getZ(),
                            ModSounds.PAYLOAD_ARM.get(), SoundSource.PLAYERS, 0.8f, 1.1f);
                    player.displayClientMessage(Component.translatable("message.vision_tactical_eagleeye.payload_installed")
                            .withStyle(ChatFormatting.GREEN), true);
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
            return InteractionResult.PASS;
        }

        if (held.isEmpty() && hand == InteractionHand.MAIN_HAND) {
            if (!this.level().isClientSide) {
                boolean nextHover = !isAutoHover();
                setAutoHover(nextHover);
                player.displayClientMessage(Component.translatable(nextHover
                                ? "message.vision_tactical_eagleeye.hover_enabled"
                                : "message.vision_tactical_eagleeye.hover_disabled")
                        .withStyle(nextHover ? ChatFormatting.AQUA : ChatFormatting.YELLOW), true);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        return super.mobInteract(player, hand);
    }

    @Override
    public void die(DamageSource damageSource) {
        if (!this.level().isClientSide) {
            Player owner = getOwnerPlayer();
            LatticeControllerItem.clearLinkedDroneInInventory(owner, this.getId());
            if (isFpvLinked() && owner instanceof ServerPlayer sp) {
                setFpvLinked(false);
                ModNetworking.sendFpvLinkPacket(sp, this.getId(), false, 0.0f);
            }
        }
        super.die(damageSource);
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        UUID owner = getOwnerUUID();
        if (owner != null) {
            tag.putUUID("OwnerUUID", owner);
        }
        tag.putFloat("Battery", getBattery());
        tag.putBoolean("AutoHover", isAutoHover());
        tag.putInt("PayloadType", getPayloadType());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("OwnerUUID")) {
            setOwnerUUID(tag.getUUID("OwnerUUID"));
        }
        if (tag.contains("Battery")) {
            setBattery(tag.getFloat("Battery"));
        }
        if (tag.contains("AutoHover")) {
            setAutoHover(tag.getBoolean("AutoHover"));
        }
        if (tag.contains("PayloadType")) {
            setPayloadType(tag.getInt("PayloadType"));
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 3, state -> {
            if (getBattery() <= 0.0f) {
                return state.setAndContinue(ANIM_IDLE);
            }
            if (isFpvLinked() || this.getDeltaMovement().horizontalDistanceSqr() > 0.008) {
                return state.setAndContinue(ANIM_FLY);
            }
            return state.setAndContinue(isAutoHover() ? ANIM_HOVER : ANIM_IDLE);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
