package dev.itsrealperson.vision_tactical_eagleeye.network;

import dev.itsrealperson.vision_tactical_eagleeye.entity.TacticalDroneEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-authoritative Lattice OS track database and periodic telemetry broadcaster.
 */
public final class LatticeTrackManager {
    private static final int DEFAULT_TRACK_TTL_TICKS = 10;
    private static final int SYNC_INTERVAL_TICKS = 5;

    private static final Map<UUID, Map<Integer, TimedTrack>> PLAYER_TRACKS = new ConcurrentHashMap<>();
    private static int serverTickCounter;

    private record TimedTrack(LatticeTrackEntry entry, long expireGameTick) {
    }

    private LatticeTrackManager() {
    }

    public static void updateDroneTracks(ServerPlayer owner, TacticalDroneEntity drone, List<LivingEntity> detectedEntities) {
        updateDroneTracks(owner, drone, detectedEntities, DEFAULT_TRACK_TTL_TICKS);
    }

    public static void updateDroneTracks(ServerPlayer owner, TacticalDroneEntity drone,
                                         List<LivingEntity> detectedEntities, int durationTicks) {
        if (owner == null || drone == null || detectedEntities == null) {
            return;
        }
        long now = owner.level().getGameTime();
        long expire = now + Math.max(8, durationTicks);
        Map<Integer, TimedTrack> map = PLAYER_TRACKS.computeIfAbsent(owner.getUUID(), k -> new ConcurrentHashMap<>());
        java.util.Set<Integer> seenNow = new java.util.HashSet<>(detectedEntities.size());

        for (LivingEntity target : detectedEntities) {
            if (!target.isAlive() || target.getUUID().equals(owner.getUUID())) {
                continue;
            }
            seenNow.add(target.getId());
            int threat = classifyThreat(target);
            Vec3 vel = target.getDeltaMovement();
            float maxHp = Math.max(1.0f, target.getMaxHealth());
            float hpPct = Math.max(0.0f, Math.min(1.0f, target.getHealth() / maxHp));
            String label = target.getType().getDescription().getString();
            if (label.length() > 32) {
                label = label.substring(0, 32);
            }

            LatticeTrackEntry entry = new LatticeTrackEntry(
                    target.getId(),
                    target.getX(),
                    target.getY() + target.getBbHeight() * 0.5,
                    target.getZ(),
                    (float) vel.x,
                    (float) vel.y,
                    (float) vel.z,
                    threat,
                    drone.getId(),
                    hpPct,
                    label
            );

            TimedTrack existing = map.get(target.getId());
            long mergedExpire = (durationTicks > DEFAULT_TRACK_TTL_TICKS && existing != null)
                    ? Math.max(existing.expireGameTick(), expire)
                    : expire;
            map.put(target.getId(), new TimedTrack(entry, mergedExpire));
        }

        // Immediately prune regular LOS tracks from this drone that are no longer visible (preserve long Recon Pulse tracks)
        if (durationTicks <= DEFAULT_TRACK_TTL_TICKS) {
            int droneId = drone.getId();
            map.entrySet().removeIf(e ->
                    e.getValue().entry().droneId() == droneId
                            && !seenNow.contains(e.getKey())
                            && (e.getValue().expireGameTick() - now) <= DEFAULT_TRACK_TTL_TICKS
            );
        }
    }

    public static void onServerTick(MinecraftServer server) {
        if (server == null) return;
        serverTickCounter++;
        if (serverTickCounter % SYNC_INTERVAL_TICKS != 0) {
            return;
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            syncPlayerTelemetry(player);
        }
    }

    private static void syncPlayerTelemetry(ServerPlayer player) {
        long now = player.level().getGameTime();
        AABB searchBox = player.getBoundingBox().inflate(TacticalDroneEntity.MAX_RANGE);
        List<TacticalDroneEntity> ownedDrones = player.level().getEntitiesOfClass(
                TacticalDroneEntity.class, searchBox,
                d -> d.isAlive() && d.isOwnedBy(player)
        );

        Map<Integer, TimedTrack> trackMap = PLAYER_TRACKS.get(player.getUUID());
        if (ownedDrones.isEmpty() && (trackMap == null || trackMap.isEmpty())) {
            return;
        }

        List<LatticeTrackEntry> activeTracks = new ArrayList<>();
        if (trackMap != null) {
            trackMap.entrySet().removeIf(e -> e.getValue().expireGameTick() <= now);
            for (TimedTrack timed : trackMap.values()) {
                activeTracks.add(timed.entry());
            }
            if (trackMap.isEmpty() && ownedDrones.isEmpty()) {
                PLAYER_TRACKS.remove(player.getUUID());
                ModNetworking.sendLatticeTracksPacket(player, LatticeTelemetrySnapshot.EMPTY);
                return;
            }
        }

        // Also include player's own drones as allied tracks for AR situational awareness
        for (TacticalDroneEntity d : ownedDrones) {
            Vec3 dv = d.getDeltaMovement();
            activeTracks.add(new LatticeTrackEntry(
                    d.getId(),
                    d.getX(),
                    d.getY() + d.getBbHeight() * 0.5,
                    d.getZ(),
                    (float) dv.x,
                    (float) dv.y,
                    (float) dv.z,
                    LatticeTrackEntry.THREAT_DRONE,
                    d.getId(),
                    d.getBatteryPct(),
                    "UAV-" + (d.getId() % 100)
            ));
        }

        TacticalDroneEntity primary = null;
        if (!ownedDrones.isEmpty()) {
            primary = ownedDrones.stream()
                    .sorted(Comparator.comparing((TacticalDroneEntity d) -> !d.isFpvLinked())
                            .thenComparingDouble(d -> d.distanceToSqr(player)))
                    .findFirst()
                    .orElse(null);
        }

        LatticeTelemetrySnapshot snapshot = new LatticeTelemetrySnapshot(
                activeTracks,
                ownedDrones.size(),
                primary != null ? primary.getId() : -1,
                primary != null ? primary.getBatteryPct() : 0.0f,
                primary != null ? primary.getSignalQuality() : 0.0f,
                primary != null && primary.isAutoHover(),
                primary != null ? primary.getPayloadType() : TacticalDroneEntity.PAYLOAD_RECON
        );

        ModNetworking.sendLatticeTracksPacket(player, snapshot);
    }

    public static void clearPlayer(UUID playerId) {
        if (playerId != null) {
            PLAYER_TRACKS.remove(playerId);
        }
    }

    private static int classifyThreat(LivingEntity entity) {
        if (entity instanceof TacticalDroneEntity) {
            return LatticeTrackEntry.THREAT_DRONE;
        }
        if (entity instanceof Player) {
            return LatticeTrackEntry.THREAT_PLAYER;
        }
        if (entity instanceof Enemy) {
            return LatticeTrackEntry.THREAT_HOSTILE;
        }
        return LatticeTrackEntry.THREAT_NEUTRAL;
    }
}
