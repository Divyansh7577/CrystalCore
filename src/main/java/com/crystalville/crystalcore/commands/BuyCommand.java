package com.crystalville.crystalcore.commands;

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
import java.util.HashMap;
import java.util.Map;

/**
 * /buy <item> <quantity>   - purchase items from the server shop using Crystals
 * /buy info <item>         - check an item's price without buying
 *
 * Purchasing is restricted to players holding the "Market Minister" role
 * (see RankManager / RoleCommand). OPs always bypass both the role check
 * and the Crystal cost, consistent with the rest of CrystalCore's economy.
 */
public class BuyCommand implements CommandExecutor {

    private static final String MARKET_MINISTER_ROLE = "MARKET_MINISTER";

    private final ShopManager shopManager;
    private final RankManager rankManager;

    public BuyCommand(ShopManager shopManager, RankManager rankManager) {
        this.shopManager = shopManager;
        this.rankManager = rankManager;
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

        handlePurchase(player, args);
        return true;
    }

    private void sendUsage(Player player) {
        player.sendMessage(Component.text("Usage:", NamedTextColor.RED));
        player.sendMessage(Component.text("  /buy <item name> <quantity>", NamedTextColor.GRAY));
        player.sendMessage(Component.text("  /buy info <item name>", NamedTextColor.GRAY));
    }

    private void handleInfo(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /buy info <item name>", NamedTextColor.RED));
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
                    prettyName(material) + " is not currently for sale.", NamedTextColor.YELLOW));
            return;
        }

        int price = shopManager.getPrice(material);
        player.sendMessage(Component.text(prettyName(material) + " costs ", NamedTextColor.GREEN)
                .append(Component.text(price + " Crystal(s) ", NamedTextColor.LIGHT_PURPLE))
                .append(Component.text("each.", NamedTextColor.GREEN)));
    }

    private void handlePurchase(Player player, String[] args) {
        if (args.length < 2) {
            sendUsage(player);
            return;
        }

        // Last argument is the quantity; everything before it is the item name.
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
                    prettyName(material) + " is not currently for sale.", NamedTextColor.RED));
            return;
        }

        // Exclusive restriction: only the Market Minister (or an OP) may purchase.
        boolean isMarketMinister = rankManager.hasRole(player.getUniqueId(), MARKET_MINISTER_ROLE);
        if (!player.isOp() && !isMarketMinister) {
            player.sendMessage(Component.text(
                    "Only the Market Minister is authorized to buy items from this shop.", NamedTextColor.RED));
            return;
        }

        int price = shopManager.getPrice(material);
        int totalCost = price * quantity;

        if (!player.isOp()) {
            int have = countCrystals(player.getInventory());
            if (have < totalCost) {
                player.sendMessage(Component.text(
                        "You don't have enough Crystals. Needed: " + totalCost + ", You have: " + have,
                        NamedTextColor.RED));
                return;
            }
            removeCrystals(player.getInventory(), totalCost);
        }

        giveItems(player, material, quantity);

        if (player.isOp()) {
            player.sendMessage(Component.text("[OP Bypass] ", NamedTextColor.GOLD)
                    .append(Component.text("Purchased " + quantity + "x " + prettyName(material)
                            + " for free.", NamedTextColor.GREEN)));
        } else {
            player.sendMessage(Component.text("Purchased " + quantity + "x " + prettyName(material)
                    + " for " + totalCost + " Crystal(s).", NamedTextColor.GREEN));
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

    private int countCrystals(Inventory inventory) {
        int total = 0;
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() == CrystalItemUtil.CURRENCY_MATERIAL) {
                total += item.getAmount();
            }
        }
        return total;
    }

    private void removeCrystals(Inventory inventory, int amount) {
        int remaining = amount;
        ItemStack[] contents = inventory.getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() == CrystalItemUtil.CURRENCY_MATERIAL) {
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

    private void giveItems(Player player, Material material, int amount) {
        int maxStack = material.getMaxStackSize();
        Map<Integer, ItemStack> overflow = new HashMap<>();
        int remaining = amount;

        while (remaining > 0) {
            int stackSize = Math.min(remaining, maxStack);
            ItemStack stack = new ItemStack(material, stackSize);
            overflow.putAll(player.getInventory().addItem(stack));
            remaining -= stackSize;
        }

        for (ItemStack leftover : overflow.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
    }
          }
