package com.crystalville.crystalcore.util;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;

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
  }
