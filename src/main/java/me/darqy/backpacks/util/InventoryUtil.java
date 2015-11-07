package me.darqy.backpacks.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class InventoryUtil {

    /**
     * Safely transfers items from one inventory to another
     * 
     * @param from inventory to get items from
     * @param to inventory to put items into
     * @param item item filter. Accepts '*' for all items, or a Bukkit material
     * @return whether the transfer could proceed and was successful
     */
    public static boolean transferItems(Inventory from, Inventory to, String item) {
        if (item.equals("*") || item.equals("all")) {
            ItemStack[] items = removeNull(from.getContents());
            from.clear();

            HashMap<Integer, ItemStack> left = to.addItem(items);
            for (Map.Entry<Integer, ItemStack> entry : left.entrySet()) {
                from.setItem(entry.getKey(), entry.getValue());
            }
        } else {
            Material material = Material.matchMaterial(item.toUpperCase());
            if (material != null) {
                ItemStack[] items
                        = new ItemStack[from.getSize()];
                //All items matching material, null elements striped
                items = removeNull(from.all(material).values().toArray(items));
                from.removeItem(items);

                HashMap<Integer, ItemStack> left = to.addItem(items);
                for (ItemStack is : left.values()) {
                    from.addItem(is);
                }
            } else {
                return false;
            }
        }
        return true;
    }
    
    public static boolean isValidMaterial(String string) {
        return Material.matchMaterial(string.toUpperCase()) != null;
    }
    
    /**
     * Strips null ItemStacks from an ItemStack[]
     * 
     * @param items ItemStack[]
     * @return ItemStack[] with no null stacks
     */
    private static ItemStack[] removeNull(ItemStack[] items) {
        ItemStack[] tmp = new ItemStack[items.length];
        int counter = 0;
        for (ItemStack stack : items) {
            if (stack != null) {
                tmp[counter++] = stack;
            }
        }
        ItemStack[] ret = new ItemStack[counter];
        System.arraycopy(tmp, 0, ret, 0, counter);
        return ret;
    }
    
    /**
     * Close all open views of an inventory
     * @param inventory inventory to close open views of
     */
    public static void closeAllViews(Inventory inventory) {
        for (HumanEntity e : new ArrayList<HumanEntity>(inventory.getViewers())) {
            e.closeInventory();
        }
    }
    
}
