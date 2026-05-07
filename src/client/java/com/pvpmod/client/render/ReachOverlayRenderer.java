package com.pvpmod.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.pvpmod.client.config.ReachOverlayConfig;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * Renders a smooth 1-block-wide strip on the ground along the crosshair
 * direction, representing the player's attack reach distance.
 */
public class ReachOverlayRenderer {

    private final ReachOverlayConfig config;

    public ReachOverlayRenderer(ReachOverlayConfig config) {
        this.config = config;
    }

    public void render(MatrixStack matrices, VertexConsumerProvider consumers, Vec3d cam, float tickDelta) {
        if (!config.isEnabled()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        PlayerEntity player = client.player;
        float td = tickDelta;

        // Interpolated positions using getLerpedPos
        Vec3d lerpedPos = player.getLerpedPos(td);
        double px = lerpedPos.x;
        double py = lerpedPos.y;
        double pz = lerpedPos.z;
        double eyeY = py + (double) player.getStandingEyeHeight();

        // Look direction (unit vector)
        Vec3d look = player.getRotationVec(td);

        // Effective reach with configurable tolerance
        double reach = getAttackReach(player) + config.getReachTolerance();

        // Ground surface Y
        double groundY = py + config.getSurfaceOffset();
        int searchX = (int) Math.floor(px);
        int searchZ = (int) Math.floor(pz);
        int searchFrom = (int) Math.floor(py + 0.5);
        for (int y = searchFrom; y >= searchFrom - 5; y--) {
            if (!client.world.getBlockState(new BlockPos(searchX, y, searchZ)).isAir()) {
                groundY = y + 1.0 + config.getSurfaceOffset();
                break;
            }
        }
        double eyeAbove = eyeY - groundY;

        // Horizontal direction
        double hx = look.x, hz = look.z;
        double hLen = Math.sqrt(hx * hx + hz * hz);

        if (hLen < 0.001) {
            double yawRad = Math.toRadians(player.getYaw(td));
            hx = -Math.sin(yawRad);
            hz = Math.cos(yawRad);
            hLen = 1.0;
        }

        double dirX = hx / hLen, dirZ = hz / hLen;
        double perpX = -dirZ, perpZ = dirX;

        double farX, farZ;

        if (look.y < -0.001 && eyeAbove > 0) {
            double t = (groundY - eyeY) / look.y;
            if (t <= reach) {
                farX = px + look.x * t;
                farZ = pz + look.z * t;
            } else {
                double horizAtReach = reach * hLen;
                farX = px + dirX * horizAtReach;
                farZ = pz + dirZ * horizAtReach;
            }
        } else {
            farX = px + dirX * reach;
            farZ = pz + dirZ * reach;
        }

        double hw = 0.5;
        double nlX = px + perpX * hw,  nlZ = pz + perpZ * hw;
        double nrX = px - perpX * hw,  nrZ = pz - perpZ * hw;
        double flX = farX + perpX * hw, flZ = farZ + perpZ * hw;
        double frX = farX - perpX * hw, frZ = farZ - perpZ * hw;


        int r = config.getOverlayRed(), g = config.getOverlayGreen();
        int b = config.getOverlayBlue(), a = config.getOverlayAlpha();
        float gy = (float) groundY;

        // Fill

        drawQuad(consumers, matrices, cam, gy, nlX, nlZ, flX, flZ, frX, frZ, nrX, nrZ, r, g, b, a);
        // Border
        int br = Math.min(255, r + 50), bg = Math.min(255, g + 50);
        int bb = Math.min(255, b + 50), ba = Math.min(255, a + 100);
        drawBorder(consumers, matrices, cam, gy + 0.001f, nlX, nlZ, flX, flZ, frX, frZ, nrX, nrZ, br, bg, bb, ba);
    }

    private void drawQuad(VertexConsumerProvider consumers, MatrixStack mat, Vec3d cam, float y,
                          double x0, double z0, double x1, double z1,
                          double x2, double z2, double x3, double z3,
                          int r, int g, int b, int a) {
        mat.push();
        mat.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f m = mat.peek().getPositionMatrix();

        if (consumers != null) {
            VertexConsumer buf = consumers.getBuffer(RenderLayer.getDebugQuads());
            buf.vertex(m, (float)x0, y, (float)z0).color(r, g, b, a).next();
            buf.vertex(m, (float)x1, y, (float)z1).color(r, g, b, a).next();
            buf.vertex(m, (float)x2, y, (float)z2).color(r, g, b, a).next();
            buf.vertex(m, (float)x3, y, (float)z3).color(r, g, b, a).next();
        }
        mat.pop();
    }

    private void drawBorder(VertexConsumerProvider consumers, MatrixStack mat, Vec3d cam, float y,
                            double x0, double z0, double x1, double z1,
                            double x2, double z2, double x3, double z3,
                            int r, int g, int b, int a) {
        mat.push();
        mat.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f m = mat.peek().getPositionMatrix();

        if (consumers != null) {
            VertexConsumer buf = consumers.getBuffer(RenderLayer.getLines());
            // GL_LINES requires pairs of vertices per line segment
            // Segment 1: x0->x1
            buf.vertex(m, (float)x0, y, (float)z0).color(r, g, b, a).normal(0, 1, 0).next();
            buf.vertex(m, (float)x1, y, (float)z1).color(r, g, b, a).normal(0, 1, 0).next();
            // Segment 2: x1->x2
            buf.vertex(m, (float)x1, y, (float)z1).color(r, g, b, a).normal(0, 1, 0).next();
            buf.vertex(m, (float)x2, y, (float)z2).color(r, g, b, a).normal(0, 1, 0).next();
            // Segment 3: x2->x3
            buf.vertex(m, (float)x2, y, (float)z2).color(r, g, b, a).normal(0, 1, 0).next();
            buf.vertex(m, (float)x3, y, (float)z3).color(r, g, b, a).normal(0, 1, 0).next();
            // Segment 4: x3->x0
            buf.vertex(m, (float)x3, y, (float)z3).color(r, g, b, a).normal(0, 1, 0).next();
            buf.vertex(m, (float)x0, y, (float)z0).color(r, g, b, a).normal(0, 1, 0).next();
        }
        mat.pop();
    }

    private static double lerp(double a, double b, float t) { return a + (b - a) * t; }

    private double getAttackReach(PlayerEntity player) {
        return player.isCreative() ? 5.0 : 3.0;
    }
}
