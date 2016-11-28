package com.thepalace.core.player.impl;

import com.thepalace.core.Core;
import com.thepalace.core.events.CorePlayerJoinDelayedEvent;
import com.thepalace.core.player.CPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;

import java.util.UUID;

public class CorePlayerManagerListener implements Listener {

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onPlayerLogin(AsyncPlayerPreLoginEvent event) {
        Core.getPlayerManager().playerLoggedIn(event.getUniqueId(), event.getName());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (Core.getPlayerManager().getPlayer(event.getPlayer()) == null) return;
        Core.getPlayerManager().getPlayer(event.getPlayer()).setStatus(CPlayer.PlayerStatus.JOINED);
        // Call join delayed event
        Core.runTaskLater(() -> {
            CorePlayerJoinDelayedEvent e = new CorePlayerJoinDelayedEvent(Core.getPlayerManager().getPlayer(event.getPlayer()), event.getJoinMessage());
            Core.callEvent(e);
            event.setJoinMessage(e.getJoinMessage());
        }, 10L);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (event.getPlayer() == null) return;
        if (event.getPlayer().getUniqueId() == null) return;
        onLeave(event.getPlayer().getUniqueId());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerKick(PlayerKickEvent event) {
        if (event.getPlayer() == null) return;
        if (event.getPlayer().getUniqueId() == null) return;
        onLeave(event.getPlayer().getUniqueId());
    }

    public void onLeave(UUID uuid) {
        if (Core.getPlayerManager().getPlayer(uuid) == null) return;
        Core.getPlayerManager().getPlayer(uuid).resetManagers();
        Core.getPlayerManager().getPlayer(uuid).setStatus(CPlayer.PlayerStatus.LEFT);
        Core.getPlayerManager().playerLoggedOut(uuid);
    }
}
