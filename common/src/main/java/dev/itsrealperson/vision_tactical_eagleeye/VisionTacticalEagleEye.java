package dev.itsrealperson.vision_tactical_eagleeye;

import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.platform.Platform;
import dev.itsrealperson.vision_goggles.api.VisionGogglesAPI;
import dev.itsrealperson.vision_goggles.recipe.StationBlueprints;
import dev.itsrealperson.vision_tactical_eagleeye.network.LatticeTrackManager;
import dev.itsrealperson.vision_tactical_eagleeye.network.ModNetworking;
import dev.itsrealperson.vision_tactical_eagleeye.registry.ModCreativeTabs;
import dev.itsrealperson.vision_tactical_eagleeye.registry.ModEntities;
import dev.itsrealperson.vision_tactical_eagleeye.registry.ModItems;
import dev.itsrealperson.vision_tactical_eagleeye.registry.ModSounds;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class VisionTacticalEagleEye {
    public static final String MOD_ID = "vision_tactical_eagleeye";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final String LATTICE_LUA_SCRIPT = """
            -- 09_anduril_lattice_os.lua
            -- Anduril EagleEye & Lattice OS telemetry readout for Vision Goggles Lua HUD
            module = {
                id = "vision_tactical_eagleeye:anduril_lattice_os",
                name = "Anduril Lattice OS Data-Link",
                description = "Displays real-time UAV fleet, FPV link, signal RSSI and tracked threat count."
            }

            function onRenderHud(w, h, dt)
                if not eagleeye then return end
                local drones = eagleeye.getActiveDroneCount()
                local fpv = eagleeye.isFpvActive()
                if drones <= 0 and not fpv then return end

                local bat = math.floor((eagleeye.getDroneBattery() or 0) * 100)
                local sig = math.floor((eagleeye.getSignalQuality() or 0) * 100)
                local tracks = eagleeye.getTrackedTargetCount() or 0
                local mode = fpv and "FPV-LINK" or "AUTO-HOVER"
                local line = string.format("LATTICE // %s | UAV:%d | BAT:%d%% | SIG:%d%% | TRK:%d",
                    mode, drones, bat, sig, tracks)
                hud.drawText(8, h - 22, line, 0x00E5FF)
            end
            """;

    private VisionTacticalEagleEye() {
    }

    public static void init() {
        ModSounds.register();
        ModEntities.register();
        ModItems.register();
        ModCreativeTabs.register();
        ModNetworking.register();

        TickEvent.SERVER_POST.register(LatticeTrackManager::onServerTick);
        PlayerEvent.PLAYER_QUIT.register(player -> LatticeTrackManager.clearPlayer(player.getUUID()));

        registerExternalLuaScriptSafe("09_anduril_lattice_os.lua", LATTICE_LUA_SCRIPT);
        registerStationBlueprintsSafe();

        LOGGER.info("[Vision Tactical: EagleEye & Drones] Core initialized.");
    }

    private static void registerExternalLuaScriptSafe(String fileName, String source) {
        try {
            VisionGogglesAPI.registerExternalLuaScript(fileName, source);
        } catch (Throwable e) {
            LOGGER.warn("[EagleEye] Could not register Lua script via API ({}), writing fallback.", e.getMessage());
            try {
                Path modulesDir = Platform.getConfigFolder().resolve("vision_goggles").resolve("modules");
                Path scriptsDir = Platform.getConfigFolder().resolve("vision_goggles").resolve("scripts");
                Files.createDirectories(modulesDir);
                Files.createDirectories(scriptsDir);
                Files.writeString(modulesDir.resolve(fileName), source, StandardCharsets.UTF_8);
                Files.writeString(scriptsDir.resolve(fileName), source, StandardCharsets.UTF_8);
            } catch (Throwable ignored) {
            }
        }
    }

    private static void registerStationBlueprintsSafe() {
        try {
            VisionGogglesAPI.registerStationBlueprint(new StationBlueprints.Blueprint(
                    "eagleeye_datalink_module",
                    ModItems.EAGLEEYE_DATALINK_MODULE,
                    "gui.vision_goggles.category.tactical",
                    List.of(
                            new StationBlueprints.IngredientReq(() -> Items.GOLD_INGOT, 2),
                            new StationBlueprints.IngredientReq(() -> Items.REDSTONE, 4),
                            new StationBlueprints.IngredientReq(() -> Items.AMETHYST_SHARD, 2),
                            new StationBlueprints.IngredientReq(() -> Items.ENDER_EYE, 1)
                    ),
                    false
            ));

            VisionGogglesAPI.registerStationBlueprint(new StationBlueprints.Blueprint(
                    "tactical_drone",
                    ModItems.TACTICAL_DRONE,
                    "gui.vision_goggles.category.tactical",
                    List.of(
                            new StationBlueprints.IngredientReq(() -> Items.IRON_INGOT, 4),
                            new StationBlueprints.IngredientReq(() -> Items.COPPER_INGOT, 4),
                            new StationBlueprints.IngredientReq(() -> Items.REDSTONE, 4),
                            new StationBlueprints.IngredientReq(() -> Items.SPYGLASS, 1)
                    ),
                    false
            ));
        } catch (Throwable e) {
            LOGGER.warn("[EagleEye] Could not register Modification Station blueprints: {}", e.getMessage());
        }
    }
}
