package com.casinocore.core;

import java.util.Locale;

public enum PluginVariant {
    PROTECTED(true, false),
    NORMAL(false, false),
    CLEAR(false, false),
    DEMO(false, true);

    private final boolean protectionEnabled;
    private final boolean demo;

    PluginVariant(boolean protectionEnabled, boolean demo) {
        this.protectionEnabled = protectionEnabled;
        this.demo = demo;
    }

    public boolean isProtectionEnabled() {
        return protectionEnabled;
    }

    public boolean isDemo() {
        return demo;
    }

    public static PluginVariant fromConfig(String raw) {
        if (raw == null || raw.isBlank()) {
            return PROTECTED;
        }

        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        if ("STANDARD".equals(normalized) || "RELEASE".equals(normalized)) {
            return NORMAL;
        }
        if ("DEMO".equals(normalized)) {
            return DEMO;
        }

        try {
            return PluginVariant.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return PROTECTED;
        }
    }
}
