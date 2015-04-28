package me.darqy.backpacks;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public class BackpackListener implements Listener {
    
    @EventHandler(ignoreCancelled = true, priority=EventPriority.MONITOR)
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        if (inventory.getHolder() instanceof BackpackInventoryHolder) { 
            BackpackInventoryHolder holder = BackpackInventoryHolder.of(inventory);
            // flag the backpack as modified
            holder.setUnsavedChanges(true);
        }
    }
    
}
