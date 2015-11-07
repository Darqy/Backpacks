package me.darqy.backpacks.io;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.inventory.Inventory;

public interface BackpackIOHandler {
    
    /**
     * Saves a backpacks contents to disk.
     * @param owner
     * @param backpack
     * @param inventory
     * @throws IOException 
     */
    public abstract void saveBackpack(UUID owner, String backpack, Inventory inventory) throws IOException;
    
    /**
     * Loads a backpack from disk
     * @param player
     * @param backpack
     * @return whether or not the backpack storage was available
     */
    public abstract Inventory loadBackpack(UUID player, String backpack);
    
    /**
     * Loads all possible backpacks from disk
     * @return returns two-layer map, first key being player UUID, second being the backpack name
     */
    public abstract Map<UUID, Map<String, Inventory>> loadAll();
    
    /**
     * A list of all the backpacks a player has. (loaded or not)
     * @param owner
     * @return a List of backpack names
     */
    public abstract List<String> getBackpackList(UUID owner);
    
    /**
     * Removes the disk contents of a backpack
     * @param owner
     * @param backpack
     */
    public abstract void removeBackpack(UUID owner, String backpack);
    
}
