package com.casinocore.gui;

import com.casinocore.core.CasinoPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class GuiNavigation {

    private GuiNavigation() {
    }

    public static void openHub(CasinoPlugin plugin, Player player) {
        Bukkit.getScheduler().runTask(plugin.getPlugin(), () -> new CasinoHubGUI(plugin, player).open());
    }
}
