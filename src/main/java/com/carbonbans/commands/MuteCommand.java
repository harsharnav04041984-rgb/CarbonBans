package com.carbonbans.commands;

import com.carbonbans.CarbonBans;
import com.carbonbans.data.Punishment;
import com.carbonbans.data.PunishmentType;
import com.carbonbans.data.ResolvedPlayer;
import com.carbonbans.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class MuteCommand implements CommandExecutor {

    private final CarbonBans plugin;

    public MuteCommand(CarbonBans plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        boolean ipMode = label.equalsIgnoreCase("muteip") || label.equalsIgnoreCase("cmuteip");
        boolean tempMode = label.equalsIgnoreCase("tempmute") || label.equalsIgnoreCase("ctempmute");

        String perm = ipMode ? "carbonbans.muteip" : tempMode ? "carbonbans.tempmute" : "carbonbans.mute";
        if (!sender.hasPermission(perm) && !sender.hasPermission("carbonbans.admin")) {
            sender.sendMessage(plugin.getMessageUtil().get("no-permission"));
            return true;
        }

        if (args.length < (tempMode ? 2 : 1)) {
            sender.sendMessage(plugin.getMessageUtil().get("usage-mute"));
            return true;
        }

        String targetName = args[0];
        long duration = -1;
        int reasonStart = 1;

        if (tempMode) {
            duration = TimeUtil.parse(args[1]);
            if (duration == -2) {
                sender.sendMessage(plugin.getMessageUtil().get("invalid-duration"));
                return true;
            }
            reasonStart = 2;
        } else if (args.length > 1) {
            long parsed = TimeUtil.parse(args[1]);
            if (parsed != -2) {
                duration = parsed;
                reasonStart = 2;
            }
        }

        String reason = args.length > reasonStart
                ? String.join(" ", Arrays.copyOfRange(args, reasonStart, args.length))
                : plugin.getConfig().getString("default-reason", "No reason specified");

        long finalDuration = duration;

        plugin.getPlayerResolver().resolve(targetName).thenAccept(optional -> {
            if (optional.isEmpty()) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        sender.sendMessage(plugin.getMessageUtil().get("player-not-found")));
                return;
            }

            ResolvedPlayer resolved = optional.get();
            Player onlineTarget = Bukkit.getPlayer(resolved.uuid);
            String ip = (onlineTarget != null && onlineTarget.getAddress() != null)
                    ? onlineTarget.getAddress().getAddress().getHostAddress()
                    : null;

            if (ipMode && ip == null) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        sender.sendMessage("§cTarget must be online to resolve their IP for an IP mute."));
                return;
            }

            Punishment p = new Punishment();
            p.uuid = resolved.uuid;
            p.name = resolved.name;
            p.ip = ip;
            p.ipPunishment = ipMode;
            p.type = PunishmentType.MUTE;
            p.reason = reason;
            p.staff = sender.getName();
            p.issuedAt = System.currentTimeMillis();
            p.expiresAt = finalDuration == -1 ? -1 : System.currentTimeMillis() + finalDuration;

            String durationStr = finalDuration == -1 ? "Permanent" : TimeUtil.format(finalDuration);

            plugin.getPunishmentManager().addPunishment(p).thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
                Map<String, String> ph = new HashMap<>();
                ph.put("player", p.name);
                ph.put("reason", reason);
                ph.put("duration", durationStr);
                ph.put("staff", sender.getName());

                sender.sendMessage(plugin.getMessageUtil().get("mute-success", ph));

                if (plugin.getConfig().getBoolean("broadcast.mute", true)) {
                    Bukkit.broadcastMessage(plugin.getMessageUtil().get("mute-broadcast", ph));
                }

                Player stillOnline = Bukkit.getPlayer(resolved.uuid);
                if (stillOnline != null) {
                    Map<String, String> chatPh = new HashMap<>(ph);
                    chatPh.put("time_left", durationStr);
                    stillOnline.sendMessage(plugin.getMessageUtil().get("muted-chat", chatPh));
                }
            }));
        });

        return true;
    }
}