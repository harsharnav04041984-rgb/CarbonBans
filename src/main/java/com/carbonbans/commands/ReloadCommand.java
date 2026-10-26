package com.carbonbans.commands;

import com.carbonbans.CarbonBans;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ReloadCommand implements CommandExecutor {

    private final CarbonBans plugin;

    public ReloadCommand(CarbonBans plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("carbonbans.reload") && !sender.hasPermission("carbonbans.admin")) {
                sender.sendMessage(plugin.getMessageUtil().get("no-permission"));
                return true;
            }
            plugin.reloadConfig();
            plugin.getMessageUtil().load();
            sender.sendMessage(plugin.getMessageUtil().get("reload-success"));
            return true;
        }
        sender.sendMessage("§cUsage: /carbonbans reload");
        return true;
    }
}