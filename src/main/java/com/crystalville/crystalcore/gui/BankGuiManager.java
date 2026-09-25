package com.crystalville.crystalcore.gui;

import com.crystalville.crystalcore.managers.BankManager;
import com.crystalville.crystalcore.managers.RankManager;
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

public final class BankGuiManager {

    public static final int SIZE = 54;
    public static final int BALANCE_SLOT = 4;
    public static final int[] DEPOSIT_SLOTS = {19, 20, 21, 22, 23, 24, 25};
    public static final int CONFIRM_DEPOSIT_SLOT = 31;
    public static final int WITHDRAW_100_SLOT = 38;
    public static final int WITHDRAW_500_SLOT = 40;
    public static final int WITHDRAW_ALL_SLOT = 42;
    public static final int CLOSE_SLOT = 49;

    private static final String FINANCE_MINISTER_ROLE = "FINANCE_MINISTER";

    private final BankManager bankManager;
    private final RankManager rankManager;

    public BankGuiManager(BankManager bankManager, RankManager rankManager) {
        this.bankManager = bankManager;
        this.rankManager = rankManager;
    }

    public boolean isUnlimited(Player player) {
        return player.isOp() || rankManager.hasRole(player.getUniqueId(), FINANCE_MINISTER_ROLE);
    }

    public Inventory open(Player player) {
        BankGuiHolder holder = new BankGuiHolder(player.getUniqueId());
        Inventory inv = Bukkit.createInventory(
                holder, SIZE, Component.text("Crystal Bank", NamedTextColor.DARK_AQUA));
        holder.setInventory(inv);

        ItemStack filler = namedItem(Material.GRAY_STAINED_GLASS_PANE, " ", NamedTextColor.GRAY, null);
        for (int i = 0; i < SIZE; i++) {
            inv.setItem(i, filler);
        }
        for (int slot : DEPOSIT_SLOTS) {
            inv.setItem(slot, null);
        }

        refreshBalance(inv, player);

        inv.setItem(CONFIRM_DEPOSIT_SLOT, namedItem(Material.EMERALD_BLOCK,
                "Confirm Deposit", NamedTextColor.GREEN,
                List.of("Deposits the Crystals placed", "in the slots above.")));

        inv.setItem(WITHDRAW_100_SLOT, namedItem(Material.AMETHYST_SHARD,
                "Withdraw 100", NamedTextColor.LIGHT_PURPLE, null));

        inv.setItem(WITHDRAW_500_SLOT, namedItem(Material.AMETHYST_CLUSTER,
                "Withdraw 500", NamedTextColor.LIGHT_PURPLE, null));

        inv.setItem(WITHDRAW_ALL_SLOT, namedItem(Material.CHEST,
                "Withdraw All", NamedTextColor.GOLD,
                List.of("Withdraws your entire bank balance.")));

        inv.setItem(CLOSE_SLOT, namedItem(Material.BARRIER, "Close", NamedTextColor.RED, null));

        return inv;
    }

    public void refreshBalance(Inventory inv, Player player) {
        long balance = bankManager.getBalance(player.getUniqueId());
        boolean unlimited = isUnlimited(player);
        long cap = bankManager.getCap(player.getUniqueId());

        String limitLine = unlimited
                ? "Limit: Unlimited (Finance Minister/OP)"
                : "Limit: " + String.format("%,d", cap) + " Crystals";

        inv.setItem(BALANCE_SLOT, namedItem(Material.NETHER_STAR, "Your Bank Balance", NamedTextColor.AQUA,
                List.of(String.format("%,d", balance) + " Crystals", limitLine)));
    }

    public static boolean isDepositSlot(int slot) {
        for (int depositSlot : DEPOSIT_SLOTS) {
            if (depositSlot == slot) {
                return true;
            }
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
                        .map(line -> Component.text(line, NamedTextColor.GRAY)
                                .decoration(TextDecoration.ITALIC, false))
                        .toList());
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }
    }
