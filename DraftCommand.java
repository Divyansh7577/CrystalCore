package com.crystalville.crystalcore.commands;

import com.crystalville.crystalcore.managers.MailboxManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /draft info
 * Shows the player their total pending (unclaimed) Crystal balance -
 * built up from /pay payments that couldn't fit in their inventory, or
 * that arrived while they were offline. Does not withdraw anything;
 * use /claim <amount> for that.
 */
public class DraftCommand implements CommandExecutor {

    private final MailboxManager mailboxManager;

    public DraftCommand(MailboxManager mailboxManager) {
        this.mailboxManager = mailboxManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 0 || !args[0].equalsIgnoreCase("info")) {
            player.sendMessage(Component.text("Usage: /draft info", NamedTextColor.RED));
            return true;
        }

        int totalPending = mailboxManager.getTotalPending(player.getUniqueId());

        if (totalPending <= 0) {
            player.sendMessage(Component.text("You have no pending Crystals.", NamedTextColor.GRAY));
            return true;
        }

        player.sendMessage(Component.text("You have ", NamedTextColor.GREEN)
                .append(Component.text(totalPending + " Crystal(s) ", NamedTextColor.LIGHT_PURPLE))
                .append(Component.text("waiting in your draft.", NamedTextColor.GREEN)));
        player.sendMessage(Component.text(
                "Use /claim <amount> to withdraw them into your inventory.", NamedTextColor.GRAY));

        return true;
    }
                             }
