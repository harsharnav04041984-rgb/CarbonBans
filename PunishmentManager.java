package com.carbonbans.manager;

import com.carbonbans.CarbonBans;
import com.carbonbans.data.Database;
import com.carbonbans.data.Punishment;
import com.carbonbans.data.PunishmentType;
import org.bukkit.OfflinePlayer;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class PunishmentManager {

    private final CarbonBans plugin;
    private final Database database;

    public PunishmentManager(CarbonBans plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
    }

    public CompletableFuture<Void> addPunishment(Punishment p) {
        return CompletableFuture.runAsync(() -> {
            try {
                if ((p.type == PunishmentType.BAN || p.type == PunishmentType.MUTE) && p.uuid != null) {
                    try (PreparedStatement deactivate = database.getConnection().prepareStatement(
                            "UPDATE punishments SET active=0 WHERE uuid=? AND type=? AND active=1")) {
                        deactivate.setString(1, p.uuid.toString());
                        deactivate.setString(2, p.type.name());
                        deactivate.executeUpdate();
                    }
                }
                String sql = "INSERT INTO punishments (uuid,name,ip,is_ip_punishment,type,reason,staff,issued_at,expires_at,active) " +
                        "VALUES (?,?,?,?,?,?,?,?,?,1)";
                try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
                    ps.setString(1, p.uuid != null ? p.uuid.toString() : null);
                    ps.setString(2, p.name);
                    ps.setString(3, p.ip);
                    ps.setInt(4, p.ipPunishment ? 1 : 0);
                    ps.setString(5, p.type.name());
                    ps.setString(6, p.reason);
                    ps.setString(7, p.staff);
                    ps.setLong(8, p.issuedAt);
                    ps.setLong(9, p.expiresAt);
                    ps.executeUpdate();
                }
            } catch (SQLException e) { e.printStackTrace(); }
        }, database.getExecutor());
    }

    /**
     * Only matches by IP if the stored punishment was explicitly an IP-type punishment
     * (/ipban or /muteip). Regular bans/mutes match strictly by UUID.
     */
    public CompletableFuture<Optional<Punishment>> getActivePunishment(UUID uuid, String ip, PunishmentType type) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM punishments WHERE type=? AND active=1 " +
                    "AND (uuid=? OR (is_ip_punishment=1 AND ip IS NOT NULL AND ip=?)) " +
                    "AND (expires_at=-1 OR expires_at>?) ORDER BY id DESC LIMIT 1";
            try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
                ps.setString(1, type.name());
                ps.setString(2, uuid != null ? uuid.toString() : "");
                ps.setString(3, ip != null ? ip : "");
                ps.setLong(4, System.currentTimeMillis());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return Optional.of(map(rs));
                }
            } catch (SQLException e) { e.printStackTrace(); }
            return Optional.empty();
        }, database.getExecutor());
    }

    public CompletableFuture<Boolean> unpunish(UUID uuid, PunishmentType type) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "UPDATE punishments SET active=0 WHERE uuid=? AND type=? AND active=1";
            try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, type.name());
                return ps.executeUpdate() > 0;
            } catch (SQLException e) { e.printStackTrace(); return false; }
        }, database.getExecutor());
    }

    public CompletableFuture<Boolean> clearWarns(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "UPDATE punishments SET active=0 WHERE uuid=? AND type='WARN' AND active=1";
            try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                return ps.executeUpdate() > 0;
            } catch (SQLException e) { e.printStackTrace(); return false; }
        }, database.getExecutor());
    }

    public CompletableFuture<List<Punishment>> getHistory(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            List<Punishment> list = new ArrayList<>();
            String sql = "SELECT * FROM punishments WHERE uuid=? ORDER BY issued_at DESC";
            try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) list.add(map(rs));
                }
            } catch (SQLException e) { e.printStackTrace(); }
            return list;
        }, database.getExecutor());
    }

    public CompletableFuture<Integer> countActiveWarns(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT COUNT(*) as c FROM punishments WHERE uuid=? AND type='WARN' AND active=1";
            try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt("c");
                }
            } catch (SQLException e) { e.printStackTrace(); }
            return 0;
        }, database.getExecutor());
    }

    public CompletableFuture<List<Punishment>> getActiveBans() {
        return getActiveByType(PunishmentType.BAN);
    }

    public CompletableFuture<List<Punishment>> getActiveMutes() {
        return getActiveByType(PunishmentType.MUTE);
    }

    private CompletableFuture<List<Punishment>> getActiveByType(PunishmentType type) {
        return CompletableFuture.supplyAsync(() -> {
            List<Punishment> list = new ArrayList<>();
            String sql = "SELECT * FROM punishments WHERE type=? AND active=1 AND (expires_at=-1 OR expires_at>?) ORDER BY issued_at DESC";
            try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
                ps.setString(1, type.name());
                ps.setLong(2, System.currentTimeMillis());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) list.add(map(rs));
                }
            } catch (SQLException e) { e.printStackTrace(); }
            return list;
        }, database.getExecutor());
    }

    private Punishment map(ResultSet rs) throws SQLException {
        Punishment p = new Punishment();
        p.id = rs.getInt("id");
        String uuidStr = rs.getString("uuid");
        p.uuid = (uuidStr != null && !uuidStr.isEmpty()) ? UUID.fromString(uuidStr) : null;
        p.name = rs.getString("name");
        p.ip = rs.getString("ip");
        p.ipPunishment = rs.getInt("is_ip_punishment") == 1;
        p.type = PunishmentType.valueOf(rs.getString("type"));
        p.reason = rs.getString("reason");
        p.staff = rs.getString("staff");
        p.issuedAt = rs.getLong("issued_at");
        p.expiresAt = rs.getLong("expires_at");
        p.active = rs.getInt("active") == 1;
        return p;
    }

    public String resolveIp(OfflinePlayer target) {
        if (target.isOnline() && target.getPlayer() != null && target.getPlayer().getAddress() != null) {
            return target.getPlayer().getAddress().getAddress().getHostAddress();
        }
        return null;
    }
}