package com.crystalville.crystalcore.commands;

import com.crystalville.crystalcore.managers.EnterpriseAccount;
import com.crystalville.crystalcore.managers.EnterpriseManager;
import com.crystalville.crystalcore.util.EnterpriseBookUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * /issue guide <EnterpriseID>
 * Re-issues the Enterprise Handbook for the given company to the command
 * sender. Restricted to that Enterprise's owner or an OP.
 */
public class IssueGuideCommand implements CommandExecutor {

    private final EnterpriseManager enterpriseManager;

    public IssueGuideCommand(EnterpriseManager enterpriseManager) {
        this.enterpriseManager = enterpriseManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return true;
        }
        Player player = (Player) sender;

        if (args.length < 2 || !args[0].equalsIgnoreCase("guide")) {
            player.sendMessage(Component.text("Usage: /issue guide <EnterpriseID>", NamedTextColor.RED));
            return true;
        }

        String enterpriseId = EnterpriseManager.normalize(args[1]);
        EnterpriseAccount account = enterpriseManager.get(enterpriseId);

        if (account == null) {
            player.sendMessage(Component.text(
                    "No Enterprise found with ID '" + enterpriseId + "'.", NamedTextColor.RED));
            return true;
        }

        boolean authorized = player.isOp() || account.isOwner(player.getUniqueId());
        if (!authorized) {
            player.sendMessage(Component.text(
                    "Only the Enterprise owner or an OP can reissue the handbook.", NamedTextColor.RED));
            return true;
        }

        ItemStack handbook = EnterpriseBookUtil.createHandbook(account);
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(handbook);
        for (ItemStack leftover : overflow.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }

        player.sendMessage(Component.text(
                "Issued the Enterprise Handbook for " + account.name + ".", NamedTextColor.GREEN));

        return true;
    }
    }
