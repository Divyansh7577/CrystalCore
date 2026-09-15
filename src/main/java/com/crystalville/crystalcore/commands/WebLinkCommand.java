package com.crystalville.crystalcore.commands;

import com.crystalville.crystalcore.CrystalCore;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.net.ConnectException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Links a Minecraft account to a Crystal Ville website account
 * through the Crystal Ville Supabase service.
 */
public final class WebLinkCommand implements CommandExecutor {

    private final CrystalCore plugin;
    private final HttpClient http;

    public WebLinkCommand(CrystalCore plugin) {
        this.plugin = plugin;

        /*
         * Crystal Ville servers may run in environments where IPv6
         * connectivity is unavailable or unreliable.
         *
         * Prefer IPv4 for the outbound Supabase connection.
         */
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.net.preferIPv6Addresses", "false");

        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {

        // Only players can use this command.
        if (!(sender instanceof Player player)) {
            sender.sendMessage(
                    "This command can only be run by a player."
            );
            return true;
        }

        // The website generates a 6-digit code.
        if (args.length != 1 || !args[0].matches("\\d{6}")) {
            player.sendMessage(
                    ChatColor.RED
                            + "Usage: /cvlink <6-digit-code>"
            );
            return true;
        }

        String code = args[0];

        // Read Supabase configuration.
        String baseUrl = plugin.getConfig()
                .getString("website-link.supabase-url", "")
                .trim();

        String apiKey = plugin.getConfig()
                .getString("website-link.supabase-publishable-key", "")
                .trim();

        // Configuration check.
        if (baseUrl.isEmpty() || apiKey.isEmpty()) {

            player.sendMessage(
                    ChatColor.RED
                            + "Website linking is not configured on this server."
            );

            plugin.getLogger().severe(
                    "Website linking configuration is missing."
            );

            return true;
        }

        // Remove trailing slash from Supabase URL.
        baseUrl = baseUrl.replaceAll("/+$", "");

        // Supabase RPC endpoint.
        String endpoint =
                baseUrl
                        + "/rest/v1/rpc/complete_minecraft_link";

        // Build JSON safely.
        String json =
                "{"
                        + "\"p_code\":\""
                        + escape(code)
                        + "\","
                        + "\"p_username\":\""
                        + escape(player.getName())
                        + "\","
                        + "\"p_uuid\":\""
                        + escape(player.getUniqueId().toString())
                        + "\""
                        + "}";

        player.sendMessage(
                ChatColor.GRAY
                        + "Checking your Crystal Ville website code..."
        );

        final String finalEndpoint = endpoint;
        final String finalJson = json;
        final String finalApiKey = apiKey;

        /*
         * Everything below runs asynchronously.
         * This prevents the HTTP request from freezing
         * the Minecraft server's main thread.
         */
        plugin.getServer()
                .getScheduler()
                .runTaskAsynchronously(plugin, () -> {

                    try {

                        // Log the hostname for easier diagnostics.
                        try {
                            URI uri = URI.create(finalEndpoint);
                            String host = uri.getHost();

                            plugin.getLogger().info(
                                    "Crystal Ville WebLink: connecting to "
                                            + host
                            );

                            if (host != null) {
                                InetAddress[] addresses =
                                        InetAddress.getAllByName(host);

                                for (InetAddress address : addresses) {
                                    plugin.getLogger().info(
                                            "Crystal Ville WebLink: resolved "
                                                    + host
                                                    + " -> "
                                                    + address
                                                            .getHostAddress()
                                    );
                                }
                            }

                        } catch (Exception dnsError) {

                            plugin.getLogger().warning(
                                    "Crystal Ville WebLink: DNS diagnostic failed: "
                                            + dnsError.getClass()
                                                    .getName()
                                            + " | "
                                            + dnsError.getMessage()
                            );
                        }

                        // Create HTTP request.
                        HttpRequest request =
                                HttpRequest.newBuilder(
                                                URI.create(finalEndpoint)
                                        )
                                        .timeout(
                                                Duration.ofSeconds(20)
                                        )
                                        .header(
                                                "apikey",
                                                finalApiKey
                                        )
                                        .header(
                                                "Authorization",
                                                "Bearer "
                                                        + finalApiKey
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
                                                        .ofString(finalJson)
                                        )
                                        .build();

                        plugin.getLogger().info(
                                "Crystal Ville WebLink: sending request..."
                        );

                        // Send request.
                        HttpResponse<String> response =
                                http.send(
                                        request,
                                        HttpResponse.BodyHandlers.ofString()
                                );

                        String body =
                                response.body() == null
                                        ? ""
                                        : response.body();

                        int status =
                                response.statusCode();

                        plugin.getLogger().info(
                                "Crystal Ville WebLink: HTTP "
                                        + status
                        );

                        /*
                         * Do NOT log the API key or request JSON.
                         * Only log the returned response.
                         */
                        plugin.getLogger().info(
                                "Crystal Ville WebLink: response="
                                        + body
                        );

                        boolean success =
                                status >= 200
                                        && status < 300
                                        && body.contains(
                                                "\"success\":true"
                                        );

                        // Return to Minecraft's main thread.
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
                                    }
                                });

                    } catch (UnknownHostException e) {

                        logNetworkError(
                                "DNS/host resolution failed",
                                e
                        );

                        sendNetworkFailure(player);

                    } catch (ConnectException e) {

                        logNetworkError(
                                "Could not establish connection to Supabase",
                                e
                        );

                        sendNetworkFailure(player);

                    } catch (Exception e) {

                        logNetworkError(
                                "Unexpected WebLink connection error",
                                e
                        );

                        sendNetworkFailure(player);
                    }
                });

        return true;
    }

    /**
     * Logs detailed network information without exposing credentials.
     */
    private void logNetworkError(
            String description,
            Exception e
    ) {

        plugin.getLogger().severe(
                "Crystal Ville WebLink: "
                        + description
        );

        plugin.getLogger().severe(
                "Exception: "
                        + e.getClass().getName()
        );

        plugin.getLogger().severe(
                "Message: "
                        + String.valueOf(e.getMessage())
        );

        Throwable cause = e.getCause();

        if (cause != null) {

            plugin.getLogger().severe(
                    "Cause: "
                            + cause.getClass().getName()
            );

            plugin.getLogger().severe(
                    "Cause message: "
                            + String.valueOf(
                                    cause.getMessage()
                            )
            );
        }
    }

    /**
     * Sends a simple message to the player.
     * Detailed diagnostics remain in the server console.
     */
    private void sendNetworkFailure(Player player) {

        plugin.getServer()
                .getScheduler()
                .runTask(plugin, () -> {

                    if (!player.isOnline()) {
                        return;
                    }

                    player.sendMessage(
                            ChatColor.RED
                                    + "Could not reach the Crystal Ville website service."
                    );

                    player.sendMessage(
                            ChatColor.GRAY
                                    + "Please try again in a moment."
                    );
                });
    }

    /**
     * Escapes text for JSON.
     */
    private static String escape(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    /**
     * Extracts the error field from a Supabase JSON response.
     */
    private static String extractError(String body) {

        if (body == null || body.isEmpty()) {
            return "The website service returned an empty response.";
        }

        int errorIndex =
                body.indexOf("\"error\":");

        if (errorIndex >= 0) {

            int start =
                    body.indexOf(
                            '"',
                            errorIndex + 8
                    );

            if (start >= 0) {

                int end =
                        body.indexOf(
                                '"',
                                start + 1
                        );

                if (end > start) {

                    return body.substring(
                            start + 1,
                            end
                    );
                }
            }
        }

        int messageIndex =
                body.indexOf("\"message\":");

        if (messageIndex >= 0) {

            int start =
                    body.indexOf(
                            '"',
                            messageIndex + 10
                    );

            if (start >= 0) {

                int end =
                        body.indexOf(
                                '"',
                                start + 1
                        );

                if (end > start) {

                    return body.substring(
                            start + 1,
                            end
                    );
                }
            }
        }

        return "Invalid or already-used code.";
    }
  }
