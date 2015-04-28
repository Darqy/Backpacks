package me.darqy.backpacks.io;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import me.darqy.backpacks.BackpackInventoryHolder;
import me.darqy.backpacks.BackpacksPlugin;
import me.darqy.backpacks.util.InventoryUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitTask;

public class BackpackGroupCache {
    
    private final BackpacksPlugin plugin;
    private final long saveInterval, savesPerTick, saveTickDelay;
    private final String group;
    //               Owner,   Packname, Backpack
    private final Map<UUID, Map<String, Inventory>> backpacks;
    
    private BukkitTask saveQueueProcessor;
    private BukkitTask autoSaveScheduler;
    
    private BackpackIOHandler io;

    public BackpackGroupCache(BackpacksPlugin plugin, String group, long autoSaveInterval, long savesPerTick, long saveTickDelay) {
        this(plugin, group, autoSaveInterval, savesPerTick, saveTickDelay, new HashMap());
    }
    
    public BackpackGroupCache(BackpacksPlugin plugin, String group, long autoSaveInterval, long savesPerTick, long saveTickDelay, Map<UUID, Map<String, Inventory>> backpacks) {
        this.plugin = plugin;
        this.group = group;
        this.saveInterval = autoSaveInterval;
        this.savesPerTick = savesPerTick;
        this.saveTickDelay = saveTickDelay;
        this.backpacks = backpacks;
    }
    
    private void validState() throws IllegalStateException {
        if (this.io == null) {
            throw new IllegalStateException("Must first call init(BackpackIOHandler)");
        }
    }
    
    /**
     * Load the backpack from disk if not cached, else return memory resident
     * @param owner
     * @param backpack
     * @return the backpack if it exists else null
     */
    public Inventory getBackpack(UUID owner, String backpack) {
        validState();
        
        Map<String, Inventory> playerPacks = getPlayerBackpacks(owner);
        Inventory inventory = playerPacks.get(backpack);
        if (inventory == null) {
            inventory = io.loadBackpack(owner, backpack);
            if (inventory != null) {
                playerPacks.put(backpack, inventory);
            }
        }
        
        return inventory;
    }
    
    // save queue
    private final ArrayDeque<SaveQueueEntry> saveQueue = new ArrayDeque();
    
    private static final class SaveQueueEntry {
        final UUID owner;
        final String backpack;
        public SaveQueueEntry(UUID owner, String backpack) {
            this.owner = owner;
            this.backpack = backpack;
        }
    }
    
    private void queueAutosave() {
        if (!saveQueue.isEmpty()) {
            plugin.getLogger().log(Level.WARNING, "Backpacks are saving slowly, there''s still {0} backpacks left in the Queue.", saveQueue.size());
            plugin.getLogger().log(Level.WARNING, "Skipping saving this round.");
            return;
        }
        
        for (Map.Entry<UUID, Map<String, Inventory>> owners : backpacks.entrySet()) {           
            for (String backpack : owners.getValue().keySet()) {
                saveQueue.addLast(new SaveQueueEntry(owners.getKey(), backpack));
            }            
        }
    }
    
    public void saveAllBackpacks() {
        validState();
        
        // clear the queue to avoid any save conflicts
        saveQueue.clear();
        
        for (Map.Entry<UUID, Map<String, Inventory>> owners : backpacks.entrySet()) {           
            for (Map.Entry<String, Inventory> packs : owners.getValue().entrySet()) {
                plugin.debug("Saving {0}''s {1} backpack.", owners.getKey(), packs.getKey());
                try {
                    io.saveBackpack(owners.getKey(), packs.getKey(), packs.getValue());
                } catch (IOException e) {
                    plugin.getLogger().log(Level.SEVERE, "Error while saving {0}''s {1} backpack in group {2}", new Object[] {owners.getKey(), packs.getKey(), group, e});
                }
            }            
        }
    }
    
    public void unloadBackpack(UUID owner, String backpack) {
        Inventory inventory = getBackpack(owner, backpack); // retrieve inventory
        if (inventory == null) {
            return;
        }
        
        // close open inventory views
        InventoryUtil.closeAllViews(inventory);
        
        backpacks.get(owner).remove(backpack);
    }
    
    public void unloadAllBackpacks() {
        for (Iterator<Map.Entry<UUID, Map<String, Inventory>>> i = backpacks.entrySet().iterator(); i.hasNext();) {
            Map.Entry<UUID, Map<String, Inventory>> owners = i.next();
            for (Iterator<Map.Entry<String, Inventory>> bi = owners.getValue().entrySet().iterator(); bi.hasNext();) {
                Map.Entry<String, Inventory> backpack = bi.next();
                // close open views
                InventoryUtil.closeAllViews(backpack.getValue());
                bi.remove();
            }
            i.remove();
        }
    }
    
