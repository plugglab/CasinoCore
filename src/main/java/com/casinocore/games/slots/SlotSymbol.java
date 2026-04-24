package com.casinocore.games.slots;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Random;

public enum SlotSymbol {
    DIAMOND("§b§lDiamond", Material.DIAMOND, 1, "§b§lJACKPOT SYMBOL"),
    EMERALD("§a§lEmerald", Material.EMERALD, 5, "§aHigh Value"),
    GOLD_INGOT("§6§lGold", Material.GOLD_INGOT, 8, "§6High Value"),
    IRON_INGOT("§7§lIron", Material.IRON_INGOT, 12, "§7Medium Value"),
    REDSTONE("§c§lRedstone", Material.REDSTONE, 15, "§cMedium Value"),
    LAPIS("§9§lLapis", Material.LAPIS_LAZULI, 15, "§9Medium Value"),
    COAL("§8§lCoal", Material.COAL, 20, "§8Low Value"),
    APPLE("§c§lApple", Material.APPLE, 24, "§cLow Value");

    private final String displayName;
    private final Material material;
    private final int weight;
    private final String lore;

    SlotSymbol(String displayName, Material material, int weight, String lore) {
        this.displayName = displayName;
        this.material = material;
        this.weight = weight;
        this.lore = lore;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getMaterial() {
        return material;
    }

    public int getWeight() {
        return weight;
    }

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

        return COAL;
    }

    public static SlotSymbol fromMaterial(Material material) {
        for (SlotSymbol symbol : values()) {
            if (symbol.material == material) {
                return symbol;
            }
        }
        return null;
    }

    public static double calculateMultiplier(SlotSymbol[] symbols) {
        if (symbols.length != 3) {
            return 0;
        }

        if (symbols[0] == symbols[1] && symbols[1] == symbols[2]) {
            return switch (symbols[0]) {
                case DIAMOND -> 50.0;
                case EMERALD -> 25.0;
                case GOLD_INGOT -> 15.0;
                case IRON_INGOT -> 10.0;
                case REDSTONE, LAPIS -> 5.0;
                case COAL, APPLE -> 3.0;
            };
        }

        if (symbols[0] == symbols[1] || symbols[1] == symbols[2] || symbols[0] == symbols[2]) {
            SlotSymbol matchingSymbol = symbols[0] == symbols[1] ? symbols[0]
                : (symbols[1] == symbols[2] ? symbols[1] : symbols[0]);

            return switch (matchingSymbol) {
                case DIAMOND -> 10.0;
                case EMERALD -> 5.0;
                case GOLD_INGOT -> 3.0;
                case IRON_INGOT, REDSTONE, LAPIS -> 2.0;
                case COAL, APPLE -> 1.5;
            };
        }

        return 0;
    }

    public static String getOutcomeDescription(SlotSymbol[] symbols) {
        if (symbols.length != 3) {
            return "Invalid";
        }

        if (symbols[0] == symbols[1] && symbols[1] == symbols[2]) {
            if (symbols[0] == DIAMOND) {
                return "§6§lJACKPOT!";
            }
            return "§a§lTHREE OF A KIND!";
        }

        if (symbols[0] == symbols[1] || symbols[1] == symbols[2] || symbols[0] == symbols[2]) {
            return "§e§lTWO MATCH!";
        }

        return "§c§lNO MATCH";
    }
}
