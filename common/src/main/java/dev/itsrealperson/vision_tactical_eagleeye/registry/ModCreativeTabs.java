package dev.itsrealperson.vision_tactical_eagleeye.registry;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.itsrealperson.vision_tactical_eagleeye.VisionTacticalEagleEye;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(VisionTacticalEagleEye.MOD_ID, Registries.CREATIVE_MODE_TAB);

    public static final RegistrySupplier<CreativeModeTab> MAIN_TAB = TABS.register("main",
            () -> CreativeTabRegistry.create(builder -> {
                builder.title(Component.translatable("itemGroup.vision_tactical_eagleeye.main"));
                builder.icon(() -> new ItemStack(ModItems.TACTICAL_DRONE.get()));
                builder.displayItems((parameters, output) -> {
                    output.accept(new ItemStack(ModItems.TACTICAL_DRONE.get()));
                    output.accept(new ItemStack(ModItems.LATTICE_FPV_CONTROLLER.get()));
                    output.accept(new ItemStack(ModItems.EAGLEEYE_DATALINK_MODULE.get()));
                    output.accept(new ItemStack(ModItems.DRONE_PAYLOAD_EMP.get()));
                    output.accept(new ItemStack(ModItems.DRONE_PAYLOAD_HE.get()));
                });
            }));

    private ModCreativeTabs() {
    }

    public static void register() {
        TABS.register();
    }
}
