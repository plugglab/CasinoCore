package com.casinocore.crash;

import org.bukkit.plugin.Plugin;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Conservative runtime checks for common debug and decompiler-adjacent environments.
 * This is not tamper-proof. It is intended as an operational tripwire.
 */
public final class AntiDecompiler {

    private static final List<String> JVM_FLAG_MARKERS = List.of(
        "-agentlib:jdwp", "-xdebug", "-javaagent", "jdwp", "debug", "intellij"
    );
    private static final List<String> CLASS_PATH_MARKERS = List.of(
        "fernflower", "cfr", "bytecode-viewer", "recaf", "jadx", "decompiler"
    );

    private final Plugin plugin;
    private final CorruptedLogger logger;

    public AntiDecompiler(Plugin plugin, CorruptedLogger logger) {
        this.plugin = plugin;
        this.logger = logger;
    }

    public DetectionResult inspect() {
        List<String> reasons = new ArrayList<>();

        for (String argument : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
            String lowered = argument.toLowerCase(Locale.ROOT);
            if (matchesAny(lowered, JVM_FLAG_MARKERS)) {
                reasons.add("jvm-arg:" + argument);
            }
        }

        String classPath = System.getProperty("java.class.path", "").toLowerCase(Locale.ROOT);
        if (matchesAny(classPath, CLASS_PATH_MARKERS)) {
            reasons.add("classpath-marker");
        }

        for (Thread thread : Thread.getAllStackTraces().keySet()) {
            String name = thread.getName().toLowerCase(Locale.ROOT);
            if (name.contains("jdwp") || name.contains("debug")) {
                reasons.add("thread:" + thread.getName());
            }
        }

        return new DetectionResult(!reasons.isEmpty(), reasons);
    }

    public void enforce(boolean crashOnDetect) {
        DetectionResult result = inspect();
        if (!result.detected()) {
            return;
        }

        logger.warning("anti-decompiler tripwire detected: " + String.join(", ", result.reasons()));
        if (crashOnDetect) {
            plugin.getServer().getPluginManager().disablePlugin(plugin);
            throw new IllegalStateException("AntiDecompiler triggered: " + String.join(", ", result.reasons()));
        }
    }

    private boolean matchesAny(String value, List<String> markers) {
        for (String marker : markers) {
            if (value.contains(marker)) {
                return true;
            }
        }
        return false;
    }

    public record DetectionResult(boolean detected, List<String> reasons) {
    }
}
