package com.carbonbans.util;

import com.carbonbans.CarbonBans;
import com.carbonbans.data.ResolvedPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class PlayerResolver {

    private final CarbonBans plugin;

    public PlayerResolver(CarbonBans plugin) {
        this.plugin = plugin;
    }

    /**
     * Resolves a player name to a UUID safely:
     * 1. Checks online players first (instant, no blocking)
     * 2. Checks our own stored join history (no blocking, no network call)
     *
     * If the player has never joined this server before, resolution will fail
     * since there's no reliable way to get their UUID without blocking I/O
     * (Mojang API lookup), which we intentionally avoid for performance.
     */
    public CompletableFuture<Optional<ResolvedPlayer>> resolve(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return CompletableFuture.completedFuture(
                    Optional.of(new ResolvedPlayer(online.getUniqueId(), online.getName())));
        }

        return plugin.getDatabase().getPlayerByName(name).thenApply(result -> {
            if (result != null) return Optional.of(result);
            return Optional.<ResolvedPlayer>empty();
        });
    }
}