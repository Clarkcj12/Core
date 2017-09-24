package network.palace.core.player.impl;

import network.palace.core.Core;
import network.palace.core.events.CoreOnlineCountUpdate;
import network.palace.core.events.EconomyUpdateEvent;
import network.palace.core.player.CPlayer;
import network.palace.core.player.CPlayerScoreboardManager;
import network.palace.core.player.PlayerStatus;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.concurrent.Callable;

/**
 * The type Core player default scoreboard.
 */
public class CorePlayerDefaultScoreboard implements Listener {
    private int playerCount = 0;

    /**
     * Instantiates a new Core player default scoreboard.
     */
    public CorePlayerDefaultScoreboard() {
        if (!isDefaultSidebarEnabled()) return;
        Core.registerListener(this);
    }

    /**
     * Setups the scoreboard for the player.
     *
     * @param player the player
     */
    public void setup(CPlayer player) {
        if (!isDefaultSidebarEnabled()) return;
        CPlayerScoreboardManager scoreboard = player.getScoreboard();
        // Title
        scoreboard.title(ChatColor.GOLD + "" + ChatColor.BOLD + "The " + ChatColor.DARK_PURPLE + ChatColor.BOLD + "Palace " + ChatColor.GOLD + "" + ChatColor.BOLD + "Network");
        // Blank space
        scoreboard.setBlank(10);
        // Balance temp
        scoreboard.set(9, ChatColor.GREEN + "$ Loading...");
        // Blank space
        scoreboard.setBlank(8);
        // Tokens temp
        scoreboard.set(7, ChatColor.GREEN + "\u272a Loading...");
        // Blank space
        scoreboard.setBlank(6);
        // Rank
        scoreboard.set(5, ChatColor.GREEN + "Rank: " + player.getRank().getTagColor() + player.getRank().getName());
        // Blank space
        scoreboard.setBlank(4);
        // Players number
        scoreboard.set(3, ChatColor.GREEN + "Online Players: " + playerCount);
        // Server name
        scoreboard.set(2, ChatColor.GREEN + "Server: " + Core.getServerType());
        // Blank
        scoreboard.setBlank(1);
        // Store link #Sellout
        scoreboard.set(0, ChatColor.YELLOW + "store.palace.network");
        // Load balance async
        loadBalance(player, scoreboard, 9);
        // Load tokens async
        loadTokens(player, scoreboard, 7);
    }

    /**
     * On online count update.
     *
     * @param event the event
     */
    @EventHandler
    public void onOnlineCountUpdate(CoreOnlineCountUpdate event) {
        playerCount = event.getCount();
        for (CPlayer player : Core.getPlayerManager().getOnlinePlayers()) {
            if (player.getStatus() != PlayerStatus.JOINED) return;
            if (!player.getScoreboard().isSetup()) return;
            player.getScoreboard().set(3, ChatColor.GREEN + "Online Players: " + playerCount);
        }
    }

    /**
     * On economy update.
     *
     * @param event the event
     */
    @EventHandler
    public void onEconomyUpdate(EconomyUpdateEvent event) {
        int amount = event.getAmount();
        CPlayer player = Core.getPlayerManager().getPlayer(event.getUuid());
        if (player == null) return;
        switch (event.getCurrency()) {
            case BALANCE:
                setBalance(9, player.getScoreboard(), amount);
                break;
            case TOKENS:
                setTokens(7, player.getScoreboard(), amount);
                break;
        }
    }

    public void loadTokens(CPlayer player, CPlayerScoreboardManager scoreboard, int position) {
        Core.runTaskAsynchronously(() -> {
            int tokens = player.getTokens();
            Core.callSyncMethod((Callable<Object>) () -> {
                setTokens(position, scoreboard, tokens);
                return true;
            });
        });
    }

    public void loadBalance(CPlayer player, CPlayerScoreboardManager scoreboard, int position) {
        Core.runTaskAsynchronously(() -> {
            int balance = player.getBalance();
            Core.callSyncMethod((Callable<Object>) () -> {
                setBalance(position, scoreboard, balance);
                return true;
            });
        });
    }

    private void setTokens(int position, CPlayerScoreboardManager scoreboard, int tokens) {
        if (tokens > 2147483646) {
            scoreboard.set(position, ChatColor.GREEN + "\u272a " + 2147483646 + "+");
        } else {
            scoreboard.set(position, ChatColor.GREEN + "\u272a " + tokens);
        }
    }

    private void setBalance(int position, CPlayerScoreboardManager scoreboard, int balance) {
        if (balance > 2147483646) {
            scoreboard.set(position, ChatColor.GREEN + "$ " + 2147483646 + "+");
        } else {
            scoreboard.set(position, ChatColor.GREEN + "$ " + balance);
        }
    }

    private boolean isDefaultSidebarEnabled() {
        return Core.getCoreConfig().getBoolean("isDefaultSidebarEnabled", true);
    }
}
