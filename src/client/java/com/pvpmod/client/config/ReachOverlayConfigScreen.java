package com.pvpmod.client.config;

import com.pvpmod.client.PvpModClient;
import com.pvpmod.client.stats.HitDistanceTracker;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.nio.file.Path;

/**
 * Tabbed configuration screen for PVP Reach Overlay.
 * Three tabs: "Reach" (overlay + 3D toggle), "Cross" (crosshair color),
 * and "Stats" (hit distance HUD, bing sound, logging).
 */
public class ReachOverlayConfigScreen extends Screen {

    private final Screen parent;
    private final ReachOverlayConfig config;
    private static int activeTab = 0; // static: remembers last open tab

    private static final net.minecraft.util.Identifier DIRT_TEXTURE =
            net.minecraft.util.Identifier.of("minecraft", "textures/gui/options_background.png");

    // Reach tab widgets
    private ColorSlider rRed, rGreen, rBlue, rAlpha;
    private ToleranceSlider rTolerance;

    // Cross tab widgets
    private ColorSlider cRed, cGreen, cBlue, cAlpha;
    private ToleranceSlider cTolerance;

    // Stats tab widgets
    private ThresholdSlider sThreshold;
    private boolean clearConfirm = false; // two-click safety for Clear Stats
    private ButtonWidget clearButton = null; // reference for click-outside reset

    // Log link click area tracking
    private int logLinkX, logLinkY, logLinkW, logLinkH;
    private boolean logLinkVisible = false;

    public ReachOverlayConfigScreen(Screen parent) {
        super(Text.translatable("iwanttohitfromthreeblocks.config.title"));
        this.parent = parent;
        this.config = PvpModClient.getConfig();
    }

    @Override
    protected void init() {
        clearChildren();
        int cx = this.width / 2;
        int w = 200;
        int tabW = 80;

        // ── Tab buttons (top-left) ──
        addDrawableChild(ButtonWidget.builder(
                Text.literal(activeTab == 0 ? "§a▶ Reach" : "  Reach"),
                btn -> { activeTab = 0; init(); }
        ).dimensions(4, 4, tabW, 20).build());

        addDrawableChild(ButtonWidget.builder(
                Text.literal(activeTab == 1 ? "§a▶ Cross" : "  Cross"),
                btn -> { activeTab = 1; init(); }
        ).dimensions(4 + tabW + 4, 4, tabW, 20).build());

        addDrawableChild(ButtonWidget.builder(
                Text.literal(activeTab == 2 ? "§a▶ Stats" : "  Stats"),
                btn -> { activeTab = 2; init(); }
        ).dimensions(4 + (tabW + 4) * 2, 4, tabW, 20).build());

        if (activeTab == 0) {
            initReachTab(cx, w);
        } else if (activeTab == 1) {
            initCrossTab(cx, w);
        } else {
            initStatsTab(cx, w);
        }
    }

    // ── Reach tab ──────────────────────────────────────────────────

