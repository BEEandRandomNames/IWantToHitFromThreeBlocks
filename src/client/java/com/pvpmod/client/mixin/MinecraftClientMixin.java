package com.pvpmod.client.mixin;

import com.pvpmod.client.PvpModClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to detect miss-clicks (left-click on air or block, not entity).
 * Resets the bing combo streak when the player doesn't hit an entity.
 */
@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @Inject(method = "doAttack", at = @At("HEAD"))
    private void pvpReachOverlay$onDoAttack(CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient client = (MinecraftClient)(Object) this;
        if (client.crosshairTarget == null
                || client.crosshairTarget.getType() != HitResult.Type.ENTITY) {
            // Miss-click or block hit — reset combo
            if (PvpModClient.getHitTracker() != null) {
                PvpModClient.getHitTracker().resetCombo();
            }
        }
    }
}
