package com.casinocore.utils;

import com.casinocore.core.CasinoPlugin;
import com.casinocore.core.PluginVariant;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProtectionManager {

    private static final String PRODUCT_NAME = "CasinoCore";
    private static final Pattern JSON_STRING_PATTERN_TEMPLATE = Pattern.compile("\"%s\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final CasinoPlugin plugin;
    private final HttpClient httpClient;
    private boolean valid = true;
    private String failureReason = "";
    private long lastValidatedAtEpochSecond = 0L;

    public ProtectionManager(CasinoPlugin plugin) {
        this.plugin = plugin;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    public boolean initialize() {
        PluginVariant variant = plugin.getVariant();
        if (!variant.isProtectionEnabled()) {
            valid = true;
            return true;
        }

        valid = validateNow(true);
        if (valid) {
            long refreshMinutes = Math.max(1L, plugin.getConfigManager().getConfig().getLong("license.api.refresh-interval-minutes", 15L));
            Bukkit.getScheduler().runTaskTimerAsynchronously(
                plugin.getPlugin(),
                this::validateLater,
                TimeUnit.MINUTES.toSeconds(1L) * 20L,
                TimeUnit.MINUTES.toSeconds(refreshMinutes) * 20L
            );
        }
        return valid;
    }

    public boolean isValid() {
        return valid;
    }

    public boolean canUseProtectedFeature(Player player) {
        if (valid) {
            return true;
        }

        if (player != null) {
            player.sendMessage("CasinoCore protection blocked this build: " + failureReason);
        }
        return false;
    }

    private void validateLater() {
        if (!validateNow(false)) {
            plugin.getPlugin().getLogger().severe("Protection validation failed after startup: " + failureReason);
            Bukkit.getScheduler().runTask(plugin.getPlugin(), () -> plugin.getPlugin().getServer().getPluginManager().disablePlugin(plugin.getPlugin()));
        }
    }

    private boolean validateNow(boolean startup) {
        try {
            ValidationRequest request = ValidationRequest.create(plugin, getCurrentJarSha256());
            ValidationToken token = validateOnline(request);
            validateToken(token, request);
            writeCache(token);
            markValid("Protection validation passed for owner " + token.owner + " using online token.");
            return true;
        } catch (Exception onlineException) {
            plugin.getPlugin().getLogger().log(Level.WARNING, "Online protection validation failed. Trying cached token.", onlineException);
            try {
                ValidationRequest request = ValidationRequest.create(plugin, getCurrentJarSha256());
                ValidationToken cachedToken = readCachedToken();
                validateCachedToken(cachedToken, request);
                markValid("Protection validation passed using cached token.");
                return true;
            } catch (Exception cacheException) {
                plugin.getPlugin().getLogger().log(startup ? Level.SEVERE : Level.WARNING, "Cached protection token validation failed.", cacheException);
                fail(cacheException.getMessage() == null || cacheException.getMessage().isBlank() ? "validation error" : cacheException.getMessage());
                return false;
            }
        }
    }

    private ValidationToken validateOnline(ValidationRequest request) throws Exception {
        String url = plugin.getConfigManager().getConfig().getString("license.api.validate-url", "").trim();
        if (url.isEmpty()) {
            throw new IllegalStateException("license.api.validate-url is missing");
        }

        int timeoutMs = Math.max(1000, plugin.getConfigManager().getConfig().getInt("license.api.timeout-ms", 10000));
        String userAgent = plugin.getConfigManager().getConfig().getString("license.api.user-agent", "CasinoCore-License/1.0");
        HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofMillis(timeoutMs))
            .header("Content-Type", "application/json; charset=utf-8")
            .header("Accept", "application/json")
            .header("User-Agent", userAgent)
            .POST(HttpRequest.BodyPublishers.ofString(request.toJson(), StandardCharsets.UTF_8))
            .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("license api returned status " + response.statusCode());
        }

        String body = response.body();
        if (!readJsonBoolean(body, "ok", true)) {
            String error = readJsonString(body, "error", "license api rejected request");
            throw new IllegalStateException(error);
        }

        String payload = readJsonRequiredString(body, "payload");
        String signature = readJsonRequiredString(body, "signature");
        verifySignature(payload, signature);
        return ValidationToken.parse(payload, signature);
    }

    private void validateToken(ValidationToken token, ValidationRequest request) {
        long now = Instant.now().getEpochSecond();
        if (!PRODUCT_NAME.equals(token.product)) {
            throw new IllegalStateException("license product mismatch");
        }
        if (token.issuedAtEpochSecond > 0 && token.issuedAtEpochSecond > now + 300L) {
            throw new IllegalStateException("license token issued in the future");
        }
        if (token.expiresAtEpochSecond > 0 && now > token.expiresAtEpochSecond) {
            throw new IllegalStateException("license token expired");
        }
        if (!token.nonce.isBlank() && !token.nonce.equals(request.nonce)) {
            throw new IllegalStateException("license nonce mismatch");
        }
        if (!token.variant.isBlank() && !token.variant.equalsIgnoreCase(request.variant)) {
            throw new IllegalStateException("license variant mismatch");
        }
        if (!token.jarHash.isBlank() && !token.jarHash.equalsIgnoreCase(request.jarHash)) {
            throw new IllegalStateException("jar hash mismatch");
        }
        if (!token.serverId.isBlank() && !token.serverId.equalsIgnoreCase(request.serverId)) {
            throw new IllegalStateException("server id mismatch");
        }
        if (!token.owner.isBlank() && !request.owner.isBlank() && !token.owner.equalsIgnoreCase(request.owner)) {
            throw new IllegalStateException("license owner mismatch");
        }
        if (!token.licenseKeyHash.isBlank() && !token.licenseKeyHash.equalsIgnoreCase(hashLicenseKey(request.licenseKey))) {
            throw new IllegalStateException("license key hash mismatch");
        }
    }

    private void validateCachedToken(ValidationToken token, ValidationRequest request) {
        validateToken(token, request);
        long now = Instant.now().getEpochSecond();
        long graceMinutes = Math.max(0L, plugin.getConfigManager().getConfig().getLong("license.api.offline-grace-minutes", 1440L));
        long maxOfflineEpochSecond = token.expiresAtEpochSecond + TimeUnit.MINUTES.toSeconds(graceMinutes);
        if (token.expiresAtEpochSecond <= 0L || now > maxOfflineEpochSecond) {
            throw new IllegalStateException("cached license token expired");
        }
    }

    private ValidationToken readCachedToken() throws Exception {
        File cacheFile = getCacheFile();
        if (!cacheFile.isFile()) {
            throw new IllegalStateException("license cache is missing");
        }

        YamlConfiguration cache = YamlConfiguration.loadConfiguration(cacheFile);
        String payload = cache.getString("payload", "").trim();
        String signature = cache.getString("signature", "").trim();
        if (payload.isEmpty() || signature.isEmpty()) {
            throw new IllegalStateException("license cache is incomplete");
        }

        verifySignature(payload, signature);
        return ValidationToken.parse(payload, signature);
    }

    private void writeCache(ValidationToken token) throws Exception {
        File cacheFile = getCacheFile();
        File parent = cacheFile.getParentFile();
        if (parent != null && !parent.isDirectory()) {
            Files.createDirectories(parent.toPath());
        }

        YamlConfiguration cache = new YamlConfiguration();
        cache.set("payload", token.payloadLine);
        cache.set("signature", token.signatureLine);
        cache.set("meta.owner", token.owner);
        cache.set("meta.server-id", token.serverId);
        cache.set("meta.variant", token.variant);
        cache.set("meta.issued-at", token.issuedAtEpochSecond);
        cache.set("meta.expires-at", token.expiresAtEpochSecond);
        cache.set("meta.refresh-after", token.refreshAfterEpochSecond);
        cache.save(cacheFile);
    }

    private File getCacheFile() {
        String fileName = plugin.getConfigManager().getConfig().getString("license.cache.file", "license-cache.yml").trim();
        if (fileName.isEmpty()) {
            fileName = "license-cache.yml";
        }
        return new File(plugin.getPlugin().getDataFolder(), fileName);
    }

    private void verifySignature(String payloadLine, String signatureLine) throws Exception {
        ConfigurationSection section = plugin.getConfigManager().getConfig().getConfigurationSection("license.public-key");
        if (section == null) {
            throw new IllegalStateException("public key not configured");
        }

        String pem = String.join("", section.getStringList("lines"))
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replaceAll("\\s+", "");
        byte[] keyBytes = Base64.getDecoder().decode(pem);
        PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(keyBytes));

        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(publicKey);
        signature.update(payloadLine.getBytes(StandardCharsets.UTF_8));
        if (!signature.verify(Base64.getDecoder().decode(signatureLine))) {
            throw new IllegalStateException("invalid license signature");
        }
    }

    private String getCurrentJarSha256() throws Exception {
        File source = new File(plugin.getPlugin().getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
        byte[] bytes = Files.readAllBytes(source.toPath());
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
        StringBuilder builder = new StringBuilder();
        for (byte value : digest) {
            builder.append(String.format(Locale.ROOT, "%02x", value));
        }
        return builder.toString();
    }

    private String hashLicenseKey(String licenseKey) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(licenseKey.trim().getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte value : digest) {
                builder.append(String.format(Locale.ROOT, "%02x", value));
            }
            return builder.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("failed to hash license key", exception);
        }
    }

    private void markValid(String logMessage) {
        valid = true;
        failureReason = "";
        lastValidatedAtEpochSecond = Instant.now().getEpochSecond();
        plugin.getPlugin().getLogger().info(logMessage + " lastValidatedAt=" + lastValidatedAtEpochSecond);
    }

    private void fail(String reason) {
        valid = false;
        failureReason = reason;
    }

    private String readJsonRequiredString(String json, String key) {
        String value = readJsonString(json, key, null);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("license api response missing " + key);
        }
        return value;
    }

    private String readJsonString(String json, String key, String fallback) {
        Pattern pattern = Pattern.compile(String.format(Locale.ROOT, JSON_STRING_PATTERN_TEMPLATE.pattern(), Pattern.quote(key)));
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) {
            return fallback;
        }

        return unescapeJson(matcher.group(1));
    }

    private boolean readJsonBoolean(String json, String key, boolean fallback) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(true|false)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) {
            return fallback;
        }
        return Boolean.parseBoolean(matcher.group(1));
    }

    private String unescapeJson(String value) {
        return value
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
            .replace("\\/", "/")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t");
    }

    private static final class ValidationRequest {
        private final String product;
        private final String variant;
        private final String serverId;
        private final String owner;
        private final String licenseKey;
        private final String jarHash;
        private final String instanceId;
        private final String machineFingerprint;
        private final String nonce;
        private final String pluginVersion;
        private final long timestampEpochSecond;

        private ValidationRequest(
            String product,
            String variant,
            String serverId,
            String owner,
            String licenseKey,
            String jarHash,
            String instanceId,
            String machineFingerprint,
            String nonce,
            String pluginVersion,
            long timestampEpochSecond
        ) {
            this.product = product;
            this.variant = variant;
            this.serverId = serverId;
            this.owner = owner;
            this.licenseKey = licenseKey;
            this.jarHash = jarHash;
            this.instanceId = instanceId;
            this.machineFingerprint = machineFingerprint;
            this.nonce = nonce;
            this.pluginVersion = pluginVersion;
            this.timestampEpochSecond = timestampEpochSecond;
        }

        private static ValidationRequest create(CasinoPlugin plugin, String jarHash) {
            String licenseKey = plugin.getConfigManager().getConfig().getString("license.key", "").trim();
            if (licenseKey.isEmpty()) {
                throw new IllegalStateException("license.key is missing");
            }

            String serverId = plugin.getConfigManager().getConfig().getString("license.server-id", "").trim();
            if (serverId.isEmpty()) {
                throw new IllegalStateException("license.server-id is missing");
            }

            String owner = plugin.getConfigManager().getConfig().getString("license.owner", "").trim();
            String variant = plugin.getVariant().name().toLowerCase(Locale.ROOT);
            String pluginVersion = plugin.getPlugin().getDescription().getVersion();
            String instanceId = resolveOrCreateInstanceId(plugin);
            String machineFingerprint = calculateMachineFingerprint(plugin, serverId, jarHash, instanceId);
            return new ValidationRequest(
                PRODUCT_NAME,
                variant,
                serverId,
                owner,
                licenseKey,
                jarHash,
                instanceId,
                machineFingerprint,
                generateNonce(),
                pluginVersion,
                Instant.now().getEpochSecond()
            );
        }

        private String toJson() {
            return "{"
                + "\"product\":\"" + jsonEscape(product) + "\","
                + "\"variant\":\"" + jsonEscape(variant) + "\","
                + "\"serverId\":\"" + jsonEscape(serverId) + "\","
                + "\"owner\":\"" + jsonEscape(owner) + "\","
                + "\"licenseKey\":\"" + jsonEscape(licenseKey) + "\","
                + "\"jarHash\":\"" + jsonEscape(jarHash) + "\","
                + "\"instanceId\":\"" + jsonEscape(instanceId) + "\","
                + "\"machineFingerprint\":\"" + jsonEscape(machineFingerprint) + "\","
                + "\"nonce\":\"" + jsonEscape(nonce) + "\","
                + "\"pluginVersion\":\"" + jsonEscape(pluginVersion) + "\","
                + "\"timestamp\":" + timestampEpochSecond
                + "}";
        }

        private static String generateNonce() {
            byte[] bytes = new byte[16];
            RANDOM.nextBytes(bytes);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        }

        private static String jsonEscape(String value) {
            return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
        }

        private static String resolveOrCreateInstanceId(CasinoPlugin plugin) {
            try {
                File dataFolder = plugin.getPlugin().getDataFolder();
                if (!dataFolder.isDirectory()) {
                    Files.createDirectories(dataFolder.toPath());
                }

                String fileName = plugin.getConfigManager().getConfig().getString("license.instance.file", "instance-id.txt").trim();
                if (fileName.isEmpty()) {
                    fileName = "instance-id.txt";
                }

                File instanceFile = new File(dataFolder, fileName);
                if (instanceFile.isFile()) {
                    String stored = Files.readString(instanceFile.toPath(), StandardCharsets.UTF_8).trim();
                    if (!stored.isEmpty()) {
                        return stored;
                    }
                }

                byte[] bytes = new byte[24];
                RANDOM.nextBytes(bytes);
                String generated = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
                Files.writeString(instanceFile.toPath(), generated, StandardCharsets.UTF_8);
                return generated;
            } catch (Exception exception) {
                throw new IllegalStateException("failed to resolve instance id", exception);
            }
        }

        private static String calculateMachineFingerprint(CasinoPlugin plugin, String serverId, String jarHash, String instanceId) {
            try {
                StringBuilder source = new StringBuilder();
                source.append(plugin.getPlugin().getServer().getIp()).append('|');
                source.append(plugin.getPlugin().getServer().getPort()).append('|');
                source.append(plugin.getPlugin().getServer().getWorldContainer().getAbsolutePath()).append('|');
                source.append(plugin.getPlugin().getDataFolder().getAbsolutePath()).append('|');
                source.append(serverId).append('|');
                source.append(jarHash).append('|');
                source.append(instanceId);

                byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(source.toString().getBytes(StandardCharsets.UTF_8));
                StringBuilder builder = new StringBuilder();
                for (byte value : digest) {
                    builder.append(String.format(Locale.ROOT, "%02x", value));
                }
                return builder.toString();
            } catch (Exception exception) {
                throw new IllegalStateException("failed to calculate machine fingerprint", exception);
            }
        }
    }

    private static final class ValidationToken {
        private final String payloadLine;
        private final String signatureLine;
        private final String product;
        private final String owner;
        private final String serverId;
        private final String variant;
        private final String jarHash;
        private final String nonce;
        private final String licenseKeyHash;
        private final long issuedAtEpochSecond;
        private final long refreshAfterEpochSecond;
        private final long expiresAtEpochSecond;

        private ValidationToken(
            String payloadLine,
            String signatureLine,
            String product,
            String owner,
            String serverId,
            String variant,
            String jarHash,
            String nonce,
            String licenseKeyHash,
            long issuedAtEpochSecond,
            long refreshAfterEpochSecond,
            long expiresAtEpochSecond
        ) {
            this.payloadLine = payloadLine;
            this.signatureLine = signatureLine;
            this.product = product;
            this.owner = owner;
            this.serverId = serverId;
            this.variant = variant;
            this.jarHash = jarHash;
            this.nonce = nonce;
            this.licenseKeyHash = licenseKeyHash;
            this.issuedAtEpochSecond = issuedAtEpochSecond;
            this.refreshAfterEpochSecond = refreshAfterEpochSecond;
            this.expiresAtEpochSecond = expiresAtEpochSecond;
        }

        private static ValidationToken parse(String payloadLine, String signatureLine) {
            String rawPayload = new String(Base64.getDecoder().decode(payloadLine), StandardCharsets.UTF_8);
            YamlConfiguration parsed = YamlConfiguration.loadConfiguration(new StringReader(rawPayload));
            return new ValidationToken(
                payloadLine,
                signatureLine,
                parsed.getString("product", ""),
                parsed.getString("owner", ""),
                parsed.getString("serverId", ""),
                parsed.getString("variant", ""),
                parsed.getString("jarHash", ""),
                parsed.getString("nonce", ""),
                parsed.getString("licenseKeyHash", ""),
                parsed.getLong("issuedAt", 0L),
                parsed.getLong("refreshAfter", 0L),
                parsed.getLong("expiresAt", 0L)
            );
        }
    }
}
