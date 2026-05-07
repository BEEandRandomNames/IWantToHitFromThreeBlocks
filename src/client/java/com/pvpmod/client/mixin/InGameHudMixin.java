package com.pvpmod.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.pvpmod.client.PvpModClient;
import com.pvpmod.client.config.ReachOverlayConfig;
import com.pvpmod.client.stats.HitDistanceTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into InGameHud to overlay a colored crosshair when an
 * attackable entity is within reach + tolerance distance.
 *
 * APPROACH: Instead of tinting vanilla's crosshair (which leaks
 * shader color to hearts/hunger bars via blend mode changes), we
 * let vanilla render normally, then OVERDRAW a colored crosshair
 * texture on top. This is fully isolated — no render state leaks.
 *
 * TEXTURE PACK COMPATIBILITY: The GUI_ICONS Identifier goes through
 * Minecraft's resource manager, so any resource pack that replaces
 * textures/gui/icons.png is automatically used by our overdraw too.
 * This ensures our colored crosshair always matches the current
 * crosshair shape, regardless of the active resource pack.
 */
@Mixin(InGameHud.class)
public class InGameHudMixin {

    // 1.20.2+ uses GUI sprite atlas instead of icons.png
    private static final Identifier CROSSHAIR_TEXTURE = new Identifier("hud/crosshair");

    // ── Crosshair hit distance text state ──────────────────────────
    private static long pvpro$lastRenderedHitTime = 0;
    private static double pvpro$lastRenderedHitDistance = -1;
    private static float pvpro$hitHue = 0.0f;
    private static final float HUE_STEP = 0.13f; // ~47° per hit
    private static final long FADE_DURATION_MS = 200;
    private static final float HIT_TEXT_SCALE = 0.875f;

    /**
     * After vanilla finishes rendering the crosshair, overdraw a
     * colored version on top if an entity is within reach.
     * Also renders the hit distance text above the crosshair.
     */
    @Inject(method = "renderCrosshair", at = @At("RETURN"))
    private void pvpReachOverlay$afterCrosshair(DrawContext context, CallbackInfo ci) {

        MinecraftClient client = MinecraftClient.getInstance();
        int scaledWidth = client.getWindow().getScaledWidth();
        int scaledHeight = client.getWindow().getScaledHeight();

        // ── Part 1: Colored crosshair overlay ─────────────────────
        ReachOverlayConfig config = PvpModClient.getConfig();
        if (config != null && config.isCrossColorEnabled()) {
            if (client.player != null && client.world != null) {
                PlayerEntity player = client.player;
                double baseReach = player.isCreative() ? 5.0 : 3.0;
                double effectiveReach = baseReach + config.getCrossTolerance();

                Vec3d eyePos = player.getEyePos();
                Vec3d lookVec = player.getRotationVec(1.0f);
                Vec3d endPos = eyePos.add(lookVec.multiply(effectiveReach));

                Box searchBox = player.getBoundingBox()
                        .stretch(lookVec.multiply(effectiveReach))
                        .expand(1.0);

                EntityHitResult entityHit = ProjectileUtil.raycast(
                        player, eyePos, endPos, searchBox,
                        entity -> !entity.isSpectator() && entity.canHit(),
                        effectiveReach * effectiveReach
                );

                if (entityHit != null && entityHit.getEntity() != null) {
                    int cx = (scaledWidth - 15) / 2;
                    int cy = (scaledHeight - 15) / 2;

                    float r = config.getCrossRed() / 255.0f;
                    float g = config.getCrossGreen() / 255.0f;
                    float b = config.getCrossBlue() / 255.0f;
                    float a = config.getCrossAlpha() / 255.0f;

                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                    RenderSystem.setShaderColor(r, g, b, a);
                    context.drawGuiTexture(CROSSHAIR_TEXTURE, cx, cy, 15, 15);
                    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
                }
            }
        }

        // ── Part 2: Hit distance text above crosshair (RGB fade) ──
        if (config == null || !config.isHitTextEnabled()) return;
        HitDistanceTracker tracker = PvpModClient.getHitTracker();
        if (tracker == null) return;

        long trackerHitTime = tracker.getLastHitTime();
        if (trackerHitTime <= 0) return;

        // Detect new hit: tracker's timestamp changed
        if (trackerHitTime != pvpro$lastRenderedHitTime) {
            pvpro$lastRenderedHitTime = trackerHitTime;
            pvpro$lastRenderedHitDistance = tracker.getLastHitDistance();
            // Advance hue for next color
            pvpro$hitHue += HUE_STEP;
            if (pvpro$hitHue > 1.0f) pvpro$hitHue -= 1.0f;
        }

        long elapsed = System.currentTimeMillis() - pvpro$lastRenderedHitTime;
        if (elapsed >= FADE_DURATION_MS || pvpro$lastRenderedHitDistance < 0) return;

        // Progress: 0.0 (just appeared) → 1.0 (fully faded)
        float progress = elapsed / (float) FADE_DURATION_MS;
        float alpha = 1.0f - progress;

        // Smoothly transition hue during the fade
        float currentHue = pvpro$hitHue + progress * HUE_STEP;
        if (currentHue > 1.0f) currentHue -= 1.0f;

        int rgb = java.awt.Color.HSBtoRGB(currentHue, 0.9f, 1.0f);
        int rr = (rgb >> 16) & 0xFF;
        int gg = (rgb >> 8) & 0xFF;
        int bb = rgb & 0xFF;
        int argb = ((int) (alpha * 255) << 24) | (rr << 16) | (gg << 8) | bb;

        String hitText = String.format("%.2f", pvpro$lastRenderedHitDistance);
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

        int centerX = scaledWidth / 2;
        int crosshairTop = (scaledHeight - 15) / 2;

        context.getMatrices().push();
        context.getMatrices().translate(centerX, crosshairTop - 2, 0);
        context.getMatrices().scale(HIT_TEXT_SCALE, HIT_TEXT_SCALE, 1.0f);

        int textWidth = textRenderer.getWidth(hitText);
        int textY = -textRenderer.fontHeight;
        context.drawTextWithShadow(textRenderer, hitText,
                -textWidth / 2, textY, argb);

        context.getMatrices().pop();
    }
}
