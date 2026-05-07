package com.pvpmod.client;

import com.pvpmod.client.config.ReachOverlayConfig;
import com.pvpmod.client.hud.HitDistanceHud;
import com.pvpmod.client.render.ReachOverlay3DRenderer;
import com.pvpmod.client.render.ReachOverlayRenderer;
import com.pvpmod.client.stats.HitDistanceTracker;
import com.pvpmod.client.stats.HitLogger;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.ActionResult;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PvpModClient implements ClientModInitializer {

    public static final String MOD_ID = "iwanttohitfromthreeblocks";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static KeyBinding toggleReachKey;
    private static KeyBinding toggleCrossKey;
    private static KeyBinding toggleStatsKey;
    private static KeyBinding switch2DKey;
    private static KeyBinding switch3DKey;
    private static ReachOverlayConfig config;
    private static ReachOverlayRenderer renderer;
    private static ReachOverlay3DRenderer renderer3D;
    private static HitDistanceTracker hitTracker;
    private static HitDistanceHud hitHud;
    private static HitLogger hitLogger;

    private static final String CATEGORY = "category.iwanttohitfromthreeblocks";

    @Override
    public void onInitializeClient() {
        LOGGER.info("[PVP Reach Overlay] Initializing...");

        config = ReachOverlayConfig.load();
        renderer = new ReachOverlayRenderer(config);
        renderer3D = new ReachOverlay3DRenderer(config);
        hitLogger = new HitLogger();
        hitTracker = new HitDistanceTracker(config, hitLogger);
        hitHud = new HitDistanceHud(config, hitTracker);

        // Keybinding: Toggle reach overlay (unbound by default)
        toggleReachKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.iwanttohitfromthreeblocks.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                CATEGORY
        ));

        // Keybinding: Toggle cross color (unbound by default)
        toggleCrossKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.iwanttohitfromthreeblocks.toggleCross",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                CATEGORY
        ));

        // Keybinding: Switch to 2D mode (DOWN arrow)
        switch2DKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.iwanttohitfromthreeblocks.switch2D",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_DOWN,
                CATEGORY
        ));

        // Keybinding: Switch to 3D mode (UP arrow)
        switch3DKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.iwanttohitfromthreeblocks.switch3D",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UP,
                CATEGORY
        ));

        // Keybinding: Toggle stats HUD (unbound by default)
        toggleStatsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.iwanttohitfromthreeblocks.toggleStats",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                CATEGORY
        ));

        // World render events (2D and 3D reach overlays) are now handled via WorldRendererMixin

        // Register HUD render for hit distance display
        HudRenderCallback.EVENT.register((ctx, tickDelta) -> {
            hitHud.render(ctx, tickDelta);
        });

        // Register attack entity callback for hit distance tracking
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient() && entity != null) {
                hitTracker.onAttackEntity(player, entity);
            }
            return ActionResult.PASS;
        });

        // Register tick events for keybindings (silent toggle) + combo death detection
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Check for combo target death each tick
            hitTracker.tick();

            if (toggleReachKey.wasPressed()) {
                config.setEnabled(!config.isEnabled());
                config.save();
            }
            if (toggleCrossKey.wasPressed()) {
                config.setCrossColorEnabled(!config.isCrossColorEnabled());
                config.save();
            }
            if (toggleStatsKey.wasPressed()) {
                config.setStatsHudEnabled(!config.isStatsHudEnabled());
                config.save();
            }
            if (switch2DKey.wasPressed()) {
                config.setReach3DEnabled(false);
                config.save();
            }
            if (switch3DKey.wasPressed()) {
                config.setReach3DEnabled(true);
                config.save();
            }
        });

        LOGGER.info("[PVP Reach Overlay] Initialized successfully.");
    }

    public static ReachOverlayConfig getConfig() {
        return config;
    }

    public static HitDistanceTracker getHitTracker() {
        return hitTracker;
    }

    public static ReachOverlayRenderer getRenderer() {
        return renderer;
    }

    public static ReachOverlay3DRenderer getRenderer3D() {
        return renderer3D;
    }
}
