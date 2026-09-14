package com.crystalville.crystalcore.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /inventory <player>        - opens a live view of the target's inventory (OP only).
 * /inventory clear <player>  - fully clears the target's inventory, armor, and offhand (OP only).
 *
 * Viewing opens the target's actual PlayerInventory object, so it doubles as
 * an edit tool (standard for admin "invsee"-style commands) - the target must
 * be online for either action, since offline inventories aren't live-editable.
 */
public class InventoryCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return true;
        }
        Player admin = (Player) sender;

        if (!admin.isOp()) {
            admin.sendMessage(Component.text("Only OPs can use /inventory.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendUsage(admin);
            return true;
        }

        if (args[0].equalsIgnoreCase("clear")) {
            handleClear(admin, args);
            return true;
        }

        handleView(admin, args);
        return true;
    }

    private void sendUsage(Player admin) {
        admin.sendMessage(Component.text("Usage:", NamedTextColor.RED));
        admin.sendMessage(Component.text("  /inventory <player>", NamedTextColor.GRAY));
        admin.sendMessage(Component.text("  /inventory clear <player>", NamedTextColor.GRAY));
    }

    private void handleView(Player admin, String[] args) {
        String targetName = args[0];
        Player target = Bukkit.getPlayerExact(targetName);

        if (target == null) {
            admin.sendMessage(Component.text(
                    "Player '" + targetName + "' is not online.", NamedTextColor.RED));
            return;
        }

        admin.openInventory(target.getInventory());
        admin.sendMessage(Component.text(
                "Viewing " + target.getName() + "'s inventory (live - changes affect them too).",
                NamedTextColor.AQUA));
    }

    private void handleClear(Player admin, String[] args) {
        if (args.length < 2) {
            admin.sendMessage(Component.text("Usage: /inventory clear <player>", NamedTextColor.RED));
            return;
        }

        String targetName = args[1];
        Player target = Bukkit.getPlayerExact(targetName);

        if (target == null) {
            admin.sendMessage(Component.text(
                    "Player '" + targetName + "' is not online.", NamedTextColor.RED));
            return;
        }

        target.getInventory().clear();
        target.getInventory().setArmorContents(new org.bukkit.inventory.ItemStack[]{
                new org.bukkit.inventory.ItemStack(Material.AIR),
                new org.bukkit.inventory.ItemStack(Material.AIR),
                new org.bukkit.inventory.ItemStack(Material.AIR),
                new org.bukkit.inventory.ItemStack(Material.AIR)
        });
        target.getInventory().setItemInOffHand(new org.bukkit.inventory.ItemStack(Material.AIR));

        admin.sendMessage(Component.text(
                "Cleared " + target.getName() + "'s inventory.", NamedTextColor.GREEN));
        target.sendMessage(Component.text(
                "Your inventory was cleared by " + admin.getName() + ".", NamedTextColor.YELLOW));
    }
  }
