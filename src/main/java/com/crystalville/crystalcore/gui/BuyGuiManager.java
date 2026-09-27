package com.crystalville.crystalcore.gui;

import com.crystalville.crystalcore.managers.ShopManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * A browsable, paginated GUI listing every buyable shop item. Clicking an
 * item buys ONE at its buy price - same restriction and cost logic as
 * /buy (Market Minister/OP only). Shift-click buys a full stack at once.
 */
public final class BuyGuiManager {

    public static final int SIZE = 54;
    public static final int ITEMS_PER_PAGE = 45;
    public static final int PREV_PAGE_SLOT = 45;
    public static final int CLOSE_SLOT = 49;
    public static final int NEXT_PAGE_SLOT = 53;

    private final ShopManager shopManager;

    public BuyGuiManager(ShopManager shopManager) {
        this.shopManager = shopManager;
    }

    public Inventory open(Player player, int page) {
        List<Material> buyable = new ArrayList<>(shopManager.getAllEntries().keySet());
        int maxPage = Math.max(0, (buyable.size() - 1) / ITEMS_PER_PAGE);
        int clampedPage = Math.max(0, Math.min(page, maxPage));

        BuyGuiHolder holder = new BuyGuiHolder(player.getUniqueId(), clampedPage);
        Inventory inv = Bukkit.createInventory(holder, SIZE,
                Component.text("Buy Items - Page " + (clampedPage + 1) + "/" + (maxPage + 1),
                        NamedTextColor.DARK_PURPLE));
        holder.setInventory(inv);

        ItemStack filler = namedItem(Material.GRAY_STAINED_GLASS_PANE, " ", NamedTextColor.GRAY, null);
        for (int i = ITEMS_PER_PAGE; i < SIZE; i++) inv.setItem(i, filler);

        int start = clampedPage * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, buyable.size());
        for (int i = start; i < end; i++) {
            Material material = buyable.get(i);
            int buyPrice = shopManager.getBuyPrice(material);
            inv.setItem(i - start, namedItem(material, prettyName(material), NamedTextColor.WHITE,
                    List.of(
                            "Buy price: " + buyPrice + " Crystal(s) each",
                            "Click to buy 1",
                            "Shift-click to buy a full stack"
                    )));
        }

        if (clampedPage > 0) {
            inv.setItem(PREV_PAGE_SLOT, namedItem(Material.ARROW, "Previous Page", NamedTextColor.YELLOW, null));
        }
        if (clampedPage < maxPage) {
            inv.setItem(NEXT_PAGE_SLOT, namedItem(Material.ARROW, "Next Page", NamedTextColor.YELLOW, null));
        }
        inv.setItem(CLOSE_SLOT, namedItem(Material.BARRIER, "Close", NamedTextColor.RED, null));

        return inv;
    }

    public Material getMaterialAt(int page, int slot) {
        if (slot < 0 || slot >= ITEMS_PER_PAGE) return null;
        List<Material> buyable = new ArrayList<>(shopManager.getAllEntries().keySet());
        int index = page * ITEMS_PER_PAGE + slot;
        return index < buyable.size() ? buyable.get(index) : null;
    }

    private String prettyName(Material material) {
        String[] parts = material.name().toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(" ");
        }
        return sb.toString().trim();
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
