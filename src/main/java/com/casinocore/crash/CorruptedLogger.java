package com.casinocore.crash;

import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Small logger wrapper that centralizes crash-style output.
 */
public final class CorruptedLogger {

    private final Plugin plugin;
    private final Logger logger;

    public CorruptedLogger(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public void dump(List<String> lines) {
        for (String line : lines) {
            logger.log(Level.SEVERE, line);
        }
    }

    public void info(String message) {
        logger.log(Level.INFO, "[{0}/CRASH] {1}", new Object[]{plugin.getName(), message});
    }

    public void warning(String message) {
        logger.log(Level.WARNING, "[{0}/CRASH] {1}", new Object[]{plugin.getName(), message});
    }
}
