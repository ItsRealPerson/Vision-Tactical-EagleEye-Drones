package dev.itsrealperson.vision_tactical_eagleeye.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.shaders.Uniform;
import dev.itsrealperson.vision_goggles.client.ThermalEntityPainter;
import dev.itsrealperson.vision_tactical_eagleeye.VisionTacticalEagleEye;
import dev.itsrealperson.vision_tactical_eagleeye.mixin.client.PostChainAccessor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;

import java.util.function.IntSupplier;

/**
 * Dedicated GLSL PostChain manager for the Tactical Drone's military EO-IR camera feed.
 * Runs exclusively while FPV is active, independent of Vision Goggles.
 */
@Environment(EnvType.CLIENT)
public final class DroneEoShaderManager {
    private static final ResourceLocation UAV_EO_SHADER =
            new ResourceLocation(VisionTacticalEagleEye.MOD_ID, "shaders/post/uav_eo.json");
    private static final IntSupplier THERMAL_MASK_SUPPLIER = ThermalEntityPainter::getMaskTextureId;

    private static PostChain eoChain = null;
    private static int lastWidth = -1;
    private static int lastHeight = -1;
    private static long lastFailTimeMs = 0L;

    private DroneEoShaderManager() {
    }

    public static void processIfActive(float partialTick) {
        if (!DroneFpvClientController.isFpvActive()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }

        int w = mc.getWindow().getWidth();
        int h = mc.getWindow().getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }

        if (eoChain == null) {
            long now = System.currentTimeMillis();
            if (now - lastFailTimeMs < 5000L) {
                return;
            }
            try {
                eoChain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), mc.getMainRenderTarget(), UAV_EO_SHADER);
                eoChain.resize(w, h);
                lastWidth = w;
                lastHeight = h;
            } catch (Exception e) {
                VisionTacticalEagleEye.LOGGER.warn("[EagleEye] Failed to load UAV EO PostChain: {}", e.getMessage());
                lastFailTimeMs = now;
                eoChain = null;
                return;
            }
        }

        if (w != lastWidth || h != lastHeight) {
            try {
                eoChain.resize(w, h);
                lastWidth = w;
                lastHeight = h;
            } catch (Exception e) {
                evictChain();
                return;
            }
        }

        try {
            float timeSec = (mc.level.getGameTime() + partialTick) / 20.0f;
            float signal = DroneFpvClientController.getPrimarySignal();
            for (PostPass pass : ((PostChainAccessor) eoChain).vision_tactical_eagleeye$getPasses()) {
                pass.getEffect().setSampler("ThermalMask", THERMAL_MASK_SUPPLIER);
                Uniform uTime = pass.getEffect().getUniform("Time");
                if (uTime != null) {
                    uTime.set(timeSec);
                }
                Uniform uSig = pass.getEffect().getUniform("Signal");
                if (uSig != null) {
                    uSig.set(signal);
                }
            }

            RenderSystem.disableBlend();
            RenderSystem.disableDepthTest();
            RenderSystem.resetTextureMatrix();
            eoChain.process(partialTick);
        } catch (Exception e) {
            VisionTacticalEagleEye.LOGGER.warn("[EagleEye] UAV EO PostChain render error: {}", e.getMessage());
            evictChain();
        } finally {
            mc.getMainRenderTarget().bindWrite(false);
        }
    }

    private static void evictChain() {
        if (eoChain != null) {
            try {
                eoChain.close();
            } catch (Exception ignored) {
            }
            eoChain = null;
        }
        lastFailTimeMs = System.currentTimeMillis();
    }
}
