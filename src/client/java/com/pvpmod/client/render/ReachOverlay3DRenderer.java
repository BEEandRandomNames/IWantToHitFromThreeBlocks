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
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * Renders a 3D wireframe box along the player's crosshair direction,
 * representing the attack reach distance.
 */
public class ReachOverlay3DRenderer {

    private static final double BOX_HALF_WIDTH = 0.5;
    private static final double BOX_HEIGHT = 1.8;

    private final ReachOverlayConfig config;

    public ReachOverlay3DRenderer(ReachOverlayConfig config) {
        this.config = config;
    }

    public void render(MatrixStack matrices, VertexConsumerProvider consumers, Vec3d cam, float tickDelta) {
        if (!config.isEnabled() || !config.isReach3DEnabled()) return;

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

        Vec3d look = player.getRotationVec(td);
        double reach = getAttackReach(player) + config.getReachTolerance();

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

        double farX = px + look.x * reach;
        double farY = eyeY + look.y * reach;
        double farZ = pz + look.z * reach;

        double hw = BOX_HALF_WIDTH;
        double hh = BOX_HEIGHT / 2.0;

        double lx = look.x, ly = look.y, lz = look.z;
        double rx = lz, ry = 0, rz = -lx;
        double rLen = Math.sqrt(rx * rx + ry * ry + rz * rz);
        if (rLen < 0.001) { rx = perpX; ry = 0; rz = perpZ; rLen = 1.0; }
        rx /= rLen; ry /= rLen; rz /= rLen;

        double ux = ry * lz - rz * ly;
        double uy = rz * lx - rx * lz;
        double uz = rx * ly - ry * lx;
        double uLen = Math.sqrt(ux * ux + uy * uy + uz * uz);
        if (uLen > 0.001) { ux /= uLen; uy /= uLen; uz /= uLen; }

        double n0x = px - rx*hw - ux*hh, n0y = eyeY - ry*hw - uy*hh, n0z = pz - rz*hw - uz*hh;
        double n1x = px + rx*hw - ux*hh, n1y = eyeY + ry*hw - uy*hh, n1z = pz + rz*hw - uz*hh;
        double n2x = px + rx*hw + ux*hh, n2y = eyeY + ry*hw + uy*hh, n2z = pz + rz*hw + uz*hh;
        double n3x = px - rx*hw + ux*hh, n3y = eyeY - ry*hw + uy*hh, n3z = pz - rz*hw + uz*hh;

        double f0x = farX - rx*hw - ux*hh, f0y = farY - ry*hw - uy*hh, f0z = farZ - rz*hw - uz*hh;
        double f1x = farX + rx*hw - ux*hh, f1y = farY + ry*hw - uy*hh, f1z = farZ + rz*hw - uz*hh;
        double f2x = farX + rx*hw + ux*hh, f2y = farY + ry*hw + uy*hh, f2z = farZ + rz*hw + uz*hh;
        double f3x = farX - rx*hw + ux*hh, f3y = farY - ry*hw + uy*hh, f3z = farZ - rz*hw + uz*hh;



        int r = config.getOverlayRed(), g = config.getOverlayGreen();
        int b = config.getOverlayBlue(), a = config.getOverlayAlpha();
        int br = Math.min(255, r + 80), bg = Math.min(255, g + 80);
        int bb = Math.min(255, b + 80), ba = Math.min(255, a + 120);

        matrices.push();
        matrices.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f m = matrices.peek().getPositionMatrix();



        // Near face
        drawLine(consumers, m, n0x, n0y, n0z, n1x, n1y, n1z, br, bg, bb, ba);
        drawLine(consumers, m, n1x, n1y, n1z, n2x, n2y, n2z, br, bg, bb, ba);
        drawLine(consumers, m, n2x, n2y, n2z, n3x, n3y, n3z, br, bg, bb, ba);
        drawLine(consumers, m, n3x, n3y, n3z, n0x, n0y, n0z, br, bg, bb, ba);

        // Far face
        drawLine(consumers, m, f0x, f0y, f0z, f1x, f1y, f1z, br, bg, bb, ba);
        drawLine(consumers, m, f1x, f1y, f1z, f2x, f2y, f2z, br, bg, bb, ba);
        drawLine(consumers, m, f2x, f2y, f2z, f3x, f3y, f3z, br, bg, bb, ba);
        drawLine(consumers, m, f3x, f3y, f3z, f0x, f0y, f0z, br, bg, bb, ba);

        // Connecting edges
        drawLine(consumers, m, n0x, n0y, n0z, f0x, f0y, f0z, br, bg, bb, ba);
        drawLine(consumers, m, n1x, n1y, n1z, f1x, f1y, f1z, br, bg, bb, ba);
        drawLine(consumers, m, n2x, n2y, n2z, f2x, f2y, f2z, br, bg, bb, ba);
        drawLine(consumers, m, n3x, n3y, n3z, f3x, f3y, f3z, br, bg, bb, ba);

        matrices.pop();

        // Far face fill
        drawFarFace(consumers, matrices, cam, f0x, f0y, f0z, f1x, f1y, f1z,
                f2x, f2y, f2z, f3x, f3y, f3z, r, g, b, Math.min(a, 40));
    }

    private void drawLine(VertexConsumerProvider consumers, Matrix4f m,
                          double x0, double y0, double z0,
                          double x1, double y1, double z1,
                          int r, int g, int b, int a) {
        if (consumers != null) {
            VertexConsumer buf = consumers.getBuffer(RenderLayer.getLines());
            buf.vertex(m, (float) x0, (float) y0, (float) z0).color(r, g, b, a).normal(0, 1, 0);
            buf.vertex(m, (float) x1, (float) y1, (float) z1).color(r, g, b, a).normal(0, 1, 0);
        }
    }

    private void drawFarFace(VertexConsumerProvider consumers, MatrixStack mat, Vec3d cam,
                              double x0, double y0, double z0,
                              double x1, double y1, double z1,
                              double x2, double y2, double z2,
                              double x3, double y3, double z3,
                              int r, int g, int b, int a) {
        mat.push();
        mat.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f m = mat.peek().getPositionMatrix();

        if (consumers != null) {
            VertexConsumer buf = consumers.getBuffer(RenderLayer.getDebugQuads());
            buf.vertex(m, (float) x0, (float) y0, (float) z0).color(r, g, b, a);
            buf.vertex(m, (float) x1, (float) y1, (float) z1).color(r, g, b, a);
            buf.vertex(m, (float) x2, (float) y2, (float) z2).color(r, g, b, a);
            buf.vertex(m, (float) x3, (float) y3, (float) z3).color(r, g, b, a);
        }
        mat.pop();
    }

    private static double lerp(double a, double b, float t) {
        return a + (b - a) * t;
    }

    private double getAttackReach(PlayerEntity player) {
        return player.getEntityInteractionRange();
    }
}
