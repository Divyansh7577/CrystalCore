package com.crystalville.crystalcore.commands;

import com.crystalville.crystalcore.managers.RankManager;
import com.crystalville.crystalcore.managers.ShopManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

/**
 * /shop add <item name> <price>
 * Adds a new item to the shop, or updates the price of an existing one.
 * Persists immediately to config.yml. Restricted to OPs and players holding
 * the Market Minister role.
 */
public class ShopCommand implements CommandExecutor {

    private static final String MARKET_MINISTER_ROLE = "MARKET_MINISTER";

    private final ShopManager shopManager;
    private final RankManager rankManager;

    public ShopCommand(ShopManager shopManager, RankManager rankManager) {
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

        boolean isMarketMinister = rankManager.hasRole(player.getUniqueId(), MARKET_MINISTER_ROLE);
        if (!player.isOp() && !isMarketMinister) {
            player.sendMessage(Component.text(
                    "Only OPs and the Market Minister can manage the shop.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0 || !args[0].equalsIgnoreCase("add")) {
            player.sendMessage(Component.text("Usage: /shop add <item name> <price>", NamedTextColor.RED));
            return true;
        }

        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: /shop add <item name> <price>", NamedTextColor.RED));
            return true;
        }

        String priceStr = args[args.length - 1];
        int price;
        try {
            price = Integer.parseInt(priceStr);
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Price must be a whole number.", NamedTextColor.RED));
            return true;
        }

        if (price <= 0) {
            player.sendMessage(Component.text("Price must be greater than zero.", NamedTextColor.RED));
            return true;
        }

        String itemQuery = String.join(" ", Arrays.copyOfRange(args, 1, args.length - 1));
        String normalized = itemQuery.trim().toUpperCase().replace(' ', '_').replace('-', '_');
        Material material = Material.matchMaterial(normalized);

        if (material == null) {
            player.sendMessage(Component.text("Unknown item: " + itemQuery, NamedTextColor.RED));
            return true;
        }

        boolean wasAlreadyListed = shopManager.isForSale(material);
        shopManager.setPrice(material, price);

        String prettyName = prettyName(material);
        if (wasAlreadyListed) {
            player.sendMessage(Component.text(
                    "Updated " + prettyName + "'s price to " + price + " Crystal(s).", NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text(
                    "Added " + prettyName + " to the shop for " + price + " Crystal(s).", NamedTextColor.GREEN));
        }

        return true;
    }

    private String prettyName(Material material) {
        String[] parts = material.name().toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }
  }
