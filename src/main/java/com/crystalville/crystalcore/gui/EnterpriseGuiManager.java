package com.crystalville.crystalcore.gui;

import com.crystalville.crystalcore.managers.EnterpriseAccount;
import com.crystalville.crystalcore.managers.EnterprisePermission;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * Builds and refreshes the Enterprise Bank GUI: a live balance display,
 * seven deposit slots for physically placing Crystals, a Confirm Deposit
 * button, quick Withdraw buttons, and info buttons for history/members.
 * All actions are still permission-checked at click time.
 */
public final class EnterpriseGuiManager {

    public static final int SIZE = 54;
    public static final int BALANCE_SLOT = 4;
    public static final int[] DEPOSIT_SLOTS = {19, 20, 21, 22, 23, 24, 25};
    public static final int CONFIRM_DEPOSIT_SLOT = 31;
    public static final int WITHDRAW_100_SLOT = 38;
    public static final int WITHDRAW_500_SLOT = 40;
    public static final int WITHDRAW_ALL_SLOT = 42;
    public static final int HISTORY_SLOT = 47;
    public static final int MEMBERS_SLOT = 51;
    public static final int CLOSE_SLOT = 49;

    public Inventory open(Player player, EnterpriseAccount account) {
        EnterpriseGuiHolder holder = new EnterpriseGuiHolder(player.getUniqueId(), account.enterpriseId);
        Inventory inv = Bukkit.createInventory(holder, SIZE,
                Component.text(account.name, NamedTextColor.DARK_AQUA));
        holder.setInventory(inv);

        ItemStack filler = namedItem(Material.GRAY_STAINED_GLASS_PANE, " ", NamedTextColor.GRAY, null);
        for (int i = 0; i < SIZE; i++) inv.setItem(i, filler);
        for (int slot : DEPOSIT_SLOTS) inv.setItem(slot, null);

        refresh(inv, player, account);

        inv.setItem(CONFIRM_DEPOSIT_SLOT, namedItem(Material.EMERALD_BLOCK, "Confirm Deposit",
                NamedTextColor.GREEN, List.of("Deposits the Crystals placed", "in the slots above.")));
        inv.setItem(WITHDRAW_100_SLOT, namedItem(Material.AMETHYST_SHARD, "Withdraw 100",
                NamedTextColor.LIGHT_PURPLE, null));
        inv.setItem(WITHDRAW_500_SLOT, namedItem(Material.AMETHYST_CLUSTER, "Withdraw 500",
                NamedTextColor.LIGHT_PURPLE, null));
        inv.setItem(WITHDRAW_ALL_SLOT, namedItem(Material.CHEST, "Withdraw All",
                NamedTextColor.GOLD, List.of("Withdraws all available funds.")));
        inv.setItem(HISTORY_SLOT, namedItem(Material.PAPER, "View History",
                NamedTextColor.YELLOW, List.of("Prints recent transactions", "to your chat.")));
        inv.setItem(MEMBERS_SLOT, namedItem(Material.PLAYER_HEAD, "View Members",
                NamedTextColor.AQUA, List.of("Prints the member list", "to your chat.")));
        inv.setItem(CLOSE_SLOT, namedItem(Material.BARRIER, "Close", NamedTextColor.RED, null));

        return inv;
    }

    public void refresh(Inventory inv, Player player, EnterpriseAccount account) {
        boolean canView = account.hasPermission(player.getUniqueId(), EnterprisePermission.VIEW_BALANCE);
        List<String> lore = canView
                ? List.of(String.format("%,d", account.balance) + " / "
                        + String.format("%,d", account.maxBalance) + " Crystals")
                : List.of("You don't have permission", "to view this balance.");

        inv.setItem(BALANCE_SLOT, namedItem(Material.NETHER_STAR, account.name, NamedTextColor.AQUA, lore));
    }

    public static boolean isDepositSlot(int slot) {
        for (int s : DEPOSIT_SLOTS) {
            if (s == slot) return true;
        }
        return false;
    }

    private ItemStack namedItem(Material material, String name, NamedTextColor color, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name, color).decoration(TextDecoration.ITALIC, false));
            if (lore != null) {
                meta.lore(lore.stream()
                        .map(l -> Component.text(l, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                        .toList());
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }
  }
