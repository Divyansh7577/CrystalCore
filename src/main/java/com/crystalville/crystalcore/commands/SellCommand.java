package com.crystalville.crystalcore.commands;

import com.crystalville.crystalcore.managers.ShopManager;
import com.crystalville.crystalcore.util.CrystalItemUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * /sell <item> <quantity>  - sell items from your inventory for Crystals
 * /sell info <item>        - check an item's sell price without selling
 *
 * Open to all players (no Market Minister restriction) - selling is normal
 * commerce. Uses the exact same price list as /buy (config.yml "shop" section).
 */
public class SellCommand implements CommandExecutor {

    private final ShopManager shopManager;

    public SellCommand(ShopManager shopManager) {
        this.shopManager = shopManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("info")) {
            handleInfo(player, args);
            return true;
        }

        handleSale(player, args);
        return true;
    }

    private void sendUsage(Player player) {
        player.sendMessage(Component.text("Usage:", NamedTextColor.RED));
        player.sendMessage(Component.text("  /sell <item name> <quantity>", NamedTextColor.GRAY));
        player.sendMessage(Component.text("  /sell info <item name>", NamedTextColor.GRAY));
    }

    private void handleInfo(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /sell info <item name>", NamedTextColor.RED));
            return;
        }

        String itemQuery = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        Material material = resolveMaterial(itemQuery);

        if (material == null) {
            player.sendMessage(Component.text("Unknown item: " + itemQuery, NamedTextColor.RED));
            return;
        }

        if (!shopManager.isForSale(material)) {
            player.sendMessage(Component.text(
                    prettyName(material) + " cannot be sold here.", NamedTextColor.YELLOW));
            return;
        }

        int price = shopManager.getPrice(material);
        player.sendMessage(Component.text("Selling " + prettyName(material) + " gives you ", NamedTextColor.GREEN)
                .append(Component.text(price + " Crystal(s) ", NamedTextColor.LIGHT_PURPLE))
                .append(Component.text("each.", NamedTextColor.GREEN)));
    }

    private void handleSale(Player player, String[] args) {
        if (args.length < 2) {
            sendUsage(player);
            return;
        }

        String quantityStr = args[args.length - 1];
        int quantity;
        try {
            quantity = Integer.parseInt(quantityStr);
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Quantity must be a whole number.", NamedTextColor.RED));
            return;
        }

        if (quantity <= 0) {
            player.sendMessage(Component.text("Quantity must be greater than zero.", NamedTextColor.RED));
            return;
        }

        String itemQuery = String.join(" ", Arrays.copyOfRange(args, 0, args.length - 1));
        Material material = resolveMaterial(itemQuery);

        if (material == null) {
            player.sendMessage(Component.text("Unknown item: " + itemQuery, NamedTextColor.RED));
            return;
        }

        if (!shopManager.isForSale(material)) {
            player.sendMessage(Component.text(
                    prettyName(material) + " cannot be sold here.", NamedTextColor.RED));
            return;
        }

        // Never allow selling the currency item itself back for more currency.
        if (material == CrystalItemUtil.CURRENCY_MATERIAL) {
            player.sendMessage(Component.text("You cannot sell Crystals themselves.", NamedTextColor.RED));
            return;
        }

        int have = countItems(player.getInventory(), material);
        if (have < quantity) {
            player.sendMessage(Component.text(
                    "You don't have enough " + prettyName(material)
                            + ". Needed: " + quantity + ", You have: " + have, NamedTextColor.RED));
            return;
        }

        int price = shopManager.getPrice(material);
        int totalPayout = price * quantity;

        removeItems(player.getInventory(), material, quantity);
        giveCrystals(player, totalPayout);

        player.sendMessage(Component.text("Sold " + quantity + "x " + prettyName(material)
                + " for " + totalPayout + " Crystal(s).", NamedTextColor.GREEN));
    }

    private Material resolveMaterial(String query) {
        String normalized = query.trim().toUpperCase().replace(' ', '_').replace('-', '_');
        return Material.matchMaterial(normalized);
    }

    private String prettyName(Material material) {
        String[] parts = material.name().toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }

    private int countItems(Inventory inventory, Material material) {
        int total = 0;
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() == material) {
                total += item.getAmount();
            }
        }
        return total;
    }

    private void removeItems(Inventory inventory, Material material, int amount) {
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

    private void giveCrystals(Player player, int amount) {
        int maxStack = CrystalItemUtil.CURRENCY_MATERIAL.getMaxStackSize();
        Map<Integer, ItemStack> overflow = new HashMap<>();
        int remaining = amount;

        while (remaining > 0) {
            int stackSize = Math.min(remaining, maxStack);
            ItemStack stack = CrystalItemUtil.createCrystal(stackSize);
            overflow.putAll(player.getInventory().addItem(stack));
            remaining -= stackSize;
        }

        for (ItemStack leftover : overflow.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
    }
  }
