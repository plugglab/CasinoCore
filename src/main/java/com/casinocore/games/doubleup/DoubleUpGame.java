package com.casinocore.games.doubleup;

import com.casinocore.core.CasinoPlugin;
import com.casinocore.games.BaseCasinoGame;
import com.casinocore.gui.GuiNavigation;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DoubleUpGame extends BaseCasinoGame {

    private final Map<UUID, DoubleUpGUI> sessions;

    public DoubleUpGame(CasinoPlugin plugin) {
        super(plugin, "doubleup", "Double Up", "Press your luck and try to multiply your pot.");
        this.sessions = new ConcurrentHashMap<>();
    }

    @Override
    public boolean play(Player player, double bet) {
        boolean betWithdrawn = false;
        try {
            if (!preGameValidation(player, bet)) {
                return false;
            }
            if (sessions.containsKey(player.getUniqueId())) {
                sendMessage(player, t("doubleup.already-open"));
                return false;
            }
            if (!withdrawBet(player, bet)) {
                return false;
            }
            betWithdrawn = true;
            setCooldown(player);

            DoubleUpGUI gui = new DoubleUpGUI(plugin, this, player, bet);
            sessions.put(player.getUniqueId(), gui);
            Bukkit.getScheduler().runTask(plugin.getPlugin(), gui::open);
            return true;
        } catch (Exception e) {
            handleGameError(player, bet, e, betWithdrawn);
            return false;
        }
    }

    @Override
    protected boolean executeGame(Player player, double bet) {
        return false;
    }

    public void handleClick(Player player, DoubleUpGUI gui, int slot) {
        if (slot == 47) {
            gui.cashOut();
        } else if (slot == 51) {
            gui.tryDouble();
        } else if (slot == 49 && gui.isRoundOver()) {
            sessions.remove(player.getUniqueId());
            player.closeInventory();
            GuiNavigation.openHub(plugin, player);
        } else if (slot == 53 && gui.isRoundOver()) {
            sessions.remove(player.getUniqueId());
            player.closeInventory();
            play(player, gui.getBaseBet());
        }
    }

    public void finishSession(Player player) {
        sessions.remove(player.getUniqueId());
    }

    public double getRoundWinChance() {
        return plugin.getConfigManager().getConfig().getDouble("games.doubleup.chance-to-double-percent", 50.0);
    }

    public void payCashout(Player player, double baseBet, double pot, int streak) {
        sessions.remove(player.getUniqueId());
        if (payWinnings(player, pot)) {
            handleWin(player, baseBet, pot);
            sendMessage(player, plugin.getLocaleManager().formatText("doubleup.cashout", Map.of(
                "streak", String.valueOf(streak),
                "payout", plugin.getEconomyManager().format(pot)
            )));
        } else {
            sendMessage(player, t("doubleup.cashout-failed"));
        }
    }

    public void lose(Player player, double baseBet, double pot) {
        sessions.remove(player.getUniqueId());
        handleLoss(player, baseBet);
        sendMessage(player, plugin.getLocaleManager().formatText("doubleup.failed", Map.of(
            "amount", plugin.getEconomyManager().format(pot)
        )));
    }

    public void handleClose(DoubleUpGUI gui) {
        if (gui.isRoundOver()) {
            sessions.remove(gui.getPlayer().getUniqueId());
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin.getPlugin(), () -> {
            if (sessions.containsKey(gui.getPlayer().getUniqueId())) {
                gui.getPlayer().openInventory(gui.getInventory());
            }
        }, 1L);
    }

    private String t(String key) {
        return plugin.getLocaleManager().getText(key);
    }
}
