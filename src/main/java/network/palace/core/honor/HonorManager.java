package network.palace.core.honor;

import network.palace.core.Core;
import network.palace.core.player.CPlayer;
import org.bukkit.ChatColor;
import org.bukkit.Sound;

import java.util.*;

/**
 * @author Innectic
 * @since 6/6/2017
 */
public class HonorManager {

    private final Set<HonorMapping> mappings = new HashSet<>();
    private final TreeSet<Integer> honorMappings = new TreeSet<>();
    private int highest = 0;

    /**
     * Provide mappings to the manager
     *
     * @param mappings the mappings that should be used
     */
    public void provideMappings(List<HonorMapping> mappings) {
        this.mappings.clear();
        honorMappings.clear();
        mappings.forEach(m -> {
            if (m.getLevel() > highest) {
                highest = m.getLevel();
            }
        });
        this.mappings.addAll(mappings);
        this.mappings.stream().map(HonorMapping::getHonor).forEach(honorMappings::add);
        if (Core.getPlayerManager().getOnlinePlayers().isEmpty()) {
            return;
        }
        for (CPlayer p : Core.getPlayerManager().getOnlinePlayers()) {
            displayHonor(p);
        }
    }

    /**
     * Get the honor level for a player
     *
     * @param player the player to get the map for
     * @return the mapper for the player
     */
    public HonorMapping getMapped(CPlayer player) {
        if (player.getHonor() == 0) {
            return new HonorMapping(1, 0);
        }
        Integer i = honorMappings.floor(player.getHonor());
        if (i == null) return new HonorMapping(1, 0);
        Optional<HonorMapping> mapping = getMapped(i);
        if (!mapping.isPresent()) {
            return new HonorMapping(1, 0);
        }
        int xp = player.getHonor() - mapping.get().getHonor();
        return new HonorMapping(mapping.get().getLevel(), xp);
    }

    /**
     * Get level from honor
     *
     * @param honor amount of honor
     * @return level mapping
     */
    public HonorMapping getLevel(int honor) {
        if (honor == 0) {
            return new HonorMapping(1, 0);
        }
        Integer i = honorMappings.floor(honor);
        if (i == null) return new HonorMapping(1, 0);
        Optional<HonorMapping> mapping = getMapped(i);
        return mapping.orElseGet(() -> new HonorMapping(1, 0));
    }

    /**
     * Get next level from honor amount
     *
     * @param honor amount of honor
     * @return mapping of next level
     */
    public HonorMapping getNextLevel(int honor) {
        if (honor == 0) {
            return new HonorMapping(1, 0);
        }
        Integer i = honorMappings.floor(honor);
        if (i == null) return new HonorMapping(1, 0);
        Optional<HonorMapping> mapping = getMapped(i);
        if (!mapping.isPresent()) {
            return new HonorMapping(1, 0);
        }
        int xp = honor - mapping.get().getHonor();
        Integer nextLevelHonor = honorMappings.ceiling(xp == 0 ? honor + 1 : honor);
        if (nextLevelHonor == null) {
            return new HonorMapping(1, 0);
        }
        Optional<HonorMapping> next = getMapped(nextLevelHonor);
        return next.orElseGet(() -> new HonorMapping(1, 0));
    }

    /**
     * Get progress to next level
     *
     * @param honor amount of honor
     * @return percentage complete from current level to next level as a float
     */
    public float progressToNextLevel(int honor) {
        if (honor == 0) {
            return 0.0f;
        }
        Integer i = honorMappings.floor(honor);
        if (i == null) return 1.0f;
        Optional<HonorMapping> mapping = getMapped(i);
        if (!mapping.isPresent()) {
            return 1.0f;
        }
        int xp = honor - mapping.get().getHonor();
        Integer nextLevelHonor = honorMappings.ceiling(xp == 0 ? honor + 1 : honor);
        if (nextLevelHonor == null) {
            return 1.0f;
        }
        Optional<HonorMapping> next = getMapped(nextLevelHonor);
        if (!next.isPresent()) {
            return 1.0f;
        }
        int diff = next.get().getHonor() - mapping.get().getHonor();
        if (diff == 0) return 1.0f;
        return (float) ((double) xp / (double) diff);
    }

    /**
     * Display the player's honor level to them
     *
     * @param player the player to display to
     */
    public void displayHonor(CPlayer player) {
        displayHonor(player, false);
    }

    /**
     * Display the player's honor level to them
     *
     * @param player the player to display to
     * @param first  whether or not this is the first set
     * @implNote Do NOT use this method outside of the player join task!
     */
    public void displayHonor(CPlayer player, boolean first) {
        HonorMapping mapping = getMapped(player);
        if (mapping == null) {
            if (!Core.isGameMode()) {
                player.setLevel(1);
                player.setExp(0.0f);
            }
            return;
        }
        if (player.getPreviousHonorLevel() != mapping.getLevel() && !first && mapping.getLevel() > 1) {
            // Level change
            if (player.getPreviousHonorLevel() < mapping.getLevel()) {
                // Level increase (most common)
                player.sendMessage("\n");
                player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "LEVEL UP: " + ChatColor.YELLOW +
                        "You are now " + getColorFromLevel(mapping.getLevel()) + "" + ChatColor.BOLD + "Level " + mapping.getLevel());
                player.sendMessage("\n");
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
            }
        }
        player.setPreviousHonorLevel(mapping.getLevel());
        float progress = progressToNextLevel(player.getHonor());
        if (Core.isGameMode()) {
            return;
        }
        player.setLevel(mapping.getLevel());
        player.setExp(progress);
        Core.getCraftingMenu().update(player, 1, Core.getCraftingMenu().getPlayerHead(player));
    }

    private ChatColor getColorFromLevel(int level) {
        if (level < 10) {
            return ChatColor.GREEN;
        } else if (level < 20) {
            return ChatColor.DARK_GREEN;
        } else if (level < 30) {
            return ChatColor.YELLOW;
        } else if (level < 40) {
            return ChatColor.RED;
        } else if (level < 50) {
            return ChatColor.AQUA;
        } else if (level < 60) {
            return ChatColor.BLUE;
        } else if (level < 70) {
            return ChatColor.DARK_BLUE;
        } else if (level < 80) {
            return ChatColor.LIGHT_PURPLE;
        } else if (level < 90) {
            return ChatColor.DARK_PURPLE;
        } else {
            return ChatColor.GOLD;
        }
    }

    /**
     * Get the mapped honor for a player
     *
     * @param honor the player's honor
     * @return the mapping
     */
    private Optional<HonorMapping> getMapped(int honor) {
        return mappings.stream().filter(mapping -> mapping.getHonor() == honor).findFirst();
    }

    public int getTopLevel() {
        return highest;
    }
}
