package dev.itsrealperson.vision_tactical_eagleeye.network;

import java.util.Collections;
import java.util.List;

/**
 * Immutable telemetry + track packet payload sent from server to operator client.
 */
public record LatticeTelemetrySnapshot(
        List<LatticeTrackEntry> tracks,
        int activeDroneCount,
        int primaryDroneId,
        float primaryBatteryPct,
        float primarySignal,
        boolean primaryAutoHover,
        int primaryPayload
) {
    public static final LatticeTelemetrySnapshot EMPTY = new LatticeTelemetrySnapshot(
            Collections.emptyList(), 0, -1, 0.0f, 0.0f, false, 0
    );
}
