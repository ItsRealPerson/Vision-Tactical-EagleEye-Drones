package dev.itsrealperson.vision_tactical_eagleeye.registry;

import dev.architectury.registry.level.entity.EntityAttributeRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.itsrealperson.vision_tactical_eagleeye.VisionTacticalEagleEye;
import dev.itsrealperson.vision_tactical_eagleeye.entity.TacticalDroneEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(VisionTacticalEagleEye.MOD_ID, Registries.ENTITY_TYPE);

    public static final RegistrySupplier<EntityType<TacticalDroneEntity>> TACTICAL_DRONE =
            ENTITIES.register("tactical_drone", () ->
                    EntityType.Builder.<TacticalDroneEntity>of(TacticalDroneEntity::new, MobCategory.MISC)
                            .sized(0.6f, 0.35f)
                            .clientTrackingRange(10)
                            .updateInterval(1)
                            .build(new ResourceLocation(VisionTacticalEagleEye.MOD_ID, "tactical_drone").toString())
            );

    private ModEntities() {
    }

    public static void register() {
        ENTITIES.register();
        EntityAttributeRegistry.register(TACTICAL_DRONE, TacticalDroneEntity::createAttributes);
    }
}
