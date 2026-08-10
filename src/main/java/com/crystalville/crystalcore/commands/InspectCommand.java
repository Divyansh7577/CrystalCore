package com.crystalville.crystalcore.commands;

import com.crystalville.crystalcore.managers.InspectorManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /i - toggles Chest Inspector Mode. While active, clicking any chest,
 * barrel, or shulker box shows its full activity log instead of opening it.
 * OP only.
 */
public class InspectCommand implements CommandExecutor {

    private final InspectorManager inspectorManager;

    public InspectCommand(InspectorManager inspectorManager) {
        this.inspectorManager = inspectorManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return true;
        }
        Player player = (Player) sender;

        if (!player.isOp()) {
            player.sendMessage(Component.text("Only OPs can use /i.", NamedTextColor.RED));
            return true;
        }

        boolean nowInspecting = inspectorManager.toggle(player.getUniqueId());

        if (nowInspecting) {
            player.sendMessage(Component.text(
                    "Chest Inspector Mode enabled. Click any chest, barrel, or shulker box to view its log.",
                    NamedTextColor.AQUA));
        } else {
            player.sendMessage(Component.text("Chest Inspector Mode disabled.", NamedTextColor.YELLOW));
        }

        return true;
    }
}
