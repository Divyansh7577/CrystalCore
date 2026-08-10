package com.crystalville.crystalcore.commands;

import com.crystalville.crystalcore.managers.HoleFillerManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /holefiller             - toggles Hole Filler Mode on/off
 * /holefiller off         - explicitly disables it
 * /holefiller <material>  - sets the fill block and enables the mode
 * OP only.
 */
public class HoleFillerCommand implements CommandExecutor {

    private final HoleFillerManager holeFillerManager;

    public HoleFillerCommand(HoleFillerManager holeFillerManager) {
        this.holeFillerManager = holeFillerManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return true;
        }
        Player player = (Player) sender;

        if (!player.isOp()) {
            player.sendMessage(Component.text("Only OPs can use /holefiller.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            boolean nowEnabled = holeFillerManager.toggle(player.getUniqueId());
            Material material = holeFillerManager.getMaterial(player.getUniqueId());
            if (nowEnabled) {
                player.sendMessage(Component.text(
                        "Hole Filler Mode enabled (filling with " + prettyName(material) + ").",
                        NamedTextColor.GREEN));
            } else {
                player.sendMessage(Component.text("Hole Filler Mode disabled.", NamedTextColor.YELLOW));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("off")) {
            holeFillerManager.setEnabled(player.getUniqueId(), false);
            player.sendMessage(Component.text("Hole Filler Mode disabled.", NamedTextColor.YELLOW));
            return true;
        }

        if (args[0].equalsIgnoreCase("on")) {
            holeFillerManager.setEnabled(player.getUniqueId(), true);
            Material material = holeFillerManager.getMaterial(player.getUniqueId());
            player.sendMessage(Component.text(
                    "Hole Filler Mode enabled (filling with " + prettyName(material) + ").", NamedTextColor.GREEN));
            return true;
        }

        String normalized = args[0].trim().toUpperCase().replace(' ', '_').replace('-', '_');
        Material material = Material.matchMaterial(normalized);
        if (material == null || !material.isBlock()) {
            player.sendMessage(Component.text("Unknown block: " + args[0], NamedTextColor.RED));
            return true;
        }

        holeFillerManager.setMaterial(player.getUniqueId(), material);
        holeFillerManager.setEnabled(player.getUniqueId(), true);
        player.sendMessage(Component.text(
                "Hole Filler Mode enabled (filling with " + prettyName(material) + ").", NamedTextColor.GREEN));
        return true;
    }

    private String prettyName(Material material) {
        String[] parts = material.name().toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }
              }
