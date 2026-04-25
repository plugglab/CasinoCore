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
                sendLocaleMessage(player, "slots.finish-session");
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
            sendLocaleMessage(player, "game.disabled");
            return;
        }
        if (!validateBet(player, bet) || !checkBalance(player, bet)) {
            return;
        }
        if (!withdrawBet(player, bet)) {
            return;
        }

        setCooldown(player);
        plugin.getUxManager().showBossBarSequence(player, plugin.getLocaleManager().getText("slots.spinning"), org.bukkit.boss.BarColor.YELLOW, 90L);
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
                            plugin.getLocaleManager().getText("slots.win-title"),
                            plugin.getLocaleManager().formatText("slots.symbols", Map.of("symbols", results[0].getDisplayName() + " " + results[1].getDisplayName() + " " + results[2].getDisplayName())),
                            plugin.getLocaleManager().formatText("slots.multiplier", Map.of("value", multiplier + "x")),
                            plugin.getLocaleManager().formatText("slots.winnings", Map.of("amount", plugin.getEconomyManager().format(winnings))),
                            plugin.getLocaleManager().formatText("slots.profit", Map.of("amount", plugin.getEconomyManager().format(profit)))
                        );
                        sendMessage(player, winMessage);
                        if (jackpot) {
                            plugin.getMessageManager().broadcast(
                                plugin.getLocaleManager().formatText("slots.broadcast-jackpot", Map.of(
                                    "player", player.getName(),
                                    "amount", plugin.getEconomyManager().format(winnings),
                                    "symbols", results[0].getDisplayName() + " " + results[1].getDisplayName() + " " + results[2].getDisplayName()
                                ))
                            );
                        }
                    } else {
                        sendLocaleMessage(player, "slots.winnings-error");
                    }
                });
            } else {
                handleLoss(player, bet);
                sendMessage(player, plugin.getUxManager().formatGameMessage(
                    "red",
                    plugin.getLocaleManager().getText("slots.loss-title"),
                    plugin.getLocaleManager().formatText("slots.symbols-loss", Map.of("symbols", results[0].getDisplayName() + " " + results[1].getDisplayName() + " " + results[2].getDisplayName())),
                    plugin.getLocaleManager().formatText("slots.bet", Map.of("amount", plugin.getEconomyManager().format(bet))),
                    plugin.getLocaleManager().getText("slots.try-again")
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
