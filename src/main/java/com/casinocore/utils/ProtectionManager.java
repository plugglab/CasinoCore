package com.casinocore.utils;

import com.casinocore.core.CasinoPlugin;
import org.bukkit.entity.Player;

public class ProtectionManager {

    private final CasinoPlugin plugin;

    public ProtectionManager(CasinoPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean initialize() {
        plugin.getPlugin().getLogger().info("Protection manager is disabled in this build.");
        return true;
    }

    public boolean isValid() {
        return true;
    }

    public boolean canUseProtectedFeature(Player player) {
        return true;
    }
}
