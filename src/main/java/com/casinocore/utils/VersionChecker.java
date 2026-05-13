package com.casinocore.utils;

import com.casinocore.core.CasinoPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VersionChecker implements Listener {

    private static final String VERSION_URL = "https://plugglab.github.io/stable.json";
    private static final String ADMIN_PERMISSION = "casinocore.admin";
    private static final Pattern STRING_FIELD_PATTERN =
        Pattern.compile("\"(version|latestVersion|latest|name)\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern URL_FIELD_PATTERN =
        Pattern.compile("\"(url|downloadUrl|download|pageUrl)\"\\s*:\\s*\"([^\"]+)\"");

    private final CasinoPlugin plugin;
    private final HttpClient httpClient;

    private volatile VersionStatus status = VersionStatus.unknown();

    public VersionChecker(CasinoPlugin plugin) {
        this.plugin = plugin;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    public void checkAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin.getPlugin(), () -> {
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(VERSION_URL))
                    .timeout(Duration.ofSeconds(10))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    status = VersionStatus.error("Version feed returned HTTP " + response.statusCode());
                    plugin.getPlugin().getLogger().warning("Version check failed: HTTP " + response.statusCode());
                    return;
                }

                String body = response.body();
                String remoteVersion = extractField(body, STRING_FIELD_PATTERN);
                String downloadUrl = extractField(body, URL_FIELD_PATTERN);

                if (remoteVersion == null || remoteVersion.isBlank()) {
                    status = VersionStatus.error("Version feed did not contain a supported version field");
                    plugin.getPlugin().getLogger().warning("Version check failed: no supported version field in " + VERSION_URL);
                    return;
                }

                String currentVersion = plugin.getPlugin().getDescription().getVersion();
                boolean updateAvailable = !currentVersion.equalsIgnoreCase(remoteVersion.trim());
                status = new VersionStatus(currentVersion, remoteVersion.trim(), updateAvailable, downloadUrl, null);

                if (updateAvailable) {
                    plugin.getPlugin().getLogger().warning(
                        "Update available: current=" + currentVersion + ", latest=" + remoteVersion.trim()
                    );
                }
            } catch (Exception exception) {
                status = VersionStatus.error(exception.getMessage());
                plugin.getPlugin().getLogger().warning("Version check failed: " + exception.getMessage());
            }
        });
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission(ADMIN_PERMISSION)) {
            return;
        }

        VersionStatus currentStatus = status;
        if (currentStatus.errorMessage() != null) {
            plugin.getMessageManager().sendPlayer(
                player,
                "<yellow>CasinoCore version check failed:</yellow> <white>" + escapeMiniMessage(currentStatus.errorMessage()) + "</white>"
            );
            return;
        }

        if (!currentStatus.updateAvailable()) {
            return;
        }

        StringBuilder message = new StringBuilder()
            .append("<yellow>CasinoCore update available.</yellow> ")
            .append("<gray>Current:</gray> <white>").append(escapeMiniMessage(currentStatus.currentVersion())).append("</white> ")
            .append("<gray>Latest:</gray> <green>").append(escapeMiniMessage(currentStatus.latestVersion())).append("</green>");

        if (currentStatus.downloadUrl() != null && !currentStatus.downloadUrl().isBlank()) {
            message.append(" <gray>URL:</gray> <aqua>").append(escapeMiniMessage(currentStatus.downloadUrl())).append("</aqua>");
        }

        plugin.getMessageManager().sendPlayer(player, message.toString());
    }

    private String extractField(String body, Pattern pattern) {
        Matcher matcher = pattern.matcher(body);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(2);
    }

    private String escapeMiniMessage(String value) {
        return value.replace("<", "\\<").replace(">", "\\>");
    }

    private record VersionStatus(
        String currentVersion,
        String latestVersion,
        boolean updateAvailable,
        String downloadUrl,
        String errorMessage
    ) {
        private static VersionStatus unknown() {
            return new VersionStatus(null, null, false, null, null);
        }

        private static VersionStatus error(String errorMessage) {
            return new VersionStatus(null, null, false, null, errorMessage == null ? "unknown error" : errorMessage);
        }
    }
}
