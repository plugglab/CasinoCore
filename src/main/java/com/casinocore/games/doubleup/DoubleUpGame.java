package com.casinocore.games.doubleup;

import com.casinocore.core.CasinoPlugin;
import com.casinocore.games.BaseCasinoGame;
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
                sendMessage(player, "<yellow>You already have a Double Up table open.</yellow>");
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
            sendMessage(player,
                "<gold><bold>Double Up Cashout</bold></gold>\n" +
                    "<gray>Rounds Survived:</gray> <white>" + streak + "</white>\n" +
                    "<gray>Payout:</gray> <green>" + plugin.getEconomyManager().format(pot) + "</green>"
            );
        } else {
            sendMessage(player, "<red>Failed to pay your Double Up cashout.</red>");
        }
    }

    public void lose(Player player, double baseBet, double pot) {
        sessions.remove(player.getUniqueId());
        handleLoss(player, baseBet);
        sendMessage(player,
            "<red><bold>Double Up Failed</bold></red>\n" +
                "<gray>Pot Lost:</gray> <red>" + plugin.getEconomyManager().format(pot) + "</red>"
        );
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
}
