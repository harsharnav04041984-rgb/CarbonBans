package com.carbonbans.gui;

import com.carbonbans.CarbonBans;
import com.carbonbans.data.AltEntry;
import com.carbonbans.data.Punishment;
import com.carbonbans.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.text.SimpleDateFormat;
import java.util.*;

public class GuiManager {

    private final CarbonBans plugin;
    public final NamespacedKey KEY_UUID;
    public final NamespacedKey KEY_ACTION;

    private static final int PAGE_SIZE = 45;

    public GuiManager(CarbonBans plugin) {
        this.plugin = plugin;
        this.KEY_UUID = new NamespacedKey(plugin, "target_uuid");
        this.KEY_ACTION = new NamespacedKey(plugin, "action");
    }

    // ---------------- BAN LIST ----------------
    public void openBanList(Player viewer, int page) {
        plugin.getPunishmentManager().getActiveBans().thenAccept(list ->
                Bukkit.getScheduler().runTask(plugin, () -> showPunishmentList(viewer, GuiType.BAN_LIST, list, page)));
    }

    // ---------------- MUTE LIST ----------------
    public void openMuteList(Player viewer, int page) {
        plugin.getPunishmentManager().getActiveMutes().thenAccept(list ->
                Bukkit.getScheduler().runTask(plugin, () -> showPunishmentList(viewer, GuiType.MUTE_LIST, list, page)));
    }

    private void showPunishmentList(Player viewer, GuiType type, List<Punishment> list, int page) {
        int maxPage = Math.max(0, (list.size() - 1) / PAGE_SIZE);
        page = Math.min(Math.max(page, 0), maxPage);

        String title = (type == GuiType.BAN_LIST ? "Ban List" : "Mute List") + " (Page " + (page + 1) + "/" + (maxPage + 1) + ")";
        CarbonGuiHolder holder = new CarbonGuiHolder(type, page, null);
        Inventory inv = Bukkit.createInventory(holder, 54, ChatColor.DARK_RED + title);
        holder.setInventory(inv);

        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, list.size());
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

        for (int i = start; i < end; i++) {
            Punishment p = list.get(i);
            OfflinePlayer target = p.uuid != null ? Bukkit.getOfflinePlayer(p.uuid) : null;

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(target);
            meta.setDisplayName(ChatColor.YELLOW + (p.name != null ? p.name : "Unknown"));

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Reason: " + ChatColor.WHITE + p.reason);
            lore.add(ChatColor.GRAY + "Staff: " + ChatColor.WHITE + p.staff);
            lore.add(ChatColor.GRAY + "Issued: " + ChatColor.WHITE + sdf.format(new Date(p.issuedAt)));
            lore.add(ChatColor.GRAY + "Expires: " + ChatColor.WHITE +
                    (p.expiresAt == -1 ? "Permanent" : TimeUtil.format(p.expiresAt - System.currentTimeMillis()) + " left"));
            lore.add("");
            lore.add(ChatColor.GREEN + "Right-click to " + (type == GuiType.BAN_LIST ? "unban" : "unmute"));
            meta.setLore(lore);

            meta.getPersistentDataContainer().set(KEY_UUID, PersistentDataType.STRING, p.uuid != null ? p.uuid.toString() : "");
            meta.getPersistentDataContainer().set(KEY_ACTION, PersistentDataType.STRING, type == GuiType.BAN_LIST ? "unban" : "unmute");

            head.setItemMeta(meta);
            inv.setItem(i - start, head);
        }

        addNavigation(inv, page, maxPage);
        viewer.openInventory(inv);
    }

    // ---------------- ALTS ----------------
    public void openAlts(Player viewer, OfflinePlayer target, int page) {
        plugin.getDatabase().getIp(target.getUniqueId()).thenCompose(ip -> {
            if (ip == null) return java.util.concurrent.CompletableFuture.completedFuture(new ArrayList<AltEntry>());
            return plugin.getDatabase().getAltAccounts(ip, target.getUniqueId());
        }).thenAccept(alts -> Bukkit.getScheduler().runTask(plugin, () -> showAlts(viewer, target, alts, page)));
    }

    private void showAlts(Player viewer, OfflinePlayer target, List<AltEntry> alts, int page) {
        int maxPage = Math.max(0, (alts.size() - 1) / PAGE_SIZE);
        page = Math.min(Math.max(page, 0), maxPage);

        String title = "Alts of " + target.getName() + " (Page " + (page + 1) + "/" + (maxPage + 1) + ")";
        CarbonGuiHolder holder = new CarbonGuiHolder(GuiType.ALTS, page, target.getUniqueId());
        Inventory inv = Bukkit.createInventory(holder, 54, ChatColor.DARK_PURPLE + title);
        holder.setInventory(inv);

        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, alts.size());
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

        if (alts.isEmpty()) {
            ItemStack none = new ItemStack(Material.BARRIER);
            ItemMeta m = none.getItemMeta();
            m.setDisplayName(ChatColor.GREEN + "No alts found!");
            none.setItemMeta(m);
            inv.setItem(22, none);
        }

        for (int i = start; i < end; i++) {
            AltEntry alt = alts.get(i);
            OfflinePlayer altPlayer = Bukkit.getOfflinePlayer(alt.uuid);

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(altPlayer);
            meta.setDisplayName(ChatColor.AQUA + alt.name);

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Shared IP: " + ChatColor.WHITE + alt.ip);
            lore.add(ChatColor.GRAY + "Last Seen: " + ChatColor.WHITE + sdf.format(new Date(alt.lastSeen)));
            lore.add("");
            lore.add(ChatColor.YELLOW + "Click to view history");
            meta.setLore(lore);

            meta.getPersistentDataContainer().set(KEY_UUID, PersistentDataType.STRING, alt.uuid.toString());
            meta.getPersistentDataContainer().set(KEY_ACTION, PersistentDataType.STRING, "history");

            head.setItemMeta(meta);
            inv.setItem(i - start, head);
        }

        addNavigation(inv, page, maxPage);
        viewer.openInventory(inv);
    }

    private void addNavigation(Inventory inv, int page, int maxPage) {
        if (page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta m = prev.getItemMeta();
            m.setDisplayName(ChatColor.GREEN + "Previous Page");
            prev.setItemMeta(m);
            inv.setItem(45, prev);
        }
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta cm = close.getItemMeta();
        cm.setDisplayName(ChatColor.RED + "Close");
        close.setItemMeta(cm);
        inv.setItem(49, close);

        if (page < maxPage) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta m = next.getItemMeta();
            m.setDisplayName(ChatColor.GREEN + "Next Page");
            next.setItemMeta(m);
            inv.setItem(53, next);
        }
    }
}