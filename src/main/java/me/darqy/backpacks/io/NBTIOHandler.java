package me.darqy.backpacks.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import me.darqy.backpacks.BackpackInventoryHolder;
import me.darqy.backpacks.util.NMSUtil;
import net.minecraft.server.v1_8_R2.NBTCompressedStreamTools;
import net.minecraft.server.v1_8_R2.NBTTagCompound;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class NBTIOHandler extends BackpackIOHandler {

    private final BackpackGroupCache cache;
    private final File data_dir;
    private final Map<String, NBTTagCompound> tags = new HashMap();
    
    private static final ExtensionFileFilter dotDatFilter = new ExtensionFileFilter(".dat");

    public NBTIOHandler(BackpackGroupCache cache, File dir) {
        this.cache = cache;
        data_dir = dir;
        data_dir.mkdirs();
        if (!data_dir.isDirectory()) {
            throw new IllegalArgumentException("File must be a directory!");
        }
    }

    @Override
    public void removeBackpack(UUID owner, String backpack) {
        getNBT(owner.toString()).remove(backpack);
    }

    @Override
    public List<String> getBackpackList(UUID owner) {
        List<String> list = new ArrayList();
        for (Object o : getNBT(owner.toString()).c()) {
            list.add((String) o);
        }
        return list;
    }

    @Override
    public int getBackpackCount(UUID owner) {
        return getNBT(owner.toString()).c().size();
    }

    @Override
    public void saveBackpack(UUID owner, String backpack, Inventory inventory) throws IOException {
        // Create new compound tag for this backpack
        NBTTagCompound backpackTag = new NBTTagCompound();
        backpackTag.setString("Name", backpack);
        
        // Save contents of inventory to tag
        NMSUtil.invToNBT(backpackTag, inventory.getContents());
        
        // Update and save player's NBT file
        getNBT(owner.toString()).set(backpack, backpackTag);
        savePlayerFile(owner.toString());
    }
    
    @Override
    public Inventory loadBackpack(UUID owner, String backpack) {
        NBTTagCompound playerNBT = getNBT(owner.toString());
        if (!playerNBT.hasKeyOfType(backpack, NMSUtil.NBT_TAG_COMPOUND_ID)) {
            // player file does not contain a backpack by this name
            return null;
        }
        
        NBTTagCompound backpackTag = playerNBT.getCompound(backpack);
        ItemStack[] contents = NMSUtil.itemStacksFromNBT(backpackTag);
        
        Inventory inv = BackpackInventoryHolder.asEmptyInventory(cache, owner, backpack, contents.length);
        inv.setContents(contents);
        
        return inv;
    }
    
    @Override
    public Map<UUID, Map<String, Inventory>> loadAll() {
        Map<UUID, Map<String, Inventory>> loaded = new HashMap();
        for (File file : data_dir.listFiles(dotDatFilter)) {
            UUID owner = UUID.fromString(dotDatFilter.stripExtension(file.getName()));
            Map<String, Inventory> ownerPacks = new HashMap();
            loaded.put(owner, ownerPacks);
            
            NBTTagCompound tag = getNBT(owner.toString());
            if (tag.isEmpty()) {
                continue;
            }
            
            // iterate all keys present on TagCompound
            for (Object o : tag.c()) {
                // load backpack and put in map
                ownerPacks.put((String) o, loadBackpack(owner, (String) o));
            }
        }
        return loaded;
    }
    
    private void savePlayerFile(String player) throws IOException {
        File file = getFile(player);
        NBTCompressedStreamTools.a(getNBT(player), new FileOutputStream(file));
    }

    private NBTTagCompound getNBT(String player) {
        NBTTagCompound tag = tags.get(player);
        if (tag == null) {
            File file = getFile(player);
            if (file.exists()) {
                try {
                    tag = NBTCompressedStreamTools.a(new FileInputStream(file));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                tag = new NBTTagCompound();
            }
            tags.put(player, tag);
        }
        return tag;
    }

    private File getFile(String player) {
        return new File(data_dir, player + ".dat");
    }

}
