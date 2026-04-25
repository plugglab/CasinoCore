package com.casinocore.utils;

import com.casinocore.core.CasinoPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages message sending with MiniMessage and legacy color support
 */
public class MessageManager {

    private final CasinoPlugin plugin;
    private final MiniMessage miniMessage;
    private final LegacyComponentSerializer legacySerializer;

    public MessageManager(CasinoPlugin plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
        this.legacySerializer = LegacyComponentSerializer.legacySection();
    }

    /**
     * Send a message from config to a player
     *
     * @param sender The command sender
     * @param messageKey The message key in config
     */
    public void sendMessage(CommandSender sender, String messageKey) {
        sendMessage(sender, messageKey, new HashMap<>());
    }

    /**
     * Send a message from config to a player with placeholders
     *
     * @param sender The command sender
     * @param messageKey The message key in config
     * @param placeholders Map of placeholders to replace
     */
    public void sendMessage(CommandSender sender, String messageKey, Map<String, String> placeholders) {
        String message = plugin.getLocaleManager().formatMessage(messageKey, placeholders);

        if (message == null || message.isEmpty()) {
            plugin.getPlugin().getLogger().warning("Message key not found: " + messageKey);
            return;
        }

        // Add prefix if not already present
        if (!message.contains(plugin.getConfigManager().getPrefix())) {
            message = plugin.getConfigManager().getPrefix() + message;
        }

        send(sender, message);
    }

    /**
     * Send a raw message with MiniMessage formatting
     *
     * @param sender The command sender
     * @param message The message to send (supports MiniMessage format)
     */
    public void send(CommandSender sender, String message) {
        try {
            Component component = miniMessage.deserialize(message);
            sender.sendMessage(component);
        } catch (Exception e) {
            // Fallback to legacy format if MiniMessage parsing fails
            sender.sendMessage(legacySerializer.deserialize(message));
        }
    }

    /**
     * Send a message to a player
     *
     * @param player The player
     * @param message The message
     */
    public void sendPlayer(Player player, String message) {
        send(player, message);
    }

    /**
     * Broadcast a message to all online players
     *
     * @param message The message to broadcast
     */
    public void broadcast(String message) {
        try {
            Component component = miniMessage.deserialize(message);
            plugin.getPlugin().getServer().broadcast(component);
        } catch (Exception e) {
            // Fallback to legacy format
            plugin.getPlugin().getServer().broadcastMessage(
                legacySerializer.serialize(legacySerializer.deserialize(message))
            );
        }
    }

    /**
     * Parse a string with MiniMessage
     *
     * @param message The message to parse
     * @return The parsed Component
     */
    public Component parse(String message) {
        return miniMessage.deserialize(message);
    }

    /**
     * Convert legacy color codes to MiniMessage format
     *
     * @param legacy The legacy formatted string
     * @return MiniMessage formatted string
     */
    public String legacyToMiniMessage(String legacy) {
        Component component = legacySerializer.deserialize(legacy);
        return miniMessage.serialize(component);
    }

    /**
     * Reload messages (called when config is reloaded)
     */
    public void reload() {
        plugin.getLocaleManager().load();
        plugin.getPlugin().getLogger().info("Messages reloaded");
    }

    /**
     * Create a placeholder map for easy placeholder replacement
     *
     * @return A new empty placeholder map
     */
    public Map<String, String> createPlaceholderMap() {
        return new HashMap<>();
    }
}
