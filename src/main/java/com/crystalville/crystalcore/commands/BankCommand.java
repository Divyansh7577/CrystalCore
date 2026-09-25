package com.crystalville.crystalcore.commands;

import com.crystalville.crystalcore.gui.BankGuiManager;
import com.crystalville.crystalcore.managers.BankManager;
import com.crystalville.crystalcore.util.CrystalItemUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * /bank balance             - shows your bank balance and cap
 * /bank add <amount>        - deposits Crystals from your inventory into your bank
 * /bank withdraw <amount>   - withdraws Crystals from your bank into your inventory
 * /bank gui                 - opens the visual Crystal Bank interface
 *
 * Normal players' bank balance is capped at 10,000 Crystals by default,
 * unless individually raised via /increase limit. OPs and Finance
 * Minister role holders have no cap.
 */
public class BankCommand implements CommandExecutor {

    private final BankManager bankManager;
    private final BankGuiManager bankGuiManager;

    public BankCommand(BankManager bankManager, BankGuiManager bankGuiManager) {
        this.bankManager = bankManager;
        this.bankGuiManager = bankGuiManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "balance":
                handleBalance(player);
                break;
            case "add":
                handleAdd(player, args);
                break;
            case "withdraw":
                handleWithdraw(player, args);
                break;
            case "gui":
                player.openInventory(bankGuiManager.open(player));
                break;
            default:
                sendUsage(player);
        }

        return true;
    }

    private void sendUsage(Player player) {
        player.sendMessage(Component.text("Usage:", NamedTextColor.RED));
        player.sendMessage(Component.text("  /bank balance", NamedTextColor.GRAY));
        player.sendMessage(Component.text("  /bank add <amount>", NamedTextColor.GRAY));
        player.sendMessage(Component.text("  /bank withdraw <amount>", NamedTextColor.GRAY));
        player.sendMessage(Component.text("  /bank gui", NamedTextColor.GRAY));
    }

    private void handleBalance(Player player) {
        long balance = bankManager.getBalance(player.getUniqueId());
        boolean unlimited = bankGuiManager.isUnlimited(player);

        player.sendMessage(Component.text("Bank balance: ", NamedTextColor.GREEN)
                .append(Component.text(String.format("%,d", balance) + " Crystal(s)", NamedTextColor.LIGHT_PURPLE)));

        if (unlimited) {
            player.sendMessage(Component.text("Limit: Unlimited (Finance Minister/OP).", NamedTextColor.GOLD));
        } else {
            player.sendMessage(Component.text(
                    "Limit: " + String.format("%,d", bankManager.getCap(player.getUniqueId())) + " Crystal(s).",
                    NamedTextColor.GRAY));
        }
    }

    private void handleAdd(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /bank add <amount>", NamedTextColor.RED));
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Amount must be a whole number.", NamedTextColor.RED));
            return;
        }

        if (amount <= 0) {
            player.sendMessage(Component.text("Amount must be greater than zero.", NamedTextColor.RED));
            return;
        }

        boolean unlimited = bankGuiManager.isUnlimited(player);
        long currentBalance = bankManager.getBalance(player.getUniqueId());
        long cap = bankManager.getCap(player.getUniqueId());

        if (!unlimited && currentBalance >= cap) {
            player.sendMessage(Component.text(
                    "Your bank is already at its " + String.format("%,d", cap)
                            + " Crystal limit.", NamedTextColor.RED));
            return;
        }

        int have = countCrystals(player.getInventory());
        if (have < amount) {
            player.sendMessage(Component.text(
                    "You don't have enough Crystals. Needed: " + amount + ", You have: " + have,
                    NamedTextColor.RED));
            return;
        }

        long deposited = bankManager.deposit(player.getUniqueId(), amount, unlimited);

        if (deposited <= 0) {
            player.sendMessage(Component.text("Nothing was added.", NamedTextColor.YELLOW));
            return;
        }

        removeCrystals(player.getInventory(), (int) deposited);

        player.sendMessage(Component.text("Added " + deposited + " Crystal(s) to your bank.",
                NamedTextColor.GREEN));

        if (!unlimited && deposited < amount) {
            player.sendMessage(Component.text(
                    (amount - deposited) + " Crystal(s) couldn't be added - bank limit reached.",
                    NamedTextColor.YELLOW));
        }
    }

    private void handleWithdraw(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /bank withdraw <amount>", NamedTextColor.RED));
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Amount must be a whole number.", NamedTextColor.RED));
            return;
        }

        if (amount <= 0) {
            player.sendMessage(Component.text("Amount must be greater than zero.", NamedTextColor.RED));
            return;
        }

        long balance = bankManager.getBalance(player.getUniqueId());
        if (balance <= 0) {
            player.sendMessage(Component.text("You have nothing in your bank.", NamedTextColor.YELLOW));
            return;
        }

        long withdrawn = bankManager.withdraw(player.getUniqueId(), amount);

        if (withdrawn <= 0) {
            player.sendMessage(Component.text("Nothing was withdrawn.", NamedTextColor.YELLOW));
            return;
        }

        CrystalItemUtil.giveCrystals(player, withdrawn);

        player.sendMessage(Component.text("Withdrew " + withdrawn + " Crystal(s) from your bank.",
                NamedTextColor.GREEN));

        if (withdrawn < amount) {
            player.sendMessage(Component.text(
                    "You only had " + withdrawn + " Crystal(s) available to withdraw.", NamedTextColor.GRAY));
        }
    }

    private int countCrystals(Inventory inventory) {
        int total = 0;
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() == CrystalItemUtil.CURRENCY_MATERIAL) {
                total += item.getAmount();
            }
        }
        return total;
    }

    private void removeCrystals(Inventory inventory, int amount) {
        int remaining = amount;
        ItemStack[] contents = inventory.getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() == CrystalItemUtil.CURRENCY_MATERIAL) {
                int stackAmount = item.getAmount();
                if (stackAmount <= remaining) {
                    remaining -= stackAmount;
                    inventory.setItem(i, null);
                } else {
                    item.setAmount(stackAmount - remaining);
                    remaining = 0;
                }
            }
        }
    }
                           }
