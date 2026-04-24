package com.casinocore.games.horserace;

import com.casinocore.core.CasinoPlugin;
import com.casinocore.games.BaseCasinoGame;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class HorseRaceGame extends BaseCasinoGame {

    private static final String[] HORSE_KEYS = {"blaze", "comet", "thunder", "shadow", "storm"};
    private static final double HOUSE_CUT = 0.90;

    private final Map<UUID, HorseRaceGUI> openBoards;
    private volatile HorseRaceRound currentRound;

    public HorseRaceGame(CasinoPlugin plugin) {
        super(plugin, "horserace", "Horse Race", "Back a horse, watch the race, and cash out if your pick wins.");
        this.openBoards = new ConcurrentHashMap<>();
    }

    @Override
    public boolean play(Player player, double bet) {
        if (!preGameValidation(player, bet)) {
            return false;
        }

        HorseRaceGUI existing = openBoards.get(player.getUniqueId());
        if (existing != null) {
            existing.setPendingBet(bet);
            Bukkit.getScheduler().runTask(plugin.getPlugin(), existing::render);
            sendMessage(player, "<yellow>Updated your race board bet to " + plugin.getEconomyManager().format(bet) + ".</yellow>");
            return true;
        }

        HorseRaceGUI gui = new HorseRaceGUI(plugin, this, player, bet, HORSE_KEYS);
        openBoards.put(player.getUniqueId(), gui);
        ensureRound();
        Bukkit.getScheduler().runTask(plugin.getPlugin(), gui::open);
        return true;
    }

    @Override
    protected boolean executeGame(Player player, double bet) {
        return false;
    }

    public void handleClick(Player player, HorseRaceGUI gui, int slot) {
        String horse = gui.mapHorse(slot);
        if (horse != null) {
            if (isBettingClosed()) {
                sendMessage(player, "<yellow>Betting is closed for this race.</yellow>");
                return;
            }
            gui.selectHorse(horse);
            return;
        }

        switch (slot) {
            case 7 -> joinOrStartRace(player, gui);
            case 8 -> {
                openBoards.remove(player.getUniqueId());
                gui.back();
            }
            default -> {
            }
        }
    }

    public void handleClose(Player player, HorseRaceGUI gui) {
        openBoards.remove(player.getUniqueId());
    }

    public String getDisplayHorseName(String key) {
        return plugin.getConfigManager().getConfig().getString("games.horserace.horses." + key + ".name", key);
    }

    public double getHorseMultiplier(String key) {
        HorseRaceRound round = ensureRound();
        double poolOnHorse = round.getPoolForHorse(key);
        if (poolOnHorse <= 0.0) {
            return plugin.getConfigManager().getConfig().getDouble("games.horserace.horses." + key + ".multiplier", 2.0);
        }

        double totalPool = round.getTotalPool();
        return Math.max(1.1, (totalPool * HOUSE_CUT) / poolOnHorse);
    }

    public double getPlayerBetForHorse(String key) {
        HorseRaceRound round = currentRound;
        return round == null ? 0.0 : round.getPoolForHorse(key);
    }

    public HorseRaceRound getCurrentRound() {
        return ensureRound();
    }

    private double getHorseWeight(String key) {
        return plugin.getConfigManager().getConfig().getDouble("games.horserace.horses." + key + ".weight", 1.0);
    }

    private HorseRaceRound ensureRound() {
        HorseRaceRound round = currentRound;
        if (round == null || round.getState() == HorseRaceRound.State.FINISHED) {
            round = new HorseRaceRound(HORSE_KEYS.length);
            currentRound = round;
        }
        return round;
    }

    private boolean isBettingClosed() {
        HorseRaceRound.State state = ensureRound().getState();
        return state == HorseRaceRound.State.RACING || state == HorseRaceRound.State.FINISHED;
    }

    private void joinOrStartRace(Player player, HorseRaceGUI gui) {
        HorseRaceRound round = ensureRound();
        if (round.getState() == HorseRaceRound.State.RACING) {
            sendMessage(player, "<yellow>This race is already running. Wait for the next one.</yellow>");
            return;
        }
        if (round.getState() == HorseRaceRound.State.FINISHED) {
            currentRound = new HorseRaceRound(HORSE_KEYS.length);
            round = currentRound;
        }

        String selectedHorse = gui.getSelectedHorse();
        if (selectedHorse == null) {
            sendMessage(player, "<yellow>Select a horse before joining the race.</yellow>");
            return;
        }

        if (round.getBets().containsKey(player.getUniqueId())) {
            sendMessage(player, "<yellow>You are already entered in this race.</yellow>");
            return;
        }

        if (!preGameValidation(player, gui.getBetAmount()) || !withdrawBet(player, gui.getBetAmount())) {
            return;
        }

        setCooldown(player);
        round.getBets().put(player.getUniqueId(), new HorseRaceRound.BetEntry(player.getUniqueId(), selectedHorse, gui.getBetAmount()));
        refreshOpenBoards();

        if (round.getState() == HorseRaceRound.State.OPEN) {
            startCountdown(round);
        } else {
            sendMessage(player, "<green>You joined the current race. Countdown: " + round.getCountdownSeconds() + "s.</green>");
        }
    }

    private void startCountdown(HorseRaceRound round) {
        round.setState(HorseRaceRound.State.COUNTDOWN);
        round.setCountdownSeconds(10);
        refreshOpenBoards();

        round.setTask(new BukkitRunnable() {
            @Override
            public void run() {
                if (round != currentRound) {
                    cancel();
                    return;
                }

                int countdown = round.getCountdownSeconds();
                if (countdown <= 0) {
                    cancel();
                    startRace(round);
                    return;
                }

                round.setCountdownSeconds(countdown - 1);
                refreshOpenBoards();
            }
        }.runTaskTimer(plugin.getPlugin(), 0L, 20L));
    }

    private void startRace(HorseRaceRound round) {
        if (round.getBets().isEmpty()) {
            round.setState(HorseRaceRound.State.FINISHED);
            refreshOpenBoards();
            return;
        }

        round.setState(HorseRaceRound.State.RACING);
        round.setWinner(rollWinner());
        refreshOpenBoards();

        round.setTask(new BukkitRunnable() {
            @Override
            public void run() {
                if (round != currentRound) {
                    cancel();
                    return;
                }

                int winnerIndex = indexOf(round.getWinner());
                for (int i = 0; i < round.getPositions().length; i++) {
                    int movement = ThreadLocalRandom.current().nextInt(3) - 1;
                    if (i == winnerIndex && round.getPositions()[i] < 8) {
                        movement = Math.max(1, movement + 1);
                    }
                    round.getPositions()[i] = Math.max(0, Math.min(8, round.getPositions()[i] + movement));
                }
                round.getPositions()[winnerIndex] = Math.min(8, round.getPositions()[winnerIndex] + 1);
                refreshOpenBoards();

                if (round.getPositions()[winnerIndex] >= 8) {
                    cancel();
                    finishRace(round);
                }
            }
        }.runTaskTimer(plugin.getPlugin(), 0L, 8L));
    }

    private String rollWinner() {
        double total = 0.0;
        for (String horse : HORSE_KEYS) {
            total += getHorseWeight(horse);
        }

        double roll = ThreadLocalRandom.current().nextDouble(total);
        double cumulative = 0.0;
        for (String horse : HORSE_KEYS) {
            cumulative += getHorseWeight(horse);
            if (roll <= cumulative) {
                return horse;
            }
        }
        return HORSE_KEYS[0];
    }

    private void finishRace(HorseRaceRound round) {
        round.setState(HorseRaceRound.State.FINISHED);
        String winner = round.getWinner();
        double totalPool = round.getTotalPool();
        double winnerPool = round.getPoolForHorse(winner);

        for (HorseRaceRound.BetEntry betEntry : round.getBets().values()) {
            Player player = Bukkit.getPlayer(betEntry.playerId());
            if (player == null) {
                continue;
            }

            boolean won = winner.equals(betEntry.horseKey());
            double payout = won && winnerPool > 0.0 ? betEntry.betAmount() * ((totalPool * HOUSE_CUT) / winnerPool) : 0.0;
            HorseRaceGUI gui = openBoards.get(player.getUniqueId());

            if (won) {
                if (payWinnings(player, payout)) {
                    handleWin(player, betEntry.betAmount(), payout);
                } else {
                    sendMessage(player, "<red>Horse race payout failed. Contact an administrator.</red>");
                }
            } else {
                handleLoss(player, betEntry.betAmount());
            }

            if (gui != null) {
                gui.showResult(winner, won, payout, won && winnerPool > 0.0 ? ((totalPool * HOUSE_CUT) / winnerPool) : 0.0);
            }
        }

        refreshOpenBoards();
    }

    public void refreshOpenBoards() {
        for (HorseRaceGUI gui : openBoards.values()) {
            Bukkit.getScheduler().runTask(plugin.getPlugin(), gui::render);
        }
    }

    private int indexOf(String horseKey) {
        for (int i = 0; i < HORSE_KEYS.length; i++) {
            if (HORSE_KEYS[i].equals(horseKey)) {
                return i;
            }
        }
        return 0;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        HorseRaceRound round = currentRound;
        if (round != null && round.getTask() != null) {
            round.getTask().cancel();
        }
        openBoards.clear();
    }
}
