package com.carbonbans.data;

import java.util.UUID;

public class ResolvedPlayer {
    public final UUID uuid;
    public final String name;

    public ResolvedPlayer(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }
}