    public void loadAll() {
        validState();

        loadAll(io.loadAll());
    }
    
    public void loadAll(Map<UUID, Map<String, Inventory>> packs) {
        backpacks.clear();
        backpacks.putAll(packs);
    }
    
    /**
     * Sets an owner's backpack to the given inventory.
     * @param owner
     * @param backpack
     * @param inventory if null, remove the backpack
     */
    public void setBackpack(UUID owner, String backpack, Inventory inventory) {
        validState();
        
        if (inventory == null) {
            io.removeBackpack(owner, backpack);
        } else {
            getPlayerBackpacks(owner).put(backpack, inventory);
        }
    }
    
    /**
     * Renames a backpack
     * @param owner
     * @param oldBackpack
     * @param newBackpack 
     */
    public void renameBackpack(UUID owner, String oldBackpack, String newBackpack) {
        validState();
        Inventory existing = getBackpack(owner, oldBackpack);
        if (existing == null) {
            // if old backpack doesn't exist, don't do anything
            return;
        }
        
        // remove old backpack from disk
        io.removeBackpack(owner, oldBackpack);
        
        // remove old backpack from cache
        getPlayerBackpacks(owner).remove(oldBackpack);
        
        Inventory newInv = BackpackInventoryHolder.asEmptyInventory(this, owner, newBackpack, existing.getSize());
        newInv.setContents(existing.getContents());
        
        BackpackInventoryHolder.of(newInv).setUnsavedChanges(true);
        
        setBackpack(owner, newBackpack, newInv);
    }
    
    private Map<String, Inventory> getPlayerBackpacks(UUID owner) {
        Map<String, Inventory> packs = backpacks.get(owner);
        if (packs == null) {
            backpacks.put(owner, (packs = new HashMap()));
        }
        return packs;
    }
    
    public int getBackpackCount(Player player) {
        return getBackpackCount(player.getUniqueId());
    }
    
    public int getBackpackCount(UUID owner) {
        validState();
        return io.getBackpackCount(owner);
    }
    
    public List<String> getBackpackNames(Player player) {
        return getBackpackNames(player.getUniqueId());
    }
    
    public List<String> getBackpackNames(UUID owner) {
        validState();
        return io.getBackpackList(owner);
    }
    
    public Map<UUID, Map<String, Inventory>> getAllCached() {
        return backpacks;
    }
    
        
    public void init(BackpackIOHandler ioHandler, boolean scheduleAutosave) {
        this.io = ioHandler;
        
        if (!scheduleAutosave) {
            return;
        }
        
        // schedule the save processor
        saveQueueProcessor = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                if (saveQueue.isEmpty()) {
                    return;
                }
                
                for (int i = 0; i < savesPerTick; i++) {
                    boolean saved = false;
                    do {
                        if (saveQueue.isEmpty()) {
                            return;
                        }
                        
                        SaveQueueEntry sqe = saveQueue.removeFirst();

                        Inventory inv = getBackpack(sqe.owner, sqe.backpack);
                        BackpackInventoryHolder h = BackpackInventoryHolder.of(inv);
                        boolean shouldSave = h.hasUnsavedChanges();
                        OfflinePlayer offlinep = Bukkit.getOfflinePlayer(sqe.owner);
                        if (shouldSave) {
                            plugin.debug("Auto saving {0}''s {1} backpack.." , offlinep.getName(), sqe.backpack);
                            try {
                                io.saveBackpack(sqe.owner, sqe.backpack, inv);
                                h.setUnsavedChanges(false);
                                saved = true;
                            } catch (IOException e) {
                                plugin.getLogger().log(Level.SEVERE, "Failed to save {0}''s {1} backpack in group {2} during auto save.",
                                        new Object[]{offlinep.getName(), sqe.backpack, group, e});
                                saved = false;
                            }
                        }
                        
                        if (!offlinep.isOnline() && inv.getViewers().isEmpty()) {
                            plugin.debug("Unloading backpack because player is offline and no one is viewing their backpack.");
                            unloadBackpack(sqe.owner, sqe.backpack);
                        }
                    } while (!saved);
                }
            }
        }, saveTickDelay, saveTickDelay);
        
        // scheule the auto save task
        autoSaveScheduler = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                queueAutosave();
            }
        }, saveInterval, saveInterval);   
    }
    
    public void disable() {
        plugin.debug("Shutting down cache");
        autoSaveScheduler.cancel();
        saveQueueProcessor.cancel();
        plugin.debug("Saving all backpacks");
        saveAllBackpacks();
        plugin.debug("Unloading all backpacks");
        unloadAllBackpacks();
    }
    
}
