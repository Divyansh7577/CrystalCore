package com.crystalville.crystalcore.commands;

import com.crystalville.crystalcore.managers.BankManager;
import com.crystalville.crystalcore.managers.EnterpriseAccount;
import com.crystalville.crystalcore.managers.EnterpriseManager;
import com.crystalville.crystalcore.managers.EnterprisePermission;
import com.crystalville.crystalcore.managers.EnterpriseTransaction;
import com.crystalville.crystalcore.managers.RankManager;
import com.crystalville.crystalcore.util.CrystalItemUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * /enterprise create <ID> <Company Name...>
 * /enterprise info <ID>
 * /enterprise balance <ID>
 * /enterprise deposit <ID> <amount>
 * /enterprise withdraw <ID> <amount>
 * /enterprise add <ID> <player> <perms|ALL>
 * /enterprise remove <ID> <player>
 * /enterprise setperms <ID> <player> <perms|ALL>
 * /enterprise members <ID>
 * /enterprise history <ID>
 * /enterprise list
 *
 * Any player can create and own an Enterprise Bank. The owner always has
 * every permission implicitly; other members act strictly within whatever
 * permissions they've been granted. Enterprise balances are completely
 * separate from personal /bank balances.
 */
public class EnterpriseCommand implements CommandExecutor {

    private static final String FINANCE_MINISTER_ROLE = "FINANCE_MINISTER";
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    private final EnterpriseManager enterpriseManager;
    private final BankManager bankManager;
    private final RankManager rankManager;

    public EnterpriseCommand(EnterpriseManager enterpriseManager, BankManager bankManager, RankManager rankManager) {
        this.enterpriseManager = enterpriseManager;
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

        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create":
                handleCreate(player, args);
                break;
            case "info":
                handleInfo(player, args);
                break;
            case "balance":
                handleBalance(player, args);
                break;
            case "deposit":
                handleDeposit(player, args);
                break;
            case "withdraw":
                handleWithdraw(player, args);
                break;
            case "add":
                handleAddMember(player, args);
                break;
            case "remove":
                handleRemoveMember(player, args);
                break;
            case "setperms":
                handleSetPermissions(player, args);
                break;
            case "members":
                handleListMembers(player, args);
                break;
            case "history":
                handleHistory(player, args);
                break;
            case "list":
                handleList(player);
                break;
            default:
                sendUsage(player);
        }

