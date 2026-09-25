package com.crystalville.crystalcore.commands;

import com.crystalville.crystalcore.managers.BankManager;
import com.crystalville.crystalcore.managers.RankManager;
import com.crystalville.crystalcore.util.PlayerResolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /increase limit <player> <new limit>
 * Raises a specific player's personal Crystal Bank cap above the default
 * 10,000. OP or Finance Minister only.
 */
public class IncreaseLimitCommand implements CommandExecutor {

    private static final String FINANCE_MINISTER_ROLE = "FINANCE_MINISTER";

    private final BankManager bankManager;
    private final RankManager rankManager;

    public IncreaseLimitCommand(BankManager bankManager, RankManager rankManager) {
        this.bankManager = bankManager;
        this.rankManager = rankManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return true;
        }
        Player player = (Player) sender;

        boolean authorized = player.isOp() || rankManager.hasRole(player.getUniqueId(), FINANCE_MINISTER_ROLE);
        if (!authorized) {
            player.sendMessage(Component.text(
                    "Only OPs and the Finance Minister can use /increase.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 3 || !args[0].equalsIgnoreCase("limit")) {
            player.sendMessage(Component.text("Usage: /increase limit <player> <new limit>", NamedTextColor.RED));
            return true;
        }

        OfflinePlayer target = PlayerResolver.resolve(args[1]);
        if (target == null) {
            player.sendMessage(Component.text("Player '" + args[1] + "' was not found.", NamedTextColor.RED));
            return true;
        }

        long newLimit;
        try {
            newLimit = Long.parseLong(args[2]);
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("New limit must be a whole number.", NamedTextColor.RED));
            return true;
        }

        if (newLimit <= 0) {
            player.sendMessage(Component.text("New limit must be greater than zero.", NamedTextColor.RED));
            return true;
        }

        bankManager.setCap(target.getUniqueId(), newLimit);

        player.sendMessage(Component.text("Set " + target.getName() + "'s personal bank limit to "
                + String.format("%,d", newLimit) + " Crystal(s).", NamedTextColor.GREEN));

        Player onlineTarget = target.getPlayer();
        if (onlineTarget != null) {
            onlineTarget.sendMessage(Component.text("Your personal bank limit was raised to "
                    + String.format("%,d", newLimit) + " Crystal(s) by " + player.getName() + ".",
                    NamedTextColor.AQUA));
        }

        return true;
    }
              }
