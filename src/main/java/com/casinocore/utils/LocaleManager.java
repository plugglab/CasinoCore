package com.casinocore.utils;

import com.casinocore.core.CasinoPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class LocaleManager {

    private static final String DEFAULT_LOCALE = "en";

    private final CasinoPlugin plugin;
    private String activeLocale = DEFAULT_LOCALE;
    private YamlConfiguration activeBundle = new YamlConfiguration();
    private YamlConfiguration fallbackBundle = new YamlConfiguration();

    public LocaleManager(CasinoPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        String configuredLocale = plugin.getConfigManager().getConfiguredLocale();
        fallbackBundle = loadBundle(DEFAULT_LOCALE);
        activeLocale = sanitizeLocale(configuredLocale);
        activeBundle = DEFAULT_LOCALE.equals(activeLocale) ? fallbackBundle : loadBundle(activeLocale);
    }

    public String getActiveLocale() {
        return activeLocale;
    }

    public String getMessage(String key) {
        String value = activeBundle.getString("messages." + key);
        if (value != null && !value.isBlank()) {
            return value;
        }
        return fallbackBundle.getString("messages." + key, "");
    }

    public String getText(String key) {
        String value = activeBundle.getString("texts." + key);
        if (value != null && !value.isBlank()) {
            return value;
        }
        return fallbackBundle.getString("texts." + key, key);
    }

    public String formatText(String key, Map<String, String> placeholders) {
        String value = getText(key);
        return replacePlaceholders(value, placeholders);
    }

    public String formatMessage(String key, Map<String, String> placeholders) {
        String value = getMessage(key);
        return replacePlaceholders(value, placeholders);
    }

    public Map<String, String> placeholders(Object... pairs) {
        Map<String, String> placeholders = new HashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            placeholders.put(String.valueOf(pairs[i]), String.valueOf(pairs[i + 1]));
        }
        return placeholders;
    }

    private String replacePlaceholders(String value, Map<String, String> placeholders) {
        String formatted = value;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            formatted = formatted.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return formatted;
    }

    private YamlConfiguration loadBundle(String locale) {
        String path = "lang/" + locale + ".yml";
        YamlConfiguration bundle = new YamlConfiguration();
        try (InputStreamReader reader = new InputStreamReader(
            plugin.getPlugin().getResource(path), StandardCharsets.UTF_8)) {
            bundle.load(reader);
        } catch (Exception e) {
            plugin.getPlugin().getLogger().warning("Failed to load locale bundle " + path + ": " + e.getMessage());
        }
        return bundle;
    }

    private String sanitizeLocale(String locale) {
        if (locale == null || locale.isBlank()) {
            return DEFAULT_LOCALE;
        }
        return locale.trim().toLowerCase();
    }
}
