package com.crystalville.crystalcore.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.crystalville.crystalcore.CrystalCore;
import com.crystalville.crystalcore.database.DatabaseManager;

import java.io.File;

public class LinkCommand implements CommandExecutor {

    private final CrystalCore plugin;
    private final DatabaseManager dbManager;

    public LinkCommand(CrystalCore plugin) {
        this.plugin = plugin;
        File dbFile = new File(plugin.getDataFolder(), "database.db");
        this.dbManager = new DatabaseManager(dbFile.getAbsolutePath());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }

        Player player = (Player) sender;
        String playerName = player.getName(); // Geyser ke dot wale usernames (.username) ke liye fully supported

        if (args.length != 1) {
            player.sendMessage(ChatColor.RED + "Usage: /link <code>");
            return true;
        }

        String code = args[0];

        // Database se code verify karein
        String discordId = dbManager.verifyCodeAndGetDiscordId(code);

        if (discordId != null) {
            dbManager.saveLink(playerName, discordId);
            player.sendMessage(ChatColor.GREEN + "Successfully linked your Minecraft account (" + playerName + ") with Discord!");
        } else {
            player.sendMessage(ChatColor.RED + "Invalid or expired linking code!");
        }

        return true;
    }
}