        return true;
    }

    private void sendUsage(Player player) {
        player.sendMessage(Component.text("=== Enterprise Bank Commands ===", NamedTextColor.AQUA));
        player.sendMessage(Component.text("/enterprise create <ID> <Company Name...>", NamedTextColor.GRAY));
        player.sendMessage(Component.text("/enterprise info <ID>", NamedTextColor.GRAY));
        player.sendMessage(Component.text("/enterprise balance <ID>", NamedTextColor.GRAY));
        player.sendMessage(Component.text("/enterprise deposit <ID> <amount>", NamedTextColor.GRAY));
        player.sendMessage(Component.text("/enterprise withdraw <ID> <amount>", NamedTextColor.GRAY));
        player.sendMessage(Component.text("/enterprise add <ID> <player> <perms|ALL>", NamedTextColor.GRAY));
        player.sendMessage(Component.text("/enterprise remove <ID> <player>", NamedTextColor.GRAY));
        player.sendMessage(Component.text("/enterprise setperms <ID> <player> <perms|ALL>", NamedTextColor.GRAY));
        player.sendMessage(Component.text("/enterprise members <ID>", NamedTextColor.GRAY));
        player.sendMessage(Component.text("/enterprise history <ID>", NamedTextColor.GRAY));
        player.sendMessage(Component.text("/enterprise list", NamedTextColor.GRAY));
        player.sendMessage(Component.text(
                "Permissions: VIEW_BALANCE, DEPOSIT, WITHDRAW, VIEW_HISTORY, MANAGE_MEMBERS", NamedTextColor.DARK_GRAY));
    }

    // ---------------------------------------------------------------- create

    private void handleCreate(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: /enterprise create <ID> <Company Name...>", NamedTextColor.RED));
            return;
        }

        String enterpriseId = EnterpriseManager.normalize(args[1]);

        if (!EnterpriseManager.isValidId(enterpriseId)) {
            player.sendMessage(Component.text(
                    "Enterprise ID must be 3-20 characters, letters/numbers/underscores only.", NamedTextColor.RED));
            return;
        }

        if (enterpriseManager.exists(enterpriseId)) {
            player.sendMessage(Component.text(
                    "An Enterprise with ID '" + enterpriseId + "' already exists.", NamedTextColor.RED));
            return;
        }

        String name = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

        EnterpriseAccount account = enterpriseManager.create(enterpriseId, name, player.getUniqueId());

        player.sendMessage(Component.text("Created Enterprise Bank ", NamedTextColor.GREEN)
                .append(Component.text(account.name + " ", NamedTextColor.GOLD))
                .append(Component.text("(ID: " + account.enterpriseId + ").", NamedTextColor.GREEN)));
        player.sendMessage(Component.text(
                "Players can now pay your company with: /payenterprise " + account.enterpriseId + " <amount>",
                NamedTextColor.GRAY));
    }

    // ------------------------------------------------------------------ info

    private void handleInfo(Player player, String[] args) {
        EnterpriseAccount account = requireAccount(player, args, 1);
        if (account == null) {
            return;
        }

        player.sendMessage(Component.text("=== " + account.name + " ===", NamedTextColor.AQUA));
        player.sendMessage(Component.text("ID: ", NamedTextColor.GRAY)
                .append(Component.text(account.enterpriseId, NamedTextColor.WHITE)));

        OfflinePlayer owner = Bukkit.getOfflinePlayer(account.ownerUuid);
        player.sendMessage(Component.text("Owner: ", NamedTextColor.GRAY)
                .append(Component.text(owner.getName() != null ? owner.getName() : "Unknown", NamedTextColor.WHITE)));

        player.sendMessage(Component.text("Members: ", NamedTextColor.GRAY)
                .append(Component.text(account.members.size(), NamedTextColor.WHITE)));

        if (account.hasPermission(player.getUniqueId(), EnterprisePermission.VIEW_BALANCE)) {
            player.sendMessage(Component.text("Balance: ", NamedTextColor.GRAY)
                    .append(Component.text(String.format("%,d", account.balance) + " / "
                            + String.format("%,d", EnterpriseAccount.MAX_BALANCE) + " Crystals",
                            NamedTextColor.LIGHT_PURPLE)));
        }
    }

    // --------------------------------------------------------------- balance

    private void handleBalance(Player player, String[] args) {
        EnterpriseAccount account = requireAccount(player, args, 1);
        if (account == null) {
            return;
        }

        if (!account.hasPermission(player.getUniqueId(), EnterprisePermission.VIEW_BALANCE)) {
            player.sendMessage(Component.text(
                    "You don't have permission to view this Enterprise's balance.", NamedTextColor.RED));
            return;
        }

        player.sendMessage(Component.text(account.name + " balance: ", NamedTextColor.GREEN)
                .append(Component.text(String.format("%,d", account.balance) + " / "
                        + String.format("%,d", EnterpriseAccount.MAX_BALANCE) + " Crystals",
                        NamedTextColor.LIGHT_PURPLE)));
    }

    // --------------------------------------------------------------- deposit

    private void handleDeposit(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: /enterprise deposit <ID> <amount>", NamedTextColor.RED));
            return;
        }

        EnterpriseAccount account = requireAccount(player, args, 1);
        if (account == null) {
            return;
        }

        if (!account.hasPermission(player.getUniqueId(), EnterprisePermission.DEPOSIT)) {
            player.sendMessage(Component.text(
                    "You don't have permission to deposit into this Enterprise.", NamedTextColor.RED));
            return;
        }

        int amount = parsePositiveInt(player, args[2]);
        if (amount <= 0) {
            return;
        }

        if (!player.isOp()) {
            int have = countCrystals(player.getInventory());
            if (have < amount) {
                player.sendMessage(Component.text(
                        "You don't have enough Crystals. Needed: " + amount + ", You have: " + have,
                        NamedTextColor.RED));
                return;
            }
        }

        long added = account.addBalance(amount);
        if (added <= 0) {
            player.sendMessage(Component.text(
                    account.name + " is already at its " + String.format("%,d", EnterpriseAccount.MAX_BALANCE)
                            + " Crystal limit.", NamedTextColor.RED));
            return;
        }

        if (!player.isOp()) {
            removeCrystals(player.getInventory(), (int) added);
        }

        account.addTransaction(player.getName(), "DEPOSIT", added);
        enterpriseManager.save();

        player.sendMessage(Component.text("Deposited " + added + " Crystal(s) into " + account.name + ".",
                NamedTextColor.GREEN));

        if (added < amount) {
            player.sendMessage(Component.text(
                    (amount - added) + " Crystal(s) couldn't be deposited - Enterprise limit reached.",
                    NamedTextColor.YELLOW));
        }
    }

    // -------------------------------------------------------------- withdraw

    private void handleWithdraw(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: /enterprise withdraw <ID> <amount>", NamedTextColor.RED));
            return;
        }

        EnterpriseAccount account = requireAccount(player, args, 1);
        if (account == null) {
            return;
        }

        if (!account.hasPermission(player.getUniqueId(), EnterprisePermission.WITHDRAW)) {
            player.sendMessage(Component.text(
                    "You don't have permission to withdraw from this Enterprise.", NamedTextColor.RED));
            return;
        }

        int amount = parsePositiveInt(player, args[2]);
        if (amount <= 0) {
            return;
        }

        if (account.balance <= 0) {
            player.sendMessage(Component.text(account.name + " has no funds to withdraw.", NamedTextColor.YELLOW));
            return;
        }

        long withdrawn = account.removeBalance(amount);
        if (withdrawn <= 0) {
            player.sendMessage(Component.text("Nothing was withdrawn.", NamedTextColor.YELLOW));
            return;
        }

        account.addTransaction(player.getName(), "WITHDRAWAL", withdrawn);
        enterpriseManager.save();

        payoutCrystals(player, (int) withdrawn);

        player.sendMessage(Component.text("Withdrew " + withdrawn + " Crystal(s) from " + account.name + ".",
                NamedTextColor.GREEN));

        if (withdrawn < amount) {
            player.sendMessage(Component.text(
                    account.name + " only had " + withdrawn + " Crystal(s) available.", NamedTextColor.GRAY));
        }
    }

    // ---------------------------------------------------------- add / remove

    private void handleAddMember(Player player, String[] args) {
        if (args.length < 4) {
            player.sendMessage(Component.text(
                    "Usage: /enterprise add <ID> <player> <perms|ALL>", NamedTextColor.RED));
            return;
        }

        EnterpriseAccount account = requireAccount(player, args, 1);
        if (account == null) {
            return;
        }

        if (!account.hasPermission(player.getUniqueId(), EnterprisePermission.MANAGE_MEMBERS)) {
            player.sendMessage(Component.text(
                    "You don't have permission to manage this Enterprise's members.", NamedTextColor.RED));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
        if (target.getName() == null && !target.hasPlayedBefore()) {
            player.sendMessage(Component.text(
                    "Player '" + args[2] + "' has never joined this server.", NamedTextColor.RED));
            return;
        }

        Set<EnterprisePermission> perms = parsePermissions(args[3]);
        if (perms.isEmpty()) {
            player.sendMessage(Component.text("No valid permissions given.", NamedTextColor.RED));
            return;
        }

        account.members.put(target.getUniqueId(), perms);
        enterpriseManager.save();

        player.sendMessage(Component.text(
                "Added " + target.getName() + " to " + account.name + " with permissions: "
                        + permsToString(perms), NamedTextColor.GREEN));

        Player onlineTarget = target.getPlayer();
        if (onlineTarget != null) {
            onlineTarget.sendMessage(Component.text(
                    "You were added to " + account.name + " (" + account.enterpriseId + ") by "
                            + player.getName() + ".", NamedTextColor.AQUA));
        }
    }

    private void handleRemoveMember(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: /enterprise remove <ID> <player>", NamedTextColor.RED));
            return;
        }

        EnterpriseAccount account = requireAccount(player, args, 1);
        if (account == null) {
            return;
        }

        if (!account.hasPermission(player.getUniqueId(), EnterprisePermission.MANAGE_MEMBERS)) {
            player.sendMessage(Component.text(
                    "You don't have permission to manage this Enterprise's members.", NamedTextColor.RED));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);

        if (target.getUniqueId().equals(account.ownerUuid)) {
            player.sendMessage(Component.text("You cannot remove the Enterprise owner.", NamedTextColor.RED));
            return;
        }

        if (account.members.remove(target.getUniqueId()) == null) {
            player.sendMessage(Component.text(
                    target.getName() + " is not a member of " + account.name + ".", NamedTextColor.YELLOW));
            return;
        }

        enterpriseManager.save();
        player.sendMessage(Component.text(
                "Removed " + target.getName() + " from " + account.name + ".", NamedTextColor.GREEN));
    }

    private void handleSetPermissions(Player player, String[] args) {
        if (args.length < 4) {
            player.sendMessage(Component.text(
                    "Usage: /enterprise setperms <ID> <player> <perms|ALL>", NamedTextColor.RED));
            return;
        }

        EnterpriseAccount account = requireAccount(player, args, 1);
        if (account == null) {
            return;
        }

        if (!account.hasPermission(player.getUniqueId(), EnterprisePermission.MANAGE_MEMBERS)) {
            player.sendMessage(Component.text(
                    "You don't have permission to manage this Enterprise's members.", NamedTextColor.RED));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
        if (!account.members.containsKey(target.getUniqueId())) {
            player.sendMessage(Component.text(
                    target.getName() + " is not a member of " + account.name + ".", NamedTextColor.RED));
            return;
        }

        Set<EnterprisePermission> perms = parsePermissions(args[3]);
        if (perms.isEmpty()) {
            player.sendMessage(Component.text("No valid permissions given.", NamedTextColor.RED));
            return;
        }

        account.members.put(target.getUniqueId(), perms);
        enterpriseManager.save();

        player.sendMessage(Component.text(
                "Updated " + target.getName() + "'s permissions to: " + permsToString(perms), NamedTextColor.GREEN));
    }

    // ------------------------------------------------------------- members

    private void handleListMembers(Player player, String[] args) {
        EnterpriseAccount account = requireAccount(player, args, 1);
        if (account == null) {
            return;
        }

        player.sendMessage(Component.text("=== " + account.name + " Members ===", NamedTextColor.AQUA));

        OfflinePlayer owner = Bukkit.getOfflinePlayer(account.ownerUuid);
        player.sendMessage(Component.text(
                (owner.getName() != null ? owner.getName() : "Unknown") + " - OWNER (all permissions)",
                NamedTextColor.GOLD));

        if (account.members.isEmpty()) {
            player.sendMessage(Component.text("No other members.", NamedTextColor.GRAY));
            return;
        }

        for (Map.Entry<UUID, Set<EnterprisePermission>> entry : account.members.entrySet()) {
            OfflinePlayer member = Bukkit.getOfflinePlayer(entry.getKey());
            String memberName = member.getName() != null ? member.getName() : entry.getKey().toString();
            player.sendMessage(Component.text(
                    memberName + " - " + permsToString(entry.getValue()), NamedTextColor.WHITE));
        }
    }

    // ------------------------------------------------------------- history

    private void handleHistory(Player player, String[] args) {
        EnterpriseAccount account = requireAccount(player, args, 1);
        if (account == null) {
            return;
        }

        if (!account.hasPermission(player.getUniqueId(), EnterprisePermission.VIEW_HISTORY)) {
            player.sendMessage(Component.text(
                    "You don't have permission to view this Enterprise's history.", NamedTextColor.RED));
            return;
        }

        player.sendMessage(Component.text("=== " + account.name + " Transaction History ===", NamedTextColor.AQUA));

        if (account.history.isEmpty()) {
            player.sendMessage(Component.text("No transactions yet.", NamedTextColor.GRAY));
            return;
        }

        List<EnterpriseTransaction> history = account.history;
        int start = Math.max(0, history.size() - 15);
        for (int i = history.size() - 1; i >= start; i--) {
            EnterpriseTransaction tx = history.get(i);
            String sign = tx.type.equals("WITHDRAWAL") ? "-" : "+";
            String time = TIME_FORMAT.format(new Date(tx.timestampMillis));

            player.sendMessage(Component.text(
                    "[" + time + "] " + tx.playerName + " -> " + account.name + " -> "
                            + sign + String.format("%,d", tx.amount) + " Crystals -> "
                            + prettyType(tx.type), NamedTextColor.GRAY));
        }
    }

    private String prettyType(String type) {
        switch (type) {
            case "PAYMENT":
                return "Payment";
            case "DEPOSIT":
                return "Deposit";
            case "WITHDRAWAL":
                return "Withdrawal";
            default:
                return type;
        }
    }

    // ---------------------------------------------------------------- list

    private void handleList(Player player) {
        List<EnterpriseAccount> accounts = enterpriseManager.getForPlayer(player.getUniqueId());

        if (accounts.isEmpty()) {
            player.sendMessage(Component.text("You are not part of any Enterprise.", NamedTextColor.YELLOW));
            return;
        }

        player.sendMessage(Component.text("=== Your Enterprises ===", NamedTextColor.AQUA));
        for (EnterpriseAccount account : accounts) {
            String role = account.isOwner(player.getUniqueId()) ? "Owner" : "Member";
            player.sendMessage(Component.text(
                    account.name + " (" + account.enterpriseId + ") - " + role, NamedTextColor.WHITE));
        }
    }

    // -------------------------------------------------------------- helpers

    private EnterpriseAccount requireAccount(Player player, String[] args, int idIndex) {
        if (args.length <= idIndex) {
            player.sendMessage(Component.text("You must specify an Enterprise ID.", NamedTextColor.RED));
            return null;
        }

        String enterpriseId = EnterpriseManager.normalize(args[idIndex]);
        EnterpriseAccount account = enterpriseManager.get(enterpriseId);

        if (account == null) {
            player.sendMessage(Component.text(
                    "No Enterprise found with ID '" + enterpriseId + "'.", NamedTextColor.RED));
            return null;
        }

        return account;
    }

    private Set<EnterprisePermission> parsePermissions(String input) {
        Set<EnterprisePermission> result = new LinkedHashSet<>();

        if (input.equalsIgnoreCase("ALL")) {
            result.addAll(Arrays.asList(EnterprisePermission.values()));
            return result;
        }

        for (String part : input.split(",")) {
            EnterprisePermission perm = EnterprisePermission.fromString(part);
            if (perm != null) {
                result.add(perm);
            }
        }
        return result;
    }

    private String permsToString(Set<EnterprisePermission> perms) {
        if (perms.size() == EnterprisePermission.values().length) {
            return "ALL";
        }
        StringBuilder sb = new StringBuilder();
        for (EnterprisePermission perm : perms) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(perm.name());
        }
        return sb.toString();
    }

    private int parsePositiveInt(Player player, String input) {
        int amount;
        try {
            amount = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Amount must be a whole number.", NamedTextColor.RED));
            return -1;
        }
        if (amount <= 0) {
            player.sendMessage(Component.text("Amount must be greater than zero.", NamedTextColor.RED));
            return -1;
        }
        return amount;
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

    /**
     * Pays out Crystals to a member withdrawing from an Enterprise: as much
     * as fits in their inventory, overflow into their personal Crystal Bank
     * (respecting its cap), and only drops on the ground as a last resort -
     * consistent with how /sell overflow is handled.
     */
    private void payoutCrystals(Player player, int totalAmount) {
        int inventoryCapacity = CrystalItemUtil.freeCapacity(player);
        int toInventory = Math.min(totalAmount, inventoryCapacity);
        int remainder = totalAmount - toInventory;

        if (toInventory > 0) {
            giveCrystalsToInventory(player, toInventory);
        }

        if (remainder <= 0) {
            return;
        }

        boolean unlimited = player.isOp() || rankManager.hasRole(player.getUniqueId(), FINANCE_MINISTER_ROLE);
        long depositedToBank = bankManager.deposit(player.getUniqueId(), remainder, unlimited);
        int stillOverflow = remainder - (int) depositedToBank;

        if (depositedToBank > 0) {
            player.sendMessage(Component.text(
                    "Your inventory was full - " + depositedToBank
                            + " Crystal(s) were deposited into your personal bank.", NamedTextColor.AQUA));
        }

        if (stillOverflow > 0) {
            dropCrystals(player, stillOverflow);
            player.sendMessage(Component.text(
                    "Your personal bank is also full - " + stillOverflow + " Crystal(s) were dropped at your feet.",
                    NamedTextColor.YELLOW));
        }
    }

    private void giveCrystalsToInventory(Player player, int amount) {
        int maxStack = CrystalItemUtil.CURRENCY_MATERIAL.getMaxStackSize();
        int remaining = amount;
        while (remaining > 0) {
            int stackSize = Math.min(remaining, maxStack);
            ItemStack stack = CrystalItemUtil.createCrystal(stackSize);
            player.getInventory().addItem(stack);
            remaining -= stackSize;
        }
    }

    private void dropCrystals(Player player, int amount) {
        int maxStack = CrystalItemUtil.CURRENCY_MATERIAL.getMaxStackSize();
        int remaining = amount;
        while (remaining > 0) {
            int stackSize = Math.min(remaining, maxStack);
            ItemStack stack = CrystalItemUtil.createCrystal(stackSize);
            player.getWorld().dropItemNaturally(player.getLocation(), stack);
            remaining -= stackSize;
        }
    }
}
