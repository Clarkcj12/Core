package network.palace.core.npc;

import lombok.Getter;
import network.palace.core.Core;
import network.palace.core.events.CorePlayerJoinedEvent;
import network.palace.core.player.CPlayer;
import network.palace.core.utils.MiscUtil;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.event.world.ChunkLoadEvent;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;

public final class SoftNPCManager implements Listener {

    @Getter private IDManager iDManager;
    @Getter private final Set<WeakReference<AbstractMob>> mobRefs = new HashSet<>();

    public SoftNPCManager() {
        iDManager = new IDManager();
        Core.registerListener(this);
    }

    private void ensureAllValid() {
        mobRefs.removeIf(mob -> mob.get() == null);
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        ensureAllValid();
        for (Entity entity : event.getChunk().getEntities()) {
            if (entity instanceof Player) {
                CPlayer player = Core.getPlayerManager().getPlayer((Player) entity);
                if (player == null) return;
                for (WeakReference<AbstractMob> mobRef : mobRefs) {
                    final AbstractMob npcMob = mobRef.get();
                    if (npcMob == null) continue;
                    if (npcMob.isSpawned() && npcMob.getViewers().size() == 0) {
                        if (player.getLocation().getChunk().equals(npcMob.getLocation().getLocation().getChunk())) {
                            npcMob.forceSpawn(player);
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(CorePlayerJoinedEvent event) {
        ensureAllValid();
        CPlayer player = event.getPlayer();
        for (WeakReference<AbstractMob> mobRef : mobRefs) {
            final AbstractMob npcMob = mobRef.get();
            if (npcMob == null) continue;
            if (npcMob.isSpawned() && npcMob.getViewers().size() == 0) {
                if (event.getPlayer().getLocation().getWorld().equals(npcMob.getLocation().getWorld())) {
                    npcMob.forceSpawn(player);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        ensureAllValid();
        CPlayer player = Core.getPlayerManager().getPlayer(event.getPlayer());
        for (WeakReference<AbstractMob> mobRef : mobRefs) {
            final AbstractMob npcMob = mobRef.get();
            if (npcMob == null) continue;
            if (npcMob.getViewers().contains(player)) npcMob.removeViewer(player);
            npcMob.forceDespawn(player);
        }
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) {
        ensureAllValid();
        CPlayer player = Core.getPlayerManager().getPlayer(event.getPlayer());
        for (WeakReference<AbstractMob> mobRef : mobRefs) {
            final AbstractMob npcMob = mobRef.get();
            if (npcMob == null) continue;
            if (npcMob.getViewers().contains(player)) npcMob.removeViewer(player);
            npcMob.forceDespawn(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        ensureAllValid();
        CPlayer player = Core.getPlayerManager().getPlayer(event.getPlayer());
        for (WeakReference<AbstractMob> mobRef : mobRefs) {
            final AbstractMob npcMob = mobRef.get();
            if (npcMob == null) continue;
            if (!npcMob.isSpawned()) continue;
            if (npcMob.getLocation().getWorld().equals(event.getRespawnLocation().getWorld())) {
                npcMob.forceSpawn(player);
            } else {
                npcMob.forceDespawn(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerWorldChange(PlayerChangedWorldEvent event) {
        ensureAllValid();
        CPlayer player = Core.getPlayerManager().getPlayer(event.getPlayer());
        for (WeakReference<AbstractMob> mobRef : mobRefs) {
            AbstractMob mobNPC = mobRef.get();
            if (mobNPC == null || mobNPC.getViewers().size() != 0 && MiscUtil.contains(mobNPC.getTargets(), event.getPlayer())) continue;
            if (mobNPC.getLocation().getWorld() == null) {
                if (event.getPlayer().getWorld().equals(mobNPC.getLocation().getWorld())) {
                    mobNPC.forceSpawn(player);
                } else {
                    mobNPC.forceDespawn(player);
                }
            }
        }
    }
}
