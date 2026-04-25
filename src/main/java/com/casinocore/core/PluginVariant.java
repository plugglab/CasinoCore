package com.casinocore.core;

import java.util.Locale;

public enum PluginVariant {
    PROTECTED(true, false),
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

        try {
            return PluginVariant.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return PROTECTED;
        }
    }
}
