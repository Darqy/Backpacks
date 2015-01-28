package me.darqy.backpacks.util;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.server.v1_8_R1.ItemStack;
import net.minecraft.server.v1_8_R1.NBTTagCompound;
import net.minecraft.server.v1_8_R1.NBTTagList;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R1.inventory.CraftItemStack;
import org.bukkit.inventory.Inventory;

public final class InventoryUtil {

    /**
     * Writes an Inventory to an NBT Compound
     * 
     * @param tag compound to save to
     * @param inventory inventory to be saved
     */
    public static void invToNbt(NBTTagCompound tag, Inventory inventory) {
        NBTTagList list = new NBTTagList();
        org.bukkit.inventory.ItemStack[] contents = inventory.getContents();

        tag.setInt("size", contents.length);

        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null) {
                NBTTagCompound nbt = new NBTTagCompound();
                ItemStack nms = CraftItemStack.asNMSCopy(contents[i]);

                nms.save(nbt);
                nbt.setByte("Slot", (byte) i);
                list.add(nbt);
            }
        }

        tag.set("contents", list);
    }

    /**
     * Retrieves an Inventory from an NBT Compound
     * 
     * @param tag Compound to read from
     * @param title title given to inventory
     * @return new Inventory object
     */
    public static Inventory invFromNbt(NBTTagCompound tag, String title) {
        final int size = tag.getInt("size");
        final Inventory inventory = Bukkit.createInventory(null, size, title);
        final NBTTagList contents = tag.getList("contents", 10 /* NBTTagCompound id*/); 

        for (int i = 0, length = contents.size(); i < length; i++) {
            NBTTagCompound nbt = (NBTTagCompound) contents.get(i);
            byte slot = nbt.getByte("Slot");
            ItemStack is = ItemStack.createStack(nbt);

            if (is != null) {
                inventory.setItem(slot, CraftItemStack.asBukkitCopy(is));
            }
        }

        return inventory;
    }

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
            org.bukkit.inventory.ItemStack[] items = removeNull(from.getContents());
            from.clear();

            HashMap<Integer, org.bukkit.inventory.ItemStack> left = to.addItem(items);
            for (Map.Entry<Integer, org.bukkit.inventory.ItemStack> entry : left.entrySet()) {
                from.setItem(entry.getKey(), entry.getValue());
            }
        } else {
            Material material = Material.matchMaterial(item.toUpperCase());
            if (material != null) {
                org.bukkit.inventory.ItemStack[] items
                        = new org.bukkit.inventory.ItemStack[from.getSize()];
                //All items matching material, null elements striped
                items = removeNull(from.all(material).values().toArray(items));
                from.removeItem(items);

                HashMap<Integer, org.bukkit.inventory.ItemStack> left = to.addItem(items);
                for (org.bukkit.inventory.ItemStack is : left.values()) {
                    from.addItem(is);
                }
            } else {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Strips null ItemStacks from an ItemStack[]
     * 
     * @param items ItemStack[]
     * @return ItemStack[] with no null stacks
     */
    private static org.bukkit.inventory.ItemStack[] removeNull(org.bukkit.inventory.ItemStack[] items) {
        org.bukkit.inventory.ItemStack[] tmp = new org.bukkit.inventory.ItemStack[items.length];
        int counter = 0;
        for (org.bukkit.inventory.ItemStack stack : items) {
            if (stack != null) {
                tmp[counter++] = stack;
            }
        }
        org.bukkit.inventory.ItemStack[] ret = new org.bukkit.inventory.ItemStack[counter];
        System.arraycopy(tmp, 0, ret, 0, counter);
        return ret;
    }
}
