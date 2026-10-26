package com.carbonbans.commands;

import com.carbonbans.CarbonBans;
import com.carbonbans.data.ResolvedPlayer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AltsCommand implements CommandExecutor {

    private final CarbonBans plugin;

    public AltsCommand(CarbonBans plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }
        if (!player.hasPermission("carbonbans.alts") && !player.hasPermission("carbonbans.admin")) {
            player.sendMessage(plugin.getMessageUtil().get("no-permission"));
            return true;
        }
        if (args.length < 1) {
            player.sendMessage("§cUsage: /alts <player>");
            return true;
        }

        String targetName = args[0];

        plugin.getPlayerResolver().resolve(targetName).thenAccept(optional -> {
            if (optional.isEmpty()) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        player.sendMessage(plugin.getMessageUtil().get("player-not-found")));
                return;
            }

            ResolvedPlayer resolved = optional.get();
            Bukkit.getScheduler().runTask(plugin, () ->
                    plugin.getGuiManager().openAlts(player, Bukkit.getOfflinePlayer(resolved.uuid), 0));
        });

        return true;
    }
}