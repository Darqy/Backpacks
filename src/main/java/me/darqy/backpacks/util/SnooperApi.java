package me.darqy.backpacks.util;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;

/**
 * SnooperApi provides a simple method to restrict a player from
 * modifying a section of an inventory. The restriction ends as
 * soon as the player closes the inventory.
 * @author matt
 */
public class SnooperApi {
    
    public static final String META_KEY = "invsnooper";
    
    private static Plugin plugin;
    
    public enum InvSection {
        TOP, BOTTOM
    }

    public static void initialize(Plugin instance) {
        plugin = instance;
        plugin.getServer().getPluginManager().registerEvents(new Listener() {
            
            @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
            public void onInventoryClick(InventoryClickEvent e) {
                if (!(e.getWhoClicked() instanceof Player)) {
                    return;
                }
                Player player = (Player) e.getWhoClicked();

                Constraint constraint = getSnooper(player);
                if (constraint != null) {
                    if (constraint.slotRestricted(e.getRawSlot()) || e.isShiftClick()) {
                        e.setCancelled(true);
                        player.sendMessage(ChatColor.RED + "You may not edit those items.");
                    }
                }
            }

            @EventHandler
            public void onInventoryClose(InventoryCloseEvent e) {
                if (!(e.getPlayer() instanceof Player)) {
                    return;
                }
                Player player = (Player) e.getPlayer();

                if (isSnooper(player)) {
                    unregisterSnooper(player);
                }
            }

            @EventHandler
            public void onPlayerQuit(PlayerQuitEvent e) {
                if (isSnooper(e.getPlayer())) {
                    unregisterSnooper(e.getPlayer());
                }
            }
        }, plugin);
    }

    public static Constraint getSnooper(Player player) {
        if (player.hasMetadata(META_KEY)) {
            for (MetadataValue val : player.getMetadata(META_KEY)) {
                if (val.getOwningPlugin().equals(plugin)) {
                    return (Constraint) val.value();
                }
            }
        }
        return null;
    }
    
    public static boolean isSnooper(Player player) {
        return getSnooper(player) != null;
    }
    
    public static void registerSnooper(Player p, Constraint constraint) {
        p.setMetadata(META_KEY, new FixedMetadataValue(plugin, constraint));
    }
    
    public static void unregisterSnooper(Player p) {
        p.removeMetadata(META_KEY, plugin);
    }
        
    public static Constraint constraintHalf(InvSection section, InventoryView inventory) {
        int topSize = inventory.getTopInventory().getSize();
        switch (section) {
            case TOP:
                return new Constraint(0, topSize - 1);
            case BOTTOM:
                return new Constraint(topSize, Integer.MAX_VALUE);
        }
        return constraintAll();
    }

    public static Constraint constraintAll() {
        return new Constraint(Integer.MIN_VALUE, Integer.MAX_VALUE);
    }
    
    public static class Constraint {
        
        int begin, end;
        
        public Constraint(int begin, int end) {
            this.begin = begin;
            this.end = end;
        }
        
        boolean slotRestricted(int rawSlotId) {
            return (rawSlotId >= begin && rawSlotId <= end);
        }
        
    }
    
}
