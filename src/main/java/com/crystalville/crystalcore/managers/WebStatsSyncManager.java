package com.crystalville.crystalcore.managers;

import com.crystalville.crystalcore.CrystalCore;
import com.crystalville.crystalcore.util.CrystalItemUtil;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Syncs live Minecraft player data to the linked Crystal Ville web profile. */
public final class WebStatsSyncManager {
    private final CrystalCore plugin;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    public WebStatsSyncManager(CrystalCore plugin) {
        this.plugin = plugin;
    }

    public void sync(Player player) {
        if (player == null || !player.isOnline()) return;
        if (!plugin.getConfig().getBoolean("website-link.enabled", true)) return;

        String baseUrl = plugin.getConfig().getString("website-link.supabase-url", "").trim().replaceAll("/+$", "");
        String apiKey = plugin.getConfig().getString("website-link.supabase-publishable-key", "").trim();
        if (baseUrl.isEmpty() || apiKey.isEmpty()) return;

        StatsManager stats = plugin.getStatsManager();
        RankManager ranks = plugin.getRankManager();
        long balance = countCrystals(player);
        long playtimeMinutes = stats.getLivePlaytimeSeconds(player.getUniqueId()) / 60L;
        long kills = stats.getKills(player.getUniqueId());
        long deaths = stats.getDeaths(player.getUniqueId());
        long blocks = stats.getBlocksBroken(player.getUniqueId());
        String rank = ranks.hasRank(player.getUniqueId()) ? ranks.getRankName(player.getUniqueId()) : "Player";

        String endpoint = baseUrl + "/rest/v1/rpc/sync_minecraft_profile";
        String json = "{" +
                "\"p_username\":\"" + escape(player.getName()) + "\"," +
                "\"p_uuid\":\"" + escape(player.getUniqueId().toString()) + "\"," +
                "\"p_balance\":" + balance + "," +
                "\"p_playtime_minutes\":" + playtimeMinutes + "," +
                "\"p_kills\":" + kills + "," +
                "\"p_deaths\":" + deaths + "," +
                "\"p_blocks_broken\":" + blocks + "," +
                "\"p_rank\":\"" + escape(rank) + "\"" +
                "}";

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                        .timeout(Duration.ofSeconds(15))
                        .header("apikey", apiKey)
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();

                HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
                String responseBody = response.body() == null ? "" : response.body();
                String normalizedBody = responseBody.replaceAll("\\s+", "");
                if (response.statusCode() < 200 || response.statusCode() >= 300 || !normalizedBody.contains("\"success\":true")) {
                    plugin.getLogger().warning("Crystal Ville WebStats: sync failed for " + player.getName()
                            + " HTTP " + response.statusCode() + " response=" + responseBody);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Crystal Ville WebStats: sync error for " + player.getName()
                        + " - " + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        });
    }

    private long countCrystals(Player player) {
        long total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == CrystalItemUtil.CURRENCY_MATERIAL) {
                total += item.getAmount();
            }
        }
        return total;
    }

    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
          }
