package com.carbonbans.data;

import com.carbonbans.CarbonBans;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Database {

    private final CarbonBans plugin;
    private Connection connection;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public Database(CarbonBans plugin) {
        this.plugin = plugin;
    }

    public void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            File folder = plugin.getDataFolder();
            if (!folder.exists()) folder.mkdirs();
            connection = DriverManager.getConnection("jdbc:sqlite:" + new File(folder, "data.db").getAbsolutePath());
            createTables();
            migrateSchema();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createTables() throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS punishments (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "uuid TEXT," +
                    "name TEXT," +
                    "ip TEXT," +
                    "is_ip_punishment INTEGER DEFAULT 0," +
                    "type TEXT," +
                    "reason TEXT," +
                    "staff TEXT," +
                    "issued_at INTEGER," +
                    "expires_at INTEGER," +
                    "active INTEGER)");

            st.execute("CREATE TABLE IF NOT EXISTS player_ips (" +
                    "uuid TEXT PRIMARY KEY," +
                    "name TEXT," +
                    "ip TEXT," +
                    "last_seen INTEGER)");
        }
    }

    // Safely adds the is_ip_punishment column if upgrading from an old database
    private void migrateSchema() {
        try (Statement st = connection.createStatement()) {
            st.execute("ALTER TABLE punishments ADD COLUMN is_ip_punishment INTEGER DEFAULT 0");
        } catch (SQLException ignored) {
            // Column already exists, safe to ignore
        }
    }

    public Connection getConnection() { return connection; }
    public ExecutorService getExecutor() { return executor; }

    public void close() {
        executor.shutdown();
        try { if (connection != null) connection.close(); } catch (Exception ignored) {}
    }

    // ---------------- Player IP Tracking ----------------

    public CompletableFuture<Void> upsertPlayerData(UUID uuid, String name, String ip) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO player_ips (uuid,name,ip,last_seen) VALUES (?,?,?,?) " +
                    "ON CONFLICT(uuid) DO UPDATE SET name=excluded.name, ip=excluded.ip, last_seen=excluded.last_seen";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, name);
                ps.setString(3, ip);
                ps.setLong(4, System.currentTimeMillis());
                ps.executeUpdate();
            } catch (SQLException e) { e.printStackTrace(); }
        }, executor);
    }

    public CompletableFuture<String> getIp(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT ip FROM player_ips WHERE uuid=?";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getString("ip");
                }
            } catch (SQLException e) { e.printStackTrace(); }
            return null;
        }, executor);
    }

    public CompletableFuture<List<AltEntry>> getAltAccounts(String ip, UUID exclude) {
        return CompletableFuture.supplyAsync(() -> {
            List<AltEntry> list = new ArrayList<>();
            String sql = "SELECT * FROM player_ips WHERE ip=? AND uuid!=?";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, ip);
                ps.setString(2, exclude.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        AltEntry entry = new AltEntry();
                        entry.uuid = UUID.fromString(rs.getString("uuid"));
                        entry.name = rs.getString("name");
                        entry.ip = rs.getString("ip");
                        entry.lastSeen = rs.getLong("last_seen");
                        list.add(entry);
                    }
                }
            } catch (SQLException e) { e.printStackTrace(); }
            return list;
        }, executor);
    }

    public CompletableFuture<ResolvedPlayer> getPlayerByName(String name) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT uuid, name FROM player_ips WHERE LOWER(name)=LOWER(?) ORDER BY last_seen DESC LIMIT 1";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return new ResolvedPlayer(UUID.fromString(rs.getString("uuid")), rs.getString("name"));
                    }
                }
            } catch (SQLException e) { e.printStackTrace(); }
            return null;
        }, executor);
    }
}