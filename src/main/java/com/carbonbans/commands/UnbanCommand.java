package com.carbonbans.commands;

import com.carbonbans.CarbonBans;
import com.carbonbans.data.PunishmentType;
import com.carbonbans.data.ResolvedPlayer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.HashMap;
import java.util.Map;

public class UnbanCommand implements CommandExecutor {

    private final CarbonBans plugin;

    public UnbanCommand(CarbonBans plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("carbonbans.unban") && !sender.hasPermission("carbonbans.admin")) {
            sender.sendMessage(plugin.getMessageUtil().get("no-permission"));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage("§cUsage: /unban <player>");
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
            plugin.getPunishmentManager().unpunish(resolved.uuid, PunishmentType.BAN).thenAccept(success ->
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        Map<String, String> ph = new HashMap<>();
                        ph.put("player", resolved.name);
                        sender.sendMessage(success
                                ? plugin.getMessageUtil().get("unban-success", ph)
                                : plugin.getMessageUtil().get("not-banned"));
                    }));
        });

        return true;
    }
}