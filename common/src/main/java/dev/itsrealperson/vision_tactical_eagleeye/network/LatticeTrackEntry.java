package dev.itsrealperson.vision_tactical_eagleeye.network;

/**
 * Immutable snapshot of a single entity tracked by a Lattice OS drone.
 *
 * @param entityId    World entity numeric ID.
 * @param x           World X coordinate.
 * @param y           World Y coordinate (chest/center height).
 * @param z           World Z coordinate.
 * @param vx          Velocity X (blocks/tick).
 * @param vy          Velocity Y (blocks/tick).
 * @param vz          Velocity Z (blocks/tick).
 * @param threatLevel 0 = neutral, 1 = hostile, 2 = player, 3 = allied drone.
 * @param droneId     ID of the drone reporting this track.
 * @param healthPct   Normalized health in [0.0, 1.0].
 * @param label       Short display label / entity type name.
 */
public record LatticeTrackEntry(
        int entityId,
        double x,
        double y,
        double z,
        float vx,
        float vy,
        float vz,
        int threatLevel,
        int droneId,
        float healthPct,
        String label
) {
    public static final int THREAT_NEUTRAL = 0;
    public static final int THREAT_HOSTILE = 1;
    public static final int THREAT_PLAYER = 2;
    public static final int THREAT_DRONE = 3;
}
