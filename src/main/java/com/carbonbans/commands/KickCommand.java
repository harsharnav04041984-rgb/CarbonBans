package com.carbonbans.commands;

import com.carbonbans.CarbonBans;
import com.carbonbans.data.Punishment;
import com.carbonbans.data.PunishmentType;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class KickCommand implements CommandExecutor {

    private final CarbonBans plugin;

    public KickCommand(CarbonBans plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("carbonbans.kick") && !sender.hasPermission("carbonbans.admin")) {
            sender.sendMessage(plugin.getMessageUtil().get("no-permission"));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(plugin.getMessageUtil().get("usage-kick"));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(plugin.getMessageUtil().get("player-not-found"));
            return true;
        }

        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        Punishment p = new Punishment();
        p.uuid = target.getUniqueId();
        p.name = target.getName();
        p.ip = plugin.getPunishmentManager().resolveIp(target);
        p.type = PunishmentType.KICK;
        p.reason = reason;
        p.staff = sender.getName();
        p.issuedAt = System.currentTimeMillis();
        p.expiresAt = -1;

        plugin.getPunishmentManager().addPunishment(p);

        Map<String, String> ph = new HashMap<>();
        ph.put("player", target.getName());
        ph.put("reason", reason);
        ph.put("staff", sender.getName());

        target.kickPlayer(plugin.getMessageUtil().get("kick-screen", ph));
        sender.sendMessage(plugin.getMessageUtil().get("kick-success", ph));

        if (plugin.getConfig().getBoolean("broadcast.kick", true)) {
            Bukkit.broadcastMessage(plugin.getMessageUtil().get("kick-broadcast", ph));
        }
        return true;
    }
}