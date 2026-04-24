package com.casinocore.games.horserace;

import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class HorseRaceRound {

    enum State {
        OPEN,
        COUNTDOWN,
        RACING,
        FINISHED
    }

    private final Map<UUID, BetEntry> bets = new ConcurrentHashMap<>();
    private final int[] positions;
    private volatile State state;
    private volatile int countdownSeconds;
    private volatile String winner;
    private volatile BukkitTask task;

    HorseRaceRound(int horseCount) {
        this.positions = new int[horseCount];
        this.state = State.OPEN;
        this.countdownSeconds = 10;
    }

    Map<UUID, BetEntry> getBets() {
        return bets;
    }

    int[] getPositions() {
        return positions;
    }

    State getState() {
        return state;
    }

    void setState(State state) {
        this.state = state;
    }

    int getCountdownSeconds() {
        return countdownSeconds;
    }

    void setCountdownSeconds(int countdownSeconds) {
        this.countdownSeconds = countdownSeconds;
    }

    String getWinner() {
        return winner;
    }

    void setWinner(String winner) {
        this.winner = winner;
    }

    BukkitTask getTask() {
        return task;
    }

    void setTask(BukkitTask task) {
        this.task = task;
    }

    double getTotalPool() {
        return bets.values().stream().mapToDouble(BetEntry::betAmount).sum();
    }

    double getPoolForHorse(String horseKey) {
        return bets.values().stream()
            .filter(entry -> entry.horseKey().equals(horseKey))
            .mapToDouble(BetEntry::betAmount)
            .sum();
    }

    record BetEntry(UUID playerId, String horseKey, double betAmount) {
    }
}
