package com.crystalville.crystalcore.commands;

import com.crystalville.crystalcore.CrystalCore;
import com.crystalville.crystalcore.managers.RankManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RankCommand implements CommandExecutor {

    private final CrystalCore plugin;
    private final RankManager rankManager;

    public RankCommand(CrystalCore plugin, RankManager rankManager) {
        this.plugin = plugin;
        this.rankManager = rankManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "Only OPs can use /rank.");
            return true;
        }

        if (args.length != 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /rank <player> <rankname> <colour>");
            sender.sendMessage(ChatColor.GRAY + "Colour can be a name (GOLD, AQUA, RED...) or a hex code (#FF00AA).");
            return true;
        }

        String playerName = args[0];
        String rankName = args[1];
        String colorInput = args[2];

        ChatColor resolved;
        try {
            resolved = RankManager.parseColor(colorInput);
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "Invalid colour. Use a ChatColor name or hex code.");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        if (target == null || (target.getName() == null && !target.hasPlayedBefore())) {
            sender.sendMessage(ChatColor.RED + "Player '" + playerName + "' has never joined this server.");
            return true;
        }

        rankManager.setRank(target, rankName, colorInput);

        sender.sendMessage(ChatColor.GREEN + "Set " + playerName + "'s rank to "
                + resolved + rankName + ChatColor.GREEN + ".");

        Player online = target.getPlayer();
        if (online != null) {
            String prefix = rankManager.getFormattedPrefix(online.getUniqueId());
            Component displayName = LegacyComponentSerializer.legacySection().deserialize(prefix)
                    .append(Component.text(online.getName()));
            online.playerListName(displayName);
            online.sendMessage(ChatColor.GREEN + "Your rank has been updated to "
                    + resolved + rankName + ChatColor.GREEN + " by " + sender.getName() + ".");
        }

        return true;
    }
              }
