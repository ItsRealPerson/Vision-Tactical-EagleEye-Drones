package dev.itsrealperson.vision_tactical_eagleeye.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.itsrealperson.vision_tactical_eagleeye.VisionTacticalEagleEye;
import dev.itsrealperson.vision_tactical_eagleeye.entity.TacticalDroneEntity;
import dev.itsrealperson.vision_tactical_eagleeye.item.DronePayloadItem;
import dev.itsrealperson.vision_tactical_eagleeye.item.EagleEyeModuleItem;
import dev.itsrealperson.vision_tactical_eagleeye.item.LatticeControllerItem;
import dev.itsrealperson.vision_tactical_eagleeye.item.TacticalDroneItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(VisionTacticalEagleEye.MOD_ID, Registries.ITEM);

    public static final RegistrySupplier<Item> TACTICAL_DRONE = ITEMS.register("tactical_drone",
            () -> new TacticalDroneItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    public static final RegistrySupplier<Item> LATTICE_FPV_CONTROLLER = ITEMS.register("lattice_fpv_controller",
            () -> new LatticeControllerItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    public static final RegistrySupplier<Item> EAGLEEYE_DATALINK_MODULE = ITEMS.register("eagleeye_datalink_module",
            () -> new EagleEyeModuleItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final RegistrySupplier<Item> DRONE_PAYLOAD_EMP = ITEMS.register("drone_payload_emp",
            () -> new DronePayloadItem(TacticalDroneEntity.PAYLOAD_EMP, new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));

    public static final RegistrySupplier<Item> DRONE_PAYLOAD_HE = ITEMS.register("drone_payload_he",
            () -> new DronePayloadItem(TacticalDroneEntity.PAYLOAD_HE, new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));

    private ModItems() {
    }

    public static void register() {
        ITEMS.register();
    }
}
