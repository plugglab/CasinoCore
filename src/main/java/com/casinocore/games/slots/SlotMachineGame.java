package com.casinocore.games.slots;

import com.casinocore.core.CasinoPlugin;
import com.casinocore.games.BaseCasinoGame;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class SlotMachineGame extends BaseCasinoGame {

    private final Map<UUID, SlotMachineGUI> openMachines;

    public SlotMachineGame(CasinoPlugin plugin) {
        super(plugin, "slots", "Slot Machine", "Spin the slots and match symbols to win big!");
        this.openMachines = new ConcurrentHashMap<>();
    }

    @Override
    protected boolean executeGame(Player player, double bet) {
        try {
            if (openMachines.containsKey(player.getUniqueId())) {
                sendMessage(player, "<yellow>Please finish your current slot machine session first.</yellow>");
                return false;
            }

            Bukkit.getScheduler().runTask(plugin.getPlugin(), () -> {
                SlotMachineGUI gui = new SlotMachineGUI(plugin, this, player, bet);
                openMachines.put(player.getUniqueId(), gui);
                gui.open();
            });
            return true;
        } catch (Exception e) {
            throw e;
        }
    }

    public void pullLever(Player player, SlotMachineGUI gui) {
        if (gui.isSpinning()) {
            return;
        }

        double bet = gui.getBet();
        if (!isEnabled()) {
            sendMessage(player, "<red>This game is currently disabled!</red>");
            return;
        }
        if (!validateBet(player, bet) || !checkBalance(player, bet)) {
            return;
        }
        if (!withdrawBet(player, bet)) {
            return;
        }

        setCooldown(player);
        plugin.getUxManager().showBossBarSequence(player, "Reels spinning...", org.bukkit.boss.BarColor.YELLOW, 90L);
        gui.spin(() -> handleSpinComplete(player, gui, bet));
    }

    public void backToHub(Player player) {
        openMachines.remove(player.getUniqueId());
    }

    private void handleSpinComplete(Player player, SlotMachineGUI gui, double bet) {
        try {
            SlotSymbol[] results = gui.getFinalResults();
            if (results == null) {
                return;
            }

            double multiplier = SlotSymbol.calculateMultiplier(results);
            double winnings = bet * multiplier;
            boolean jackpot = isJackpot(results);

            if (multiplier > 0) {
                Bukkit.getScheduler().runTask(plugin.getPlugin(), () -> {
                    if (payWinnings(player, winnings)) {
                        double profit = winnings - bet;
                        handleWin(player, bet, winnings);
                        String winMessage = plugin.getUxManager().formatGameMessage(
                            "green",
                            "Slot Machine Win",
                            "<white>Symbols:</white> " + results[0].getDisplayName() + " " +
                                results[1].getDisplayName() + " " + results[2].getDisplayName(),
                            "<white>Multiplier:</white> <gold>" + multiplier + "x</gold>",
                            "<white>Winnings:</white> <green>" + plugin.getEconomyManager().format(winnings) + "</green>",
                            "<white>Profit:</white> <green>+" + plugin.getEconomyManager().format(profit) + "</green>"
                        );
                        sendMessage(player, winMessage);
                        if (jackpot) {
                            plugin.getMessageManager().broadcast(
                                "<gold><bold>[Slots Jackpot]</bold></gold> <white>" + player.getName() + "</white> hit <green>" +
                                    plugin.getEconomyManager().format(winnings) + "</green> <gray>with</gray> " +
                                    results[0].getDisplayName() + " " + results[1].getDisplayName() + " " + results[2].getDisplayName()
                            );
                        }
                    } else {
                        sendMessage(player, "<red>Error processing winnings. Contact an administrator.</red>");
                    }
                });
            } else {
                handleLoss(player, bet);
                sendMessage(player, plugin.getUxManager().formatGameMessage(
                    "red",
                    "Slot Machine Loss",
                    "<white>Symbols:</white> <gray>" + results[0].getDisplayName() + " " +
                        results[1].getDisplayName() + " " + results[2].getDisplayName() + "</gray>",
                    "<white>Bet:</white> <red>" + plugin.getEconomyManager().format(bet) + "</red>",
                    "<gray>Pull the lever again if you want another round.</gray>"
                ));
            }
        } catch (Exception e) {
            plugin.getPlugin().getLogger().log(Level.SEVERE, "Error handling slot result for " + player.getName(), e);
        }
    }

    @Override
    public boolean canPlay(Player player) {
        return !openMachines.containsKey(player.getUniqueId()) && super.canPlay(player);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        openMachines.clear();
    }

    private boolean isJackpot(SlotSymbol[] results) {
        return results.length == 3
            && results[0] == SlotSymbol.DIAMOND
            && results[1] == SlotSymbol.DIAMOND
            && results[2] == SlotSymbol.DIAMOND;
    }
}
