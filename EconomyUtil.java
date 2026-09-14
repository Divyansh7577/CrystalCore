package com.crystalville.crystalcore.managers;

import com.crystalville.crystalcore.util.CrystalItemUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public final class EconomyUtil {

    private EconomyUtil() {
    }

    public static boolean transfer(Player sender, Player target, Material currency, int amount, String verb) {
        String displayName = currency == CrystalItemUtil.CURRENCY_MATERIAL
                ? ChatColor.stripColor(CrystalItemUtil.DISPLAY_NAME)
                : prettyName(currency);

        if (amount <= 0) {
            sender.sendMessage(ChatColor.RED + "Amount must be greater than zero.");
            return false;
        }

        if (sender.getUniqueId().equals(target.getUniqueId())) {
            sender.sendMessage(ChatColor.RED + "You cannot " + verb + " yourself.");
            return false;
        }

        if (!target.isOnline()) {
            sender.sendMessage(ChatColor.RED + "That player is not online.");
            return false;
        }

        if (sender.isOp()) {
            giveItems(target, currency, amount);
            sender.sendMessage(ChatColor.GOLD + "[OP Bypass] " + ChatColor.GREEN
                    + "Sent " + amount + "x " + displayName + " to " + target.getName()
                    + " (unlimited, nothing deducted from your inventory).");
            target.sendMessage(ChatColor.GREEN + "You received " + amount + "x "
                    + displayName + " from " + sender.getName() + ".");
            return true;
        }

        int have = countItems(sender.getInventory(), currency);
        if (have < amount) {
            sender.sendMessage(ChatColor.RED + "You don't have enough " + displayName
                    + ". Needed: " + amount + ", You have: " + have);
            return false;
        }

        removeItems(sender.getInventory(), currency, amount);
        giveItems(target, currency, amount);

        sender.sendMessage(ChatColor.GREEN + "You " + verb + "ed " + amount + "x "
                + displayName + " to " + target.getName() + ".");
        target.sendMessage(ChatColor.GREEN + "You received " + amount + "x "
                + displayName + " from " + sender.getName() + ".");
        return true;
    }

    private static String prettyName(Material material) {
        String[] parts = material.name().toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }

    private static int countItems(Inventory inventory, Material material) {
        int total = 0;
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() == material) {
                total += item.getAmount();
            }
        }
        return total;
    }

    private static void removeItems(Inventory inventory, Material material, int amount) {
        int remaining = amount;
        ItemStack[] contents = inventory.getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() == material) {
                int stackAmount = item.getAmount();
                if (stackAmount <= remaining) {
                    remaining -= stackAmount;
                    inventory.setItem(i, null);
                } else {
                    item.setAmount(stackAmount - remaining);
                    remaining = 0;
                }
            }
        }
    }

    private static void giveItems(Player target, Material material, int amount) {
        int maxStack = material.getMaxStackSize();
        Map<Integer, ItemStack> overflow = new HashMap<>();
        int remaining = amount;

        while (remaining > 0) {
            int stackSize = Math.min(remaining, maxStack);
            ItemStack stack = material == CrystalItemUtil.CURRENCY_MATERIAL
                    ? CrystalItemUtil.createCrystal(stackSize)
                    : new ItemStack(material, stackSize);
            overflow.putAll(target.getInventory().addItem(stack));
            remaining -= stackSize;
        }

        for (ItemStack leftover : overflow.values()) {
            target.getWorld().dropItemNaturally(target.getLocation(), leftover);
        }
    }
              }
