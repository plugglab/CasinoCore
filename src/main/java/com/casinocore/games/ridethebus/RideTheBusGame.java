package com.casinocore.games.ridethebus;

import com.casinocore.core.CasinoPlugin;
import com.casinocore.games.BaseCasinoGame;
import com.casinocore.gui.GuiNavigation;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RideTheBusGame extends BaseCasinoGame {

    private final Map<UUID, RideTheBusGUI> sessions;

    public RideTheBusGame(CasinoPlugin plugin) {
        super(plugin, "ridethebus", "Ride the Bus", "Clear four card calls in a row to reach the final payout.");
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
                sendMessage(player, t("ridethebus.already-open"));
                return false;
            }
            if (!withdrawBet(player, bet)) {
                return false;
            }
            betWithdrawn = true;
            setCooldown(player);

            RideTheBusGUI gui = new RideTheBusGUI(plugin, this, player, bet);
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

    public void handleClick(Player player, RideTheBusGUI gui, int slot) {
        gui.handleClick(slot);
    }

    public void resolve(Player player, double bet, boolean won, double payout, Map<String, String> placeholders) {
        sessions.remove(player.getUniqueId());
        if (won) {
            if (payWinnings(player, payout)) {
                handleWin(player, bet, payout);
                sendMessage(player, plugin.getLocaleManager().formatText("ridethebus.result.win", placeholders));
            } else {
                sendMessage(player, t("ridethebus.payout-failed"));
            }
            return;
        }

        handleLoss(player, bet);
        sendMessage(player, plugin.getLocaleManager().formatText("ridethebus.result.loss", placeholders));
    }

    public void replay(Player player, double bet) {
        sessions.remove(player.getUniqueId());
        player.closeInventory();
        play(player, bet);
    }

    public void backToHub(Player player) {
        sessions.remove(player.getUniqueId());
        player.closeInventory();
        GuiNavigation.openHub(plugin, player);
    }

    public void handleClose(RideTheBusGUI gui) {
        if (gui.isResolved()) {
            sessions.remove(gui.getPlayer().getUniqueId());
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin.getPlugin(), () -> {
            if (sessions.containsKey(gui.getPlayer().getUniqueId())) {
                gui.getPlayer().openInventory(gui.getInventory());
            }
        }, 1L);
    }

    public double getStageMultiplier(String key, double fallback) {
        return plugin.getConfigManager().getConfig().getDouble("games.ridethebus.payouts." + key, fallback);
    }

    private String t(String key) {
        return plugin.getLocaleManager().getText(key);
    }
}
