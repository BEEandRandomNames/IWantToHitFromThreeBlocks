package com.pvpmod.client.stats;

import com.pvpmod.client.PvpModClient;
import com.pvpmod.client.config.ReachOverlayConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * Tracks hit distances when the player attacks an entity.
 * Calculates distance from eye position to closest point on
 * entity hitbox (matching Minecraft's internal reach check).
 *
 * Features:
 * - "Bing" bell sound on threshold hits (>= threshold)
 * - Combo system: 3+ consecutive threshold hits = escalating pitch
 *   Pitch goes from 0.5f (lowest note) to 2.0f (highest note),
 *   stepping through note block's 25 pitch levels.
 * - After max pitch: zombie door break sound (bone-crushing effect)
 * - On combo target death: wither death sound (victory fanfare)
 * - Miss or below-threshold resets combo.
 */
public class HitDistanceTracker {

    private final ReachOverlayConfig config;
    private final HitLogger logger;

    // Last hit info (read by HUD renderer)
    private double lastHitDistance = -1;
    private long lastHitTime = 0;

    // Combo tracking
    private int comboStreak = 0;  // consecutive hits >= threshold
    private Entity comboTarget = null; // entity being combo'd
    private boolean comboTargetWasAlive = false; // track death transition

    // Max note level: combo hit 2 + 24 steps = combo 26 is max bell pitch
    private static final int MAX_BELL_COMBO = 26;

    public HitDistanceTracker(ReachOverlayConfig config, HitLogger logger) {
        this.config = config;
        this.logger = logger;
    }

    /**
     * Called when the player attacks an entity.
     */
    public void onAttackEntity(PlayerEntity player, Entity target) {
        if (player == null || target == null) return;

        // Calculate distance from player's eye to closest point on target hitbox
        Vec3d eyePos = player.getEyePos();
        Box targetBox = target.getBoundingBox();

        double closestX = Math.max(targetBox.minX, Math.min(eyePos.x, targetBox.maxX));
        double closestY = Math.max(targetBox.minY, Math.min(eyePos.y, targetBox.maxY));
        double closestZ = Math.max(targetBox.minZ, Math.min(eyePos.z, targetBox.maxZ));

        double distance = eyePos.distanceTo(new Vec3d(closestX, closestY, closestZ));

        // Store for HUD display
        long now = System.currentTimeMillis();

        // Combo timeout: if more than 1 second since last hit, reset combo
        if (lastHitTime > 0 && (now - lastHitTime) > 1000) {
            comboStreak = 0;
            comboTarget = null;
        }

        lastHitDistance = distance;
        lastHitTime = now;

        // Log the hit (file logging only if enabled, session stats always tracked)
        String targetName = target.getType().getName().getString();
        logger.trackHit(distance); // always track for session avg
        if (config.isHitLoggingEnabled()) {
            logger.logHit(distance, targetName); // write to CSV file
        }

        // Combo + bing sound logic
        if (config.isBingSoundEnabled() && distance >= config.getBingSoundThreshold()) {
            // If switching targets mid-combo, reset
            if (comboTarget != null && comboTarget != target) {
                comboStreak = 0;
            }

            comboStreak++;
            comboTarget = target;
            comboTargetWasAlive = isEntityAlive(target);

            if (comboStreak > MAX_BELL_COMBO) {
                // Past max bell pitch — play zombie door break sound
                playDoorBreakSound(player);
            } else {
                float pitch = calculateComboPitch();
                playBingSound(player, pitch);
            }
        } else {
            // Below threshold — reset combo
            comboStreak = 0;
            comboTarget = null;
        }
    }

    /**
     * Called every client tick to detect combo target death.
     * When the combo target entity dies during the door-break phase,
     * play wither death sound as a victory fanfare.
     */
    public void tick() {
        if (comboTarget == null || comboStreak == 0) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        boolean alive = isEntityAlive(comboTarget);

        // Detect death transition: was alive, now dead/removed
        if (comboTargetWasAlive && !alive) {
            // Only play wither death if we reached the door-break phase
            if (comboStreak > MAX_BELL_COMBO) {
                playWitherDeathSound(client.player);
            }
            // Reset combo regardless
            comboStreak = 0;
            comboTarget = null;
            comboTargetWasAlive = false;
        }
    }

    /**
     * Check if an entity is still alive and in the world.
     */
    private boolean isEntityAlive(Entity entity) {
        if (entity == null || entity.isRemoved()) return false;
        if (entity instanceof LivingEntity living) {
            return living.isAlive() && living.getHealth() > 0;
        }
        return !entity.isRemoved();
    }

    /**
     * Calculate the pitch based on current combo streak.
     * Hits 1-2: base pitch (0.5f, lowest note block tuning)
     * Hits 3+: pitch escalates through note block's 25 levels
     * until reaching 2.0f (highest), then stays there.
     *
     * Note block pitch formula: pitch = 2^((note - 12) / 12)
     * note 0 = 0.5f, note 12 = 1.0f, note 24 = 2.0f
     */
    private float calculateComboPitch() {
        if (comboStreak <= 2) {
            return 0.5f; // base pitch for first 2 hits
        }
        // From 3rd hit onwards, escalate through 25 note levels
        int comboNote = Math.min(24, comboStreak - 2);
        return (float) Math.pow(2.0, (comboNote - 12) / 12.0);
    }

    /**
     * Play the note block bell sound with specified pitch.
     */
    private void playBingSound(PlayerEntity player, float pitch) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        client.world.playSound(
                player,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(),
                SoundCategory.PLAYERS,
                1.0f,   // volume
                pitch
        );
    }

    /**
     * Play zombie door break sound (bone-crushing effect for ultra combos).
     */
    private void playDoorBreakSound(PlayerEntity player) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        client.world.playSound(
                player,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR,
                SoundCategory.PLAYERS,
                0.8f,   // volume (slightly lower to not be too jarring)
                1.0f    // fixed pitch
        );
    }

    /**
     * Play wither death sound when combo target is killed.
     * The ultimate victory fanfare.
     */
    private void playWitherDeathSound(PlayerEntity player) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        client.world.playSound(
                player,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_WITHER_DEATH,
                SoundCategory.PLAYERS,
                1.0f,   // volume
                1.0f    // normal pitch
        );
    }

    // ── Getters for HUD ──────────────────────────────────────────

    public double getLastHitDistance() { return lastHitDistance; }
    public long getLastHitTime() { return lastHitTime; }
    public int getComboStreak() { return comboStreak; }
    public void resetCombo() { comboStreak = 0; comboTarget = null; }

    public long getTimeSinceLastHit() {
        if (lastHitTime == 0) return Long.MAX_VALUE;
        return System.currentTimeMillis() - lastHitTime;
    }

    public HitLogger getLogger() { return logger; }

    /**
     * Get performance rank based on session average distance.
     * Higher average = better (hitting from max range).
     */
    public static String getRankLetter(double avg) {
        if (avg >= 2.80) return "S";
        if (avg >= 2.60) return "A";
        if (avg >= 2.40) return "B";
        if (avg >= 2.20) return "C";
        return "D";
    }

    /**
     * Get rank color code for display.
     * S = rainbow (cycling), A = light green, B = yellow,
     * C = orange, D = light red.
     */
    public static int getRankColor(double avg) {
        if (avg >= 2.80) return 0; // S = special rainbow, handled separately
        if (avg >= 2.60) return 0xFF88FF88; // A = light green
        if (avg >= 2.40) return 0xFFFFFF44; // B = yellow
        if (avg >= 2.20) return 0xFFFF8844; // C = orange
        return 0xFFFF6666;                   // D = light red
    }

    /**
     * Get rainbow color for S rank (cycles through hue over time).
     */
    public static int getSRankRainbowColor() {
        float hue = (System.currentTimeMillis() % 3000) / 3000.0f;
        return 0xFF000000 | java.awt.Color.HSBtoRGB(hue, 0.8f, 1.0f);
    }
}
