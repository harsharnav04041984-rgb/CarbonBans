package com.carbonbans.listeners;

import com.carbonbans.CarbonBans;
import com.carbonbans.data.Punishment;
import com.carbonbans.data.PunishmentType;
import com.carbonbans.util.TimeUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ChatListener implements Listener {

    private final CarbonBans plugin;

    public ChatListener(CarbonBans plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String ip = player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : null;

        Optional<Punishment> mute = plugin.getPunishmentManager()
                .getActivePunishment(player.getUniqueId(), ip, PunishmentType.MUTE).join();

        if (mute.isPresent()) {
            event.setCancelled(true);
            Punishment p = mute.get();
            Map<String, String> ph = new HashMap<>();
            ph.put("reason", p.reason);
            ph.put("time_left", p.expiresAt == -1 ? "Permanent" : TimeUtil.format(p.expiresAt - System.currentTimeMillis()));
            player.sendMessage(plugin.getMessageUtil().get("muted-chat", ph));
        }
    }
}