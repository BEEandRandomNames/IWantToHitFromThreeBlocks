package com.pvpmod.client.config;

import com.pvpmod.client.PvpModClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * Screen for positioning and resizing the Hit Distance HUD element.
 * Visible boundary covers full screen including Done button area.
 * Pistachio green corner handles indicate resize zones.
 */
public class HitDistanceEditScreen extends Screen {

    private final Screen parent;
    private final ReachOverlayConfig config;

    private int hudPixelX, hudPixelY;
    private float hudScale;

    private boolean dragging = false;
    private int resizeCorner = 0; // 0=None, 1=TL, 2=TR, 3=BL, 4=BR
    private int dragOffsetX, dragOffsetY;

    // Context Menu widgets
    private boolean contextMenuOpen = false;
    private ButtonWidget borderToggleButton;
    private AlphaSlider alphaSlider;

    private static final int MENU_W = 140;
    private static final int MENU_H = 65;


    private static final int RESIZE_ZONE = 14;
    private static final int MARGIN = 4; // safe margin from screen edges
    private static final int CORNER_SIZE = 5; // visual corner handle size

    public HitDistanceEditScreen(Screen parent) {
        super(Text.literal("Edit HUD Position"));
        this.parent = parent;
        this.config = PvpModClient.getConfig();
        this.hudScale = config.getHudScale();
    }

