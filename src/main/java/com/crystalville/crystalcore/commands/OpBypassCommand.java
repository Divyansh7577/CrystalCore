package com.crystalville.crystalcore.commands;

import com.crystalville.crystalcore.managers.OpBypassManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /opbypass          - toggles your own OP Bypass on/off
 * /opbypass on       - explicitly enables it
 * /opbypass off      - explicitly disables it
 * /opbypass status   - shows current state
 *
 * OP only. Named "opbypass" (not "op bypass") deliberately - a plugin
 * command literally named "op" would completely override Minecraft's
 * vanilla /op command used to grant server operator status, which would
 * be dangerous to lose on a live server.
 *
 * "Bypass" means unlimited Crystals / free items across /pay, /buy,
 * /sell overflow, /bank cap, /enterprise, and /payenterprise. Turning it
 * off makes you play the economy exactly like a normal player.
 */
public class OpBypassCommand implements CommandExecutor {

    private final OpBypassManager opBypassManager;

    public OpBypassCommand(OpBypassManager opBypassManager) {
        this.opBypassManager = opBypassManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return true;
        }
        Player player = (Player) sender;

        if (!player.isOp()) {
            player.sendMessage(Component.text("Only OPs can use /opbypass.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            boolean nowEnabled = opBypassManager.toggle(player.getUniqueId());
            announce(player, nowEnabled);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "on":
                opBypassManager.setEnabled(player.getUniqueId(), true);
                announce(player, true);
                break;
            case "off":
                opBypassManager.setEnabled(player.getUniqueId(), false);
                announce(player, false);
                break;
            case "status":
                boolean enabled = opBypassManager.isEnabled(player.getUniqueId());
                player.sendMessage(Component.text("OP Bypass is currently ", NamedTextColor.GRAY)
                        .append(Component.text(enabled ? "ON" : "OFF",
                                enabled ? NamedTextColor.GREEN : NamedTextColor.RED)));
                break;
            default:
                sendUsage(player);
        }

        return true;
    }

    private void sendUsage(Player player) {
        player.sendMessage(Component.text("Usage:", NamedTextColor.RED));
        player.sendMessage(Component.text("  /opbypass           - toggle", NamedTextColor.GRAY));
        player.sendMessage(Component.text("  /opbypass on|off|status", NamedTextColor.GRAY));
    }

    private void announce(Player player, boolean enabled) {
        if (enabled) {
            player.sendMessage(Component.text(
                    "OP Bypass enabled - Crystals/items are now unlimited for you again.", NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text(
                    "OP Bypass disabled - you'll now pay/receive Crystals like a normal player.",
                    NamedTextColor.YELLOW));
        }
    }
  }
