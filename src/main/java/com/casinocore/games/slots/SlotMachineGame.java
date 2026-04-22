package com.casinocore.games.slots;

import com.casinocore.core.CasinoPlugin;
import com.casinocore.games.BaseCasinoGame;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class SlotMachineGame extends BaseCasinoGame {

    private final Set<UUID> activePlayers;

    public SlotMachineGame(CasinoPlugin plugin) {
        super(plugin, "slots", "Slot Machine", "Spin the slots and match symbols to win big!");
        this.activePlayers = ConcurrentHashMap.newKeySet();
    }

    @Override
    protected boolean executeGame(Player player, double bet) {
        try {
            if (activePlayers.contains(player.getUniqueId())) {
                sendMessage(player, "<yellow>Please wait for your current game to finish.</yellow>");
                return false;
            }

            activePlayers.add(player.getUniqueId());
            Bukkit.getScheduler().runTask(plugin.getPlugin(), () -> {
                try {
                    SlotMachineGUI gui = new SlotMachineGUI(plugin, player, bet);
                    gui.open();
                    plugin.getUxManager().showBossBarSequence(player, "Reels spinning...", org.bukkit.boss.BarColor.YELLOW, 90L);
                    Bukkit.getScheduler().runTaskLater(plugin.getPlugin(), () -> startSpin(player, gui, bet), 10L);
                } catch (Exception e) {
                    plugin.getPlugin().getLogger().log(Level.SEVERE, "Error opening slot machine for " + player.getName(), e);
                    activePlayers.remove(player.getUniqueId());
                }
            });
            return true;
        } catch (Exception e) {
            activePlayers.remove(player.getUniqueId());
            throw e;
        }
    }

    private void startSpin(Player player, SlotMachineGUI gui, double bet) {
        try {
            gui.spin(() -> handleSpinComplete(player, gui, bet));
        } catch (Exception e) {
            plugin.getPlugin().getLogger().log(Level.SEVERE, "Error starting spin for " + player.getName(), e);
            activePlayers.remove(player.getUniqueId());
        }
    }

    private void handleSpinComplete(Player player, SlotMachineGUI gui, double bet) {
        try {
            SlotSymbol[] results = gui.getFinalResults();
            if (results == null) {
                activePlayers.remove(player.getUniqueId());
                return;
            }

            double multiplier = SlotSymbol.calculateMultiplier(results);
            double winnings = bet * multiplier;

            if (multiplier > 0) {
                Bukkit.getScheduler().runTask(plugin.getPlugin(), () -> {
                    if (payWinnings(player, winnings)) {
                        double profit = winnings - bet;
                        handleWin(player, bet, winnings);
                        sendMessage(player, plugin.getUxManager().formatGameMessage(
                            "green",
                            "Slot Machine Win",
                            "<white>Symbols:</white> <gold>" + results[0].getDisplayName() + " " +
                                results[1].getDisplayName() + " " + results[2].getDisplayName() + "</gold>",
                            "<white>Multiplier:</white> <gold>" + multiplier + "x</gold>",
                            "<white>Winnings:</white> <green>" + plugin.getEconomyManager().format(winnings) + "</green>",
                            "<white>Profit:</white> <green>+" + plugin.getEconomyManager().format(profit) + "</green>"
                        ));
                    } else {
                        sendMessage(player, "<red>Error processing winnings. Contact an administrator.</red>");
                    }
                    activePlayers.remove(player.getUniqueId());
                });
            } else {
                handleLoss(player, bet);
                sendMessage(player, plugin.getUxManager().formatGameMessage(
                    "red",
                    "Slot Machine Loss",
                    "<white>Symbols:</white> <gray>" + results[0].getDisplayName() + " " +
                        results[1].getDisplayName() + " " + results[2].getDisplayName() + "</gray>",
                    "<white>Bet:</white> <red>" + plugin.getEconomyManager().format(bet) + "</red>",
                    "<gray>Better luck next time.</gray>"
                ));
                activePlayers.remove(player.getUniqueId());
            }
        } catch (Exception e) {
            plugin.getPlugin().getLogger().log(Level.SEVERE, "Error handling slot result for " + player.getName(), e);
            activePlayers.remove(player.getUniqueId());
        }
    }

    @Override
    public boolean canPlay(Player player) {
        return !activePlayers.contains(player.getUniqueId()) && super.canPlay(player);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        activePlayers.clear();
    }
}
