package com.samifying.hideylink.listener;

import com.samifying.hideylink.HideyLink;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

@RequiredArgsConstructor
public class LogoutListener implements Listener {

    private final HideyLink plugin;

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getLogger().info("Removing cache for " + player.getName());

        // Removing player from cache
        plugin.getPlayers().remove(player.getUniqueId());
    }
}
