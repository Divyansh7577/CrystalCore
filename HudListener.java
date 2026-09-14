package com.crystalville.crystalcore.listeners;

import com.crystalville.crystalcore.managers.HudManager;
import com.crystalville.crystalcore.managers.RankManager;
import com.crystalville.crystalcore.managers.StatsManager;
import com.crystalville.crystalcore.util.CrystalItemUtil;
import com.crystalville.crystalcore.util.HudItemUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Builds and maintains the "Crystal Ville" scoreboard-sidebar HUD:
 * join date, rank, Crystal balance, kills, deaths, playtime, and each
 * player's own editable footer credit line. The header/logo text is
 * hardcoded here and cannot be changed by any command - only the footer
 * (per-player, via /edit) can be changed.
 */
public class HudListener implements Listener {

    private static final String HEADER_TEXT = "CRYSTAL VILLE";
    private static final SimpleDateFormat JOIN_DATE_FORMAT = new SimpleDateFormat("MMM dd, yyyy");

    private final JavaPlugin plugin;
    private final HudManager hudManager;
    private final RankManager rankManager;
    private final StatsManager statsManager;

    public HudListener(JavaPlugin plugin, HudManager hudManager, RankManager rankManager,
                        StatsManager statsManager) {
        this.plugin = plugin;
        this.hudManager = hudManager;
        this.rankManager = rankManager;
        this.statsManager = statsManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        statsManager.startSession(player.getUniqueId());

        if (hudManager.isEnabled(player.getUniqueId())) {
            refresh(player);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        statsManager.endSession(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        statsManager.incrementDeaths(victim.getUniqueId());
        if (hudManager.isEnabled(victim.getUniqueId())) {
            refresh(victim);
        }

        Player killer = victim.getKiller();
        if (killer != null) {
            statsManager.incrementKills(killer.getUniqueId());
            if (hudManager.isEnabled(killer.getUniqueId())) {
                refresh(killer);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (!HudItemUtil.isToggleItem(item)) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();
        boolean nowEnabled = hudManager.toggle(player.getUniqueId());

        if (nowEnabled) {
            refresh(player);
            player.sendMessage(Component.text("HUD enabled.", NamedTextColor.GREEN));
        } else {
            player.setScoreboard(plugin.getServer().getScoreboardManager().getMainScoreboard());
            player.sendMessage(Component.text("HUD disabled.", NamedTextColor.YELLOW));
        }
    }

    /** Rebuilds and applies the HUD scoreboard for a single player with current live values. */
    public void refresh(Player player) {
        Scoreboard scoreboard = plugin.getServer().getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective(
                "cc_hud", Criteria.DUMMY, headerComponent());
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        int score = 9;

        score = addLine(scoreboard, objective, score, joinedLine(player));
        score = addLine(scoreboard, objective, score, nameLine(player));
        score = addLine(scoreboard, objective, score, rankLine(player));
        score = addLine(scoreboard, objective, score, crystalsLine(player));
        score = addLine(scoreboard, objective, score, killsLine(player));
        score = addLine(scoreboard, objective, score, deathsLine(player));
        score = addLine(scoreboard, objective, score, playtimeLine(player));
        score = addLine(scoreboard, objective, score, Component.empty());
        addLine(scoreboard, objective, score, footerLine(player));

        player.setScoreboard(scoreboard);
    }

    private Component headerComponent() {
        return Component.text("* " + HEADER_TEXT + " *", NamedTextColor.AQUA);
    }

    private Component joinedLine(Player player) {
        long firstPlayed = player.getFirstPlayed();
        String dateStr = firstPlayed > 0
                ? JOIN_DATE_FORMAT.format(new Date(firstPlayed))
                : JOIN_DATE_FORMAT.format(new Date());
        return Component.text("Joined: ", NamedTextColor.GRAY)
                .append(Component.text(dateStr, NamedTextColor.WHITE));
    }

    private Component nameLine(Player player) {
        return Component.text("* " + player.getName(), NamedTextColor.GOLD);
    }

    private Component rankLine(Player player) {
        String rankName = rankManager.hasRank(player.getUniqueId())
                ? rankManager.getRankName(player.getUniqueId())
                : "Member";
        TextColor rankColor = rankManager.hasRank(player.getUniqueId())
                ? rankManager.resolveColor(player.getUniqueId())
                : NamedTextColor.GRAY;
        return Component.text("Rank: ", NamedTextColor.GRAY)
                .append(Component.text(rankName, rankColor));
    }

    private Component crystalsLine(Player player) {
        int total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == CrystalItemUtil.CURRENCY_MATERIAL) {
                total += item.getAmount();
            }
        }
        return Component.text("Crystals: ", NamedTextColor.GRAY)
                .append(Component.text(String.format("%,d", total), NamedTextColor.LIGHT_PURPLE));
    }

    private Component killsLine(Player player) {
        return Component.text("Kills: ", NamedTextColor.GRAY)
                .append(Component.text(statsManager.getKills(player.getUniqueId()), NamedTextColor.RED));
    }

    private Component deathsLine(Player player) {
        return Component.text("Deaths: ", NamedTextColor.GRAY)
                .append(Component.text(statsManager.getDeaths(player.getUniqueId()), NamedTextColor.DARK_RED));
    }

    private Component playtimeLine(Player player) {
        long seconds = statsManager.getLivePlaytimeSeconds(player.getUniqueId());
        return Component.text("Playtime: ", NamedTextColor.GRAY)
                .append(Component.text(StatsManager.formatPlaytime(seconds), NamedTextColor.YELLOW));
    }

    private Component footerLine(Player player) {
        return Component.text(hudManager.getFooterText(player.getUniqueId()), NamedTextColor.DARK_AQUA);
    }

    /** Adds one sidebar line using the classic invisible-team-entry trick, and returns the next score. */
    private int addLine(Scoreboard scoreboard, Objective objective, int score, Component text) {
        String entry = invisibleEntry(score);
        Team team = scoreboard.registerNewTeam("cc_line_" + score);
        team.addEntry(entry);
        team.prefix(text);
        objective.getScore(entry).setScore(score);
        return score - 1;
    }

    /** Generates a short, unique, invisible legacy-color-code string to use as a fake scoreboard entry. */
    private String invisibleEntry(int index) {
        org.bukkit.ChatColor[] colors = org.bukkit.ChatColor.values();
        org.bukkit.ChatColor first = colors[index % colors.length];
        org.bukkit.ChatColor second = colors[(index / colors.length) % colors.length];
        return first.toString() + second.toString();
    }
}
