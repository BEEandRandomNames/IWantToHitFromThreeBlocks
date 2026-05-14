package com.pvpmod.client.mixin;

import com.pvpmod.client.PvpModClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(
            net.minecraft.client.util.ObjectAllocator allocator,
            RenderTickCounter tickCounter,
            boolean renderBlockOutline,
            Camera camera,
            GameRenderer gameRenderer,
            LightmapTextureManager lightmapTextureManager,
            Matrix4f positionMatrix,
            Matrix4f projectionMatrix,
            CallbackInfo ci) {

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;

        float tickDelta = tickCounter.getTickDelta(true);

        // Create our own MatrixStack and apply the camera position matrix
        MatrixStack matrices = new MatrixStack();
        matrices.multiplyPositionMatrix(positionMatrix);

        // Get the immediate vertex consumer provider
        VertexConsumerProvider.Immediate consumers = client.getBufferBuilders().getEntityVertexConsumers();

        if (PvpModClient.getConfig() != null) {
            if (PvpModClient.getConfig().isReach3DEnabled()) {
                if (PvpModClient.getRenderer3D() != null) {
                    PvpModClient.getRenderer3D().render(matrices, consumers, camera.getPos(), tickDelta);
                }
            } else {
                if (PvpModClient.getRenderer() != null) {
                    PvpModClient.getRenderer().render(matrices, consumers, camera.getPos(), tickDelta);
                }
            }
            // Flush our vertices — the game's own draw() calls are already done by TAIL
            consumers.draw();
        }
    }
}
