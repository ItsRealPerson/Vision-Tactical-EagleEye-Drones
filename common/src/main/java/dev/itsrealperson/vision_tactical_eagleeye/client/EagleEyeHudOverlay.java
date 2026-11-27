package dev.itsrealperson.vision_tactical_eagleeye.client;

import dev.architectury.event.events.client.ClientGuiEvent;
import dev.itsrealperson.vision_goggles.api.VisionGogglesClientAPI;
import dev.itsrealperson.vision_tactical_eagleeye.VisionTacticalEagleEye;
import dev.itsrealperson.vision_tactical_eagleeye.entity.TacticalDroneEntity;
import dev.itsrealperson.vision_tactical_eagleeye.item.EagleEyeModuleItem;
import dev.itsrealperson.vision_tactical_eagleeye.item.LatticeControllerItem;
import dev.itsrealperson.vision_tactical_eagleeye.mixin.client.GameRendererAccessor;
import dev.itsrealperson.vision_tactical_eagleeye.network.LatticeTrackEntry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

/**
 * Anduril EagleEye & Lattice OS AR HUD Overlay:
 * <ul>
 *     <li>Operator AR Data-Link view (3D->2D wall-penetrating target diamonds & Lattice status bar)</li>
 *     <li>FPV Quadcopter view (gimbal pitch/roll ladder, compass tape, telemetry, AR tracks)</li>
 *     <li>Progressive analog RF interference & CRT glitching when signal degrades below 80%</li>
 * </ul>
 */
@Environment(EnvType.CLIENT)
public final class EagleEyeHudOverlay {
    private static final int COLOR_HOSTILE = 0xFFFF3B30;
    private static final int COLOR_PLAYER = 0xFFFFCC00;
    private static final int COLOR_NEUTRAL = 0xFF00E5FF;
    private static final int COLOR_HUD_GREEN = 0xFF39FF88;
    private static final int COLOR_BG_PANEL = 0x880A1118;

