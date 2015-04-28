package me.darqy.backpacks;

import java.util.UUID;
import me.darqy.backpacks.io.BackpackGroupCache;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class BackpackInventoryHolder implements InventoryHolder {

    private final BackpackGroupCache cache;
    private final UUID owner;
    private final String backpack;
    
    private boolean hasUnsavedChanges = false;
    
    private Inventory inventory = null;
    
    public BackpackInventoryHolder(BackpackGroupCache cache, UUID owner, String backpack) {
        this.cache = cache;
        this.owner = owner;
        this.backpack = backpack;
    }
    
    public boolean hasUnsavedChanges() {
        return hasUnsavedChanges;
    }

    public void setUnsavedChanges(boolean modified) {
        this.hasUnsavedChanges = modified;
    }
    
    @Override
    public Inventory getInventory() {
        if (inventory == null) {
            inventory = cache.getBackpack(owner, backpack);
        }
        return inventory;
    }
    
    public static BackpackInventoryHolder of(Inventory inventory) {
        return (BackpackInventoryHolder) inventory.getHolder();
    }
    
    public static Inventory asEmptyInventory(BackpackGroupCache cache, UUID owner, String name, int size) {
        return Bukkit.createInventory(new BackpackInventoryHolder(cache, owner, name), size, BackpacksConfig.formatInventoryTitle(name));
    }
    
}
