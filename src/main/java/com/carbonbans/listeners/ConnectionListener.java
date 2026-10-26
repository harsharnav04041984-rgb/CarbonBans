package com.carbonbans.listeners;

import com.carbonbans.CarbonBans;
import com.carbonbans.data.Punishment;
import com.carbonbans.data.PunishmentType;
import com.carbonbans.util.TimeUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ConnectionListener implements Listener {

    private final CarbonBans plugin;

    public ConnectionListener(CarbonBans plugin) { this.plugin = plugin; }

    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        UUID uuid = event.getUniqueId();
        String ip = event.getAddress().getHostAddress();

        Optional<Punishment> ban = plugin.getPunishmentManager()
                .getActivePunishment(uuid, ip, PunishmentType.BAN).join();

        if (ban.isPresent()) {
            Punishment p = ban.get();
            Map<String, String> ph = new HashMap<>();
            ph.put("reason", p.reason);
            ph.put("duration", p.expiresAt == -1 ? "Permanent" : TimeUtil.format(p.expiresAt - System.currentTimeMillis()));
            ph.put("staff", p.staff);
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, plugin.getMessageUtil().get("ban-screen", ph));
        }
    }
}