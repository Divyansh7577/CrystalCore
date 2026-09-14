package com.crystalville.crystalcore.commands;

import com.crystalville.crystalcore.listeners.HudListener;
import com.crystalville.crystalcore.managers.HudManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /edit <text...>
 * Sets the HUD's footer credit line (e.g. "Duo Gamerz X") for the
 * command's own HUD only. Open to any player - each player can only
 * ever edit their own footer, never anyone else's. Does NOT touch the
 * "CRYSTAL VILLE" header/logo - that's hardcoded in HudListener and
 * cannot be changed by any command.
 */
public class EditHudCommand implements CommandExecutor {

    private final HudManager hudManager;
    private final HudListener hudListener;

    public EditHudCommand(HudManager hudManager, HudListener hudListener) {
        this.hudManager = hudManager;
        this.hudListener = hudListener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage(Component.text("Usage: /edit <new footer text>", NamedTextColor.RED));
            return true;
        }

        String newFooter = ChatColor.translateAlternateColorCodes('&', String.join(" ", args));

        hudManager.setFooterText(player.getUniqueId(), newFooter);

        player.sendMessage(Component.text("Your HUD footer is now: ", NamedTextColor.GREEN)
                .append(Component.text(newFooter, NamedTextColor.DARK_AQUA)));

        if (hudManager.isEnabled(player.getUniqueId())) {
            hudListener.refresh(player);
        }

        return true;
    }
                             }
