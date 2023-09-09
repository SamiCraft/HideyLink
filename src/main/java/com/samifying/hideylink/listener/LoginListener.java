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
import net.luckperms.api.node.NodeEqualityPredicate;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class LoginListener implements Listener {

    private final HideyLink plugin;
    private final UserManager manager;
    private final HttpClient client;
    private final ObjectMapper mapper;

    public LoginListener(HideyLink plugin, LuckPerms perms) {
        this.plugin = plugin;
        this.manager = perms.getUserManager();

        // Registering http client
        this.client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        // Register json mapper
        this.mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @EventHandler
    public void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        try {
            UUID uuid = event.getUniqueId();

            // Skip if the player is banned
            if (plugin.getServer().getBannedPlayers().stream().anyMatch(p ->
                    p.getUniqueId().equals(uuid))) {
                return;
            }

            // Retrieving properties
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
                managePermission(uuid, event.getName(), model.isSupporter(), "group.supporter");
                managePermission(uuid, event.getName(), model.isModerator(), "group.moderator");

                // Saving data to cache
                plugin.getPlayers().put(uuid, model);
                plugin.getLogger().info("Player " + event.getName() + " authenticated as " + model.getName());
                return;
            }

            // Backend exception
            if (rsp.statusCode() == 500) {
                ErrorModel model = mapper.readValue(rsp.body(), ErrorModel.class);
                plugin.getLogger().info("Player " + event.getName() + " was rejected");
                throw new RuntimeException(model.getMessage());
            }

            // General exception
            throw new RuntimeException("Authorization failed");
        } catch (Exception e) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private void managePermission(UUID uuid, String name, boolean isAllowed, String group) {
        Logger logger = plugin.getLogger();

        CompletableFuture<User> future = manager.loadUser(uuid);
        future.thenAcceptAsync(user -> {
            Node groupNode = Node.builder(group).build();

            // Player has the group
            if (user.data().contains(groupNode, NodeEqualityPredicate.EXACT).asBoolean()) {
                if (!isAllowed) {
                    user.data().remove(Node.builder(group).build());
                    manager.saveUser(user);
                    logger.info("Group permission " + group + " was removed from " + name);
                }

                // Player is allowed and has the group
                return;
            }

            // Player is allowed but does not have the group
            if (isAllowed) {
                user.data().add(Node.builder(group).build());
                manager.saveUser(user);
                logger.info("Group permission " + group + " was added to " + name);
            }
        });
    }
}
