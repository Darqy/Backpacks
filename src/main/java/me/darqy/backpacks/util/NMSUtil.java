package me.darqy.backpacks.util;

import net.minecraft.server.v1_8_R2.EntityTracker;
import net.minecraft.server.v1_8_R2.ItemStack;
import net.minecraft.server.v1_8_R2.NBTTagCompound;
import net.minecraft.server.v1_8_R2.NBTTagList;
import net.minecraft.server.v1_8_R2.PacketPlayOutCollect;
import org.bukkit.craftbukkit.v1_8_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R2.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R2.inventory.CraftItemStack;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;

public class NMSUtil {
    
    public static final int NBT_TAG_COMPOUND_ID = 10;
    
    /**
     * Writes an ItemStack[] array to an NBTTagCompound, occupying tags "size" and "contents"
     * @param tag compound to save to
     * @param contents ItemStack[] to be saved
     */
    public static void invToNBT(NBTTagCompound tag, org.bukkit.inventory.ItemStack[] contents) {
        NBTTagList list = new NBTTagList();
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
     * Retrieves an ItemStack[] from an NBTTagCompound containing the tags "size" and "contents"
     * @param tag Compound to read from
     * @return ItemStack[]
     */
    public static org.bukkit.inventory.ItemStack[] itemStacksFromNBT(NBTTagCompound tag) {
        final int size = tag.getInt("size");
        final org.bukkit.inventory.ItemStack[] items = new org.bukkit.inventory.ItemStack[size];
        final NBTTagList contents = tag.getList("contents", NBT_TAG_COMPOUND_ID); 

        for (int i = 0, length = contents.size(); i < length; i++) {
            NBTTagCompound nbt = (NBTTagCompound) contents.get(i);
            byte slot = nbt.getByte("Slot");
            ItemStack is = ItemStack.createStack(nbt);

            if (is != null) {
                items[slot] = CraftItemStack.asBukkitCopy(is);
            }
        }

        return items;
    }

    
    public static void simulateItemPickup(Player player, Item item) {
        simulateItemPickup(player, item.getEntityId());
    }
    
    public static void simulateItemPickup(Player player, int entity) {
        CraftPlayer p = (CraftPlayer) player;
        EntityTracker tracker = ((CraftWorld) p.getWorld()).getHandle().getTracker();
        tracker.sendPacketToEntity(p.getHandle(),
                new PacketPlayOutCollect(entity, p.getEntityId()));
    }
    
}
