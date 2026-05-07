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
    private boolean resizing = false;
    private int dragOffsetX, dragOffsetY;

    private static final int BASE_WIDTH = 110;
    private static final int BASE_HEIGHT = 32;
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
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.renderBackground(ctx, mx, my, delta);

        // ── Full-screen safe-area boundary (includes Done button area) ──
        int bColor = 0x66FFFFFF;
        int bx0 = MARGIN, by0 = MARGIN;
        int bx1 = this.width - MARGIN, by1 = this.height - MARGIN;
        // Top
        ctx.fill(bx0, by0, bx1, by0 + 1, bColor);
        // Bottom
        ctx.fill(bx0, by1 - 1, bx1, by1, bColor);
        // Left
        ctx.fill(bx0, by0, bx0 + 1, by1, bColor);
        // Right
        ctx.fill(bx1 - 1, by0, bx1, by1, bColor);

        // Instructions
        ctx.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("§eDrag to move, corner to resize"),
                this.width / 2, MARGIN + 4, 0xFFFFFF);

        // ── HUD preview panel ─────────────────────────────────────
        int panelW = (int)(BASE_WIDTH * hudScale);
        int panelH = (int)(BASE_HEIGHT * hudScale);

        // Background
        ctx.fill(hudPixelX, hudPixelY,
                hudPixelX + panelW, hudPixelY + panelH,
                0x8C000000);

        // Border (green)
        int borderColor = dragging ? 0xFFFFFF00 : (resizing ? 0xFFFF8800 : 0xFF44FF44);
        // Top border
        ctx.fill(hudPixelX, hudPixelY, hudPixelX + panelW, hudPixelY + 1, borderColor);
        // Bottom border
        ctx.fill(hudPixelX, hudPixelY + panelH - 1, hudPixelX + panelW, hudPixelY + panelH, borderColor);
        // Left border
        ctx.fill(hudPixelX, hudPixelY + 1, hudPixelX + 1, hudPixelY + panelH - 1, borderColor);
        // Right border
        ctx.fill(hudPixelX + panelW - 1, hudPixelY + 1, hudPixelX + panelW, hudPixelY + panelH - 1, borderColor);

        // ── Pistachio green corner marks (1px thick, L-shaped) ─────
        int cColor = 0xCC93C572; // pistachio green
        int cl = 3; // corner line length
        int px0 = hudPixelX, py0 = hudPixelY;
        int px1 = hudPixelX + panelW, py1 = hudPixelY + panelH;
        // Top-left
        ctx.fill(px0, py0, px0 + cl, py0 + 1, cColor);
        ctx.fill(px0, py0, px0 + 1, py0 + cl, cColor);
        // Top-right
        ctx.fill(px1 - cl, py0, px1, py0 + 1, cColor);
        ctx.fill(px1 - 1, py0, px1, py0 + cl, cColor);
        // Bottom-left
        ctx.fill(px0, py1 - 1, px0 + cl, py1, cColor);
        ctx.fill(px0, py1 - cl, px0 + 1, py1, cColor);
        // Bottom-right
        ctx.fill(px1 - cl, py1 - 1, px1, py1, cColor);
        ctx.fill(px1 - 1, py1 - cl, px1, py1, cColor);

        // Preview texts (2 lines)
        ctx.getMatrices().push();
        ctx.getMatrices().translate(hudPixelX + 6, hudPixelY + 4, 0);
        ctx.getMatrices().scale(hudScale, hudScale, 1.0f);

        int fontH = this.textRenderer.fontHeight;
        int ls = 4;
        ctx.drawTextWithShadow(this.textRenderer, "2.85 block", 0, 0, 0xFFFFFFFF);
        ctx.drawTextWithShadow(this.textRenderer, "avg: 2.56 (42)", 0, fontH + ls, 0xFFAAAAAA);

        ctx.getMatrices().pop();

        super.render(ctx, mx, my, delta);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            int panelW = (int)(BASE_WIDTH * hudScale);
            int panelH = (int)(BASE_HEIGHT * hudScale);

            // Resize zone (bottom-right corner)
            int rzX = hudPixelX + panelW - RESIZE_ZONE;
            int rzY = hudPixelY + panelH - RESIZE_ZONE;
            if (mx >= rzX && mx <= hudPixelX + panelW
                    && my >= rzY && my <= hudPixelY + panelH) {
                resizing = true;
                return true;
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
            if (resizing) {
                int newW = (int)(mx - hudPixelX);
                float newScale = (float) newW / BASE_WIDTH;
                hudScale = Math.max(0.5f, Math.min(3.0f, newScale));
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
            resizing = false;
        }
        return super.mouseReleased(mx, my, button);
    }

    /**
     * Clamp HUD so panel stays strictly inside the white boundary.
     * HUD border touches but never overlaps the white boundary line.
     */
    private void clampToScreen() {
        int panelW = (int)(BASE_WIDTH * hudScale);
        int panelH = (int)(BASE_HEIGHT * hudScale);

        // Keep inside the white boundary (MARGIN+1 so borders don't overlap)
        int minX = MARGIN + 1;
        int minY = MARGIN + 1;
        int maxX = this.width - MARGIN - 1 - panelW;
        int maxY = this.height - MARGIN - 1 - panelH;

        hudPixelX = Math.max(minX, Math.min(maxX, hudPixelX));
        hudPixelY = Math.max(minY, Math.min(maxY, hudPixelY));
    }
}
