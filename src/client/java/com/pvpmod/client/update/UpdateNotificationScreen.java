package com.pvpmod.client.update;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.net.URI;

/**
 * Full-screen notification shown when a mod update is available.
 * Displayed once per game session on the title screen.
 *
 * Layout:
 * - Dirt texture background (tiled manually, avoids 1.21 blur)
 * - Dark tint overlay (loading screen look)
 * - Centered panel with version info
 * - "Yes" button → opens download URL in browser
 * - "No" button → returns to title screen
 */
public class UpdateNotificationScreen extends Screen {

    private static final Identifier DIRT_TEXTURE =
            Identifier.of("minecraft", "textures/gui/options_background.png");

    private final Screen parent;
    private final String currentVersion;
    private final String latestVersion;
    private final String downloadUrl;

    // Panel dimensions
    private static final int PANEL_W = 260;
    private static final int PANEL_H = 120;

    public UpdateNotificationScreen(Screen parent, String currentVersion, String latestVersion, String downloadUrl) {
        super(Text.translatable("iwanttohitfromthreeblocks.update.title"));
        this.parent = parent;
        this.currentVersion = currentVersion;
        this.latestVersion = latestVersion;
        this.downloadUrl = downloadUrl;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;
        int panelTop = cy - PANEL_H / 2;
        int btnY = panelTop + PANEL_H - 30;
        int btnW = 100;
        int gap = 10;

        // "Yes" button — opens download page
        addDrawableChild(ButtonWidget.builder(
                Text.translatable("iwanttohitfromthreeblocks.update.yes"),
                btn -> {
                    try {
                        Util.getOperatingSystem().open(URI.create(downloadUrl));
                    } catch (Exception ignored) {}
                    this.client.setScreen(parent);
                }
        ).dimensions(cx - btnW - gap / 2, btnY, btnW, 20).build());

        // "No" button — dismiss
        addDrawableChild(ButtonWidget.builder(
                Text.translatable("iwanttohitfromthreeblocks.update.no"),
                btn -> this.client.setScreen(parent)
        ).dimensions(cx + gap / 2, btnY, btnW, 20).build());
    }

    /**
     * Override renderBackground to prevent 1.21 blur.
     * We draw the dirt texture manually in render() instead.
     */
    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Intentionally empty — prevents 1.21 blur
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        // Step 1: Tile dirt texture manually (avoids 1.21 blur entirely)
        int tileSize = 16;
        for (int tx = 0; tx < this.width; tx += tileSize) {
            for (int ty = 0; ty < this.height; ty += tileSize) {
                ctx.drawTexture(net.minecraft.client.render.RenderLayer::getGuiTextured, DIRT_TEXTURE, tx, ty, 0.0f, 0.0f, tileSize, tileSize, tileSize, tileSize);
            }
        }

        // Step 2: Dark tint overlay (loading screen look)
        ctx.fill(0, 0, this.width, this.height, 0xAA000000);

        // Step 3: Panel background
        int cx = this.width / 2;
        int cy = this.height / 2;
        int panelLeft = cx - PANEL_W / 2;
        int panelTop = cy - PANEL_H / 2;

        ctx.fill(panelLeft, panelTop, panelLeft + PANEL_W, panelTop + PANEL_H, 0xE0202020);
        drawPanelBorder(ctx, panelLeft, panelTop, PANEL_W, PANEL_H, 0xFF555555);

        // Step 4: Render buttons (via super.render which calls renderBackground=empty, then widgets)
        super.render(ctx, mx, my, delta);

        // Step 5: Draw text ON TOP of everything
        String titleText = Text.translatable("iwanttohitfromthreeblocks.update.message").getString();
        ctx.drawCenteredTextWithShadow(this.textRenderer, titleText, cx, panelTop + 12, 0xFFFFAA00);

        String versionText = "v" + currentVersion + " §e→§r v" + latestVersion;
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal(versionText), cx, panelTop + 28, 0xFFFFFFFF);

        String questionText = Text.translatable("iwanttohitfromthreeblocks.update.question").getString();
        ctx.drawCenteredTextWithShadow(this.textRenderer, questionText, cx, panelTop + 48, 0xFFCCCCCC);
    }

    private void drawPanelBorder(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y + 1, x + 1, y + h - 1, color);
        ctx.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }
}
