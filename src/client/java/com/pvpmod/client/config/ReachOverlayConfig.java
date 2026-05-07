package com.pvpmod.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pvpmod.client.PvpModClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Configuration for the PVP Reach Overlay mod.
 * Stored as JSON in the config directory.
 */
public class ReachOverlayConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE = "iwanttohitfromthreeblocks.json";

    // ── Reach overlay fields ───────────────────────────────────────

    private boolean enabled = true;
    private int overlayRed = 0;
    private int overlayGreen = 255;
    private int overlayBlue = 0;
    private int overlayAlpha = 50;
    private float reachTolerance = 0.00f;

    // ── Cross color fields ─────────────────────────────────────────

    private boolean crossColorEnabled = true;
    private int crossRed = 255;
    private int crossGreen = 50;
    private int crossBlue = 50;
    private int crossAlpha = 255;
    private float crossTolerance = 0.00f;

    // ── Surface offset ─────────────────────────────────────────────

    private float surfaceOffset = 0.005f;

    // ── 3D Reach display ───────────────────────────────────────────

    private boolean reach3DEnabled = false;

    // ── Stats HUD fields ───────────────────────────────────────────

    private boolean statsHudEnabled = true;
    private float hudX = 0.05f;       // screen percentage (0.0 - 1.0)
    private float hudY = 0.85f;
    private float hudScale = 1.0f;
    private boolean bingSoundEnabled = true;
    private float bingSoundThreshold = 2.25f; // 2.25 - 3.00
    private boolean hitLoggingEnabled = false; // default OFF for privacy
    private boolean hitTextEnabled = true; // crosshair hit distance text

    // ── Reach overlay getters/setters ──────────────────────────────

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean v) { this.enabled = v; }

    public int getOverlayRed() { return overlayRed; }
    public void setOverlayRed(int v) { this.overlayRed = clamp(v, 0, 255); }

    public int getOverlayGreen() { return overlayGreen; }
    public void setOverlayGreen(int v) { this.overlayGreen = clamp(v, 0, 255); }

    public int getOverlayBlue() { return overlayBlue; }
    public void setOverlayBlue(int v) { this.overlayBlue = clamp(v, 0, 255); }

    public int getOverlayAlpha() { return overlayAlpha; }
    public void setOverlayAlpha(int v) { this.overlayAlpha = clamp(v, 0, 255); }

    public float getReachTolerance() { return reachTolerance; }
    public void setReachTolerance(float v) { this.reachTolerance = Math.max(0f, Math.min(0.25f, v)); }

    // ── Cross color getters/setters ────────────────────────────────

    public boolean isCrossColorEnabled() { return crossColorEnabled; }
    public void setCrossColorEnabled(boolean v) { this.crossColorEnabled = v; }

    public int getCrossRed() { return crossRed; }
    public void setCrossRed(int v) { this.crossRed = clamp(v, 0, 255); }

    public int getCrossGreen() { return crossGreen; }
    public void setCrossGreen(int v) { this.crossGreen = clamp(v, 0, 255); }

    public int getCrossBlue() { return crossBlue; }
    public void setCrossBlue(int v) { this.crossBlue = clamp(v, 0, 255); }

    public int getCrossAlpha() { return crossAlpha; }
    public void setCrossAlpha(int v) { this.crossAlpha = clamp(v, 0, 255); }

    public float getCrossTolerance() { return crossTolerance; }
    public void setCrossTolerance(float v) { this.crossTolerance = Math.max(0f, Math.min(0.60f, v)); }

    public float getSurfaceOffset() { return surfaceOffset; }

    // ── 3D Reach getters/setters ──────────────────────────────────

    public boolean isReach3DEnabled() { return reach3DEnabled; }
    public void setReach3DEnabled(boolean v) { this.reach3DEnabled = v; }

    // ── Stats HUD getters/setters ─────────────────────────────────

    public boolean isStatsHudEnabled() { return statsHudEnabled; }
    public void setStatsHudEnabled(boolean v) { this.statsHudEnabled = v; }

    public float getHudX() { return hudX; }
    public void setHudX(float v) { this.hudX = Math.max(0f, Math.min(1f, v)); }

    public float getHudY() { return hudY; }
    public void setHudY(float v) { this.hudY = Math.max(0f, Math.min(1f, v)); }

    public float getHudScale() { return hudScale; }
    public void setHudScale(float v) { this.hudScale = Math.max(0.5f, Math.min(3.0f, v)); }

    public boolean isBingSoundEnabled() { return bingSoundEnabled; }
    public void setBingSoundEnabled(boolean v) { this.bingSoundEnabled = v; }

    public float getBingSoundThreshold() { return bingSoundThreshold; }
    public void setBingSoundThreshold(float v) { this.bingSoundThreshold = Math.max(2.25f, Math.min(3.0f, v)); }

    public boolean isHitLoggingEnabled() { return hitLoggingEnabled; }
    public void setHitLoggingEnabled(boolean v) { this.hitLoggingEnabled = v; }

    public boolean isHitTextEnabled() { return hitTextEnabled; }
    public void setHitTextEnabled(boolean v) { this.hitTextEnabled = v; }

    // ── Helpers ────────────────────────────────────────────────────

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    // ── Persistence ────────────────────────────────────────────────

    private static Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);
    }

    public static ReachOverlayConfig load() {
        Path path = getConfigPath();
        if (Files.exists(path)) {
            try {
                String json = Files.readString(path);
                ReachOverlayConfig cfg = GSON.fromJson(json, ReachOverlayConfig.class);
                if (cfg != null) {
                    PvpModClient.LOGGER.info("[PVP Reach Overlay] Config loaded.");
                    return cfg;
                }
            } catch (IOException | com.google.gson.JsonSyntaxException e) {
                PvpModClient.LOGGER.warn("[PVP Reach Overlay] Config load failed, using defaults.", e);
            }
        }
        ReachOverlayConfig cfg = new ReachOverlayConfig();
        cfg.save();
        return cfg;
    }

    public void save() {
        Path path = getConfigPath();
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(this));
        } catch (IOException e) {
            PvpModClient.LOGGER.error("[PVP Reach Overlay] Config save failed.", e);
        }
    }
}
