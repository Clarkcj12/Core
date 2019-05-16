package network.palace.core.player.impl.listeners;

import network.palace.core.Core;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.*;

/**
 * The type Core player manager listener.
 */
public class CorePlayerManagerListener implements Listener {

    /**
     * On player login.
     *
     * @param event the event
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onPlayerLogin(AsyncPlayerPreLoginEvent event) {
        if (Core.isStarting()) {
            event.setKickMessage(ChatColor.AQUA + "This server is still starting up. Try again in a few seconds!");
            event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);
            return;
        }
        if (!Core.isDashboardAndSqlDisabled()) {
            if (Core.getDashboardConnection() == null || !Core.getDashboardConnection().isConnected() || Core.getMongoHandler() == null) {
                event.setKickMessage(ChatColor.AQUA + "This server is still starting up. Try again in a few seconds!");
                event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);
                return;
            }
        }
        if (event.getLoginResult().equals(AsyncPlayerPreLoginEvent.Result.ALLOWED)) {
            Core.getPlayerManager().playerLoggedIn(event.getUniqueId(), event.getName());
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerLoginMonitor(AsyncPlayerPreLoginEvent event) {
        if (!event.getLoginResult().equals(AsyncPlayerPreLoginEvent.Result.ALLOWED)) {
            Core.getPlayerManager().removePlayer(event.getUniqueId());
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (Core.isStarting()) {
            event.setKickMessage(ChatColor.AQUA + "This server is still starting up. Try again in a few seconds!");
            event.setResult(PlayerLoginEvent.Result.KICK_OTHER);
        }
        if (!event.getResult().equals(PlayerLoginEvent.Result.ALLOWED)) {
            Core.getPlayerManager().removePlayer(event.getPlayer().getUniqueId());
        }
    }

    /**
     * On player join.
     *
     * @param event the event
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.setJoinMessage("");
        Player player = event.getPlayer();
        player.setExp(0);
        player.setLevel(0);
        Core.getPlayerManager().playerJoined(player);
    }

    /**
     * On player quit.
     *
     * @param event the event
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Core.getPlayerManager().playerLoggedOut(event.getPlayer());
        event.setQuitMessage("");
        Core.getPermissionManager().logout(event.getPlayer().getUniqueId());
    }

    /**
     * On player kick.
     *
     * @param event the event
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerKick(PlayerKickEvent event) {
        Core.getPlayerManager().playerLoggedOut(event.getPlayer());
        event.setLeaveMessage("");
        Core.getPermissionManager().logout(event.getPlayer().getUniqueId());
    }

    /**
     * On player achievement awarded.
     *
     * @param event the event
     */
    @EventHandler
    public void onPlayerAchievementAwarded(PlayerAchievementAwardedEvent event) {
        event.setCancelled(true);
    }

    /**
     * On player pickup item.
     *
     * @param event the event
     */
    @EventHandler
    public void onPlayerPickupItem(EntityPickupItemEvent event) {
        if (event.getItem().hasMetadata("special")) {
            if (event.getItem().getMetadata("special").get(0).asBoolean()) {
                event.setCancelled(true);
            }
        }
    }
}