    @Override
    protected void init() {
        // Close context menu on screen resize (e.g. F11 fullscreen toggle)
        contextMenuOpen = false;

        hudPixelX = (int)(config.getHudX() * this.width);
        hudPixelY = (int)(config.getHudY() * this.height);
        clampToScreen();

        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), btn -> {
            config.setHudX((float) hudPixelX / this.width);
            config.setHudY((float) hudPixelY / this.height);
            config.setHudScale(hudScale);
            config.save();
            this.client.setScreen(parent);
        }).dimensions(this.width / 2 - 50, this.height - 30, 100, 20).build());

        // Context Menu Widgets
        alphaSlider = new AlphaSlider(0, 0, 120, 20, Text.literal("Opacity"), config.getHudBgAlpha() / 255.0);
        alphaSlider.visible = false;
        alphaSlider.active = false;
        addDrawableChild(alphaSlider);

        borderToggleButton = ButtonWidget.builder(
                Text.literal("Border: " + (config.isHudBorderEnabled() ? "ON" : "OFF")),
                btn -> {
                    config.setHudBorderEnabled(!config.isHudBorderEnabled());
                    btn.setMessage(Text.literal("Border: " + (config.isHudBorderEnabled() ? "ON" : "OFF")));
                }
        ).dimensions(0, 0, 120, 20).build();
        borderToggleButton.visible = false;
        borderToggleButton.active = false;
        addDrawableChild(borderToggleButton);
        
        // Restore context menu if it was open (e.g. after window resize)
        if (contextMenuOpen) {
            // Put it somewhere default if resized, since we didn't save coords
            alphaSlider.visible = true;
            alphaSlider.active = true;
            borderToggleButton.visible = true;
            borderToggleButton.active = true;
        }
    }

    @Override
    public void close() {
        config.setHudScale(hudScale);
        config.save();
        super.close();
    }

    // Override renderBackground to prevent 1.21 blur effect
    // This stops the default blur that 1.21 applies to screens
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Intentionally empty — we draw our own background in render()
        // This prevents 1.21's default blur AND the F11 black screen issue
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        // Draw our own background FIRST, before anything else
        // This is more reliable than renderBackground override across MC versions
        if (this.client != null && this.client.world != null) {
            // In-game: semi-transparent dark overlay
            ctx.fill(0, 0, this.width, this.height, 0x88000000);
        } else {
            // From title screen: solid gray background (matches 1.21 style)
            ctx.fill(0, 0, this.width, this.height, 0xFF404040);
        }

        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();

        // Render widgets (Done button + context menu widgets)
        super.render(ctx, mx, my, delta);

        // ── Full-screen safe-area boundary ──
        int bColor = 0x66FFFFFF;
        int bx0 = MARGIN, by0 = MARGIN;
        int bx1 = this.width - MARGIN, by1 = this.height - MARGIN;
        ctx.fill(bx0, by0, bx1, by0 + 1, bColor);
        ctx.fill(bx0, by1 - 1, bx1, by1, bColor);
        ctx.fill(bx0, by0, bx0 + 1, by1, bColor);
        ctx.fill(bx1 - 1, by0, bx1, by1, bColor);

        ctx.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("§eDrag to move, corner to resize"),
                this.width / 2, MARGIN + 4, 0xFFFFFF);

        // ── HUD preview panel ─────────────────────────────────────
        int padX = 6, padY = 4, lineSpacing = 4;
        int fontH = this.textRenderer.fontHeight;
        int maxTextWidth = Math.max(this.textRenderer.getWidth("2.85 block"), this.textRenderer.getWidth("avg: 2.56 (42)"));
        int basePanelW = maxTextWidth + padX * 2;
        int basePanelH = padY * 2 + fontH * 2 + lineSpacing;
        int panelW = (int)(basePanelW * hudScale);
        int panelH = (int)(basePanelH * hudScale);

        int r = config.getOverlayRed();
        int g = config.getOverlayGreen();
        int b = config.getOverlayBlue();
        int a = Math.max(150, config.getOverlayAlpha());
        int idleColor = (a << 24) | (r << 16) | (g << 8) | b;
        int borderColor = dragging ? 0xFFFFFF00 : (resizeCorner > 0 ? 0xFFFF8800 : idleColor);

        ctx.getMatrices().push();
        ctx.getMatrices().translate(hudPixelX, hudPixelY, 0);
        ctx.getMatrices().scale(hudScale, hudScale, 1.0f);

        int bgAlpha = config.getHudBgAlpha();
        ctx.fill(0, 0, basePanelW, basePanelH, (bgAlpha << 24) | 0x000000);
        if (config.isHudBorderEnabled()) {
            ctx.fill(0, 0, basePanelW, 1, borderColor);
            ctx.fill(0, basePanelH - 1, basePanelW, basePanelH, borderColor);
            ctx.fill(0, 1, 1, basePanelH - 1, borderColor);
            ctx.fill(basePanelW - 1, 1, basePanelW, basePanelH - 1, borderColor);
        }

        ctx.drawTextWithShadow(this.textRenderer, "2.85 block", padX, padY, 0xFFFFFFFF);
        ctx.drawTextWithShadow(this.textRenderer, "avg: 2.56 (42)", padX, padY + fontH + lineSpacing, 0xFFAAAAAA);
        ctx.getMatrices().pop();

        // ── Corner marks ─────
        int cColor = 0xCC93C572;
        int cl = 3;
        int px0 = hudPixelX, py0 = hudPixelY;
        int px1 = hudPixelX + panelW, py1 = hudPixelY + panelH;
        ctx.fill(px0, py0, px0 + cl, py0 + 1, cColor);
        ctx.fill(px0, py0, px0 + 1, py0 + cl, cColor);
        ctx.fill(px1 - cl, py0, px1, py0 + 1, cColor);
        ctx.fill(px1 - 1, py0, px1, py0 + cl, cColor);
        ctx.fill(px0, py1 - 1, px0 + cl, py1, cColor);
        ctx.fill(px0, py1 - cl, px0 + 1, py1, cColor);
        ctx.fill(px1 - cl, py1 - 1, px1, py1, cColor);
        ctx.fill(px1 - 1, py1 - cl, px1, py1, cColor);

        // ── Context Menu ON TOP of everything (z-pushed to front) ──
        if (contextMenuOpen) {
            int cmx = alphaSlider.getX() - 10;
            int cmy = alphaSlider.getY() - 10;
            int cmw = MENU_W;
            int cmh = MENU_H;

            // Push z-level so menu renders above all HUD text
            ctx.getMatrices().push();
            ctx.getMatrices().translate(0, 0, 200);

            // Dark solid background
            ctx.fill(cmx, cmy, cmx + cmw, cmy + cmh, 0xFF1A1A1A);

            // Simple white border
            ctx.fill(cmx, cmy, cmx + cmw, cmy + 1, 0xFFFFFFFF);
            ctx.fill(cmx, cmy + cmh - 1, cmx + cmw, cmy + cmh, 0xFFFFFFFF);
            ctx.fill(cmx, cmy, cmx + 1, cmy + cmh, 0xFFFFFFFF);
            ctx.fill(cmx + cmw - 1, cmy, cmx + cmw, cmy + cmh, 0xFFFFFFFF);

            // Re-render context menu widgets ON TOP
            alphaSlider.render(ctx, mx, my, delta);
            borderToggleButton.render(ctx, mx, my, delta);

            ctx.getMatrices().pop();
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        // ── Context menu interaction takes priority over everything ──
        if (contextMenuOpen) {
            int cx = alphaSlider.getX() - 10;
            int cy = alphaSlider.getY() - 10;
            boolean insideMenu = mx >= cx && mx <= cx + MENU_W && my >= cy && my <= cy + MENU_H;

            if (insideMenu) {
                // Let widgets (slider, button) handle the click, nothing else
                return super.mouseClicked(mx, my, button);
            } else {
                // Close menu and swallow the click so nothing behind activates
                contextMenuOpen = false;
                alphaSlider.visible = false;
                alphaSlider.active = false;
                borderToggleButton.visible = false;
                borderToggleButton.active = false;
                return true;
            }
        }

        if (button == 1) { // Right click — open context menu
            int padX = 6, padY = 4, lineSpacing = 4;
            int fontH = this.client.textRenderer.fontHeight;
            int maxTextWidth = Math.max(this.client.textRenderer.getWidth("2.85 block"), this.client.textRenderer.getWidth("avg: 2.56 (42)"));
            int basePanelW = maxTextWidth + padX * 2;
            int basePanelH = padY * 2 + fontH * 2 + lineSpacing;
            int panelW = (int)(basePanelW * hudScale);
            int panelH = (int)(basePanelH * hudScale);

            if (mx >= hudPixelX && mx <= hudPixelX + panelW && my >= hudPixelY && my <= hudPixelY + panelH) {
                contextMenuOpen = true;
                int totalMenuH = MENU_H;
                int menuX = (int)mx;
                int menuY = (int)my;
                
                // Clamp menu to screen (all 4 edges)
                if (menuX + MENU_W > this.width - MARGIN) menuX = this.width - MARGIN - MENU_W;
                if (menuY + totalMenuH > this.height - MARGIN) menuY = this.height - MARGIN - totalMenuH;
                if (menuX < MARGIN) menuX = MARGIN;
                if (menuY < MARGIN) menuY = MARGIN;

                alphaSlider.setX(menuX + 10);
                alphaSlider.setY(menuY + 10);
                alphaSlider.visible = true;
                alphaSlider.active = true;

                borderToggleButton.setX(menuX + 10);
                borderToggleButton.setY(menuY + 35);
                borderToggleButton.visible = true;
                borderToggleButton.active = true;
                
                return true;
            }
        }

        if (button == 0) {
            int padX = 6, padY = 4, lineSpacing = 4;
            int fontH = this.client.textRenderer.fontHeight;
            int maxTextWidth = Math.max(this.client.textRenderer.getWidth("2.85 block"), this.client.textRenderer.getWidth("avg: 2.56 (42)"));
            int basePanelW = maxTextWidth + padX * 2;
            int basePanelH = padY * 2 + fontH * 2 + lineSpacing;
            
            int panelW = (int)(basePanelW * hudScale);
            int panelH = (int)(basePanelH * hudScale);

            // Scale resize zone proportionally, clamped to [6, 14]
            int scaledRZ = Math.max(3, Math.min(14, (int)(RESIZE_ZONE * hudScale)));

            // TL corner
            if (mx >= hudPixelX && mx <= hudPixelX + scaledRZ && my >= hudPixelY && my <= hudPixelY + scaledRZ) {
                resizeCorner = 1; return true;
            }
            // TR corner
            if (mx >= hudPixelX + panelW - scaledRZ && mx <= hudPixelX + panelW && my >= hudPixelY && my <= hudPixelY + scaledRZ) {
                resizeCorner = 2; return true;
            }
            // BL corner
            if (mx >= hudPixelX && mx <= hudPixelX + scaledRZ && my >= hudPixelY + panelH - scaledRZ && my <= hudPixelY + panelH) {
                resizeCorner = 3; return true;
            }
            // BR corner
            if (mx >= hudPixelX + panelW - scaledRZ && mx <= hudPixelX + panelW && my >= hudPixelY + panelH - scaledRZ && my <= hudPixelY + panelH) {
                resizeCorner = 4; return true;
            }

            // Drag zone (entire panel)
            if (mx >= hudPixelX && mx <= hudPixelX + panelW
                    && my >= hudPixelY && my <= hudPixelY + panelH) {
                dragging = true;
                dragOffsetX = (int)(mx - hudPixelX);
                dragOffsetY = (int)(my - hudPixelY);
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (button == 0) {
            if (dragging) {
                hudPixelX = (int)(mx - dragOffsetX);
                hudPixelY = (int)(my - dragOffsetY);
                clampToScreen();
                return true;
            }
            if (resizeCorner > 0) {
                int padX = 6, padY = 4, lineSpacing = 4;
                int fontH = this.client.textRenderer.fontHeight;
                int maxTextWidth = Math.max(this.client.textRenderer.getWidth("2.85 block"), this.client.textRenderer.getWidth("avg: 2.56 (42)"));
                int basePanelW = maxTextWidth + padX * 2;
                int basePanelH = padY * 2 + fontH * 2 + lineSpacing;
                
                float oldScale = hudScale;
                float newScale = oldScale;
                
                int fixedX = 0, fixedY = 0;
                
                if (resizeCorner == 1) { // TL
                    fixedX = hudPixelX + (int)(basePanelW * oldScale);
                    fixedY = hudPixelY + (int)(basePanelH * oldScale);
                    newScale = (float)(fixedX - mx) / basePanelW;
                } else if (resizeCorner == 2) { // TR
                    fixedX = hudPixelX;
                    fixedY = hudPixelY + (int)(basePanelH * oldScale);
                    newScale = (float)(mx - fixedX) / basePanelW;
                } else if (resizeCorner == 3) { // BL
                    fixedX = hudPixelX + (int)(basePanelW * oldScale);
                    fixedY = hudPixelY;
                    newScale = (float)(fixedX - mx) / basePanelW;
                } else if (resizeCorner == 4) { // BR
                    fixedX = hudPixelX;
                    fixedY = hudPixelY;
                    newScale = (float)(mx - fixedX) / basePanelW;
                }
                
                hudScale = Math.max(0.5f, Math.min(3.0f, newScale));
                
                if (resizeCorner == 1) {
                    hudPixelX = fixedX - (int)(basePanelW * hudScale);
                    hudPixelY = fixedY - (int)(basePanelH * hudScale);
                } else if (resizeCorner == 2) {
                    hudPixelX = fixedX;
                    hudPixelY = fixedY - (int)(basePanelH * hudScale);
                } else if (resizeCorner == 3) {
                    hudPixelX = fixedX - (int)(basePanelW * hudScale);
                    hudPixelY = fixedY;
                } else if (resizeCorner == 4) {
                    hudPixelX = fixedX;
                    hudPixelY = fixedY;
                }
                
                clampToScreen();
                return true;
            }
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (button == 0) {
            dragging = false;
            resizeCorner = 0;
        }
        return super.mouseReleased(mx, my, button);
    }

    /**
     * Clamp HUD so panel stays strictly inside the white boundary.
     * HUD border touches but never overlaps the white boundary line.
     */
    private void clampToScreen() {
        int padX = 6, padY = 4, lineSpacing = 4;
        int fontH = this.client.textRenderer.fontHeight;
        int maxTextWidth = Math.max(this.client.textRenderer.getWidth("2.85 block"), this.client.textRenderer.getWidth("avg: 2.56 (42)"));
        int basePanelW = maxTextWidth + padX * 2;
        int basePanelH = padY * 2 + fontH * 2 + lineSpacing;
        
        int panelW = (int)(basePanelW * hudScale);
        int panelH = (int)(basePanelH * hudScale);

        // Keep inside the white boundary (MARGIN+1 so borders don't overlap)
        int minX = MARGIN + 1;
        int minY = MARGIN + 1;
        int maxX = this.width - MARGIN - 1 - panelW;
        int maxY = this.height - MARGIN - 1 - panelH;

        hudPixelX = Math.max(minX, Math.min(maxX, hudPixelX));
        hudPixelY = Math.max(minY, Math.min(maxY, hudPixelY));
    }

    private class AlphaSlider extends net.minecraft.client.gui.widget.SliderWidget {
        public AlphaSlider(int x, int y, int width, int height, Text text, double value) {
            super(x, y, width, height, text, value);
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Text.literal("Opacity: " + (int)(this.value * 255)));
        }

        @Override
        protected void applyValue() {
            config.setHudBgAlpha((int)(this.value * 255));
        }
    }
}
