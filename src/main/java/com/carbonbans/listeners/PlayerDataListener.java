package com.carbonbans.listeners;

import com.carbonbans.CarbonBans;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerDataListener implements Listener {

    private final CarbonBans plugin;

    public PlayerDataListener(CarbonBans plugin) { this.plugin = plugin; }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        String ip = event.getPlayer().getAddress() != null
                ? event.getPlayer().getAddress().getAddress().getHostAddress()
                : null;
        if (ip == null) return;

        plugin.getDatabase().upsertPlayerData(
                event.getPlayer().getUniqueId(),
                event.getPlayer().getName(),
                ip
        );
    }
}