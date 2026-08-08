package com.crystalville.crystalcore.commands;

import com.crystalville.crystalcore.CrystalCore;
import com.crystalville.crystalcore.managers.EconomyUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BuyCommand implements CommandExecutor {

    private final CrystalCore plugin;

    public BuyCommand(CrystalCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player buyer = (Player) sender;

        if (args.length != 2) {
            buyer.sendMessage(ChatColor.RED + "Usage: /buy <player> <amount>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            buyer.sendMessage(ChatColor.RED + "Player '" + args[0] + "' is not online.");
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            buyer.sendMessage(ChatColor.RED + "Amount must be a whole number.");
            return true;
        }

        EconomyUtil.transfer(buyer, target, plugin.getBuyCurrency(), amount, "buy");
        return true;
    }
}
