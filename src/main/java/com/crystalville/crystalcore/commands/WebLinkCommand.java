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

/**
 * Links a Minecraft account to a Crystal Ville website account
 * through the Supabase RPC function.
 */
public final class WebLinkCommand implements CommandExecutor {

    private final CrystalCore plugin;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    public WebLinkCommand(CrystalCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {

        // Only players can use /cvlink
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }

        // Code must be exactly 6 digits
        if (args.length != 1 || !args[0].matches("\\d{6}")) {
            player.sendMessage(
                    ChatColor.RED + "Usage: /cvlink <6-digit-code>"
            );
            return true;
        }

        // Read Supabase configuration
        String baseUrl = plugin.getConfig()
                .getString("website-link.supabase-url", "")
                .trim();

        String apiKey = plugin.getConfig()
                .getString("website-link.supabase-publishable-key", "")
                .trim();

        // Make sure configuration exists
        if (baseUrl.isEmpty() || apiKey.isEmpty()) {

            player.sendMessage(
                    ChatColor.RED
                            + "Website linking is not configured on this server."
            );

            plugin.getLogger().warning(
                    "Website linking is enabled but Supabase URL/key is missing."
            );

            return true;
        }

        // Supabase RPC endpoint
        String endpoint =
                baseUrl.replaceAll("/+$", "")
                        + "/rest/v1/rpc/complete_minecraft_link";

        // JSON request body
        String json =
                "{\"p_code\":\""
                        + args[0]
                        + "\",\"p_username\":\""
                        + escape(player.getName())
                        + "\",\"p_uuid\":\""
                        + player.getUniqueId()
                        + "\"}";

        player.sendMessage(
                ChatColor.GRAY
                        + "Checking your Crystal Ville website code..."
        );

        // Run HTTP request asynchronously so the Minecraft server
        // main thread does not get blocked.
        plugin.getServer()
                .getScheduler()
                .runTaskAsynchronously(plugin, () -> {

                    try {

                        HttpRequest request =
                                HttpRequest.newBuilder(URI.create(endpoint))
                                        .timeout(Duration.ofSeconds(10))
                                        .header("apikey", apiKey)
                                        .header(
                                                "Authorization",
                                                "Bearer " + apiKey
                                        )
                                        .header(
                                                "Content-Type",
                                                "application/json"
                                        )
                                        .header(
                                                "Accept",
                                                "application/json"
                                        )
                                        .POST(
                                                HttpRequest.BodyPublishers
                                                        .ofString(json)
                                        )
                                        .build();

                        HttpResponse<String> response =
                                http.send(
                                        request,
                                        HttpResponse.BodyHandlers.ofString()
                                );

                        String body =
                                response.body() == null
                                        ? ""
                                        : response.body();

                        boolean success =
                                response.statusCode() >= 200
                                        && response.statusCode() < 300
                                        && body.contains(
                                                "\"success\":true"
                                        );

                        // Go back to Minecraft main thread
                        plugin.getServer()
                                .getScheduler()
                                .runTask(plugin, () -> {

                                    if (!player.isOnline()) {
                                        return;
                                    }

                                    if (success) {

                                        player.sendMessage(
                                                ChatColor.GREEN
                                                        + "✓ Minecraft account linked successfully!"
                                        );

                                        player.sendMessage(
                                                ChatColor.GRAY
                                                        + "Your Crystal Ville Player Hub is now connected."
                                        );

                                    } else {

                                        String error =
                                                extractError(body);

                                        player.sendMessage(
                                                ChatColor.RED
                                                        + "✗ Link failed: "
                                                        + error
                                        );

                                        plugin.getLogger().warning(
                                                "Supabase returned HTTP "
                                                        + response.statusCode()
                                                        + ": "
                                                        + body
                                        );
                                    }
                                });

                    } catch (Exception e) {

                        /*
                         * IMPORTANT:
                         * Print the actual exception class and cause.
                         * Previously only e.getMessage() was printed,
                         * which could be null.
                         */

                        String errorType =
                                e.getClass().getName();

                        String errorMessage =
                                e.getMessage();

                        plugin.getLogger().severe(
                                "Website link request failed: "
                                        + errorType
                                        + " | message="
                                        + errorMessage
                        );

                        if (e.getCause() != null) {

                            plugin.getLogger().severe(
                                    "Cause: "
                                            + e.getCause()
                                            .getClass()
                                            .getName()
                                            + " | "
                                            + e.getCause()
                                            .getMessage()
                            );
                        }

                        // Send safe message to player
                        plugin.getServer()
                                .getScheduler()
                                .runTask(plugin, () -> {

                                    if (player.isOnline()) {

                                        player.sendMessage(
                                                ChatColor.RED
                                                        + "Could not reach the Crystal Ville website service."
                                        );
                                    }
                                });
                    }
                });

        return true;
    }

    /**
     * Escapes characters that could break the JSON request.
     */
    private static String escape(String value) {

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    /**
     * Attempts to read the error returned by Supabase.
     */
    private static String extractError(String body) {

        int i = body.indexOf("\"error\":");

        if (i >= 0) {

            int start =
                    body.indexOf('"', i + 8);

            int end =
                    start >= 0
                            ? body.indexOf('"', start + 1)
                            : -1;

            if (start >= 0 && end > start) {

                return body.substring(
                        start + 1,
                        end
                );
            }
        }

        return "Invalid or already-used code.";
    }
}
