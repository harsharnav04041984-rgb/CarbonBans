package com.carbonbans.data;

import java.util.UUID;

public class Punishment {
    public int id;
    public UUID uuid;
    public String name;
    public String ip;
    public boolean ipPunishment; // true only for /ipban or /muteip
    public PunishmentType type;
    public String reason;
    public String staff;
    public long issuedAt;
    public long expiresAt; // -1 = permanent
    public boolean active;
}