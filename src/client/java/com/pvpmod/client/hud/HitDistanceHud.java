package com.pvpmod.client.hud;

import com.pvpmod.client.config.ReachOverlayConfig;
import com.pvpmod.client.stats.HitDistanceTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * Renders a holographic hit-distance display on the HUD.
 * Shows "X.XX block" and "avg: X.XX (count)" with translucent background.
 * Always visible when enabled.
 */
public class HitDistanceHud {

    private final ReachOverlayConfig config;
    private final HitDistanceTracker tracker;

    public HitDistanceHud(ReachOverlayConfig config, HitDistanceTracker tracker) {
        this.config = config;
        this.tracker = tracker;
    }

    public void render(DrawContext ctx, float tickDelta) {
        if (!config.isStatsHudEnabled()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        // Hide when F3 debug overlay is active
        if (client.getDebugHud().shouldShowDebugHud()) return;

        TextRenderer textRenderer = client.textRenderer;
        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();

        double distance = tracker.getLastHitDistance();
        if (distance < 0) distance = 0.0;

        float scale = config.getHudScale();
        int posX = (int)(config.getHudX() * screenW);
        int posY = (int)(config.getHudY() * screenH);

        // Build texts
        String blockWord = Text.translatable("iwanttohitfromthreeblocks.hud.block").getString();
        String displayText = String.format("%.2f %s", distance, blockWord);

        String hitCountStr = tracker.getLogger().getHitCountDisplay();
        double avg = tracker.getLogger().getSessionAverage();
        String avgText = String.format("avg: %.2f (%s)", avg, hitCountStr);

        ctx.getMatrices().push();
        ctx.getMatrices().translate(posX, posY, 0);
        ctx.getMatrices().scale(scale, scale, 1.0f);

        int textHeight = textRenderer.fontHeight;
        int line1W = textRenderer.getWidth(displayText);
        int line2W = textRenderer.getWidth(avgText);
        int maxTextWidth = Math.max(line1W, line2W);

        int padX = 6, padY = 4;
        int lineSpacing = 4;
        int panelW = maxTextWidth + padX * 2;
        int panelH = padY * 2 + textHeight * 2 + lineSpacing;

        // Use Reach overlay color for the border
        int r = config.getOverlayRed();
        int g = config.getOverlayGreen();
        int b = config.getOverlayBlue();
        // Use a slightly higher alpha for visibility, or max of current alpha and 150
        int a = Math.max(150, config.getOverlayAlpha());
        int borderColor = (a << 24) | (r << 16) | (g << 8) | b;

        // Background + border (origin is 0,0 now)
        ctx.fill(0, 0, panelW, panelH, 0x8C000000);
        this.drawBorder(ctx, 0, 0, panelW, panelH, borderColor);

        // Line 1: distance (white)
        ctx.drawTextWithShadow(textRenderer, displayText, padX, padY, 0xFFFFFFFF);

        // Line 2: avg (gray)
        ctx.drawTextWithShadow(textRenderer, avgText, padX, padY + textHeight + lineSpacing, 0xFFAAAAAA);

        ctx.getMatrices().pop();
    }

    private void drawBorder(DrawContext ctx, int x, int y, int width, int height, int color) {
        ctx.fill(x, y, x + width, y + 1, color);
        ctx.fill(x, y + height - 1, x + width, y + height, color);
        ctx.fill(x, y + 1, x + 1, y + height - 1, color);
        ctx.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }
}
