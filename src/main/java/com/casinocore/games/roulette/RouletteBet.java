package com.casinocore.games.roulette;

public class RouletteBet {

    private final RouletteBetType type;
    private final Integer number;

    private RouletteBet(RouletteBetType type, Integer number) {
        this.type = type;
        this.number = number;
    }

    public static RouletteBet singleNumber(int number) {
        return new RouletteBet(RouletteBetType.SINGLE_NUMBER, number);
    }

    public static RouletteBet simple(RouletteBetType type) {
        return new RouletteBet(type, null);
    }

    public RouletteBetType getType() {
        return type;
    }

    public Integer getNumber() {
        return number;
    }

    public String getDisplayLabel() {
        if (type == RouletteBetType.SINGLE_NUMBER && number != null) {
            return "Number " + number;
        }
        return type.getDisplayName();
    }

    public boolean matches(int resultNumber) {
        return switch (type) {
            case SINGLE_NUMBER -> number != null && number == resultNumber;
            case RED -> RouletteNumbers.isRed(resultNumber);
            case BLACK -> RouletteNumbers.isBlack(resultNumber);
            case EVEN -> resultNumber != 0 && resultNumber % 2 == 0;
            case ODD -> resultNumber != 0 && resultNumber % 2 != 0;
        };
    }
}
