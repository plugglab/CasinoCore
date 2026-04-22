package com.casinocore.games.roulette;

import java.util.List;
import java.util.Set;

public final class RouletteNumbers {

    public static final List<Integer> WHEEL_ORDER = List.of(
        0, 32, 15, 19, 4, 21, 2, 25, 17, 34, 6, 27, 13, 36, 11, 30, 8, 23, 10,
        5, 24, 16, 33, 1, 20, 14, 31, 9, 22, 18, 29, 7, 28, 12, 35, 3, 26
    );

    private static final Set<Integer> RED_NUMBERS = Set.of(
        1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36
    );

    private RouletteNumbers() {
    }

    public static boolean isRed(int number) {
        return RED_NUMBERS.contains(number);
    }

    public static boolean isBlack(int number) {
        return number != 0 && !RED_NUMBERS.contains(number);
    }
}
