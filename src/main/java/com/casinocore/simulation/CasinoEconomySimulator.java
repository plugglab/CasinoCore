package com.casinocore.simulation;

import java.util.Random;

public final class CasinoEconomySimulator {

    private static final int PLAYS = 10_000;

    private CasinoEconomySimulator() {
    }

    public static void main(String[] args) {
        Random random = new Random(12345L);

        print(simulateCoinFlip(random));
        print(simulateDice(random));
        print(simulateLottery(random));
        print(simulateBlackjack(random));
        print(simulateRoulette(random));
        print(simulateSlots(random));
    }

    private static SimulationResult simulateCoinFlip(Random random) {
        double totalBet = 0.0;
        double totalPayout = 0.0;
        double payoutMultiplier = 1.9;

        for (int i = 0; i < PLAYS; i++) {
            totalBet += 1.0;
            if (random.nextBoolean()) {
                totalPayout += payoutMultiplier;
            }
        }

        return new SimulationResult("CoinFlip", totalBet, totalPayout);
    }

    private static SimulationResult simulateDice(Random random) {
        double totalBet = 0.0;
        double totalPayout = 0.0;
        RiskProfile[] profiles = {
            new RiskProfile(45, 1.64),
            new RiskProfile(65, 2.57),
            new RiskProfile(85, 6.0)
        };

        for (int i = 0; i < PLAYS; i++) {
            RiskProfile profile = profiles[random.nextInt(profiles.length)];
            totalBet += 1.0;
            int roll = random.nextInt(100) + 1;
            if (roll > profile.threshold()) {
                totalPayout += profile.multiplier();
            }
        }

        return new SimulationResult("Dice", totalBet, totalPayout);
    }

    private static SimulationResult simulateLottery(Random random) {
        double totalBet = 0.0;
        double totalPayout = 0.0;

        for (int i = 0; i < PLAYS; i++) {
            totalBet += 1.0;
            int winning = random.nextInt(100) + 1;
            int player = random.nextInt(100) + 1;
            int difference = Math.abs(winning - player);

            if (difference == 0) {
                totalPayout += 32.0;
            } else if (difference <= 5) {
                totalPayout += 4.5;
            } else if (difference <= 10) {
                totalPayout += 2.2;
            }
        }

        return new SimulationResult("Lottery", totalBet, totalPayout);
    }

    private static SimulationResult simulateBlackjack(Random random) {
        double totalBet = 0.0;
        double totalPayout = 0.0;

        for (int i = 0; i < PLAYS; i++) {
            totalBet += 1.0;
            Hand player = new Hand();
            Hand dealer = new Hand();

            player.add(drawCard(random));
            dealer.add(drawCard(random));
            player.add(drawCard(random));
            dealer.add(drawCard(random));

            if (player.isBlackjack() || dealer.isBlackjack()) {
                if (player.isBlackjack() && dealer.isBlackjack()) {
                    totalPayout += 1.0;
                } else if (player.isBlackjack()) {
                    totalPayout += 2.2;
                }
                continue;
            }

            while (player.bestValue() < 17) {
                player.add(drawCard(random));
                if (player.isBust()) {
                    break;
                }
            }

            if (player.isBust()) {
                continue;
            }

            while (shouldDealerHit(dealer)) {
                dealer.add(drawCard(random));
            }

            if (dealer.isBust() || player.bestValue() > dealer.bestValue()) {
                totalPayout += 1.9;
            } else if (player.bestValue() == dealer.bestValue()) {
                totalPayout += 1.0;
            }
        }

        return new SimulationResult("Blackjack", totalBet, totalPayout);
    }

    private static SimulationResult simulateRoulette(Random random) {
        double totalBet = 0.0;
        double totalPayout = 0.0;

        for (int i = 0; i < PLAYS; i++) {
            totalBet += 1.0;
            int result = random.nextInt(37);
            int betType = random.nextInt(5);

            switch (betType) {
                case 0 -> {
                    int chosen = random.nextInt(37);
                    if (chosen == result) {
                        totalPayout += 35.0;
                    }
                }
                case 1 -> {
                    if (isRed(result)) {
                        totalPayout += 1.9;
                    }
                }
                case 2 -> {
                    if (isBlack(result)) {
                        totalPayout += 1.9;
                    }
                }
                case 3 -> {
                    if (result != 0 && result % 2 == 0) {
                        totalPayout += 1.9;
                    }
                }
                default -> {
                    if (result % 2 == 1) {
                        totalPayout += 1.9;
                    }
                }
            }
        }

        return new SimulationResult("Roulette", totalBet, totalPayout);
    }

