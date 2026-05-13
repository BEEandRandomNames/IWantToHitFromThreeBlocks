package com.pvpmod.client.update;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pvpmod.client.PvpModClient;
import net.fabricmc.loader.api.FabricLoader;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Checks for mod updates by fetching a JSON manifest from GitHub.
 * Runs asynchronously on a daemon thread to avoid blocking game startup.
 *
 * The update.json format:
 * {
 *   "latest_version": "1.1.0",
 *   "download_url": "https://modrinth.com/mod/iwanttohitfromthreeblocks",
 *   "versions": {
 *     "1.21.1": "1.1.0",
 *     "1.20.2": "1.1.0",
 *     ...
 *   }
 * }
 */
public class UpdateChecker {

    private static final String UPDATE_URL =
            "https://raw.githubusercontent.com/BEEandRandomNames/IWantToHitFromThreeBlocks/main/update.json";

    // Update state (read by TitleScreenMixin)
    private static volatile boolean checked = false;
    private static volatile boolean updateAvailable = false;
    private static volatile String currentVersion = "";
    private static volatile String latestVersion = "";
    private static volatile String downloadUrl = "";

    /**
     * Start the async update check. Called once during mod initialization.
     */
    public static void checkAsync() {
        Thread thread = new Thread(() -> {
            try {
                // Get current mod version from fabric.mod.json
                currentVersion = FabricLoader.getInstance()
                        .getModContainer(PvpModClient.MOD_ID)
                        .map(c -> c.getMetadata().getVersion().getFriendlyString())
                        .orElse("0.0.0");

                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(5))
                        .build();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(UPDATE_URL))
                        .timeout(Duration.ofSeconds(5))
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();

                    // Try to find version-specific update first, then fall back to latest_version
                    String mcVersion = getMcVersion();
                    String remoteVersion;

                    if (json.has("versions") && json.getAsJsonObject("versions").has(mcVersion)) {
                        remoteVersion = json.getAsJsonObject("versions").get(mcVersion).getAsString();
                    } else {
                        remoteVersion = json.get("latest_version").getAsString();
                    }

                    downloadUrl = json.has("download_url")
                            ? json.get("download_url").getAsString()
                            : "https://modrinth.com/mod/iwanttohitfromthreeblocks";

                    latestVersion = remoteVersion;
                    updateAvailable = isNewer(remoteVersion, currentVersion);

                    PvpModClient.LOGGER.info("[PVP Reach Overlay] Update check: current={}, remote={}, updateAvailable={}",
                            currentVersion, remoteVersion, updateAvailable);
                }
            } catch (Exception e) {
                // Silently ignore — no internet, timeout, parse error, etc.
                PvpModClient.LOGGER.debug("[PVP Reach Overlay] Update check failed: {}", e.getMessage());
            } finally {
                checked = true;
            }
        }, "PVP-Mod-UpdateChecker");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Compare two semantic version strings (e.g., "1.2.3" > "1.2.0").
     * Returns true if 'remote' is strictly newer than 'current'.
     */
    private static boolean isNewer(String remote, String current) {
        try {
            String[] r = remote.split("\\.");
            String[] c = current.split("\\.");
            int len = Math.max(r.length, c.length);
            for (int i = 0; i < len; i++) {
                int rv = i < r.length ? Integer.parseInt(r[i]) : 0;
                int cv = i < c.length ? Integer.parseInt(c[i]) : 0;
                if (rv > cv) return true;
                if (rv < cv) return false;
            }
        } catch (NumberFormatException ignored) {}
        return false;
    }

    /**
     * Get the current Minecraft version from Fabric Loader.
     */
    private static String getMcVersion() {
        return FabricLoader.getInstance()
                .getModContainer("minecraft")
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }

    // ── Getters (thread-safe via volatile) ──

    public static boolean isChecked() { return checked; }
    public static boolean isUpdateAvailable() { return updateAvailable; }
    public static String getCurrentVersion() { return currentVersion; }
    public static String getLatestVersion() { return latestVersion; }
    public static String getDownloadUrl() { return downloadUrl; }

    /**
     * FOR TESTING ONLY: Force the update notification to appear.
     * Call this method to simulate an available update.
     */
    public static void simulateUpdate() {
        currentVersion = "1.0.0";
        latestVersion = "1.1.0";
        downloadUrl = "https://modrinth.com/mod/iwanttohitfromthreeblocks";
        updateAvailable = true;
        checked = true;
    }
}
