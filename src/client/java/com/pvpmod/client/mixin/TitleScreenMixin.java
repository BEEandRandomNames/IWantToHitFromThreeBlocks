package com.pvpmod.client.mixin;

import com.pvpmod.client.update.UpdateChecker;
import com.pvpmod.client.update.UpdateNotificationScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into the Title Screen to show update notification.
 * Only shows once per game session (static flag).
 * Uses render injection instead of init to avoid screen-switch during init.
 */
@Mixin(TitleScreen.class)
public class TitleScreenMixin {

    /** Ensures we only show the notification once per game session. */
    @Unique
    private static boolean pvpmod$notificationShown = false;

    @Inject(method = "render", at = @At("TAIL"))
    private void pvpmod$onTitleScreenRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (pvpmod$notificationShown) return;
        if (!UpdateChecker.isChecked()) return;

        // Mark as shown regardless of whether there's an update
        pvpmod$notificationShown = true;

        if (!UpdateChecker.isUpdateAvailable()) return;

        // Schedule screen switch for next tick to avoid render-during-render issues
        MinecraftClient client = MinecraftClient.getInstance();
        Screen titleScreen = (Screen) (Object) this;
        client.execute(() -> {
            client.setScreen(
                    new UpdateNotificationScreen(
                            titleScreen,
                            UpdateChecker.getCurrentVersion(),
                            UpdateChecker.getLatestVersion(),
                            UpdateChecker.getDownloadUrl()
                    )
            );
        });
    }
}
