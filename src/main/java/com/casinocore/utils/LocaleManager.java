package com.casinocore.utils;

import com.casinocore.core.CasinoPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class LocaleManager {

    private static final String DEFAULT_LOCALE = "en";

    private static final String[] BUNDLED_LOCALES = {
            "en", "pl", "de", "fr", "es"
    };

    private final CasinoPlugin plugin;

    private String activeLocale = DEFAULT_LOCALE;
    private YamlConfiguration activeBundle = new YamlConfiguration();
    private YamlConfiguration fallbackBundle = new YamlConfiguration();

    public LocaleManager(CasinoPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        saveDefaultLocales();

        String configuredLocale = plugin.getConfigManager().getConfiguredLocale();

        fallbackBundle = loadBundle(DEFAULT_LOCALE);

        activeLocale = sanitizeLocale(configuredLocale);
        activeBundle = DEFAULT_LOCALE.equals(activeLocale)
                ? fallbackBundle
                : loadBundle(activeLocale);
    }

    public String getActiveLocale() {
        return activeLocale;
    }

    public String getMessage(String key) {
        return resolve(key, "messages", false);
    }

    public String getText(String key) {
        return resolve(key, "texts", true);
    }

    private String resolve(String key, String root, boolean showMissingKey) {
        String value = resolveFromBundle(activeBundle, key, root);

        if (value == null) {
            value = resolveFromBundle(fallbackBundle, key, root);
        }

        if (value == null) {
            return showMissingKey ? key : "";
        }

        return value;
    }

    private String resolveFromBundle(YamlConfiguration bundle, String key, String root) {
        String value = safeGet(bundle, key);
        if (value != null) {
            return value;
        }

        if (root == null || root.isBlank()) {
            return null;
        }

        return safeGet(bundle, root + "." + key);
    }

    private String safeGet(YamlConfiguration bundle, String key) {
        if (bundle == null || key == null || key.isBlank()) {
            return null;
        }

        String[] parts = key.split("\\.");
        ConfigurationSection current = bundle;

        for (int i = 0; i < parts.length - 1; i++) {
            String sectionKey = findKeyIgnoreCase(current, parts[i]);
            if (sectionKey == null) {
                return null;
            }

            ConfigurationSection next = current.getConfigurationSection(sectionKey);
            if (next == null) {
                return null;
            }
            current = next;
        }

        String valueKey = findKeyIgnoreCase(current, parts[parts.length - 1]);
        if (valueKey == null || !current.isString(valueKey)) {
            return null;
        }

        String value = current.getString(valueKey);
        return value == null || value.isBlank() ? null : value;
    }

    private String findKeyIgnoreCase(ConfigurationSection section, String target) {
        for (String key : section.getKeys(false)) {
            if (key.equalsIgnoreCase(target)) {
                return key;
            }
        }
        return null;
    }

    public String formatText(String key, Map<String, String> placeholders) {
        return replacePlaceholders(getText(key), placeholders);
    }

    public String formatMessage(String key, Map<String, String> placeholders) {
        return replacePlaceholders(getMessage(key), placeholders);
    }

    public Map<String, String> placeholders(Object... pairs) {
        Map<String, String> placeholders = new HashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            placeholders.put(String.valueOf(pairs[i]), String.valueOf(pairs[i + 1]));
        }
        return placeholders;
    }

    private void saveDefaultLocales() {
        for (String locale : BUNDLED_LOCALES) {
            String path = "lang/" + locale + ".yml";
            if (plugin.getPlugin().getResource(path) != null) {
                plugin.getPlugin().saveResource(path, false);
            }
        }
    }

    private YamlConfiguration loadBundle(String locale) {
        String path = "lang/" + locale + ".yml";

        File file = new File(plugin.getPlugin().getDataFolder(), path);

        if (file.exists()) {
            try {
                return YamlConfiguration.loadConfiguration(file);
            } catch (Exception e) {
                plugin.getPlugin().getLogger().warning("Failed loading locale file " + path);
            }
        }

        if (plugin.getPlugin().getResource(path) != null) {
            try (InputStreamReader reader = new InputStreamReader(
                    plugin.getPlugin().getResource(path),
                    StandardCharsets.UTF_8)) {

                YamlConfiguration bundle = new YamlConfiguration();
                bundle.load(reader);
                return bundle;

            } catch (Exception e) {
                plugin.getPlugin().getLogger().warning("Failed loading bundled locale " + path);
            }
        }

        return new YamlConfiguration();
    }

    private String replacePlaceholders(String value, Map<String, String> placeholders) {
        String formatted = value;

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            formatted = formatted.replace("{" + entry.getKey() + "}", entry.getValue());
        }

        return formatted;
    }

    private String sanitizeLocale(String locale) {
        if (locale == null || locale.isBlank()) return DEFAULT_LOCALE;
        return locale.trim().toLowerCase();
    }
}
