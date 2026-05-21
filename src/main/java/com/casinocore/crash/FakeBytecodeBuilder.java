package com.casinocore.crash;

import java.util.List;
import java.util.Random;

/**
 * Creates fake bytecode and obfuscation-like lines that can be mixed with real stack traces.
 */
public final class FakeBytecodeBuilder {

    private static final List<String> RETURN_TYPES = List.of("V", "I", "J", "Z", "Ljava/lang/String;", "[B");
    private static final List<String> OPCODES = List.of(
        "aload_0", "invokestatic", "invokevirtual", "invokespecial", "dup",
        "athrow", "checkcast", "monitorenter", "monitorexit", "goto", "ifeq", "ifnonnull"
    );
    private static final List<String> OWNERS = List.of(
        "xX/NullPhase", "dev/null/KernelBridge", "jvm/heap/FramePoison", "anti/decomp/GuardNode",
        "vm/trace/Redacted", "bytecode/ghost/SyntheticTrap", "obf/$Proxy$_9f", "vm/core/Corruptor"
    );

    private final Random random;
    private final SpamGenerator spamGenerator;

    public FakeBytecodeBuilder(Random random, SpamGenerator spamGenerator) {
        this.random = random;
        this.spamGenerator = spamGenerator;
    }

    public String fakeMethodSignature() {
        return owner() + "." + methodName() + descriptor();
    }

    public String fakeBytecodeLine() {
        return "bc@" + Integer.toHexString(random.nextInt(0xFFFF)).toUpperCase()
            + "  " + opcode()
            + "  #" + random.nextInt(900)
            + "  <" + fakeMethodSignature() + ">  " + spamGenerator.hexWord();
    }

    public String fakeJvmFrame() {
        return "\tat " + fakeMethodSignature() + " [" + owner() + ".class:" + (10 + random.nextInt(900)) + "]";
    }

    public String fakeCorruptionMarker() {
        return "frame::" + owner() + " [" + spamGenerator.token() + "|" + spamGenerator.hexWord() + "|" + spamGenerator.offset() + "]";
    }

    private String owner() {
        return OWNERS.get(random.nextInt(OWNERS.size()));
    }

    private String methodName() {
        return switch (random.nextInt(8)) {
            case 0 -> "m";
            case 1 -> "$";
            case 2 -> "bridge$" + Integer.toHexString(random.nextInt(0xFFF));
            case 3 -> "lambda$" + Integer.toHexString(random.nextInt(0xFFFF));
            case 4 -> "verifyStack";
            case 5 -> "decryptFrame";
            case 6 -> "trap";
            default -> "decode" + Integer.toHexString(random.nextInt(0xFF));
        };
    }

    private String descriptor() {
        return "(" + randomParams() + ")" + RETURN_TYPES.get(random.nextInt(RETURN_TYPES.size()));
    }

    private String randomParams() {
        int count = random.nextInt(4);
        StringBuilder params = new StringBuilder();
        for (int i = 0; i < count; i++) {
            params.append(RETURN_TYPES.get(random.nextInt(RETURN_TYPES.size())));
        }
        return params.toString();
    }

    private String opcode() {
        return OPCODES.get(random.nextInt(OPCODES.size()));
    }
}
