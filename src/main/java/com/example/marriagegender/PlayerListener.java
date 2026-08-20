package com.example.marriagegender;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerListener implements Listener {

    private final MarriageGenderPlugin plugin;

    public PlayerListener(MarriageGenderPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getRequestManager().clearRequestsFor(event.getPlayer().getUniqueId());
    }
}
