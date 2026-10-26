package com.carbonbans.gui;

import com.carbonbans.CarbonBans;
import com.carbonbans.data.PunishmentType;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public class GuiListener implements Listener {

    private final CarbonBans plugin;

    public GuiListener(CarbonBans plugin) { this.plugin = plugin; }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof CarbonGuiHolder holder)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;

        String displayName = clicked.getItemMeta().hasDisplayName() ? clicked.getItemMeta().getDisplayName() : "";

        // Navigation
        if (displayName.contains("Next Page")) {
            openGui(player, holder, holder.getPage() + 1);
            return;
        }
        if (displayName.contains("Previous Page")) {
            openGui(player, holder, holder.getPage() - 1);
            return;
        }
        if (displayName.contains("Close")) {
            player.closeInventory();
            return;
        }

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;

        String uuidStr = meta.getPersistentDataContainer().get(plugin.getGuiManager().KEY_UUID, PersistentDataType.STRING);
        String action = meta.getPersistentDataContainer().get(plugin.getGuiManager().KEY_ACTION, PersistentDataType.STRING);
        if (uuidStr == null || uuidStr.isEmpty() || action == null) return;

        UUID targetUuid = UUID.fromString(uuidStr);
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetUuid);

        switch (action) {
            case "unban" -> {
                if (event.getClick() == ClickType.RIGHT) {
                    if (!player.hasPermission("carbonbans.unban") && !player.hasPermission("carbonbans.admin")) {
                        player.sendMessage(plugin.getMessageUtil().get("no-permission"));
                        return;
                    }
                    plugin.getPunishmentManager().unpunish(targetUuid, PunishmentType.BAN).thenRun(() ->
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                player.sendMessage("§aUnbanned " + target.getName());
                                plugin.getGuiManager().openBanList(player, holder.getPage());
                            }));
                }
            }
            case "unmute" -> {
                if (event.getClick() == ClickType.RIGHT) {
                    if (!player.hasPermission("carbonbans.unmute") && !player.hasPermission("carbonbans.admin")) {
                        player.sendMessage(plugin.getMessageUtil().get("no-permission"));
                        return;
                    }
                    plugin.getPunishmentManager().unpunish(targetUuid, PunishmentType.MUTE).thenRun(() ->
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                player.sendMessage("§aUnmuted " + target.getName());
                                plugin.getGuiManager().openMuteList(player, holder.getPage());
                            }));
                }
            }
            case "history" -> {
                player.closeInventory();
                player.performCommand("history " + target.getName());
            }
        }
    }

    private void openGui(Player player, CarbonGuiHolder holder, int page) {
        switch (holder.getType()) {
            case BAN_LIST -> plugin.getGuiManager().openBanList(player, page);
            case MUTE_LIST -> plugin.getGuiManager().openMuteList(player, page);
            case ALTS -> {
                UUID uuid = (UUID) holder.getContext();
                plugin.getGuiManager().openAlts(player, Bukkit.getOfflinePlayer(uuid), page);
            }
        }
    }
}