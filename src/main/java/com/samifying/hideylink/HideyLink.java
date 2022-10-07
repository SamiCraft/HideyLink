package com.samifying.hideylink;

import com.samifying.hideylink.listener.LoginListener;
import com.samifying.hideylink.listener.LogoutListener;
import com.samifying.hideylink.model.DataModel;
import lombok.Getter;
import net.luckperms.api.LuckPerms;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
public final class HideyLink extends JavaPlugin {

    private final Map<UUID, DataModel> players = new HashMap<>();

    @Override
    public void onEnable() {
        // Saving default config
        saveDefaultConfig();

        // Plugin startup logic
        PluginManager pm = getServer().getPluginManager();
        ServicesManager sm = getServer().getServicesManager();
        RegisteredServiceProvider<LuckPerms> provider = sm.getRegistration(LuckPerms.class);

        // Retrieving LuckPerms instance
        if (pm.getPlugin("LuckPerms") == null || provider == null) {
            getLogger().severe("LuckPerms not found");
            pm.disablePlugin(this);
            return;
        }

        // Registering listener
        getLogger().info("Registering event listeners");
        pm.registerEvents(new LoginListener(this, provider.getProvider()), this);
        pm.registerEvents(new LogoutListener(this), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("Clearing out cache");
        players.clear();
    }
}
