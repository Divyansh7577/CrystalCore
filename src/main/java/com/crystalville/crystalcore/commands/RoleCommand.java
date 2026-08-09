package com.crystalville.crystalcore.commands;

import com.crystalville.crystalcore.managers.RankManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

/**
 * /role <player> <role name>
 * Assigns a pre-built role (currently: "Market Minister") to a player.
 * OP only.
 */
public class RoleCommand implements CommandExecutor {

    private final RankManager rankManager;

    public RoleCommand(RankManager rankManager) {
        this.rankManager = rankManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage(Component.text("Only OPs can use /role.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /role <player> <role name>", NamedTextColor.RED));
            sender.sendMessage(Component.text("Available roles: Market Minister", NamedTextColor.GRAY));
            return true;
        }

        String playerName = args[0];
        String roleInput = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        RankManager.RoleDefinition role = RankManager.findRoleByName(roleInput);
        if (role == null) {
            sender.sendMessage(Component.text("Unknown role: " + roleInput, NamedTextColor.RED));
            sender.sendMessage(Component.text("Available roles: Market Minister", NamedTextColor.GRAY));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        if (target == null || (target.getName() == null && !target.hasPlayedBefore())) {
            sender.sendMessage(Component.text(
                    "Player '" + playerName + "' has never joined this server.", NamedTextColor.RED));
            return true;
        }

        rankManager.assignRole(target, role);

        sender.sendMessage(Component.text("Assigned the ", NamedTextColor.GREEN)
                .append(Component.text(role.displayName, RankManager.parseColor(role.colorHex)))
                .append(Component.text(" role to " + playerName + ".", NamedTextColor.GREEN)));

        Player online = target.getPlayer();
        if (online != null) {
            Component prefix = rankManager.getFormattedPrefixComponent(online.getUniqueId());
            Component displayName = prefix.append(Component.text(online.getName()));
            online.playerListName(displayName);

            online.sendMessage(Component.text("You have been made the ", NamedTextColor.GREEN)
                    .append(Component.text(role.displayName, RankManager.parseColor(role.colorHex)))
                    .append(Component.text(" by " + sender.getName() + ".", NamedTextColor.GREEN)));
        }

        return true;
    }
          }
