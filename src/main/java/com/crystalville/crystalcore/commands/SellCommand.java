package com.crystalville.crystalcore.commands;

import com.crystalville.crystalcore.gui.SellGuiManager;
import com.crystalville.crystalcore.managers.BankManager;
import com.crystalville.crystalcore.managers.RankManager;
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

/**
 * /sell <item> <quantity>  - sell items from your inventory for Crystals
 * /sell info <item>        - check an item's sell price without selling
 * /sell gui                - opens a visual, paginated sell interface
 *
 * Open to all players. If a sale's payout doesn't fully fit in the
 * player's inventory, the overflow is deposited into their Crystal Bank
 * (respecting the normal cap) instead of being dropped on the ground.
 */
public class SellCommand implements CommandExecutor {

    private static final String FINANCE_MINISTER_ROLE = "FINANCE_MINISTER";

    private final ShopManager shopManager;
    private final BankManager bankManager;
    private final RankManager rankManager;
    private final SellGuiManager sellGuiManager;

    public SellCommand(ShopManager shopManager, BankManager bankManager, RankManager rankManager,
                        SellGuiManager sellGuiManager) {
        this.shopManager = shopManager;
        this.bankManager = bankManager;
        this.rankManager = rankManager;
        this.sellGuiManager = sellGuiManager;
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

        if (args[0].equalsIgnoreCase("gui")) {
            player.openInventory(sellGuiManager.open(player, 0));
            return true;
        }

        handleSale(player, args);
        return true;
    }

    private void sendUsage(Player player) {
        player.sendMessage(Component.text("Usage:", NamedTextColor.RED));
        player.sendMessage(Component.text("  /sell <item name> <quantity>", NamedTextColor.GRAY));
        player.sendMessage(Component.text("  /sell info <item name>", NamedTextColor.GRAY));
        player.sendMessage(Component.text("  /sell gui", NamedTextColor.GRAY));
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

        int sellPrice = shopManager.getSellPrice(material);
        player.sendMessage(Component.text("Selling " + prettyName(material) + " gives you ", NamedTextColor.GREEN)
                .append(Component.text(sellPrice + " Crystal(s) ", NamedTextColor.LIGHT_PURPLE))
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

        int sellPrice = shopManager.getSellPrice(material);
        int totalPayout = sellPrice * quantity;

        removeItems(player.getInventory(), material, quantity);
        payoutCrystals(player, totalPayout);

        player.sendMessage(Component.text("Sold " + quantity + "x " + prettyName(material)
                + " for " + totalPayout + " Crystal(s).", NamedTextColor.GREEN));
    }

    private void payoutCrystals(Player player, int totalAmount) {
        int inventoryCapacity = CrystalItemUtil.freeCapacity(player);
        int toInventory = Math.min(totalAmount, inventoryCapacity);
        int remainder = totalAmount - toInventory;

        if (toInventory > 0) {
            CrystalItemUtil.giveCrystals(player, toInventory);
        }

        if (remainder <= 0) {
            return;
        }

        boolean unlimited = player.isOp() || rankManager.hasRole(player.getUniqueId(), FINANCE_MINISTER_ROLE);
        long depositedToBank = bankManager.deposit(player.getUniqueId(), remainder, unlimited);
        long stillOverflow = remainder - depositedToBank;

        if (depositedToBank > 0) {
            player.sendMessage(Component.text(
                    "Your inventory was full - " + depositedToBank + " Crystal(s) were deposited into your bank.",
                    NamedTextColor.AQUA));
        }

        if (stillOverflow > 0) {
            CrystalItemUtil.giveCrystals(player, stillOverflow);
            player.sendMessage(Component.text(
                    "Your bank is also full - " + stillOverflow + " Crystal(s) were dropped at your feet.",
                    NamedTextColor.YELLOW));
        }
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
                }
