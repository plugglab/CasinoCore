package com.casinocore.utils;

import com.casinocore.core.CasinoPlugin;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;

public class RegionAccessManager {

    private final CasinoPlugin plugin;

    public RegionAccessManager(CasinoPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean canUseCasino(Player player) {
        if (!isWorldGuardGateEnabled()) {
            return true;
        }

        if (player.hasPermission("casinocore.region.bypass")) {
            return true;
        }

        if (!isWorldGuardAvailable()) {
            return false;
        }

        return isAllowedLocation(player.getLocation());
    }

    public void sendBlockedMessage(Player player) {
        plugin.getMessageManager().send(player, plugin.getLocaleManager().getText("region.denied"));
    }

    public boolean isAllowedLocation(Location location) {
        if (!isWorldGuardGateEnabled()) {
            return true;
        }

        WorldGuardPlugin worldGuard = getWorldGuardPlugin();
        if (worldGuard == null) {
            return false;
        }

        Set<String> allowedRegions = getAllowedRegions();
        if (allowedRegions.isEmpty()) {
            return false;
        }

        try {
            RegionManager regionManager = WorldGuard.getInstance()
                .getPlatform()
                .getRegionContainer()
                .get(BukkitAdapter.adapt(location.getWorld()));
            if (regionManager == null) {
                return false;
            }

            ApplicableRegionSet applicableRegions = regionManager.getApplicableRegions(BukkitAdapter.asBlockVector(location));
            return applicableRegions.getRegions().stream()
                .map(region -> region.getId().toLowerCase(Locale.ROOT))
                .anyMatch(allowedRegions::contains);
        } catch (Exception exception) {
            plugin.getPlugin().getLogger().log(Level.WARNING,
                "Failed to query WorldGuard regions for casino access.", exception);
            return false;
        }
    }

    public boolean isWorldGuardAvailable() {
        return getWorldGuardPlugin() != null;
    }

    private boolean isWorldGuardGateEnabled() {
        return plugin.getConfigManager().getConfig().getBoolean("integrations.worldguard.enabled", true);
    }

    private Set<String> getAllowedRegions() {
        List<String> configured = plugin.getConfigManager().getConfig().getStringList("integrations.worldguard.allowed-regions");
        Set<String> normalized = new HashSet<>();
        for (String region : configured) {
            if (region != null && !region.isBlank()) {
                normalized.add(region.toLowerCase(Locale.ROOT));
            }
        }
        return normalized;
    }

    private WorldGuardPlugin getWorldGuardPlugin() {
        Plugin pluginInstance = plugin.getPlugin().getServer().getPluginManager().getPlugin("WorldGuard");
        if (pluginInstance instanceof WorldGuardPlugin worldGuardPlugin) {
            return worldGuardPlugin;
        }
        return null;
    }
}
