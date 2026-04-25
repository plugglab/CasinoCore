package com.casinocore.utils;

import com.casinocore.core.CasinoPlugin;
import com.casinocore.core.PluginVariant;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

public class ProtectionManager {

    private final CasinoPlugin plugin;
    private boolean valid = true;
    private String failureReason = "";

    public ProtectionManager(CasinoPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean initialize() {
        PluginVariant variant = plugin.getVariant();
        if (!variant.isProtectionEnabled()) {
            valid = true;
            return true;
        }

        valid = validateNow();
        if (valid) {
            Bukkit.getScheduler().runTaskTimer(plugin.getPlugin(), this::validateLater, 20L * 60L, 20L * 60L * 10L);
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
        if (!validateNow()) {
            plugin.getPlugin().getLogger().severe("Protection validation failed after startup: " + failureReason);
            Bukkit.getScheduler().runTask(plugin.getPlugin(), () -> plugin.getPlugin().getServer().getPluginManager().disablePlugin(plugin.getPlugin()));
        }
    }

    private boolean validateNow() {
        try {
            LicensePayload payload = readAndVerifyLicense();
            if (!"CasinoCore".equals(payload.product)) {
                fail("license product mismatch");
                return false;
            }

            if (payload.expiresAtEpochSecond > 0 && Instant.now().getEpochSecond() > payload.expiresAtEpochSecond) {
                fail("license expired");
                return false;
            }

            String jarHash = getCurrentJarSha256();
            if (!payload.allowedJarHashes.isEmpty() && !payload.allowedJarHashes.contains(jarHash)) {
                fail("jar hash mismatch");
                return false;
            }

            String configuredOwner = plugin.getConfigManager().getConfig().getString("license.owner", "");
            if (!configuredOwner.isBlank() && !configuredOwner.equalsIgnoreCase(payload.owner)) {
                fail("license owner mismatch");
                return false;
            }

            String configuredServer = plugin.getConfigManager().getConfig().getString("license.server-id", "");
            if (!configuredServer.isBlank() && !configuredServer.equalsIgnoreCase(payload.serverId)) {
                fail("server id mismatch");
                return false;
            }

            valid = true;
            failureReason = "";
            plugin.getPlugin().getLogger().info("Protection validation passed for owner " + payload.owner);
            return true;
        } catch (Exception e) {
            plugin.getPlugin().getLogger().log(Level.SEVERE, "Protection validation error", e);
            fail("validation error");
            return false;
        }
    }

    private LicensePayload readAndVerifyLicense() throws Exception {
        File licenseFile = new File(plugin.getPlugin().getDataFolder(), "license.key");
        if (!licenseFile.isFile()) {
            throw new IllegalStateException("license.key is missing");
        }

        List<String> lines = Files.readAllLines(licenseFile.toPath(), StandardCharsets.UTF_8)
            .stream()
            .map(String::trim)
            .filter(line -> !line.isEmpty())
            .toList();

        if (lines.size() < 2) {
            throw new IllegalStateException("license.key must contain payload and signature");
        }

        String payloadLine = lines.get(0);
        String signatureLine = lines.get(1);

        verifySignature(payloadLine, signatureLine);
        return LicensePayload.parse(new String(Base64.getDecoder().decode(payloadLine), StandardCharsets.UTF_8));
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

    private void fail(String reason) {
        valid = false;
        failureReason = reason;
    }

    private static final class LicensePayload {
        private final String product;
        private final String owner;
        private final String serverId;
        private final long expiresAtEpochSecond;
        private final List<String> allowedJarHashes;

        private LicensePayload(String product, String owner, String serverId, long expiresAtEpochSecond, List<String> allowedJarHashes) {
            this.product = product;
            this.owner = owner;
            this.serverId = serverId;
            this.expiresAtEpochSecond = expiresAtEpochSecond;
            this.allowedJarHashes = allowedJarHashes;
        }

        private static LicensePayload parse(String raw) {
            String product = "";
            String owner = "";
            String serverId = "";
            long expiresAt = 0L;
            List<String> hashes = new ArrayList<>();

            for (String line : raw.split("\\R")) {
                String[] pair = line.split("=", 2);
                if (pair.length != 2) {
                    continue;
                }

                String key = pair[0].trim();
                String value = pair[1].trim();
                switch (key) {
                    case "product" -> product = value;
                    case "owner" -> owner = value;
                    case "serverId" -> serverId = value;
                    case "expiresAt" -> expiresAt = Long.parseLong(value);
                    case "jarHashes" -> {
                        if (!value.isBlank()) {
                            for (String hash : value.split(",")) {
                                hashes.add(hash.trim().toLowerCase(Locale.ROOT));
                            }
                        }
                    }
                    default -> {
                    }
                }
            }

            return new LicensePayload(product, owner, serverId, expiresAt, hashes);
        }
    }
}
