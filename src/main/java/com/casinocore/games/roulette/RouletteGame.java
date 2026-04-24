package com.casinocore.games.roulette;

import com.casinocore.core.CasinoPlugin;
import com.casinocore.games.BaseCasinoGame;
import com.casinocore.gui.GuiNavigation;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

public class RouletteGame extends BaseCasinoGame {

    private final Map<UUID, RouletteGUI> openTables;
    private final Plugin bukkitPlugin;

    public RouletteGame(CasinoPlugin plugin) {
        super(plugin, "roulette", "Roulette", "Pick a number, color, or parity in the roulette table GUI.");
        this.openTables = new ConcurrentHashMap<>();
        this.bukkitPlugin = plugin.getPlugin();
    }

    @Override
    public boolean play(Player player, double bet) {
        boolean betWithdrawn = false;
        try {
            if (!preGameValidation(player, bet)) {
                return false;
            }

            if (openTables.containsKey(player.getUniqueId())) {
                sendMessage(player, "<yellow>You already have a roulette table open.</yellow>");
                return false;
            }

            RouletteGUI gui = new RouletteGUI(plugin, this, player, bet);
            openTables.put(player.getUniqueId(), gui);
            Bukkit.getScheduler().runTask(bukkitPlugin, gui::open);
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

    public Plugin getPlugin() {
        return bukkitPlugin;
    }

    public CasinoPlugin getCasinoPlugin() {
        return plugin;
    }

    public void handleClick(Player player, RouletteGUI gui, int slot) {
        if (gui.isSpinning()) {
            return;
        }

        if (slot >= 9 && slot <= 45) {
            Integer number = mapSlotToNumber(slot);
            if (number != null) {
                gui.selectNumber(number);
            }
            return;
        }

        switch (slot) {
            case 0 -> gui.selectSimple(RouletteBetType.LOW);
            case 1 -> gui.selectSimple(RouletteBetType.FIRST_DOZEN);
            case 7 -> gui.selectSimple(RouletteBetType.THIRD_DOZEN);
            case 8 -> gui.selectSimple(RouletteBetType.HIGH);
            case 45 -> gui.selectSimple(RouletteBetType.EVEN);
            case 47 -> gui.selectSimple(RouletteBetType.ODD);
            case 48 -> gui.clearSelection();
            case 49 -> gui.selectSimple(RouletteBetType.SECOND_DOZEN);
            case 50 -> startSpin(player, gui);
            case 51 -> gui.selectSimple(RouletteBetType.RED);
            case 52 -> gui.selectSimple(RouletteBetType.BLACK);
            case 53 -> {
                if (!gui.isSpinning()) {
                    openTables.remove(player.getUniqueId());
                    gui.back();
                }
            }
            default -> {
            }
        }
    }

    public void handleClose(Player player) {
        RouletteGUI gui = openTables.get(player.getUniqueId());
        if (gui != null && !gui.isSpinning()) {
            openTables.remove(player.getUniqueId());
        }
    }

    public double getPayoutMultiplier(RouletteBetType type) {
        return plugin.getConfigManager().getConfig().getDouble(
            "games.roulette.payouts." + type.getPayoutKey(),
            switch (type) {
                case SINGLE_NUMBER -> 35.0;
                case FIRST_DOZEN, SECOND_DOZEN, THIRD_DOZEN -> 3.0;
                default -> 1.9;
            }
        );
    }

    private void startSpin(Player player, RouletteGUI gui) {
        RouletteBet bet = gui.getSelectedBet();
        if (bet == null) {
            sendMessage(player, "<yellow>Select a roulette bet before spinning.</yellow>");
            return;
        }

        double stake = gui.getBetAmount();
        if (!withdrawBet(player, stake)) {
            return;
        }

        setCooldown(player);
        gui.setSpinning(true);

        int resultNumber = rollWeightedNumber();
        RouletteSpinAnimation animation = new RouletteSpinAnimation(this, gui, resultNumber, () ->
            Bukkit.getScheduler().runTask(bukkitPlugin, () -> finishSpin(player, gui, bet, resultNumber))
        );
        animation.start();
    }

    private void finishSpin(Player player, RouletteGUI gui, RouletteBet bet, int resultNumber) {
        try {
            boolean won = bet.matches(resultNumber);
            double multiplier = won ? getPayoutMultiplier(bet.getType()) : 0.0;
            double winnings = gui.getBetAmount() * multiplier;

            if (won && !payWinnings(player, winnings)) {
                sendMessage(player, "<red>Failed to pay roulette winnings. Contact an administrator.</red>");
                winnings = 0.0;
                won = false;
                multiplier = 0.0;
            }

            if (won) {
                handleWin(player, gui.getBetAmount(), winnings);
            } else {
                handleLoss(player, gui.getBetAmount());
            }

            gui.showResult(resultNumber, won, multiplier, winnings);
            gui.setSpinning(false);

            sendResultMessage(player, bet, resultNumber, gui.getBetAmount(), won, multiplier, winnings);
            logGame(player, gui.getBetAmount(), true);
        } catch (Exception e) {
            plugin.getPlugin().getLogger().log(Level.SEVERE,
                "Error finalizing roulette spin for " + player.getName(), e);
            handleGameError(player, gui.getBetAmount(), e);
        } finally {
            openTables.remove(player.getUniqueId());
        }
    }

    private void sendResultMessage(Player player, RouletteBet bet, int resultNumber, double stake,
                                   boolean won, double multiplier, double winnings) {
        String color = resultNumber == 0 ? "green" : RouletteNumbers.isRed(resultNumber) ? "red" : "gray";
        if (won) {
            sendMessage(player,
                "<green><bold>Roulette Win</bold></green>\n" +
                "<gray>Bet:</gray> <white>" + bet.getDisplayLabel() + "</white>\n" +
                "<gray>Result:</gray> <" + color + ">" + resultNumber + "</" + color + ">\n" +
                "<gray>Multiplier:</gray> <gold>" + multiplier + "x</gold>\n" +
                "<gray>Bet:</gray> <white>" + plugin.getEconomyManager().format(stake) + "</white>\n" +
                "<gray>Winnings:</gray> <gold>" + plugin.getEconomyManager().format(winnings) + "</gold>"
            );
        } else {
            sendMessage(player,
                "<red><bold>Roulette Loss</bold></red>\n" +
                "<gray>Bet:</gray> <white>" + bet.getDisplayLabel() + "</white>\n" +
                "<gray>Result:</gray> <" + color + ">" + resultNumber + "</" + color + ">\n" +
                "<gray>Lost:</gray> <white>" + plugin.getEconomyManager().format(stake) + "</white>"
            );
        }
    }

    private int rollWeightedNumber() {
        double total = 0.0;
        for (int number = 0; number <= 36; number++) {
            total += getWeight(number);
        }

        double roll = ThreadLocalRandom.current().nextDouble(total);
        double cumulative = 0.0;
        for (int number = 0; number <= 36; number++) {
            cumulative += getWeight(number);
            if (roll <= cumulative) {
                return number;
            }
        }

        return 0;
    }

    private double getWeight(int number) {
        return plugin.getConfigManager().getConfig().getDouble(
            "games.roulette.weights." + number,
            1.0
        );
    }

    private Integer mapSlotToNumber(int slot) {
        int[] slots = {
            9, 10, 11, 12, 13, 14, 15, 16, 17,
            18, 19, 20, 21, 22, 23, 24, 25, 26,
            27, 28, 29, 30, 31, 32, 33, 34, 35,
            36, 37, 38, 39, 40, 41, 42, 43, 44, 45
        };

        for (int number = 0; number < slots.length; number++) {
            if (slots[number] == slot) {
                return number;
            }
        }
        return null;
    }

    @Override
    public boolean canPlay(Player player) {
        return !openTables.containsKey(player.getUniqueId()) && super.canPlay(player);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        openTables.clear();
    }
}
