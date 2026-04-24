package com.casinocore.games.highlow;

import com.casinocore.core.CasinoPlugin;
import com.casinocore.games.BaseCasinoGame;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HighLowGame extends BaseCasinoGame {

    private final Map<UUID, HighLowGUI> sessions;

    public HighLowGame(CasinoPlugin plugin) {
        super(plugin, "highlow", "High Low", "Guess if the next card is higher or lower.");
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
                sendMessage(player, "<yellow>You already have a High Low table open.</yellow>");
                return false;
            }
            if (!withdrawBet(player, bet)) {
                return false;
            }
            betWithdrawn = true;
            setCooldown(player);

            HighLowGUI gui = new HighLowGUI(plugin, this, player, bet);
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

    public void handleClick(Player player, HighLowGUI gui, int slot) {
        if (slot == 47) {
            gui.reveal(false);
        } else if (slot == 51) {
            gui.reveal(true);
        } else if (slot == 49 && gui.isRoundOver()) {
            sessions.remove(player.getUniqueId());
            player.closeInventory();
        } else if (slot == 53 && gui.isRoundOver()) {
            sessions.remove(player.getUniqueId());
            player.closeInventory();
            play(player, gui.getBet());
        }
    }

    public void resolveRound(Player player, double bet, boolean won, double payout, String message) {
        sessions.remove(player.getUniqueId());
        if (won) {
            if (payWinnings(player, payout)) {
                handleWin(player, bet, payout);
            } else {
                sendMessage(player, "<red>High Low payout failed. Contact an administrator.</red>");
            }
        } else {
            handleLoss(player, bet);
        }
        sendMessage(player, message);
    }

    public void handleClose(HighLowGUI gui) {
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

    public double getWinMultiplier() {
        return plugin.getConfigManager().getConfig().getDouble("games.highlow.payouts.win", 1.95);
    }
}
