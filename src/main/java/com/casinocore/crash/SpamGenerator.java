package com.casinocore.crash;

import java.util.List;
import java.util.Random;

/**
 * Generates noisy fragments used by the corrupted console renderer.
 * The strings are intentionally aggressive, but bounded and deterministic enough
 * to stay readable in production logs.
 */
public final class SpamGenerator {

    private static final List<String> TOKENS = List.of(
        "NUL", "BS", "FF", "SOH", "EXC", "ERR", "NULLSTACK", "JMP", "ATHROW",
        "SIGSEGV", "VMTRAP", "OP_BREAK", "VERIFYERR", "BADFRAME", "ILLEGAL",
        "CP_UTF8", "0xDEAD", "0xBADC0DE", "STACKOVR", "HEAPFRAG", "MONITORERR"
    );

    private static final List<String> CONTROL = List.of(
        "\u0000", "\u0001", "\u0002", "\u0007", "\u0008", "\u000B", "\u0018", "\u001B"
    );

    private final Random random;

    public SpamGenerator(Random random) {
        this.random = random;
    }

    public String token() {
        return TOKENS.get(random.nextInt(TOKENS.size()));
    }

    public String control() {
        return CONTROL.get(random.nextInt(CONTROL.size()));
    }

    public String burst(int minTokens, int maxTokens) {
        int count = minTokens + random.nextInt(Math.max(1, maxTokens - minTokens + 1));
        StringBuilder out = new StringBuilder(count * 10);
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                out.append(random.nextBoolean() ? ' ' : '|');
            }
            out.append(token());
            if (random.nextInt(4) == 0) {
                out.append(control());
            }
        }
        return out.toString();
    }

    public String hexWord() {
        return "0x" + Integer.toHexString(random.nextInt()).toUpperCase();
    }

    public String offset() {
        return "+" + Integer.toHexString(random.nextInt(0xFFFF)).toUpperCase();
    }
}
