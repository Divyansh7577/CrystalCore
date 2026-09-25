package com.crystalville.crystalcore.util;

import com.crystalville.crystalcore.managers.EnterpriseAccount;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.ArrayList;
import java.util.List;

/** Builds the "Enterprise Handbook" written book given when a company is created or reissued. */
public final class EnterpriseBookUtil {

    private EnterpriseBookUtil() {
    }

    public static ItemStack createHandbook(EnterpriseAccount account) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();

        meta.title(Component.text("Enterprise Handbook"));
        meta.author(Component.text("Crystal Ville"));

        List<Component> pages = new ArrayList<>();

        pages.add(Component.text("")
                .append(Component.text("ENTERPRISE HANDBOOK\n\n", NamedTextColor.DARK_BLUE))
                .append(Component.text(account.name + "\n\n", NamedTextColor.BLACK))
                .append(Component.text("Enterprise ID:\n", NamedTextColor.DARK_GRAY))
                .append(Component.text(account.enterpriseId + "\n\n", NamedTextColor.BLACK))
                .append(Component.text("Keep this book safe - it explains\neverything about running your\ncompany.", NamedTextColor.DARK_GRAY)));

        pages.add(Component.text("")
                .append(Component.text("GETTING PAID\n\n", NamedTextColor.DARK_BLUE))
                .append(Component.text("Anyone can pay your company\ndirectly with:\n\n", NamedTextColor.BLACK))
                .append(Component.text("/payenterprise " + account.enterpriseId + " <amount>\n\n", NamedTextColor.DARK_GREEN))
                .append(Component.text("Payments happen instantly.\nMax balance: " + String.format("%,d", account.maxBalance) + " Crystals.", NamedTextColor.BLACK)));

        pages.add(Component.text("")
                .append(Component.text("VIEWING INFO\n\n", NamedTextColor.DARK_BLUE))
                .append(Component.text("/enterprise info <ID>\n", NamedTextColor.DARK_GREEN))
                .append(Component.text("Basic company info\n\n", NamedTextColor.BLACK))
                .append(Component.text("/enterprise balance <ID>\n", NamedTextColor.DARK_GREEN))
                .append(Component.text("Current balance\n\n", NamedTextColor.BLACK))
                .append(Component.text("/enterprise history <ID>\n", NamedTextColor.DARK_GREEN))
                .append(Component.text("Recent transactions\n\n", NamedTextColor.BLACK))
                .append(Component.text("/enterprise members <ID>\n", NamedTextColor.DARK_GREEN))
                .append(Component.text("List all members", NamedTextColor.BLACK)));

        pages.add(Component.text("")
                .append(Component.text("MOVING FUNDS\n\n", NamedTextColor.DARK_BLUE))
                .append(Component.text("/enterprise deposit <ID> <amt>\n", NamedTextColor.DARK_GREEN))
                .append(Component.text("Add Crystals to the company\n\n", NamedTextColor.BLACK))
                .append(Component.text("/enterprise withdraw <ID> <amt>\n", NamedTextColor.DARK_GREEN))
                .append(Component.text("Take Crystals out\n\n", NamedTextColor.BLACK))
                .append(Component.text("/enterprise gui <ID>\n", NamedTextColor.DARK_GREEN))
                .append(Component.text("Visual deposit/withdraw menu\n\n", NamedTextColor.BLACK))
                .append(Component.text("/enterprise list\n", NamedTextColor.DARK_GREEN))
                .append(Component.text("Every company you're part of", NamedTextColor.BLACK)));

        pages.add(Component.text("")
                .append(Component.text("MANAGING STAFF\n\n", NamedTextColor.DARK_BLUE))
                .append(Component.text("/enterprise add <ID> <player>\n<perms|ALL>\n", NamedTextColor.DARK_GREEN))
                .append(Component.text("Hire a member\n\n", NamedTextColor.BLACK))
                .append(Component.text("/enterprise remove <ID> <player>\n", NamedTextColor.DARK_GREEN))
                .append(Component.text("Fire a member\n\n", NamedTextColor.BLACK))
                .append(Component.text("/enterprise setperms <ID>\n<player> <perms|ALL>\n", NamedTextColor.DARK_GREEN))
                .append(Component.text("Change someone's access", NamedTextColor.BLACK)));

        pages.add(Component.text("")
                .append(Component.text("PERMISSIONS\n\n", NamedTextColor.DARK_BLUE))
                .append(Component.text("VIEW_BALANCE\n", NamedTextColor.DARK_GREEN))
                .append(Component.text("See the balance\n\n", NamedTextColor.BLACK))
                .append(Component.text("DEPOSIT / WITHDRAW\n", NamedTextColor.DARK_GREEN))
                .append(Component.text("Move funds in or out\n\n", NamedTextColor.BLACK))
                .append(Component.text("VIEW_HISTORY\n", NamedTextColor.DARK_GREEN))
                .append(Component.text("See transactions\n\n", NamedTextColor.BLACK))
                .append(Component.text("MANAGE_MEMBERS\n", NamedTextColor.DARK_GREEN))
                .append(Component.text("Hire, fire, and set\nother members' access.\n\nAs owner, you always have\nevery permission.", NamedTextColor.BLACK)));

        pages.add(Component.text("")
                .append(Component.text("LOST YOUR BOOK?\n\n", NamedTextColor.DARK_BLUE))
                .append(Component.text("The owner or an OP can\nreissue it anytime:\n\n", NamedTextColor.BLACK))
                .append(Component.text("/issue guide " + account.enterpriseId, NamedTextColor.DARK_GREEN)));

        meta.addPages(pages.toArray(new Component[0]));
        book.setItemMeta(meta);
        return book;
    }
  }