    private static SimulationResult simulateSlots(Random random) {
        double totalBet = 0.0;
        double totalPayout = 0.0;
        SlotRoll[] values = SlotRoll.values();
        int totalWeight = 0;
        for (SlotRoll value : values) {
            totalWeight += value.weight;
        }

        for (int i = 0; i < PLAYS; i++) {
            totalBet += 1.0;
            SlotRoll a = rollSymbol(random, values, totalWeight);
            SlotRoll b = rollSymbol(random, values, totalWeight);
            SlotRoll c = rollSymbol(random, values, totalWeight);
            totalPayout += slotPayout(a, b, c);
        }

        return new SimulationResult("Slots", totalBet, totalPayout);
    }

    private static SlotRoll rollSymbol(Random random, SlotRoll[] values, int totalWeight) {
        int value = random.nextInt(totalWeight);
        int cumulative = 0;
        for (SlotRoll symbol : values) {
            cumulative += symbol.weight;
            if (value < cumulative) {
                return symbol;
            }
        }
        return SlotRoll.COAL;
    }

    private static double slotPayout(SlotRoll a, SlotRoll b, SlotRoll c) {
        if (a == b && b == c) {
            return a.threeOfKind;
        }

        if (a == b || b == c || a == c) {
            SlotRoll match = a == b ? a : (b == c ? b : a);
            return match.twoOfKind;
        }

        return 0.0;
    }

    private static int drawCard(Random random) {
        int rank = random.nextInt(13);
        if (rank == 0) {
            return 11;
        }
        return Math.min(rank + 1, 10);
    }

    private static boolean shouldDealerHit(Hand dealer) {
        return dealer.bestValue() < 17;
    }

    private static boolean isRed(int number) {
        return switch (number) {
            case 1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36 -> true;
            default -> false;
        };
    }

    private static boolean isBlack(int number) {
        return number != 0 && !isRed(number);
    }

    private static void print(SimulationResult result) {
        System.out.printf(
            "%s: plays=%d totalBet=%.2f totalPayout=%.2f rtp=%.4f houseEdge=%.4f%%%n",
            result.name,
            PLAYS,
            result.totalBet,
            result.totalPayout,
            result.rtp(),
            result.houseEdgePercent()
        );
    }

    private record SimulationResult(String name, double totalBet, double totalPayout) {
        private double rtp() {
            return totalPayout / totalBet;
        }

        private double houseEdgePercent() {
            return (1.0 - rtp()) * 100.0;
        }
    }

    private record RiskProfile(int threshold, double multiplier) {
    }

    private enum SlotRoll {
        DIAMOND(1, 50.0, 10.0),
        EMERALD(5, 25.0, 5.0),
        GOLD(8, 15.0, 3.0),
        IRON(12, 10.0, 2.0),
        REDSTONE(15, 5.0, 2.0),
        LAPIS(15, 5.0, 2.0),
        COAL(20, 3.0, 1.5),
        APPLE(24, 3.0, 1.5);

        private final int weight;
        private final double threeOfKind;
        private final double twoOfKind;

        SlotRoll(int weight, double threeOfKind, double twoOfKind) {
            this.weight = weight;
            this.threeOfKind = threeOfKind;
            this.twoOfKind = twoOfKind;
        }
    }

    private static final class Hand {
        private int total;
        private int aces;
        private int cardCount;

        private void add(int value) {
            total += value;
            cardCount++;
            if (value == 11) {
                aces++;
            }
        }

        private int bestValue() {
            int current = total;
            int softAces = aces;
            while (current > 21 && softAces > 0) {
                current -= 10;
                softAces--;
            }
            return current;
        }

        private boolean isBlackjack() {
            return cardCount == 2 && bestValue() == 21;
        }

        private boolean isBust() {
            return bestValue() > 21;
        }
    }
}
