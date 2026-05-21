package com.casinocore.crash;

import org.bukkit.plugin.Plugin;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Builds a fake corrupted dump from a real Throwable.
 *
 * Example visual target:
 * <pre>
 * [CasinoCore/CRASH] ██ JVM CORRUPTION SURFACE DETECTED ██
 * [CasinoCore/CRASH] NUL|ERR|SOH frame::anti/decomp/GuardNode [EXC|0xDEADCAFE|+A2F]
 * [CasinoCore/CRASH]     at com.casinocore.games.blackjack.BlackjackGame.start(BlackjackGame.java:44)
 * [CasinoCore/CRASH] bc@8FAE  invokespecial  #442  <dev/null/KernelBridge.decryptFrame([B)Ljava/lang/String;>  0xA1B2C3D4
 * </pre>
 */
public final class CrashFormatter {

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_BOLD = "\u001B[1m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_BLACK_ON_RED = "\u001B[30;41m";
    private static final String ANSI_BRIGHT_RED = "\u001B[91m";
    private static final String ANSI_DIM = "\u001B[2m";

    private final Plugin plugin;
    private final boolean ansi;
    private final boolean redBackground;
    private final boolean unicodeNoise;
    private final SpamGenerator spamGenerator;
    private final FakeBytecodeBuilder bytecodeBuilder;
    private final Random random;

    public CrashFormatter(Plugin plugin, boolean ansi, boolean redBackground, boolean unicodeNoise, long seed) {
        this.plugin = plugin;
        this.ansi = ansi;
        this.redBackground = redBackground;
        this.unicodeNoise = unicodeNoise;
        this.random = new Random(seed);
        this.spamGenerator = new SpamGenerator(this.random);
        this.bytecodeBuilder = new FakeBytecodeBuilder(this.random, this.spamGenerator);
    }

    public List<String> format(Throwable throwable, String context) {
        List<String> lines = new ArrayList<>();
        String prefix = decorate("["
            + plugin.getName()
            + "/CRASH] ", ANSI_BLACK_ON_RED, ANSI_BOLD + ANSI_RED);

        lines.add(prefix + decorate("██ JVM CORRUPTION SURFACE DETECTED ██", ANSI_BLACK_ON_RED, ANSI_BRIGHT_RED + ANSI_BOLD));
        lines.add(prefix + decorate("timestamp=" + timestamp() + " context=" + context, ANSI_BLACK_ON_RED, ANSI_DIM + ANSI_RED));
        lines.add(prefix + decorate("signature=" + throwable.getClass().getName() + " : " + safeMessage(throwable), ANSI_BLACK_ON_RED, ANSI_BRIGHT_RED));
        lines.add(prefix + decorate(spamGenerator.burst(5, 10), ANSI_BLACK_ON_RED, ANSI_RED));
        lines.add(prefix + decorate(bytecodeBuilder.fakeCorruptionMarker(), ANSI_BLACK_ON_RED, ANSI_BRIGHT_RED));

        StackTraceElement[] trace = throwable.getStackTrace();
        int realFrames = Math.min(trace.length, 8);
        for (int i = 0; i < realFrames; i++) {
            if (random.nextBoolean()) {
                lines.add(prefix + decorate(bytecodeBuilder.fakeBytecodeLine(), ANSI_BLACK_ON_RED, ANSI_RED));
            }
            lines.add(prefix + decorate(mixRealFrame(trace[i]), ANSI_BLACK_ON_RED, ANSI_BRIGHT_RED));
            if (random.nextInt(3) == 0) {
                lines.add(prefix + decorate(bytecodeBuilder.fakeJvmFrame(), ANSI_BLACK_ON_RED, ANSI_DIM + ANSI_RED));
            }
        }

        Throwable cause = throwable.getCause();
        if (cause != null && cause != throwable) {
            lines.add(prefix + decorate("Caused by: " + cause.getClass().getName() + ": " + safeMessage(cause), ANSI_BLACK_ON_RED, ANSI_BRIGHT_RED + ANSI_BOLD));
        }

        lines.add(prefix + decorate(spamGenerator.burst(6, 12), ANSI_BLACK_ON_RED, ANSI_RED));
        lines.add(prefix + decorate("dump.guard=ANTI_DECOMPILER bytecode.guard=ENABLED renderer=CORRUPTED", ANSI_BLACK_ON_RED, ANSI_BRIGHT_RED));
        lines.add(prefix + decorate("██ END OF SCRAMBLED EXCEPTION DUMP ██", ANSI_BLACK_ON_RED, ANSI_BRIGHT_RED + ANSI_BOLD));
        return lines;
    }

    private String mixRealFrame(StackTraceElement frame) {
        StringBuilder out = new StringBuilder();
        out.append("\tat ")
            .append(frame.getClassName())
            .append('.')
            .append(frame.getMethodName())
            .append('(')
            .append(frame.getFileName() == null ? "Unknown Source" : frame.getFileName());

        if (frame.getLineNumber() >= 0) {
            out.append(':').append(frame.getLineNumber());
        }
        out.append(')');

        if (random.nextBoolean()) {
            out.append("  // ").append(bytecodeBuilder.fakeCorruptionMarker());
        }
        if (unicodeNoise && random.nextInt(4) == 0) {
            out.append(' ').append(spamGenerator.control());
        }
        return out.toString();
    }

    private String safeMessage(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.isBlank()) {
            return "NULLSTACK/" + spamGenerator.token();
        }
        return message;
    }

    private String timestamp() {
        return DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC).format(Instant.now());
    }

    private String decorate(String input, String backgroundStyle, String foregroundStyle) {
        if (!ansi) {
            return input;
        }
        StringBuilder style = new StringBuilder();
        if (redBackground) {
            style.append(backgroundStyle);
        }
        style.append(foregroundStyle);
        return style + input + ANSI_RESET;
    }
}
