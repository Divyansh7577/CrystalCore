package com.crystalville.crystalcore.commands;

import com.crystalville.crystalcore.CrystalCore;
import com.crystalville.crystalcore.managers.RankManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
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
            sender.sendMessage(Component.text("Only OPs can use /rank.", NamedTextColor.RED));
            return true;
        }

        if (args.length != 3) {
            sender.sendMessage(Component.text("Usage: /rank <player> <rankname> <colour>", NamedTextColor.RED));
            sender.sendMessage(Component.text(
                    "Colour can be a name (GOLD, AQUA, RED...) or a hex code (#FF00AA).", NamedTextColor.GRAY));
            return true;
        }

        String playerName = args[0];
        String rankName = args[1];
        String colorInput = args[2];

        TextColor resolved = RankManager.parseColor(colorInput);

        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        if (target == null || (target.getName() == null && !target.hasPlayedBefore())) {
            sender.sendMessage(Component.text(
                    "Player '" + playerName + "' has never joined this server.", NamedTextColor.RED));
            return true;
        }

        rankManager.setRank(target, rankName, colorInput);

        sender.sendMessage(Component.text("Set " + playerName + "'s rank to ", NamedTextColor.GREEN)
                .append(Component.text(rankName, resolved))
                .append(Component.text(".", NamedTextColor.GREEN)));

        // If the target is online, refresh their tab-list display name immediately.
        Player online = target.getPlayer();
        if (online != null) {
            Component prefix = rankManager.getFormattedPrefixComponent(online.getUniqueId());
            Component displayName = prefix.append(Component.text(online.getName()));
            online.playerListName(displayName);

            online.sendMessage(Component.text("Your rank has been updated to ", NamedTextColor.GREEN)
                    .append(Component.text(rankName, resolved))
                    .append(Component.text(" by " + sender.getName() + ".", NamedTextColor.GREEN)));
        }

        return true;
    }
}