    private static final String[] COMPASS_DIRS = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};

    private static boolean initialized = false;
    private static long lastRenderedGameTime = Long.MIN_VALUE;
    private static float lastRenderedPartial = -1.0f;

    private EagleEyeHudOverlay() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        try {
            VisionGogglesClientAPI.registerHudOverlayLayer((gui, goggles, partialTicks, width, height) ->
                    renderFromVisionGoggles(gui, goggles, partialTicks, width, height));
        } catch (Throwable t) {
            VisionTacticalEagleEye.LOGGER.warn("[EagleEye] Could not register VisionGoggles HUD layer: {}", t.toString());
        }

        ClientGuiEvent.RENDER_HUD.register(EagleEyeHudOverlay::renderStandaloneHud);
    }

    private static void renderFromVisionGoggles(GuiGraphics gui, ItemStack goggles, float partialTicks, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        markFrameRendered(mc, partialTicks);
        renderInternal(gui, mc, partialTicks, width, height);
    }

    private static void renderStandaloneHud(GuiGraphics gui, float partialTicks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui) {
            return;
        }
        if (wasFrameAlreadyRendered(mc, partialTicks)) {
            return;
        }
        if (!DroneFpvClientController.isFpvActive() && !shouldRenderOperatorHud(mc)) {
            return;
        }
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();
        markFrameRendered(mc, partialTicks);
        renderInternal(gui, mc, partialTicks, width, height);
    }

    private static void markFrameRendered(Minecraft mc, float partialTicks) {
        if (mc.level != null) {
            lastRenderedGameTime = mc.level.getGameTime();
            lastRenderedPartial = partialTicks;
        }
    }

    private static boolean wasFrameAlreadyRendered(Minecraft mc, float partialTicks) {
        return mc.level != null
                && mc.level.getGameTime() == lastRenderedGameTime
                && Math.abs(partialTicks - lastRenderedPartial) < 1.0e-4f;
    }

    private static boolean shouldRenderOperatorHud(Minecraft mc) {
        if (mc.player == null) {
            return false;
        }
        // Strictly require: Vision Goggles ON + eagleeye_datalink_module installed + active owned drone in range
        return EagleEyeModuleItem.hasEagleEyeActive(mc.player)
                && DroneFpvClientController.getActiveDroneCount() > 0;
    }

    private static void renderInternal(GuiGraphics gui, Minecraft mc, float partialTicks, int width, int height) {
        if (DroneFpvClientController.isFpvActive()) {
            renderFpvHud(gui, mc, partialTicks, width, height);
        } else if (shouldRenderOperatorHud(mc)) {
            renderOperatorArHud(gui, mc, partialTicks, width, height);
        }
    }

    // =========================================================================
    // A) OPERATOR AR DATA-LINK VIEW
    // =========================================================================

    private static void renderOperatorArHud(GuiGraphics gui, Minecraft mc, float partialTicks, int width, int height) {
        int linkedId = LatticeControllerItem.getPlayerLinkedDroneId(mc.player);
        if (linkedId < 0) {
            return;
        }
        Font font = mc.font;
        int droneCount = DroneFpvClientController.getActiveDroneCount();
        int batPct = Math.round(DroneFpvClientController.getPrimaryBatteryPct() * 100.0f);
        String modeStr = DroneFpvClientController.isPrimaryAutoHover() ? "AUTO-HOVER" : "MANUAL";
        List<LatticeTrackEntry> allTracks = DroneFpvClientController.getTracks();
        List<LatticeTrackEntry> tracks = allTracks.stream()
                .filter(t -> t.droneId() == linkedId || t.entityId() == linkedId)
                .toList();

        String topBar = String.format(
                "[LATTICE OS // DATA-LINK ACTIVE] | UAV #%d | BAT: %d%% | MODE: %s | TRACKS: %d",
                linkedId, batPct, modeStr, tracks.size()
        );
        int barW = font.width(topBar) + 16;
        int barX = (width - barW) / 2;
        int barY = 8;
        gui.fill(barX, barY, barX + barW, barY + 14, COLOR_BG_PANEL);
        gui.renderOutline(barX, barY, barW, 14, COLOR_NEUTRAL);
        gui.drawString(font, topBar, barX + 8, barY + 3, COLOR_NEUTRAL, false);

        renderProjectedTracks(gui, mc, tracks, partialTicks, width, height, true);
    }

    // =========================================================================
    // B) FPV QUADCOPTER VIEW + ANALOG INTERFERENCE
    // =========================================================================

    private static void renderFpvHud(GuiGraphics gui, Minecraft mc, float partialTicks, int width, int height) {
        TacticalDroneEntity drone = DroneFpvClientController.getActiveDrone(mc);
        if (drone == null || mc.player == null) {
            return;
        }
        Font font = mc.font;
        int cx = width / 2;
        int cy = height / 2;

        // 1. AR Lattice boxes over tracked entities in drone FOV (GLSL uav_eo PostChain already applied to world)
        renderProjectedTracks(gui, mc, DroneFpvClientController.getTracks(), partialTicks, width, height, false);

        // 2. Top Compass Tape
        float yaw = Mth.wrapDegrees(drone.getViewYRot(partialTicks));
        int headingDeg = Math.round((yaw + 360.0f) % 360.0f);
        String cardinal = COMPASS_DIRS[Math.round(headingDeg / 45.0f) % 8];
        String compassText = String.format("HDG %03d [%s]  //  EO-CAM UAV LINK", headingDeg, cardinal);
        int compW = font.width(compassText) + 18;
        gui.fill(cx - compW / 2, 6, cx + compW / 2, 20, COLOR_BG_PANEL);
        gui.renderOutline(cx - compW / 2, 6, compW, 14, COLOR_NEUTRAL);
        gui.drawCenteredString(font, compassText, cx, 9, COLOR_NEUTRAL);

        // 3. Central Gimbal Reticle + Dynamic Pitch/Roll Ladder
        float pitchOffset = (drone.getViewXRot(partialTicks) + drone.clientTiltPitch * 0.45f) * 0.65f;
        float rollRad = drone.clientTiltRoll * Mth.DEG_TO_RAD;
        int ladderY = Mth.clamp((int) (cy + pitchOffset), 36, height - 36);
        int rollShiftY = (int) (Math.sin(rollRad) * 26.0f);

        // Crosshair center (ice-cyan EO reticle)
        int eoReticle = 0xDDF0FAFF;
        gui.fill(cx - 10, cy, cx - 3, cy + 1, eoReticle);
        gui.fill(cx + 4, cy, cx + 11, cy + 1, eoReticle);
        gui.fill(cx, cy - 8, cx + 1, cy - 3, eoReticle);
        gui.fill(cx, cy + 4, cx + 1, cy + 9, eoReticle);

        // Artificial horizon ticks tilted by roll
        gui.fill(cx - 34, ladderY - rollShiftY, cx - 16, ladderY - rollShiftY + 1, 0xCC7AD8FF);
        gui.fill(cx + 17, ladderY + rollShiftY, cx + 35, ladderY + rollShiftY + 1, 0xCC7AD8FF);
        gui.fill(cx - 22, ladderY - 14, cx - 14, ladderY - 13, 0x887AD8FF);
        gui.fill(cx + 15, ladderY - 14, cx + 23, ladderY - 13, 0x887AD8FF);
        gui.fill(cx - 22, ladderY + 14, cx - 14, ladderY + 15, 0x887AD8FF);
        gui.fill(cx + 15, ladderY + 14, cx + 23, ladderY + 15, 0x887AD8FF);

        // Corner FPV frame brackets
        drawCornerBrackets(gui, 14, 14, width - 28, height - 28, 12, 0xBB7AD8FF);

        // 4. Left Telemetry Column (Altitude, Speed, Range)
        double alt = drone.getY();
        double spd = drone.getDeltaMovement().length() * 20.0;
        double rng = drone.distanceTo(mc.player);

        int leftX = 20;
        int colY = cy - 36;
        gui.fill(leftX - 4, colY - 4, leftX + 94, colY + 42, COLOR_BG_PANEL);
        gui.drawString(font, String.format("ALT  %6.1f m", alt), leftX, colY, 0xFFD6F0FF, false);
        gui.drawString(font, String.format("SPD  %6.1f m/s", spd), leftX, colY + 12, 0xFFD6F0FF, false);
        gui.drawString(font, String.format("RNG  %6.1f m", rng), leftX, colY + 24,
                rng > 110.0 ? COLOR_HOSTILE : 0xFFD6F0FF, false);

        // 5. Right Telemetry Column (Battery, Signal/RSSI, Mode)
        float batRatio = Mth.clamp(drone.getBatteryPct(), 0.0f, 1.0f);
        int batPctInt = Math.round(batRatio * 100.0f);
        float signal = drone.getSignalQuality();
        int linkPct = Math.round(signal * 100.0f);
        int rssiDbm = Math.round(-98.0f + signal * 56.0f);
        int batColor = batRatio > 0.30f ? COLOR_HUD_GREEN : (batRatio > 0.15f ? COLOR_PLAYER : COLOR_HOSTILE);

        int rightX = width - 122;
        gui.fill(rightX - 4, colY - 4, width - 16, colY + 52, COLOR_BG_PANEL);
        gui.drawString(font, String.format("BAT  %3d%%", batPctInt), rightX, colY, batColor, false);
        // Battery bar
        gui.fill(rightX, colY + 10, rightX + 90, colY + 14, 0x66222222);
        gui.fill(rightX, colY + 10, rightX + Math.round(90.0f * batRatio), colY + 14, batColor);

        int sigColor = signal >= 0.55f ? COLOR_NEUTRAL : (signal >= 0.25f ? COLOR_PLAYER : COLOR_HOSTILE);
        gui.drawString(font, String.format("LINK %3d%% %ddBm", linkPct, rssiDbm), rightX, colY + 19, sigColor, false);
        String flightMode = drone.isAutoHover() ? "[AUTO-HOVER]" : "[MANUAL FPV]";
        gui.drawString(font, flightMode, rightX, colY + 32, drone.isAutoHover() ? COLOR_NEUTRAL : COLOR_PLAYER, false);

        // 6. Bottom Payload & Controls Bar
        String payloadName = switch (drone.getPayloadType()) {
            case TacticalDroneEntity.PAYLOAD_EMP -> "EMP CHARGE";
            case TacticalDroneEntity.PAYLOAD_HE -> "HE WARHEAD";
            default -> "RECON PULSE";
        };
        String bottomBar = String.format("PAYLOAD: %s [G TO TRIGGER]  |  [H] HOVER  |  [V] EXIT FPV", payloadName);
        int botW = font.width(bottomBar) + 16;
        gui.fill(cx - botW / 2, height - 24, cx + botW / 2, height - 10, COLOR_BG_PANEL);
        gui.drawCenteredString(font, bottomBar, cx, height - 21, COLOR_NEUTRAL);

        // 7. Progressive Analog RF Interference
        if (signal < 0.80f) {
            renderAnalogInterference(gui, mc, signal, width, height);
        }
    }

    private static void renderDroneEoCameraOptics(GuiGraphics gui, int width, int height) {
        // Cool slate-blue EO sensor wash (clearly distinct from NVG phosphor green)
        gui.fill(0, 0, width, height, 0x1C102436);

        // Subtle EO optical camera scanlines (every 3px)
        for (int y = 0; y < height; y += 3) {
            gui.fill(0, y, width, y + 1, 0x10081420);
        }

        // Soft 4-step optical bezel vignette along screen edges
        int[] alphas = {0x44, 0x2C, 0x18, 0x0A};
        int band = 8;
        for (int i = 0; i < alphas.length; i++) {
            int col = (alphas[i] << 24) | 0x050C14;
            int o = i * band;
            gui.fill(o, o, width - o, o + band, col);
            gui.fill(o, height - o - band, width - o, height - o, col);
            gui.fill(o, o + band, o + band, height - o - band, col);
            gui.fill(width - o - band, o + band, width - o, height - o - band, col);
        }
    }

    private static void renderAnalogInterference(GuiGraphics gui, Minecraft mc, float signalQuality, int width, int height) {
        float degradation = Mth.clamp((0.80f - signalQuality) / 0.80f, 0.0f, 1.0f);
        float severity = degradation * degradation;
        long seedBase = (mc.level != null ? mc.level.getGameTime() * 31L : 0L) + (System.nanoTime() / 18_000_000L);

        // Horizontal CRT scanline static bands
        int bandCount = 4 + Math.round(severity * 24.0f);
        for (int i = 0; i < bandCount; i++) {
            long h = hash(seedBase + i * 7919L);
            int y = (int) (Math.abs(h) % Math.max(1, height));
            int bandH = 1 + (int) (Math.abs(h >> 8) % (2 + Math.round(severity * 5.0f)));
            int xOffset = (int) ((h % 29) * severity);
            int alpha = Math.min(185, (int) ((28 + (Math.abs(h >> 16) % 70)) * (0.25f + severity)));
            int color = (alpha << 24) | (((h & 1L) == 0) ? 0xD8E8F0 : 0x18242C);
            gui.fill(Math.max(0, xOffset), y, Math.min(width, width + xOffset), Math.min(height, y + bandH), color);
        }

        // Pseudo-random snow speckles
        int snowCount = Math.round(severity * 110.0f);
        for (int i = 0; i < snowCount; i++) {
            long h = hash(seedBase * 13L + i * 104729L);
            int sx = (int) (Math.abs(h) % Math.max(1, width));
            int sy = (int) (Math.abs(h >> 16) % Math.max(1, height));
            int sz = 1 + (int) ((h >> 32) & 1L);
            int alpha = Math.min(160, (int) (40 + severity * 110));
            gui.fill(sx, sy, sx + sz, sy + sz, (alpha << 24) | 0xE6F2FF);
        }

        // Blinking degradation warning
        if (severity > 0.22f && ((System.currentTimeMillis() / 320L) % 2L == 0L)) {
            String warn = "// WARNING: LATTICE RF LINK DEGRADED //";
            int warnW = mc.font.width(warn) + 14;
            int wx = (width - warnW) / 2;
            int wy = 26;
            gui.fill(wx, wy, wx + warnW, wy + 13, 0xAA3A0808);
            gui.renderOutline(wx, wy, warnW, 13, COLOR_HOSTILE);
            gui.drawCenteredString(mc.font, warn, width / 2, wy + 2, COLOR_HOSTILE);
        }
    }

    // =========================================================================
    // 3D -> 2D PROJECTION & TACTICAL TARGET MARKERS
    // =========================================================================

    private static void renderProjectedTracks(GuiGraphics gui, Minecraft mc, List<LatticeTrackEntry> tracks,
                                              float partialTicks, int width, int height, boolean drawOffscreenArrows) {
        if (tracks.isEmpty() || mc.gameRenderer == null || mc.level == null) {
            return;
        }
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 camPos = camera.getPosition();
        Vector3f look = camera.getLookVector();
        Vector3f upVec = camera.getUpVector();
        Vector3f leftVec = camera.getLeftVector();

        double fovDeg = 70.0;
        try {
            fovDeg = ((GameRendererAccessor) mc.gameRenderer).vision_tactical_eagleeye$getFov(camera, partialTicks, true);
        } catch (Throwable ignored) {
        }
        float fovRad = (float) Math.toRadians(fovDeg);
        float halfHeight = (float) Math.tan(fovRad * 0.5f);
        float aspectRatio = (float) width / Math.max(1.0f, (float) height);
        float halfWidth = halfHeight * aspectRatio;

        Font font = mc.font;
        int centerX = width / 2;
        int centerY = height / 2;
        int activeFpvId = DroneFpvClientController.isFpvActive() ? DroneFpvClientController.getActiveDroneId() : -1;

        record ProjectedTrack(LatticeTrackEntry entry, int sx, int sy, double distance, int color, double centerDistSq) {}
        java.util.List<ProjectedTrack> projected = new java.util.ArrayList<>(tracks.size());
        ProjectedTrack focused = null;
        double bestCenterDistSq = 58.0 * 58.0;

        for (LatticeTrackEntry track : tracks) {
            if (activeFpvId >= 0 && track.entityId() == activeFpvId) {
                continue;
            }
            Entity liveEntity = mc.level.getEntity(track.entityId());
            if (liveEntity == null || !liveEntity.isAlive()) {
                continue;
            }
            Vec3 interp = liveEntity.getPosition(partialTicks);
            double wx = interp.x;
            double wy = interp.y + liveEntity.getBbHeight() * 0.55;
            double wz = interp.z;

            double relX = wx - camPos.x;
            double relY = wy - camPos.y;
            double relZ = wz - camPos.z;
            double distance = Math.sqrt(relX * relX + relY * relY + relZ * relZ);
            if (distance < 0.8) {
                continue;
            }

            float forward = (float) (relX * look.x + relY * look.y + relZ * look.z);
            float up = (float) (relX * upVec.x + relY * upVec.y + relZ * upVec.z);
            float right = -(float) (relX * leftVec.x + relY * leftVec.y + relZ * leftVec.z);

            int color = switch (track.threatLevel()) {
                case LatticeTrackEntry.THREAT_HOSTILE -> COLOR_HOSTILE;
                case LatticeTrackEntry.THREAT_PLAYER -> COLOR_PLAYER;
                default -> COLOR_NEUTRAL;
            };

            if (forward <= 0.05f) {
                if (drawOffscreenArrows && track.threatLevel() == LatticeTrackEntry.THREAT_HOSTILE) {
                    drawEdgeIndicator(gui, font, right, up, true, width, height, color);
                }
                continue;
            }

            float ndcX = right / (forward * halfWidth);
            float ndcY = up / (forward * halfHeight);

            if (ndcX < -1.05f || ndcX > 1.05f || ndcY < -1.05f || ndcY > 1.05f) {
                if (drawOffscreenArrows && track.threatLevel() == LatticeTrackEntry.THREAT_HOSTILE) {
                    drawEdgeIndicator(gui, font, ndcX, ndcY, false, width, height, color);
                }
                continue;
            }

            int sx = Math.round((ndcX + 1.0f) * 0.5f * width);
            int sy = Math.round((1.0f - ndcY) * 0.5f * height);
            double dx = sx - centerX;
            double dy = sy - centerY;
            double cDistSq = dx * dx + dy * dy;

            ProjectedTrack pt = new ProjectedTrack(track, sx, sy, distance, color, cDistSq);
            projected.add(pt);
            if (cDistSq < bestCenterDistSq) {
                bestCenterDistSq = cDistSq;
                focused = pt;
            }
        }

        // Render peripheral targets first (compact bracket + mini HP bar only — no overlapping text walls)
        for (ProjectedTrack pt : projected) {
            boolean isFocused = (pt == focused);
            int boxHalf = Mth.clamp((int) (130.0 / Math.max(4.0, pt.distance)), 4, isFocused ? 13 : 9);
            int drawColor = isFocused ? pt.color : ((pt.color & 0x00FFFFFF) | 0xCC000000);
            drawBeveledTargetBox(gui, pt.sx, pt.sy, boxHalf, drawColor);

            float hpRatio = Mth.clamp(pt.entry.healthPct(), 0.0f, 1.0f);
            int barW = isFocused ? 18 : 12;
            int barX = pt.sx - barW / 2;
            int barY = pt.sy + boxHalf + 2;
            gui.fill(barX - 1, barY - 1, barX + barW + 1, barY + 3, 0x99060A0F);
            int fillW = Math.max(1, Math.round(barW * hpRatio));
            int hpCol = hpRatio > 0.5f ? 0xFF39FF88 : (hpRatio > 0.25f ? 0xFFFFCC00 : 0xFFFF3B30);
            gui.fill(barX, barY, barX + fillW, barY + 2, hpCol);

            if (isFocused) {
                int hpPct = Math.round(hpRatio * 100.0f);
                String tag = String.format("[UAV] %.1fm | %d%%", pt.distance, hpPct);
                int tagW = font.width(tag);
                int tx = Mth.clamp(pt.sx - tagW / 2, 4, width - tagW - 4);
                int ty = Mth.clamp(pt.sy - boxHalf - 11, 22, height - 12);
                gui.fill(tx - 2, ty - 1, tx + tagW + 2, ty + 9, 0xAA081016);
                gui.drawString(font, tag, tx, ty, pt.color, false);
            }
        }
    }

    private static void drawBeveledTargetBox(GuiGraphics gui, int cx, int cy, int r, int color) {
        int arm = Math.max(3, r / 2);
        // Top-left
        gui.fill(cx - r, cy - r, cx - r + arm, cy - r + 1, color);
        gui.fill(cx - r, cy - r, cx - r + 1, cy - r + arm, color);
        // Top-right
        gui.fill(cx + r - arm + 1, cy - r, cx + r + 1, cy - r + 1, color);
        gui.fill(cx + r, cy - r, cx + r + 1, cy - r + arm, color);
        // Bottom-left
        gui.fill(cx - r, cy + r, cx - r + arm, cy + r + 1, color);
        gui.fill(cx - r, cy + r - arm + 1, cx - r + 1, cy + r + 1, color);
        // Bottom-right
        gui.fill(cx + r - arm + 1, cy + r, cx + r + 1, cy + r + 1, color);
        gui.fill(cx + r, cy + r - arm + 1, cx + r + 1, cy + r + 1, color);
        // Center pip
        gui.fill(cx - 1, cy - 1, cx + 1, cy + 1, color);
    }

    private static void drawEdgeIndicator(GuiGraphics gui, Font font, float dx, float dy, boolean behind,
                                          int width, int height, int color) {
        float dirX = behind ? -dx : dx;
        float dirY = behind ? -Math.abs(dy) - 0.5f : dy;
        float len = (float) Math.hypot(dirX, dirY);
        if (len < 1.0e-4f) {
            dirY = -1.0f;
            len = 1.0f;
        }
        dirX /= len;
        dirY /= len;

        int pad = 18;
        int ex = Mth.clamp(Math.round(width * 0.5f + dirX * (width * 0.5f - pad)), pad, width - pad);
        int ey = Mth.clamp(Math.round(height * 0.5f - dirY * (height * 0.5f - pad)), pad + 14, height - pad);

        String arrow = Math.abs(dirX) > Math.abs(dirY)
                ? (dirX > 0 ? ">" : "<")
                : (dirY > 0 ? "^" : "v");
        gui.fill(ex - 5, ey - 5, ex + 6, ey + 6, 0x991A0606);
        gui.drawCenteredString(font, arrow, ex + 1, ey - 3, color);
    }

    private static void drawCornerBrackets(GuiGraphics gui, int x, int y, int w, int h, int len, int color) {
        gui.fill(x, y, x + len, y + 1, color);
        gui.fill(x, y, x + 1, y + len, color);

        gui.fill(x + w - len, y, x + w, y + 1, color);
        gui.fill(x + w - 1, y, x + w, y + len, color);

        gui.fill(x, y + h - 1, x + len, y + h, color);
        gui.fill(x, y + h - len, x + 1, y + h, color);

        gui.fill(x + w - len, y + h - 1, x + w, y + h, color);
        gui.fill(x + w - 1, y + h - len, x + w, y + h, color);
    }

    private static long hash(long x) {
        x ^= (x << 21);
        x ^= (x >>> 35);
        x ^= (x << 4);
        return x;
    }
}
