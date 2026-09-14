package com.crystalville.crystalcore.commands;

import com.crystalville.crystalcore.CrystalCore;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Links a Minecraft account to a Crystal Ville website account through Supabase RPC. */
public final class WebLinkCommand implements CommandExecutor {
    private final CrystalCore plugin;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    public WebLinkCommand(CrystalCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }

        if (args.length != 1 || !args[0].matches("\\d{6}")) {
            player.sendMessage(ChatColor.RED + "Usage: /cvlink <6-digit-code>");
            return true;
        }

        String baseUrl = plugin.getConfig().getString("website-link.supabase-url", "").trim();
        String apiKey = plugin.getConfig().getString("website-link.supabase-publishable-key", "").trim();
        if (baseUrl.isEmpty() || apiKey.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Website linking is not configured on this server.");
            plugin.getLogger().warning("Website linking is enabled but Supabase URL/key is missing.");
            return true;
        }

        String endpoint = baseUrl.replaceAll("/+$", "") + "/rest/v1/rpc/complete_minecraft_link";
        String json = "{\"p_code\":\"" + args[0] + "\",\"p_username\":\"" + escape(player.getName())
                + "\",\"p_uuid\":\"" + player.getUniqueId() + "\"}";

        player.sendMessage(ChatColor.GRAY + "Checking your Crystal Ville website code...");

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                        .timeout(Duration.ofSeconds(10))
                        .header("apikey", apiKey)
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();

                HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
                String body = response.body() == null ? "" : response.body();
                boolean success = response.statusCode() >= 200 && response.statusCode() < 300
                        && body.contains("\"success\":true");

                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (!player.isOnline()) return;
                    if (success) {
                        player.sendMessage(ChatColor.GREEN + "✓ Minecraft account linked successfully!");
                        player.sendMessage(ChatColor.GRAY + "Your Crystal Ville Player Hub is now connected.");
                    } else {
                        String error = extractError(body);
                        player.sendMessage(ChatColor.RED + "✗ Link failed: " + error);
                    }
                });
            } catch (Exception e) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (player.isOnline()) {
                        player.sendMessage(ChatColor.RED + "Could not reach the Crystal Ville website service.");
                    }
                });
                plugin.getLogger().warning("Website link request failed: " + e.getMessage());
            }
        });
        return true;
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String extractError(String body) {
        int i = body.indexOf("\"error\":");
        if (i >= 0) {
            int start = body.indexOf('"', i + 8);
            int end = start >= 0 ? body.indexOf('"', start + 1) : -1;
            if (start >= 0 && end > start) return body.substring(start + 1, end);
        }
        return "Invalid or already-used code.";
    }
}
