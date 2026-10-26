package com.carbonbans.commands;

import com.carbonbans.CarbonBans;
import com.carbonbans.data.Punishment;
import com.carbonbans.data.PunishmentType;
import com.carbonbans.data.ResolvedPlayer;
import com.carbonbans.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class WarnCommand implements CommandExecutor {

    private final CarbonBans plugin;

    public WarnCommand(CarbonBans plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("carbonbans.warn") && !sender.hasPermission("carbonbans.admin")) {
            sender.sendMessage(plugin.getMessageUtil().get("no-permission"));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(plugin.getMessageUtil().get("usage-warn"));
            return true;
        }

        String targetName = args[0];
        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        plugin.getPlayerResolver().resolve(targetName).thenAccept(optional -> {
            if (optional.isEmpty()) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        sender.sendMessage(plugin.getMessageUtil().get("player-not-found")));
                return;
            }

            ResolvedPlayer resolved = optional.get();

            Punishment p = new Punishment();
            p.uuid = resolved.uuid;
            p.name = resolved.name;
            p.ip = null;
            p.ipPunishment = false;
            p.type = PunishmentType.WARN;
            p.reason = reason;
            p.staff = sender.getName();
            p.issuedAt = System.currentTimeMillis();
            p.expiresAt = -1;

            plugin.getPunishmentManager().addPunishment(p).thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
                Map<String, String> ph = new HashMap<>();
                ph.put("player", p.name);
                ph.put("reason", reason);
                ph.put("staff", sender.getName());

                sender.sendMessage(plugin.getMessageUtil().get("warn-success", ph));
                if (plugin.getConfig().getBoolean("broadcast.warn", true)) {
                    Bukkit.broadcastMessage(plugin.getMessageUtil().get("warn-broadcast", ph));
                }

                Player online = Bukkit.getPlayer(resolved.uuid);
                if (online != null) online.sendMessage(plugin.getMessageUtil().get("warn-received", ph));

                if (plugin.getConfig().getBoolean("warn-escalation.enabled", true)) {
                    checkEscalation(p, Bukkit.getOfflinePlayer(resolved.uuid));
                }
            }));
        });

        return true;
    }

    private void checkEscalation(Punishment p, OfflinePlayer target) {
        plugin.getPunishmentManager().countActiveWarns(p.uuid).thenAccept(count -> {
            String rule = plugin.getConfig().getString("warn-escalation.thresholds." + count, null);
            if (rule == null) return;

            String[] parts = rule.split(":");
            String type = parts[0];
            long duration = TimeUtil.parse(parts.length > 1 ? parts[1] : "perm");

            Punishment escalated = new Punishment();
            escalated.uuid = p.uuid;
            escalated.name = p.name;
            escalated.ip = null;
            escalated.ipPunishment = false;
            escalated.type = type.equalsIgnoreCase("ban") ? PunishmentType.BAN : PunishmentType.MUTE;
            escalated.reason = "Automatic: reached " + count + " warnings";
            escalated.staff = "CarbonBans";
            escalated.issuedAt = System.currentTimeMillis();
            escalated.expiresAt = duration == -1 ? -1 : System.currentTimeMillis() + duration;

            plugin.getPunishmentManager().addPunishment(escalated).thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
                Map<String, String> ph = new HashMap<>();
                ph.put("player", p.name);
                ph.put("count", String.valueOf(count));

                if (escalated.type == PunishmentType.BAN) {
                    Bukkit.broadcastMessage(plugin.getMessageUtil().get("warn-escalation-ban", ph));
                    Player onlineTarget = target.getPlayer();
                    if (onlineTarget != null) {
                        Map<String, String> banPh = new HashMap<>();
                        banPh.put("reason", escalated.reason);
                        banPh.put("duration", duration == -1 ? "Permanent" : TimeUtil.format(duration));
                        banPh.put("staff", "CarbonBans");
                        onlineTarget.kickPlayer(plugin.getMessageUtil().get("ban-screen", banPh));
                    }
                } else {
                    Bukkit.broadcastMessage(plugin.getMessageUtil().get("warn-escalation-mute", ph));
                }
            }));
        });
    }
}