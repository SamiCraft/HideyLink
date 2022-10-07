package com.samifying.hideylink.listener;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.samifying.hideylink.HideyLink;
import com.samifying.hideylink.model.DataModel;
import com.samifying.hideylink.model.ErrorModel;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.node.Node;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class LoginListener implements Listener {

    private final HideyLink plugin;
    private final LuckPerms perms;
    private final HttpClient client;
    private final ObjectMapper mapper;

    public LoginListener(HideyLink plugin, LuckPerms perms) {
        this.plugin = plugin;
        this.perms = perms;

        // Registering http client
        this.client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        // Register json mapper
        this.mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        try {
            Player player = event.getPlayer();

            // Skip if the player is banned
            if (plugin.getServer().getBannedPlayers().stream().anyMatch(p ->
                    p.getUniqueId().equals(player.getUniqueId()))) {
                return;
            }

            // Retrieving properties
            String uuid = player.getUniqueId().toString();
            FileConfiguration config = plugin.getConfig();
            String guild = config.getString("auth.guild");
            String role = config.getString("auth.role");
            String moderator = config.getString("auth.moderator");
            String supporter = config.getString("auth.supporter");

            // HTTP Request to backend
            String url = "https://link.samifying.com/api/user/" + guild + "/" + role + "/" + uuid
                    + "?moderator=" + moderator + "&supporter=" + supporter;
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> rsp = client.send(req, HttpResponse.BodyHandlers.ofString());

            // Successful player login
            if (rsp.statusCode() == 200) {
                DataModel model = mapper.readValue(rsp.body(), DataModel.class);

                // Managing the permissions
                new Thread(() -> {
                    managePermission(player, model.isSupporter(), "group.supporter");
                    managePermission(player, model.isModerator(), "group.moderator");
                }, "PermissionManagerThread").start();

                // Saving data to cache
                plugin.getPlayers().put(player.getUniqueId(), model);
                plugin.getLogger().info("Player " + player.getName() + " authenticated as " + model.getName());
                return;
            }

            // Backend exception
            if (rsp.statusCode() == 500) {
                ErrorModel model = mapper.readValue(rsp.body(), ErrorModel.class);
                plugin.getLogger().info("Player " + player.getName() + " was rejected");
                throw new RuntimeException(model.getMessage());
            }

            // General exception
            throw new RuntimeException("Authorization failed");
        } catch (Exception e) {
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER,
                    e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private void managePermission(Player player, boolean isAllowed, String group) {
        Logger logger = plugin.getLogger();
        UserManager manager = perms.getUserManager();

        // Player has the group
        if (player.hasPermission(group)) {
            if (!isAllowed) {
                // Player is not allowed to have that permission
                CompletableFuture<User> future = manager.loadUser(player.getUniqueId());
                future.thenAcceptAsync(user -> {
                    user.data().remove(Node.builder(group).build());
                    manager.saveUser(user);
                });
                logger.info("Group permission " + group + " was removed from " + player.getName());
            }

            // Player is allowed and has the group
            return;
        }

        // Player is allowed but does not have the group
        if (isAllowed) {
            CompletableFuture<User> future = manager.loadUser(player.getUniqueId());
            future.thenAcceptAsync(user -> {
                user.data().add(Node.builder(group).build());
                manager.saveUser(user);
            });
            logger.info("Group permission " + group + " was added to " + player.getName());
        }
    }
}
