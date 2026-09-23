package com.crystalville.crystalcore.util;

import com.crystalville.crystalcore.managers.BankManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class CrystalItemUtil {

    public static final String DISPLAY_NAME = ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Crystal";
    public static final Material CURRENCY_MATERIAL = Material.AMETHYST_SHARD;

    private static NamespacedKey crystalKey;

    private CrystalItemUtil() {
    }

    public static void init(JavaPlugin plugin) {
        crystalKey = new NamespacedKey(plugin, "crystalville_crystal");
    }

    public static ItemStack createCrystal(int amount) {
        ItemStack stack = new ItemStack(CURRENCY_MATERIAL, amount);
        applyCrystalMeta(stack);
        return stack;
    }

    public static boolean applyCrystalMeta(ItemStack stack) {
        if (stack == null || stack.getType() != CURRENCY_MATERIAL) {
            return false;
        }
        if (isCrystal(stack)) {
            return false;
        }

        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return false;
        }

        meta.setDisplayName(DISPLAY_NAME);
        meta.setLore(Collections.singletonList(ChatColor.GRAY + "Crystal Ville SMP Currency"));
        meta.getPersistentDataContainer().set(crystalKey, PersistentDataType.BYTE, (byte) 1);
        stack.setItemMeta(meta);
        return true;
    }

    public static boolean isCrystal(ItemStack stack) {
        if (stack == null || stack.getType() != CURRENCY_MATERIAL || crystalKey == null) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return false;
        }
        return meta.getPersistentDataContainer().has(crystalKey, PersistentDataType.BYTE);
    }

    public static boolean needsRename(ItemStack stack) {
        return stack != null && stack.getType() == CURRENCY_MATERIAL && !isCrystal(stack);
    }

    /**
     * Counts how many Crystals could still fit in the player's main inventory
     * (36 storage slots, excluding armor/offhand), accounting for existing
     * partial Crystal stacks and empty slots.
     */
    public static int freeCapacity(Player player) {
        int capacity = 0;
        int maxStack = CURRENCY_MATERIAL.getMaxStackSize();

        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item == null || item.getType() == Material.AIR) {
                capacity += maxStack;
            } else if (item.getType() == CURRENCY_MATERIAL) {
                capacity += Math.max(0, maxStack - item.getAmount());
            }
        }
        return capacity;
    }

    /** Counts Crystals sitting in the player's main inventory only (no ender chest, no bank). */
    public static long countInventoryCrystals(Player player) {
        long total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == CURRENCY_MATERIAL) {
                total += item.getAmount();
            }
        }
        return total;
    }

    /** Counts Crystals sitting in the player's ender chest only. */
    public static long countEnderChestCrystals(Player player) {
        long total = 0;
        for (ItemStack item : player.getEnderChest().getContents()) {
            if (item != null && item.getType() == CURRENCY_MATERIAL) {
                total += item.getAmount();
            }
        }
        return total;
    }

    /**
     * The single source of truth for a player's "total Crystals": physical
     * inventory + ender chest + Crystal Bank balance combined. Used by both
     * the HUD display and the website stats sync, so the two never disagree.
     */
    public static long getTotalCrystals(Player player, BankManager bankManager) {
        long total = countInventoryCrystals(player) + countEnderChestCrystals(player);
        if (bankManager != null) {
            total += bankManager.getBalance(player.getUniqueId());
        }
        return total;
    }

    /**
     * Gives `amount` Crystals to a player: as many as fit directly into
     * their inventory, split correctly into full 64-stacks plus a final
     * partial stack, with any true overflow (inventory completely full)
     * dropped at their feet so nothing is ever silently lost.
     *
     * BUGFIX: earlier versions of this logic across the plugin collected
     * leftover items from repeated Inventory#addItem() calls into a
     * Map<Integer, ItemStack>. Bukkit's addItem() always returns leftovers
     * keyed starting at index 0 on EVERY call, so each loop iteration's
     * putAll() silently overwrote (destroyed) the previous iteration's
     * leftover stack at the same key. This is why a withdrawal of 800
     * Crystals could result in far fewer actually reaching the player once
     * their inventory filled up partway through. Using a List instead of a
     * Map (as done here) has no such key collision - every leftover stack
     * from every iteration is preserved and dropped on the ground.
     */
    public static void giveCrystals(Player player, long amount) {
        int maxStack = CURRENCY_MATERIAL.getMaxStackSize();
        long remaining = amount;
        List<ItemStack> overflow = new ArrayList<>();

        while (remaining > 0) {
            int stackSize = (int) Math.min(remaining, maxStack);
            ItemStack stack = createCrystal(stackSize);
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(stack);
            overflow.addAll(leftover.values());
            remaining -= stackSize;
        }

        for (ItemStack stack : overflow) {
            player.getWorld().dropItemNaturally(player.getLocation(), stack);
        }
    }
            }
