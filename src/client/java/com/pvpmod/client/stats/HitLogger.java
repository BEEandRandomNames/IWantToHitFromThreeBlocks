package com.pvpmod.client.stats;

import com.pvpmod.client.PvpModClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Logs every hit distance to a CSV file in .minecraft/iwanttohitfromthreeblocks/.
 * Files are named hit_log_YYYY-MM-DD.csv and are shareable.
 *
 * Uses semicolon (;) separator and UTF-8 BOM for proper Excel compatibility
 * with Turkish locale systems.
 */
public class HitLogger {

    private static final String LOG_DIR = "IWTHFTBlocks-log";
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private final List<Double> sessionDistances = new ArrayList<>();
    private double sessionMaxDistance = 0.0;

    /**
     * Log a hit to the CSV file and track in session.
     */
    public void trackHit(double distance) {
        sessionDistances.add(distance);
        if (distance > sessionMaxDistance) {
            sessionMaxDistance = distance;
        }
    }

    /**
     * Log a hit to the CSV file (only when logging is enabled).
     */
    public void logHit(double distance, String targetName) {

        try {
            Path dir = getLogDir();
            Files.createDirectories(dir);

            String date = LocalDate.now().toString(); // YYYY-MM-DD
            Path file = dir.resolve("hit_log_" + date + ".csv");

            // Write header with UTF-8 BOM if new file
            if (!Files.exists(file)) {
                try (OutputStream os = Files.newOutputStream(file, StandardOpenOption.CREATE)) {
                    os.write(UTF8_BOM);
                    os.write("time;distance;target\n".getBytes(StandardCharsets.UTF_8));
                }
            }

            String time = LocalDateTime.now().format(TIME_FMT);
            // Use comma as decimal separator for TR Excel compatibility
            String distStr = String.format(java.util.Locale.GERMAN, "%.2f", distance);
            String line = String.format("%s;%s;%s\n", time, distStr, targetName);
            Files.writeString(file, line,
                    StandardCharsets.UTF_8, StandardOpenOption.APPEND);

        } catch (IOException e) {
            PvpModClient.LOGGER.warn("[PVP Reach Overlay] Failed to write hit log.", e);
        }
    }

    /**
     * Get the average distance of all hits this session.
     */
    public double getSessionAverage() {
        if (sessionDistances.isEmpty()) return 0.0;
        double sum = 0;
        for (double d : sessionDistances) sum += d;
        return sum / sessionDistances.size();
    }

    /**
     * Get total number of hits this session.
     */
    public int getSessionHitCount() {
        return sessionDistances.size();
    }

    /**
     * Get hit count as display string (capped at 999+).
     */
    public String getHitCountDisplay() {
        int count = sessionDistances.size();
        return count > 999 ? "999+" : String.valueOf(count);
    }

    /**
     * Get the maximum hit distance recorded this session.
     */
    public double getSessionMaxDistance() {
        return sessionMaxDistance;
    }

    /**
     * Clear session statistics (counter only, logs are preserved).
     */
    public void clearSession() {
        sessionDistances.clear();
        sessionMaxDistance = 0.0;
    }

    private static Path getLogDir() {
        return FabricLoader.getInstance().getGameDir().resolve(LOG_DIR);
    }
}
