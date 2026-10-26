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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class CheckCommand implements CommandExecutor {

    private final CarbonBans plugin;

    public CheckCommand(CarbonBans plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("carbonbans.history") && !sender.hasPermission("carbonbans.admin")) {
            sender.sendMessage(plugin.getMessageUtil().get("no-permission"));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage("§cUsage: /check <player>");
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
            String ip = plugin.getPunishmentManager().resolveIp(Bukkit.getOfflinePlayer(resolved.uuid));

            CompletableFuture<Optional<Punishment>> banFuture = plugin.getPunishmentManager().getActivePunishment(resolved.uuid, ip, PunishmentType.BAN);
            CompletableFuture<Optional<Punishment>> muteFuture = plugin.getPunishmentManager().getActivePunishment(resolved.uuid, ip, PunishmentType.MUTE);
            CompletableFuture<Integer> warnFuture = plugin.getPunishmentManager().countActiveWarns(resolved.uuid);

            CompletableFuture.allOf(banFuture, muteFuture, warnFuture).thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
                Map<String, String> ph = new HashMap<>();
                ph.put("player", resolved.name);
                sender.sendMessage(plugin.getMessageUtil().get("check-header", ph));

                Optional<Punishment> ban = banFuture.join();
                Optional<Punishment> mute = muteFuture.join();
                int warns = warnFuture.join();
                boolean any = false;

                if (ban.isPresent()) {
                    any = true;
                    String left = ban.get().expiresAt == -1 ? "Permanent" : TimeUtil.format(ban.get().expiresAt - System.currentTimeMillis());
                    sender.sendMessage("§c- Banned: " + ban.get().reason + " (" + left + " left)");
                }
                if (mute.isPresent()) {
                    any = true;
                    String left = mute.get().expiresAt == -1 ? "Permanent" : TimeUtil.format(mute.get().expiresAt - System.currentTimeMillis());
                    sender.sendMessage("§c- Muted: " + mute.get().reason + " (" + left + " left)");
                }
                if (warns > 0) {
                    any = true;
                    sender.sendMessage("§e- Active Warnings: " + warns);
                }
                if (!any) sender.sendMessage(plugin.getMessageUtil().get("check-none"));
            }));
        });

        return true;
    }
}