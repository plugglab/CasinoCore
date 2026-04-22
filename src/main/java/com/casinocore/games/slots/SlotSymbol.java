package com.casinocore.games.slots;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Represents a slot machine symbol using vanilla Minecraft items
 */
public enum SlotSymbol {
    // Jackpot symbols (rarest)
    DIAMOND("§b§lDiamond", Material.DIAMOND, 1, "§b✦ JACKPOT SYMBOL ✦"),

    // High value symbols
    EMERALD("§a§lEmerald", Material.EMERALD, 5, "§a◆ High Value ◆"),
    GOLD_INGOT("§6§lGold", Material.GOLD_INGOT, 8, "§6◆ High Value ◆"),

    // Medium value symbols
    IRON_INGOT("§7§lIron", Material.IRON_INGOT, 12, "§7● Medium Value ●"),
    REDSTONE("§c§lRedstone", Material.REDSTONE, 15, "§c● Medium Value ●"),
    LAPIS("§9§lLapis", Material.LAPIS_LAZULI, 15, "§9● Medium Value ●"),

    // Low value symbols (common)
    COAL("§8§lCoal", Material.COAL, 20, "§8○ Low Value ○"),
    APPLE("§c§lApple", Material.APPLE, 24, "§c○ Low Value ○");

    private final String displayName;
    private final Material material;
    private final int weight; // Higher weight = more common
    private final String lore;

    SlotSymbol(String displayName, Material material, int weight, String lore) {
        this.displayName = displayName;
        this.material = material;
        this.weight = weight;
        this.lore = lore;
    }

    /**
     * Get display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get material
     */
    public Material getMaterial() {
        return material;
    }

    /**
     * Get weight for random selection
     */
    public int getWeight() {
        return weight;
    }

    /**
     * Create an ItemStack for this symbol
     */
    public ItemStack createItem() {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Get a random symbol based on weighted probability
     */
    public static SlotSymbol getRandomSymbol(Random random) {
        int totalWeight = 0;
        for (SlotSymbol symbol : values()) {
            totalWeight += symbol.weight;
        }

        int randomValue = random.nextInt(totalWeight);
        int currentWeight = 0;

        for (SlotSymbol symbol : values()) {
            currentWeight += symbol.weight;
            if (randomValue < currentWeight) {
                return symbol;
            }
        }

        return COAL; // Fallback
    }

    /**
     * Get symbol by material
     */
    public static SlotSymbol fromMaterial(Material material) {
        for (SlotSymbol symbol : values()) {
            if (symbol.material == material) {
                return symbol;
            }
        }
        return null;
    }

    /**
     * Calculate multiplier based on matching symbols
     * @param symbols Array of 3 symbols
     * @return Multiplier (0 = no win)
     */
    public static double calculateMultiplier(SlotSymbol[] symbols) {
        if (symbols.length != 3) {
            return 0;
        }

        // Check for three matching symbols
        if (symbols[0] == symbols[1] && symbols[1] == symbols[2]) {
            switch (symbols[0]) {
                case DIAMOND:
                    return 50.0; // Jackpot!
                case EMERALD:
                    return 25.0;
                case GOLD_INGOT:
                    return 15.0;
                case IRON_INGOT:
                    return 10.0;
                case REDSTONE:
                case LAPIS:
                    return 5.0;
                case COAL:
                case APPLE:
                    return 3.0;
            }
        }

        // Check for two matching symbols
        if (symbols[0] == symbols[1] || symbols[1] == symbols[2] || symbols[0] == symbols[2]) {
            SlotSymbol matchingSymbol = symbols[0] == symbols[1] ? symbols[0] :
                                        (symbols[1] == symbols[2] ? symbols[1] : symbols[0]);

            switch (matchingSymbol) {
                case DIAMOND:
                    return 10.0;
                case EMERALD:
                    return 5.0;
                case GOLD_INGOT:
                    return 3.0;
                case IRON_INGOT:
                case REDSTONE:
                case LAPIS:
                    return 2.0;
                case COAL:
                case APPLE:
                    return 1.5;
            }
        }

        // No match - loss
        return 0;
    }

    /**
     * Get outcome description
     */
    public static String getOutcomeDescription(SlotSymbol[] symbols) {
        if (symbols.length != 3) {
            return "Invalid";
        }

        if (symbols[0] == symbols[1] && symbols[1] == symbols[2]) {
            if (symbols[0] == DIAMOND) {
                return "§6§l✦ JACKPOT! ✦";
            }
            return "§a§l● THREE OF A KIND! ●";
        }

        if (symbols[0] == symbols[1] || symbols[1] == symbols[2] || symbols[0] == symbols[2]) {
            return "§e§l◆ TWO MATCH! ◆";
        }

        return "§c§l✕ NO MATCH ✕";
    }
}