    private void initReachTab(int cx, int w) {
        int y = 36;
        int sp = 24;

        addDrawableChild(ButtonWidget.builder(
                Text.literal("Overlay: " + (config.isEnabled() ? "ON" : "OFF")),
                btn -> {
                    config.setEnabled(!config.isEnabled());
                    btn.setMessage(Text.literal("Overlay: " + (config.isEnabled() ? "ON" : "OFF")));
                }
        ).dimensions(cx - w / 2, y, w, 20).build());
        y += sp;

        // 2D/3D mode toggle (with translation key indicator)
        addDrawableChild(ButtonWidget.builder(
                Text.translatable(config.isReach3DEnabled()
                        ? "iwanttohitfromthreeblocks.config.mode3D"
                        : "iwanttohitfromthreeblocks.config.mode2D"),
                btn -> {
                    config.setReach3DEnabled(!config.isReach3DEnabled());
                    btn.setMessage(Text.translatable(config.isReach3DEnabled()
                            ? "iwanttohitfromthreeblocks.config.mode3D"
                            : "iwanttohitfromthreeblocks.config.mode2D"));
                }
        ).dimensions(cx - w / 2, y, w, 20).build());
        y += sp;

        rRed = addSlider(cx, y, w, "Red", config.getOverlayRed()); y += sp;
        rGreen = addSlider(cx, y, w, "Green", config.getOverlayGreen()); y += sp;
        rBlue = addSlider(cx, y, w, "Blue", config.getOverlayBlue()); y += sp;
        rAlpha = addSlider(cx, y, w, "Alpha", config.getOverlayAlpha()); y += sp;

        rTolerance = addTolSlider(cx, y, w, config.getReachTolerance(), 0.25f); y += 38;

        addDrawableChild(ButtonWidget.builder(Text.literal("§c✖ Reset to Default"), btn -> {
            rRed.resetTo(0); rGreen.resetTo(255); rBlue.resetTo(0); rAlpha.resetTo(50);
            rTolerance.resetTo(0.00f);
        }).dimensions(cx - w / 2, y, w, 20).build());
        y += sp;

        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), btn -> close())
                .dimensions(cx - w / 2, y, w, 20).build());
    }

    // ── Cross tab ──────────────────────────────────────────────────

    private void initCrossTab(int cx, int w) {
        int y = 36;
        int sp = 24;

        addDrawableChild(ButtonWidget.builder(
                Text.literal("Cross Color: " + (config.isCrossColorEnabled() ? "ON" : "OFF")),
                btn -> {
                    config.setCrossColorEnabled(!config.isCrossColorEnabled());
                    btn.setMessage(Text.literal("Cross Color: " + (config.isCrossColorEnabled() ? "ON" : "OFF")));
                }
        ).dimensions(cx - w / 2, y, w, 20).build());
        y += sp;

        cRed = addSlider(cx, y, w, "Red", config.getCrossRed()); y += sp;
        cGreen = addSlider(cx, y, w, "Green", config.getCrossGreen()); y += sp;
        cBlue = addSlider(cx, y, w, "Blue", config.getCrossBlue()); y += sp;
        cAlpha = addSlider(cx, y, w, "Alpha", config.getCrossAlpha()); y += sp;

        cTolerance = addTolSlider(cx, y, w, config.getCrossTolerance(), 0.60f); y += 38;

        addDrawableChild(ButtonWidget.builder(Text.literal("§c✖ Reset to Default"), btn -> {
            cRed.resetTo(255); cGreen.resetTo(50); cBlue.resetTo(50); cAlpha.resetTo(255);
            cTolerance.resetTo(0.00f);
        }).dimensions(cx - w / 2, y, w, 20).build());
        y += sp;

        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), btn -> close())
                .dimensions(cx - w / 2, y, w, 20).build());
    }

    // ── Stats tab ──────────────────────────────────────────────────

    private void initStatsTab(int cx, int w) {
        int y = 36;
        int sp = 24;

        // HUD toggle
        addDrawableChild(ButtonWidget.builder(
                Text.literal("Hit Distance HUD: " + (config.isStatsHudEnabled() ? "ON" : "OFF")),
                btn -> {
                    config.setStatsHudEnabled(!config.isStatsHudEnabled());
                    btn.setMessage(Text.literal("Hit Distance HUD: " + (config.isStatsHudEnabled() ? "ON" : "OFF")));
                }
        ).dimensions(cx - w / 2, y, w, 20).build());
        y += sp;

        // Bing sound toggle
        addDrawableChild(ButtonWidget.builder(
                Text.literal("Bing Sound: " + (config.isBingSoundEnabled() ? "ON" : "OFF")),
                btn -> {
                    config.setBingSoundEnabled(!config.isBingSoundEnabled());
                    btn.setMessage(Text.literal("Bing Sound: " + (config.isBingSoundEnabled() ? "ON" : "OFF")));
                }
        ).dimensions(cx - w / 2, y, w, 20).build());
        y += sp;

        // Threshold slider (2.25 - 3.00)
        sThreshold = new ThresholdSlider(cx - w / 2, y, w, 20, config.getBingSoundThreshold());
        addDrawableChild(sThreshold);
        y += 38;

        // Hit Text toggle (crosshair hit distance text)
        addDrawableChild(ButtonWidget.builder(
                Text.literal("Hit Text: " + (config.isHitTextEnabled() ? "§aON" : "§cOFF")),
                btn -> {
                    config.setHitTextEnabled(!config.isHitTextEnabled());
                    btn.setMessage(Text.literal("Hit Text: " + (config.isHitTextEnabled() ? "§aON" : "§cOFF")));
                }
        ).dimensions(cx - w / 2, y, w, 20).build());
        y += sp;

        // Hit logging toggle (default OFF for privacy)
        addDrawableChild(ButtonWidget.builder(
                Text.literal("Hit Logging: " + (config.isHitLoggingEnabled() ? "§aON" : "§cOFF")),
                btn -> {
                    config.setHitLoggingEnabled(!config.isHitLoggingEnabled());
                    btn.setMessage(Text.literal("Hit Logging: " + (config.isHitLoggingEnabled() ? "§aON" : "§cOFF")));
                }
        ).dimensions(cx - w / 2, y, w, 20).build());
        y += sp;

        // Edit HUD position button
        addDrawableChild(ButtonWidget.builder(
                Text.literal("§e✎ Edit HUD Position"),
                btn -> this.client.setScreen(new HitDistanceEditScreen(this))
        ).dimensions(cx - w / 2, y, w, 20).build());
        y += sp;

        // Clear stats button (two-click confirmation)
        clearButton = ButtonWidget.builder(
                Text.literal(clearConfirm ? "§c⚠ Emin misiniz? (Tekrar basın)" : "§c✖ Clear Stats"),
                btn -> {
                    if (!clearConfirm) {
                        clearConfirm = true;
                        btn.setMessage(Text.literal("§c⚠ Emin misiniz? (Tekrar basın)"));
                    } else {
                        if (PvpModClient.getHitTracker() != null) {
                            PvpModClient.getHitTracker().getLogger().clearSession();
                        }
                        clearConfirm = false;
                        btn.setMessage(Text.literal("§c✖ Clear Stats"));
                    }
                }
        ).dimensions(cx - w / 2, y, w, 20).build();
        addDrawableChild(clearButton);
        y += sp + 4;

        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), btn -> close())
                .dimensions(cx - w / 2, y, w, 20).build());
    }

    // ── Render ──────────────────────────────────────────────────────

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        if (this.client != null && this.client.world != null) {
            ctx.fill(0, 0, this.width, this.height, 0x44000000);
        } else {
            int tileSize = 32;
            for (int tx = 0; tx < this.width; tx += tileSize) {
                for (int ty = 0; ty < this.height; ty += tileSize) {
                    ctx.drawTexture(net.minecraft.client.render.RenderLayer::getGuiTextured, DIRT_TEXTURE, tx, ty, 0.0f, 0.0f, tileSize, tileSize, tileSize, tileSize);
                }
            }
            ctx.fill(0, 0, this.width, this.height, 0xAA000000);
        }
        
        // Render widgets (buttons, sliders, etc.)
        super.render(ctx, mx, my, delta);

        // Stats tab: show session statistics on the right side
        // Drawn AFTER super.render() so it isn't affected by the 1.21 background blur
        if (activeTab == 2 && PvpModClient.getHitTracker() != null) {
            int sx = this.width / 2 + 115; // anchored to buttons' right edge
            int sy = 40;

            ctx.drawTextWithShadow(this.textRenderer, "§e§lSession Stats", sx, sy, 0xFFFFFF);
            sy += 14;

            int hits = PvpModClient.getHitTracker().getLogger().getSessionHitCount();
            String hitsDisplay = PvpModClient.getHitTracker().getLogger().getHitCountDisplay();
            double avg = PvpModClient.getHitTracker().getLogger().getSessionAverage();

            ctx.drawTextWithShadow(this.textRenderer,
                    "Hits: §f" + hitsDisplay, sx, sy, 0xAAAAAA);
            sy += 12;
            ctx.drawTextWithShadow(this.textRenderer,
                    String.format("Avg: §a%.2f", avg), sx, sy, 0xAAAAAA);
            sy += 12;
            double maxHit = PvpModClient.getHitTracker().getLogger().getSessionMaxDistance();
            ctx.drawTextWithShadow(this.textRenderer,
                    String.format("Max Hit Reach: §f%.2f", maxHit),
                    sx, sy, 0xAAAAAA);
            sy += 16;

            // Rank display
            if (hits > 0) {
                String rankLetter = HitDistanceTracker.getRankLetter(avg);
                ctx.drawTextWithShadow(this.textRenderer, "Rank:", sx, sy, 0xAAAAAA);
                int rankX = sx + this.textRenderer.getWidth("Rank: ");
                if ("S".equals(rankLetter)) {
                    int rainbow = HitDistanceTracker.getSRankRainbowColor();
                    ctx.drawTextWithShadow(this.textRenderer, "§l" + rankLetter, rankX + 1, sy, rainbow);
                    ctx.drawTextWithShadow(this.textRenderer, "§l" + rankLetter, rankX, sy, rainbow);
                } else {
                    int rankColor = HitDistanceTracker.getRankColor(avg);
                    ctx.drawTextWithShadow(this.textRenderer, rankLetter, rankX, sy, rankColor);
                }
            } else {
                ctx.drawTextWithShadow(this.textRenderer, "Rank: §7-", sx, sy, 0xAAAAAA);
            }
            sy += 16;

            // Logging status warning
            if (!config.isHitLoggingEnabled()) {
                String[] lines = Text.translatable("iwanttohitfromthreeblocks.config.loggingOff")
                        .getString().split("\n");
                for (String line : lines) {
                    ctx.drawTextWithShadow(this.textRenderer, line, sx, sy, 0xAAAAAA);
                    sy += 10;
                }
                logLinkVisible = false;
            } else {
                // Clickable "open logs" text
                String logText = Text.translatable("iwanttohitfromthreeblocks.config.openLogs").getString();
                int availableW = this.width - sx - 4;
                // Truncate with ellipsis if text overflows
                if (this.textRenderer.getWidth(logText) > availableW) {
                    while (logText.length() > 3 && this.textRenderer.getWidth(logText + "...") > availableW) {
                        logText = logText.substring(0, logText.length() - 1);
                    }
                    logText = logText + "...";
                }
                logLinkX = sx;
                logLinkY = sy;
                logLinkW = this.textRenderer.getWidth(logText);
                logLinkH = this.textRenderer.fontHeight;
                logLinkVisible = true;

                boolean hovered = mx >= logLinkX && mx <= logLinkX + logLinkW
                        && my >= logLinkY && my <= logLinkY + logLinkH;
                int color = hovered ? 0xAABBFF : 0x666666;

                ctx.drawTextWithShadow(this.textRenderer, logText, sx, sy, color);
                // Underline when hovered
                if (hovered) {
                    ctx.fill(sx, sy + logLinkH, sx + logLinkW, sy + logLinkH + 1, 0xAABBFF | (0x88 << 24));
                }
            }
        }

        // Tolerance description
        if (activeTab == 0 || activeTab == 1) {
            ToleranceSlider ts = activeTab == 0 ? rTolerance : cTolerance;
            if (ts != null) {
                ctx.drawCenteredTextWithShadow(this.textRenderer,
                        Text.translatable("iwanttohitfromthreeblocks.config.toleranceDesc"),
                        this.width / 2, ts.getY() + 22, 0x888888);
            }
        }

        // Color preview AFTER super.render() — drawn on top of everything
        // Uses opaque background so it's visible on both dirt (main menu) and in-game screens
        if (activeTab == 0 || activeTab == 1) {
            ColorSlider sr = activeTab == 0 ? rRed : cRed;
            ColorSlider sg = activeTab == 0 ? rGreen : cGreen;
            ColorSlider sb = activeTab == 0 ? rBlue : cBlue;
            ColorSlider sa = activeTab == 0 ? rAlpha : cAlpha;

            if (sr != null) {
                int pvX = this.width / 2 + 110, pvY = 60, pvS = 70;
                int r = (int) sr.getVal(), g = (int) sg.getVal();
                int b = (int) sb.getVal(), a = (int) sa.getVal();

                // Background for alpha preview (solid dark gray instead of checkerboard)
                ctx.fill(pvX, pvY, pvX + pvS, pvY + pvS, 0xFF333333);
                
                
                // Preview color on top
                int col = (a << 24) | (r << 16) | (g << 8) | b;
                ctx.fill(pvX, pvY, pvX + pvS, pvY + pvS, col);
                // White border
                this.drawBorder(ctx, pvX, pvY, pvS, pvS, 0xFFFFFFFF);
                ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Preview"),
                        pvX + pvS / 2, pvY - 11, 0xAAAAAA);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Reset clearConfirm if clicking anywhere other than the clear button
        if (clearConfirm && clearButton != null && !clearButton.isMouseOver(mouseX, mouseY)) {
            clearConfirm = false;
            clearButton.setMessage(Text.literal("§c✖ Clear Stats"));
        }

        // Handle log link click
        if (logLinkVisible && button == 0
                && mouseX >= logLinkX && mouseX <= logLinkX + logLinkW
                && mouseY >= logLinkY && mouseY <= logLinkY + logLinkH) {
            Path logDir = FabricLoader.getInstance().getGameDir().resolve("IWTHFTBlocks-log");
            Util.getOperatingSystem().open(logDir.toFile());
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void drawBorder(DrawContext ctx, int x, int y, int width, int height, int color) {
        ctx.fill(x, y, x + width, y + 1, color);
        ctx.fill(x, y + height - 1, x + width, y + height, color);
        ctx.fill(x, y + 1, x + 1, y + height - 1, color);
        ctx.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }

    @Override
    public void close() {
        // Save reach tab values if they exist
        if (rRed != null) {
            config.setOverlayRed((int) rRed.getVal());
            config.setOverlayGreen((int) rGreen.getVal());
            config.setOverlayBlue((int) rBlue.getVal());
            config.setOverlayAlpha((int) rAlpha.getVal());
            config.setReachTolerance(rTolerance.getFloatVal());
        }
        // Save cross tab values if they exist
        if (cRed != null) {
            config.setCrossRed((int) cRed.getVal());
            config.setCrossGreen((int) cGreen.getVal());
            config.setCrossBlue((int) cBlue.getVal());
            config.setCrossAlpha((int) cAlpha.getVal());
            config.setCrossTolerance(cTolerance.getFloatVal());
        }
        // Save stats tab values if they exist
        if (sThreshold != null) {
            config.setBingSoundThreshold(sThreshold.getFloatVal());
        }
        config.save();
        this.client.setScreen(parent);
    }

    // ── Helper: add color slider ────────────────────────────────────

    private ColorSlider addSlider(int cx, int y, int w, String label, int val) {
        ColorSlider s = new ColorSlider(cx - w / 2, y, w, 20, label, val);
        addDrawableChild(s);
        return s;
    }

    private ToleranceSlider addTolSlider(int cx, int y, int w, float val, float max) {
        ToleranceSlider s = new ToleranceSlider(cx - w / 2, y, w, 20, val, max);
        addDrawableChild(s);
        return s;
    }

    // ── Color slider (0-255) ────────────────────────────────────────

    private static class ColorSlider extends SliderWidget {
        private final String label;
        public ColorSlider(int x, int y, int w, int h, String label, int init) {
            super(x, y, w, h, Text.literal(label + ": " + init), init / 255.0);
            this.label = label;
        }
        @Override protected void updateMessage() {
            setMessage(Text.literal(label + ": " + (int) getVal()));
        }
        @Override protected void applyValue() {}
        public double getVal() { return Math.round(this.value * 255); }
        public void resetTo(int v) { this.value = v / 255.0; updateMessage(); }
    }

    // ── Tolerance slider (configurable max) ────────────────────────

    private static class ToleranceSlider extends SliderWidget {
        private final float max;
        public ToleranceSlider(int x, int y, int w, int h, float init, float max) {
            super(x, y, w, h,
                    Text.literal("Tolerance: " + String.format("0.%02d", Math.round(init * 100))),
                    init / max);
            this.max = max;
        }
        @Override protected void updateMessage() {
            setMessage(Text.literal("Tolerance: " + String.format("0.%02d", Math.round(getFloatVal() * 100))));
        }
        @Override protected void applyValue() {}
        public float getFloatVal() { return (float) (this.value * max); }
        public void resetTo(float v) { this.value = v / max; updateMessage(); }
    }

    // ── Threshold slider (2.25 - 3.00) ─────────────────────────────

    private static class ThresholdSlider extends SliderWidget {
        private static final float MIN = 2.25f;
        private static final float MAX = 3.00f;
        public ThresholdSlider(int x, int y, int w, int h, float init) {
            super(x, y, w, h,
                    Text.literal("Sound Threshold: " + String.format("%.2f", init)),
                    (init - MIN) / (MAX - MIN));
        }
        @Override protected void updateMessage() {
            setMessage(Text.literal("Sound Threshold: " + String.format("%.2f", getFloatVal())));
        }
        @Override protected void applyValue() {}
        public float getFloatVal() { return (float) (MIN + this.value * (MAX - MIN)); }
        public void resetTo(float v) { this.value = (v - MIN) / (MAX - MIN); updateMessage(); }
    }
}
