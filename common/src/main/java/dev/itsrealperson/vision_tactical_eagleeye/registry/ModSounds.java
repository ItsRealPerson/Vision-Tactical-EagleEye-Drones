package dev.itsrealperson.vision_tactical_eagleeye.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.itsrealperson.vision_tactical_eagleeye.VisionTacticalEagleEye;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(VisionTacticalEagleEye.MOD_ID, Registries.SOUND_EVENT);

    public static final RegistrySupplier<SoundEvent> DRONE_HUM = registerSound("drone_hum");
    public static final RegistrySupplier<SoundEvent> LATTICE_LINK = registerSound("lattice_link");
    public static final RegistrySupplier<SoundEvent> LATTICE_SCAN = registerSound("lattice_scan");
    public static final RegistrySupplier<SoundEvent> EMP_BLAST = registerSound("emp_blast");
    public static final RegistrySupplier<SoundEvent> DRONE_DEPLOY = registerSound("drone_deploy");
    public static final RegistrySupplier<SoundEvent> PAYLOAD_ARM = registerSound("payload_arm");

    private ModSounds() {
    }

    private static RegistrySupplier<SoundEvent> registerSound(String name) {
        ResourceLocation id = new ResourceLocation(VisionTacticalEagleEye.MOD_ID, name);
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }

    public static void register() {
        SOUNDS.register();
    }
}
