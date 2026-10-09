package com.crystalville.crystalcore.commands;

import com.crystalville.crystalcore.managers.RedeemCodeManager;
import com.crystalville.crystalcore.util.CrystalItemUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /redeem <code>
 * Claims a redeem code created via /make, instantly giving the Crystal
 * amount it was minted with. Each code can be redeemed once per player.
 * Open to everyone.
 */
public class RedeemCommand implements CommandExecutor {

    private final RedeemCodeManager redeemCodeManager;

    public RedeemCommand(RedeemCodeManager redeemCodeManager) {
        this.redeemCodeManager = redeemCodeManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return true;
        }
        Player player = (Player) sender;

        if (args.length != 1) {
            player.sendMessage(Component.text("Usage: /redeem <code>", NamedTextColor.RED));
            return true;
        }

        String code = args[0];
        RedeemCodeManager.RedeemCode redeemCode = redeemCodeManager.get(code);

        if (redeemCode == null) {
            player.sendMessage(Component.text("That redeem code doesn't exist.", NamedTextColor.RED));
            return true;
        }

        if (redeemCode.isExpired()) {
            player.sendMessage(Component.text("That redeem code has expired.", NamedTextColor.RED));
            return true;
        }

        if (redeemCode.hasBeenRedeemedBy(player.getUniqueId())) {
            player.sendMessage(Component.text("You've already redeemed this code.", NamedTextColor.YELLOW));
            return true;
        }

        redeemCodeManager.markRedeemed(code, player.getUniqueId());

        CrystalItemUtil.giveCrystals(player, redeemCode.amount);

        player.sendMessage(Component.text("Redeemed ", NamedTextColor.GREEN)
                .append(Component.text(redeemCode.amount + " Crystal(s)", NamedTextColor.LIGHT_PURPLE))
                .append(Component.text("!", NamedTextColor.GREEN)));

        return true;
    }
              }
