package com.crystalville.crystalcore.commands;

import com.crystalville.crystalcore.managers.RedeemCodeManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /make <redeem code> <amount> <expiration in minutes>
 * /make <redeem code> <amount> infinite
 * /make <redeem code> <amount>                 - same as infinite, if no 3rd arg given
 *
 * Creates (or overwrites) a redeem code that gives any player who claims
 * it the given amount of Crystals, once each, via /redeem. OP only.
 */
public class MakeCommand implements CommandExecutor {

    private final RedeemCodeManager redeemCodeManager;

    public MakeCommand(RedeemCodeManager redeemCodeManager) {
        this.redeemCodeManager = redeemCodeManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return true;
        }
        Player player = (Player) sender;

        if (!player.isOp()) {
            player.sendMessage(Component.text("Only OPs can use /make.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            sendUsage(player);
            return true;
        }

        String code = args[0];
        if (code.isEmpty() || code.length() > 64) {
            player.sendMessage(Component.text(
                    "Redeem code must be 1-64 characters.", NamedTextColor.RED));
            return true;
        }

        long amount;
        try {
            amount = Long.parseLong(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Amount must be a whole number.", NamedTextColor.RED));
            return true;
        }

        if (amount <= 0) {
            player.sendMessage(Component.text("Amount must be greater than zero.", NamedTextColor.RED));
            return true;
        }

        Long expirationMinutes;
        if (args.length < 3 || args[2].equalsIgnoreCase("infinite")) {
            expirationMinutes = null;
        } else {
            try {
                long parsed = Long.parseLong(args[2]);
                if (parsed <= 0) {
                    player.sendMessage(Component.text(
                            "Expiration must be greater than zero minutes, or 'infinite'.", NamedTextColor.RED));
                    return true;
                }
                expirationMinutes = parsed;
            } catch (NumberFormatException e) {
                player.sendMessage(Component.text(
                        "Expiration must be a whole number of minutes, or 'infinite'.", NamedTextColor.RED));
                return true;
            }
        }

        boolean existedBefore = redeemCodeManager.exists(code);
        redeemCodeManager.create(code, amount, expirationMinutes, player.getName());

        String expiryText = expirationMinutes == null
                ? "never expires"
                : "expires in " + expirationMinutes + " minute(s)";

        player.sendMessage(Component.text((existedBefore ? "Overwrote" : "Created") + " redeem code ", NamedTextColor.GREEN)
                .append(Component.text(code, NamedTextColor.LIGHT_PURPLE))
                .append(Component.text(" worth " + amount + " Crystal(s), " + expiryText + ".", NamedTextColor.GREEN)));

        player.sendMessage(Component.text("Players can claim it with: /redeem " + code, NamedTextColor.GRAY));

        return true;
    }

    private void sendUsage(Player player) {
        player.sendMessage(Component.text("Usage:", NamedTextColor.RED));
        player.sendMessage(Component.text("  /make <code> <amount> <expiration minutes>", NamedTextColor.GRAY));
        player.sendMessage(Component.text("  /make <code> <amount> infinite", NamedTextColor.GRAY));
        player.sendMessage(Component.text("  /make <code> <amount>   (defaults to infinite)", NamedTextColor.GRAY));
        player.sendMessage(Component.text("Example: /make sf12*-=6 1000 10", NamedTextColor.DARK_GRAY));
    }
    }
