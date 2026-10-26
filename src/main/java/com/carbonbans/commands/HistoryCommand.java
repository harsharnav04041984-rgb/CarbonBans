package com.carbonbans.commands;

import com.carbonbans.CarbonBans;
import com.carbonbans.data.Punishment;
import com.carbonbans.data.ResolvedPlayer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class HistoryCommand implements CommandExecutor {

    private final CarbonBans plugin;

    public HistoryCommand(CarbonBans plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("carbonbans.history") && !sender.hasPermission("carbonbans.admin")) {
            sender.sendMessage(plugin.getMessageUtil().get("no-permission"));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage("§cUsage: /history <player>");
            return true;
        }

        String targetName = args[0];

        plugin.getPlayerResolver().resolve(targetName).thenAccept(optional -> {
            if (optional.isEmpty()) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        sender.sendMessage(plugin.getMessageUtil().get("player-not-found")));
                return;
            }

            ResolvedPlayer resolved = optional.get();
            plugin.getPunishmentManager().getHistory(resolved.uuid).thenAccept(list ->
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        Map<String, String> headerPh = new HashMap<>();
                        headerPh.put("player", resolved.name);
                        sender.sendMessage(plugin.getMessageUtil().get("history-header", headerPh));

                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                        for (Punishment p : list) {
                            Map<String, String> ph = new HashMap<>();
                            ph.put("type", p.type.name());
                            ph.put("reason", p.reason);
                            ph.put("staff", p.staff);
                            ph.put("date", sdf.format(new Date(p.issuedAt)));
                            sender.sendMessage(plugin.getMessageUtil().get("history-entry", ph));
                        }
                    }));
        });

        return true;
    }
}