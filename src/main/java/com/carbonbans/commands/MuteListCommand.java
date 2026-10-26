package com.carbonbans.commands;

import com.carbonbans.CarbonBans;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MuteListCommand implements CommandExecutor {

    private final CarbonBans plugin;

    public MuteListCommand(CarbonBans plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }
        if (!player.hasPermission("carbonbans.history") && !player.hasPermission("carbonbans.admin")) {
            player.sendMessage(plugin.getMessageUtil().get("no-permission"));
            return true;
        }
        plugin.getGuiManager().openMuteList(player, 0);
        return true;
    }
}