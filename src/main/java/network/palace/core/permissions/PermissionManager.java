package network.palace.core.permissions;

import network.palace.core.Core;
import network.palace.core.player.CPlayer;
import network.palace.core.player.Rank;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The type Permission manager.
 */
public class PermissionManager {

    /**
     * The Attachments.
     */
    public Map<UUID, PermissionAttachment> attachments = new HashMap<>();
    private Map<Rank, Map<String, Boolean>> permissions = new HashMap<>();

    /**
     * Instantiates a new Permission manager.
     */
    public PermissionManager() {
        initialize();
    }

    /**
     * Initialize.
     */
    private void initialize() {
        permissions.clear();
        Rank[] ranks = Rank.values();
        Collection<CPlayer> players = Core.getPlayerManager().getOnlinePlayers();
        boolean empty = players.isEmpty();
        Rank previous = null;
        for (int i = ranks.length - 1; i >= 0; i--) {
            Rank r = ranks[i];
            Map<String, Boolean> perms = Core.getMongoHandler().getPermissions(r);
            if (previous != null) {
                for (Map.Entry<String, Boolean> perm : getPermissions(previous).entrySet()) {
                    if (perms.containsKey(perm.getKey())) {
                        if (!perms.get(perm.getKey()).equals(perm.getValue())) {
                            continue;
                        }
                    }
                    perms.put(perm.getKey(), perm.getValue());
                }
            }
            permissions.put(r, perms);
            if (!empty) {
                for (CPlayer p : players) {
                    if (p.getRank().equals(r)) {
                        setPermissions(p.getBukkitPlayer(), perms);
                    }
                }
            }
            previous = r;
        }
    }

    /**
     * Set permissions on login.
     *
     * @param player the player
     */
    public void login(CPlayer player) {
        setPermissions(player.getBukkitPlayer(), getPermissions(player.getRank()));
    }

    private void setPermissions(Player player, Map<String, Boolean> perms) {
        PermissionAttachment attachment;
        if (attachments.containsKey(player.getUniqueId())) {
            attachment = attachments.get(player.getUniqueId());
        } else {
            attachment = player.addAttachment(Core.getInstance());
        }
        for (Map.Entry<String, Boolean> entry : attachment.getPermissions().entrySet()) {
            attachment.unsetPermission(entry.getKey());
        }
        attachment.remove();
        for (Map.Entry<String, Boolean> entry : perms.entrySet()) {
            attachment.setPermission(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Gets permissions.
     *
     * @param rank the rank
     * @return the permissions
     */
    public Map<String, Boolean> getPermissions(Rank rank) {
        Map<String, Boolean> map = permissions.get(rank);
        return map == null ? new HashMap<>() : map;
    }

    /**
     * Refresh permissions.
     *
     * @param sender the sender
     */
    public void refresh(CommandSender sender) {
        Core.runTaskAsynchronously(() -> {
            sender.sendMessage(ChatColor.YELLOW + "Refreshing permissions...");
            initialize();
            sender.sendMessage(ChatColor.YELLOW + "Permissions refreshed!");
        });
    }

    /**
     * Sets permission.
     *
     * @param rank  the rank
     * @param node  the node
     * @param value the value
     */
    public void setPermission(Rank rank, String node, boolean value) {
        Map<String, Boolean> currentPermissions = new HashMap<>(permissions.get(rank));
        currentPermissions.put(node, value);
        permissions.put(rank, currentPermissions);
    }

    /**
     * Unset permission.
     *
     * @param rank the rank
     * @param node the node
     */
    public void unsetPermission(Rank rank, String node) {
        Map<String, Boolean> currentPermissions = new HashMap<>(this.permissions.get(rank));
        currentPermissions.remove(node);
        permissions.put(rank, currentPermissions);
    }
}
