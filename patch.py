import re

with open(r'c:\Users\bemre\Desktop\PVP Mod\src\client\java\com\pvpmod\client\config\HitDistanceEditScreen.java', 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Imports
content = re.sub(r'import net.minecraft.text.Text;', 'import net.minecraft.text.Text;\nimport net.minecraft.client.gui.widget.SliderWidget;', content)

# 2. Fields
fields_to_add = '''
    private float dragStartScale;
    private int dragStartHudX, dragStartHudY;
    private double dragStartMouseX, dragStartMouseY;

    // Context Menu fields
    private boolean contextMenuOpen = false;
    private int contextMenuX, contextMenuY;
    private static final int MENU_W = 120;
    private static final int MENU_H = 65;
    private ButtonWidget borderToggleBtn;
    private AlphaSlider alphaSlider;
'''
content = re.sub(r'    private float dragStartScale;\n    private int dragStartHudX, dragStartHudY;\n    private double dragStartMouseX, dragStartMouseY;', fields_to_add, content)

# 3. init method
init_content = '''    @Override
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

        // Initialize Context Menu widgets (hidden by default)
        borderToggleBtn = ButtonWidget.builder(
                Text.literal("Border: " + (config.isHudBorderEnabled() ? "ON" : "OFF")),
                btn -> {
                    config.setHudBorderEnabled(!config.isHudBorderEnabled());
                    btn.setMessage(Text.literal("Border: " + (config.isHudBorderEnabled() ? "ON" : "OFF")));
                }
        ).dimensions(0, 0, MENU_W - 10, 20).build();
        borderToggleBtn.visible = false;
        borderToggleBtn.active = false;
        addDrawableChild(borderToggleBtn);

        alphaSlider = new AlphaSlider(0, 0, MENU_W - 10, 20, config);
        alphaSlider.visible = false;
        alphaSlider.active = false;
        addDrawableChild(alphaSlider);
    }'''
content = re.sub(r'    @Override\s+protected void init\(\) \{.*?(?=\s+@Override\s+public void render)', init_content, content, flags=re.DOTALL)

# 4. Remove super.render from early in render
content = re.sub(r'\s*// Render widgets \(Done button\)\s*super\.render\(ctx, mx, my, delta\);', '', content)

# 5. Add super.render and context menu to bottom of render
render_bottom = '''        // Bottom-right
        ctx.fill(px1 - cl, py1 - 1, px1, py1, cColor);
        ctx.fill(px1 - 1, py1 - cl, px1, py1, cColor);

        // Render Context Menu Background
        if (contextMenuOpen) {
            ctx.fill(contextMenuX, contextMenuY, contextMenuX + MENU_W, contextMenuY + MENU_H, 0xDD000000);
            int bc = 0xFF555555;
            ctx.fill(contextMenuX, contextMenuY, contextMenuX + MENU_W, contextMenuY + 1, bc);
            ctx.fill(contextMenuX, contextMenuY + MENU_H - 1, contextMenuX + MENU_W, contextMenuY + MENU_H, bc);
            ctx.fill(contextMenuX, contextMenuY + 1, contextMenuX + 1, contextMenuY + MENU_H - 1, bc);
            ctx.fill(contextMenuX + MENU_W - 1, contextMenuY + 1, contextMenuX + MENU_W, contextMenuY + MENU_H - 1, bc);
        }

        // Render widgets (Done button, Context Menu widgets)
        super.render(ctx, mx, my, delta);
    }'''
content = re.sub(r'        // Bottom-right\s*ctx\.fill\(px1 - cl, py1 - 1, px1, py1, cColor\);\s*ctx\.fill\(px1 - 1, py1 - cl, px1, py1, cColor\);\s*\}', render_bottom, content)

# 6. Replace mouseClicked
mouseClicked_content = '''    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (contextMenuOpen) {
            if (mx < contextMenuX || mx > contextMenuX + MENU_W || my < contextMenuY || my > contextMenuY + MENU_H) {
                closeContextMenu();
                return true;
            }
        }

        if (button == 0 && !contextMenuOpen) {
            int padX = 6, padY = 4, lineSpacing = 4;
            int fontH = this.client.textRenderer.fontHeight;
            int maxTextWidth = Math.max(this.client.textRenderer.getWidth("2.85 block"), this.client.textRenderer.getWidth("avg: 2.56 (42)"));
            int basePanelW = maxTextWidth + padX * 2;
            int basePanelH = padY * 2 + fontH * 2 + lineSpacing;
            
            int panelW = (int)(basePanelW * hudScale);
            int panelH = (int)(basePanelH * hudScale);

            // TL corner
            if (mx >= hudPixelX && mx <= hudPixelX + RESIZE_ZONE && my >= hudPixelY && my <= hudPixelY + RESIZE_ZONE) {
                resizeCorner = 1; captureDragStart(mx, my); return true;
            }
            // TR corner
            if (mx >= hudPixelX + panelW - RESIZE_ZONE && mx <= hudPixelX + panelW && my >= hudPixelY && my <= hudPixelY + RESIZE_ZONE) {
                resizeCorner = 2; captureDragStart(mx, my); return true;
            }
            // BL corner
            if (mx >= hudPixelX && mx <= hudPixelX + RESIZE_ZONE && my >= hudPixelY + panelH - RESIZE_ZONE && my <= hudPixelY + panelH) {
                resizeCorner = 3; captureDragStart(mx, my); return true;
            }
            // BR corner
            if (mx >= hudPixelX + panelW - RESIZE_ZONE && mx <= hudPixelX + panelW && my >= hudPixelY + panelH - RESIZE_ZONE && my <= hudPixelY + panelH) {
                resizeCorner = 4; captureDragStart(mx, my); return true;
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
        
        if (button == 1 && !contextMenuOpen) {
            int padX = 6, padY = 4, lineSpacing = 4;
            int fontH = this.client.textRenderer.fontHeight;
            int maxTextWidth = Math.max(this.client.textRenderer.getWidth("2.85 block"), this.client.textRenderer.getWidth("avg: 2.56 (42)"));
            int basePanelW = maxTextWidth + padX * 2;
            int basePanelH = padY * 2 + fontH * 2 + lineSpacing;
            int panelW = (int)(basePanelW * hudScale);
            int panelH = (int)(basePanelH * hudScale);

            if (mx >= hudPixelX && mx <= hudPixelX + panelW && my >= hudPixelY && my <= hudPixelY + panelH) {
                openContextMenu((int)mx, (int)my);
                return true;
            }
        }
        
        return super.mouseClicked(mx, my, button);
    }'''
content = re.sub(r'    @Override\s+public boolean mouseClicked\(double mx, double my, int button\) \{.*?(?=\s+private void captureDragStart)', mouseClicked_content, content, flags=re.DOTALL)

# 7. Add context menu logic and slider at end
end_content = '''    private void openContextMenu(int mx, int my) {
        contextMenuOpen = true;
        
        int x = mx;
        int y = my;
        
        if (x + MENU_W > this.width) {
            x = mx - MENU_W;
        }
        if (y + MENU_H > this.height) {
            y = my - MENU_H;
        }
        
        contextMenuX = x;
        contextMenuY = y;
        
        if (alphaSlider != null) {
            alphaSlider.setX(x + 5);
            alphaSlider.setY(y + 10);
            alphaSlider.visible = true;
            alphaSlider.active = true;
        }
        if (borderToggleBtn != null) {
            borderToggleBtn.setX(x + 5);
            borderToggleBtn.setY(y + 35);
            borderToggleBtn.visible = true;
            borderToggleBtn.active = true;
        }
    }

    private void closeContextMenu() {
        contextMenuOpen = false;
        if (alphaSlider != null) {
            alphaSlider.visible = false;
            alphaSlider.active = false;
        }
        if (borderToggleBtn != null) {
            borderToggleBtn.visible = false;
            borderToggleBtn.active = false;
        }
    }

    private static class AlphaSlider extends SliderWidget {
        private final ReachOverlayConfig config;
        public AlphaSlider(int x, int y, int w, int h, ReachOverlayConfig config) {
            super(x, y, w, h, Text.literal("Alpha: " + config.getHudBackgroundAlpha()), config.getHudBackgroundAlpha() / 255.0);
            this.config = config;
        }
        @Override protected void updateMessage() {
            setMessage(Text.literal("Alpha: " + config.getHudBackgroundAlpha()));
        }
        @Override protected void applyValue() {
            config.setHudBackgroundAlpha((int)(this.value * 255));
        }
    }
}'''
content = re.sub(r'\}\s*$', end_content + '\n', content)

with open(r'c:\Users\bemre\Desktop\PVP Mod\src\client\java\com\pvpmod\client\config\HitDistanceEditScreen.java', 'w', encoding='utf-8') as f:
    f.write(content)
