package com.casinocore.games.treasure;

import com.casinocore.core.CasinoPlugin;
import com.casinocore.games.BaseCasinoGame;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TreasureGame extends BaseCasinoGame {

    private final Map<UUID, TreasureGUI> sessions;

    public TreasureGame(CasinoPlugin plugin) {
        super(plugin, "treasure", "Treasure Pick", "Pick one chest and hope it hides the prize.");
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
                sendMessage(player, "<yellow>You already have a Treasure Pick board open.</yellow>");
                return false;
            }
            if (!withdrawBet(player, bet)) {
                return false;
            }
            betWithdrawn = true;
            setCooldown(player);

            TreasureGUI gui = new TreasureGUI(plugin, this, player, bet);
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

    public void handleClick(Player player, TreasureGUI gui, int slot) {
        gui.handleSlotClick(slot);
    }

    public void resolve(Player player, double bet, boolean won, double payout) {
        sessions.remove(player.getUniqueId());
        if (won) {
            if (payWinnings(player, payout)) {
                handleWin(player, bet, payout);
                sendMessage(player,
                    "<gold><bold>Treasure Pick Win</bold></gold>\n" +
                        "<gray>Payout:</gray> <green>" + plugin.getEconomyManager().format(payout) + "</green>"
                );
            } else {
                sendMessage(player, "<red>Treasure payout failed.</red>");
            }
        } else {
            handleLoss(player, bet);
            sendMessage(player,
                "<red><bold>Treasure Pick Loss</bold></red>\n" +
                    "<gray>Lost:</gray> <red>" + plugin.getEconomyManager().format(bet) + "</red>"
            );
        }
    }

    public void finishSession(Player player) {
        sessions.remove(player.getUniqueId());
    }

    public void handleClose(TreasureGUI gui) {
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
}
