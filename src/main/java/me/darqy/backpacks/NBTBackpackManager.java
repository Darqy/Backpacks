package me.darqy.backpacks;

import me.darqy.backpacks.util.InventoryUtil;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.v1_8_R1.NBTCompressedStreamTools;
import net.minecraft.server.v1_8_R1.NBTTagCompound;
import org.bukkit.inventory.Inventory;

public class NBTBackpackManager extends BackpackManager {

    private File data_dir;
    private Map<String, NBTTagCompound> tags = new HashMap();
    
    private static final int NBT_TAG_COMPOUND_ID = 10;

    public NBTBackpackManager(File dir) {
        data_dir = dir;
        data_dir.mkdirs();
        if (!data_dir.isDirectory()) {
            throw new IllegalArgumentException("File must be a directory!");
        }
    }

    @Override
    public void renameBackpack(String player, String oldBackpack, String newBackpack) {
        getNBT(player).remove(oldBackpack);
        getPlayerBackpacks(player).renameBackpack(oldBackpack, newBackpack);
        
        try {
            saveBackpack(player, newBackpack);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public List<String> getBackpackList(String player) {
        List<String> list = new ArrayList();
        for (Object o : getNBT(player).c()) {
            list.add((String) o);
        }
        return list;
    }

    @Override
    public int getBackpackCount(String player) {
        return getNBT(player).c().size();
    }
    
    @Override
    public void saveBackpacks() throws IOException {
        for (PlayerBackpacks player : backpacks.values()) {
            for (Backpack pack : player.getBackpacks()) {
                saveBackpack(player.getPlayer(), pack);
            }
            savePlayerFile(player.getPlayer());
        }
    }
    
    @Override
    public void saveBackpack(String player, String backpack) throws IOException {
        Backpack pack = getPlayerBackpacks(player).getBackpack(backpack);
        if (pack != null) {
            saveBackpack(player, pack);
        }
    }
    
    @Override
    public boolean loadBackpack(String player, String backpack) {
        NBTTagCompound tag = getNBT(player);
        if (!tag.hasKeyOfType(backpack, NBT_TAG_COMPOUND_ID)) {
            return false;
        }
        Inventory inv = InventoryUtil.invFromNbt(tag.getCompound(backpack), "Backpack - " + backpack);
        getPlayerBackpacks(player).setBackpack(backpack, new Backpack(backpack, inv));
        return true;
    }
    
    @Override
    public void loadAll() {
        for (File file : data_dir.listFiles()) {
            if (file.getName().endsWith(".dat")) {
                String player = file.getName().substring(0, file.getName().length() - 4);
                NBTTagCompound tag = getNBT(player);

                if (tag.isEmpty()) {
                    continue;
                }                

                for (Object o : tag.c()) {
                    loadBackpack(player, (String) o);
                }
            }
        }
    }
    
    private void saveBackpack(String player, Backpack backpack) throws IOException {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("Name", backpack.getName());
        InventoryUtil.invToNbt(tag, backpack.getInventory());
        getNBT(player).set(backpack.getName(), tag);
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
                } catch (FileNotFoundException e) {
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
