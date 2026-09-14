package com.crystalville.crystalcore.commands;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Vector;

import java.util.Collection;

public class LogoCommand implements CommandExecutor {

    private static final String LOGO_TAG = "crystalcore_logo";
    private static final double DISTANCE_IN_FRONT = 3.0;
    private static final double HEIGHT_OFFSET = 1.0;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.isOp()) {
            player.sendMessage(ChatColor.RED + "Only OPs can use /logo.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Usage: /logo <text...>  |  /logo remove");
            return true;
        }

        if (args[0].equalsIgnoreCase("remove")) {
            removeNearbyLogos(player);
            return true;
        }

        String rawText = String.join(" ", args);
        String colored = ChatColor.translateAlternateColorCodes('&', rawText);

        spawnLogo(player, colored);
        return true;
    }

    private void spawnLogo(Player player, String text) {
        World world = player.getWorld();
        Location eyeLoc = player.getEyeLocation();

        Vector direction = eyeLoc.getDirection().normalize();
        Location spawnLoc = eyeLoc.clone()
                .add(direction.multiply(DISTANCE_IN_FRONT))
                .add(0, HEIGHT_OFFSET, 0);

        TextDisplay display = world.spawn(spawnLoc, TextDisplay.class);
        display.text(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                .legacySection().deserialize(text));

        display.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
        display.setShadowed(true);
        display.setSeeThrough(false);
        display.setDefaultBackground(true);
        display.setPersistent(true);
        display.addScoreboardTag(LOGO_TAG);

        player.sendMessage(ChatColor.GREEN + "Logo spawned in front of you.");
    }

    private void removeNearbyLogos(Player player) {
        Collection<Entity> nearby = player.getWorld().getNearbyEntities(
                player.getLocation(), 15, 15, 15);

        int removed = 0;
        for (Entity entity : nearby) {
            if (entity instanceof TextDisplay && entity.getScoreboardTags().contains(LOGO_TAG)) {
                entity.remove();
                removed++;
            }
        }

        if (removed == 0) {
            player.sendMessage(ChatColor.YELLOW + "No nearby CrystalCore logos found within 15 blocks.");
        } else {
            player.sendMessage(ChatColor.GREEN + "Removed " + removed + " logo(s).");
        }
    }
    }
