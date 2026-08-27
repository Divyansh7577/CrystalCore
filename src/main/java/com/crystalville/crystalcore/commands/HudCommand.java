package com.crystalville.crystalcore.commands;

import com.crystalville.crystalcore.listeners.HudListener;
import com.crystalville.crystalcore.managers.HudManager;
import com.crystalville.crystalcore.util.HudItemUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /hud          - toggles the HUD on/off for yourself
 * /hud item     - gives you the physical "HUD Toggle" arrow item
 * Open to all players.
 */
public class HudCommand implements CommandExecutor {

    private final HudManager hudManager;
    private final HudListener hudListener;

    public HudCommand(HudManager hudManager, HudListener hudListener) {
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

        if (args.length > 0 && args[0].equalsIgnoreCase("item")) {
            player.getInventory().addItem(HudItemUtil.createToggleItem());
            player.sendMessage(Component.text("You received the HUD Toggle item.", NamedTextColor.AQUA));
            return true;
        }

        boolean nowEnabled = hudManager.toggle(player.getUniqueId());
        if (nowEnabled) {
            hudListener.refresh(player);
            player.sendMessage(Component.text("HUD enabled.", NamedTextColor.GREEN));
        } else {
            player.setScoreboard(player.getServer().getScoreboardManager().getMainScoreboard());
            player.sendMessage(Component.text("HUD disabled.", NamedTextColor.YELLOW));
        }

        return true;
    }
          }
