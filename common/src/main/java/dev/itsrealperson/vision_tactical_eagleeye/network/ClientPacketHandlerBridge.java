package dev.itsrealperson.vision_tactical_eagleeye.network;

import java.util.function.Consumer;

/**
 * Server-safe client callback bridge. {@code common} networking invokes these hooks
 * without ever importing {@code net.minecraft.client.*}. Client initialization
 * registers real handlers via {@link #registerHandlers(DroneLinkHandler, Consumer)}.
 */
public final class ClientPacketHandlerBridge {
    @FunctionalInterface
    public interface DroneLinkHandler {
        void handle(int droneEntityId, boolean linked, float empFlashIntensity);
    }

    private static volatile DroneLinkHandler droneLinkHandler = (id, linked, empFlash) -> {
    };
    private static volatile Consumer<LatticeTelemetrySnapshot> latticeTracksHandler = snapshot -> {
    };

    private ClientPacketHandlerBridge() {
    }

    public static void registerHandlers(DroneLinkHandler linkHandler,
                                        Consumer<LatticeTelemetrySnapshot> tracksHandler) {
        if (linkHandler != null) {
            droneLinkHandler = linkHandler;
        }
        if (tracksHandler != null) {
            latticeTracksHandler = tracksHandler;
        }
    }

    public static void dispatchDroneLink(int droneEntityId, boolean linked, float empFlashIntensity) {
        droneLinkHandler.handle(droneEntityId, linked, empFlashIntensity);
    }

    public static void dispatchLatticeTracks(LatticeTelemetrySnapshot snapshot) {
        latticeTracksHandler.accept(snapshot != null ? snapshot : LatticeTelemetrySnapshot.EMPTY);
    }
}
