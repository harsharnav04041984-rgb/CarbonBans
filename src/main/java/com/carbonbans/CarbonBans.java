package com.carbonbans;

import com.carbonbans.commands.*;
import com.carbonbans.data.Database;
import com.carbonbans.gui.GuiListener;
import com.carbonbans.gui.GuiManager;
import com.carbonbans.listeners.ChatListener;
import com.carbonbans.listeners.ConnectionListener;
import com.carbonbans.listeners.PlayerDataListener;
import com.carbonbans.manager.PunishmentManager;
import com.carbonbans.util.MessageUtil;
import com.carbonbans.util.PlayerResolver;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class CarbonBans extends JavaPlugin {

    private static CarbonBans instance;
    private Database database;
    private PunishmentManager punishmentManager;
    private MessageUtil messageUtil;
    private GuiManager guiManager;
    private PlayerResolver playerResolver;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        File messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) saveResource("messages.yml", false);

        database = new Database(this);
        database.connect();

        messageUtil = new MessageUtil(this);
        punishmentManager = new PunishmentManager(this, database);
        guiManager = new GuiManager(this);
        playerResolver = new PlayerResolver(this);

        getCommand("ban").setExecutor(new BanCommand(this));
        getCommand("tempban").setExecutor(new BanCommand(this));
        getCommand("ipban").setExecutor(new BanCommand(this));
        getCommand("mute").setExecutor(new MuteCommand(this));
        getCommand("tempmute").setExecutor(new MuteCommand(this));
        getCommand("muteip").setExecutor(new MuteCommand(this));
        getCommand("kick").setExecutor(new KickCommand(this));
        getCommand("warn").setExecutor(new WarnCommand(this));
        getCommand("unban").setExecutor(new UnbanCommand(this));
        getCommand("unmute").setExecutor(new UnmuteCommand(this));
        getCommand("unwarn").setExecutor(new UnwarnCommand(this));
        getCommand("history").setExecutor(new HistoryCommand(this));
        getCommand("check").setExecutor(new CheckCommand(this));
        getCommand("carbonbans").setExecutor(new ReloadCommand(this));
        getCommand("banlist").setExecutor(new BanListCommand(this));
        getCommand("mutelist").setExecutor(new MuteListCommand(this));
        getCommand("alts").setExecutor(new AltsCommand(this));

        Bukkit.getPluginManager().registerEvents(new ConnectionListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ChatListener(this), this);
        Bukkit.getPluginManager().registerEvents(new GuiListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerDataListener(this), this);

        getLogger().info("CarbonBans has been enabled!");
    }

    @Override
    public void onDisable() {
        if (database != null) database.close();
    }

    public static CarbonBans getInstance() { return instance; }
    public Database getDatabase() { return database; }
    public PunishmentManager getPunishmentManager() { return punishmentManager; }
    public MessageUtil getMessageUtil() { return messageUtil; }
    public GuiManager getGuiManager() { return guiManager; }
    public PlayerResolver getPlayerResolver() { return playerResolver; }
}