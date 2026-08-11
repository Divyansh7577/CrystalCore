package com.crystalville.crystalcore.commands;

import com.crystalville.crystalcore.CrystalCore;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LinkCommand implements CommandExecutor {

    private final CrystalCore plugin;

    public LinkCommand(CrystalCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can link accounts.");
            return true;
        }

        Player player = (Player) sender;
        
        // Bedrock check: player.getName() will correctly return ".Divyansh7577"
        // No special logic needed for the dot unless you have a custom regex filter.

        if (args.length != 1) {
            player.sendMessage(ChatColor.RED + "Usage: /link <code>");
            return true;
        }

        String inputCode = args[0];
        
        // Async handling is recommended for DB operations
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String discordId = plugin.getDbManager().verifyCodeAndGetDiscordId(inputCode);

            if (discordId != null) {
                // Link successful
                plugin.getDbManager().saveLink(player.getUniqueId().toString(), discordId);
                
                // Back to main thread to modify permissions/ranks
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    // Logic to grant rank via your existing manager
                    // Example: plugin.getRankManager().setRank(player, "Linked");
                    
                    player.sendMessage(ChatColor.GREEN + "Successfully linked to Discord ID: " + discordId);
                    plugin.getLogger().info("Linked player " + player.getName() + " to " + discordId);
                });
            } else {
                player.sendMessage(ChatColor.RED + "Invalid or expired code! Please run /link on Discord again.");
            }
        });

        return true;
    }
  }
