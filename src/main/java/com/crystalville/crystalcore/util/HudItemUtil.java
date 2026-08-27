package com.crystalville.crystalcore.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * The physical "HUD Toggle" item - a branded Arrow players can right-click
 * to show/hide their Crystal Ville HUD. Obtained via /hud item.
 */
public final class HudItemUtil {

    private static NamespacedKey toggleKey;

    private HudItemUtil() {
    }

    public static void init(JavaPlugin plugin) {
        toggleKey = new NamespacedKey(plugin, "crystalville_hud_toggle");
    }

    public static ItemStack createToggleItem() {
        ItemStack stack = new ItemStack(Material.ARROW);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("HUD Toggle", NamedTextColor.AQUA)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Right-click to show or hide", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false),
                    Component.text("your Crystal Ville HUD.", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            meta.getPersistentDataContainer().set(toggleKey, PersistentDataType.BYTE, (byte) 1);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public static boolean isToggleItem(ItemStack stack) {
        if (stack == null || stack.getType() != Material.ARROW || toggleKey == null) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return false;
        }
        return meta.getPersistentDataContainer().has(toggleKey, PersistentDataType.BYTE);
    }
                            }
