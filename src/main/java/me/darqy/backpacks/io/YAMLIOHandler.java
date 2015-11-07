package me.darqy.backpacks.io;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import me.darqy.backpacks.BackpackInventoryHolder;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class YAMLIOHandler implements BackpackIOHandler {
    
    private static final String slot = "slot.";
    
    private final BackpackGroupCache cache;
    private final File data_dir;
    
    private static final ExtensionFileFilter dotYmlFilter = new ExtensionFileFilter(".yml");
    
    public YAMLIOHandler(BackpackGroupCache cache, File file) {
        this.cache = cache;
        this.data_dir = file;
        data_dir.mkdirs();
        if (!data_dir.isDirectory()) {
            throw new IllegalArgumentException("Given path must be a directory.");
        }
    }
    
    @Override
    public Inventory loadBackpack(UUID owner, String backpack) {
        File file = getBackpackFile(owner, backpack);
        if (!file.exists()) {
            return null;
        }
        
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        
        
        int size = config.getInt("size", 54); // default size of 54 slots..
        Inventory inv = BackpackInventoryHolder.asEmptyInventory(cache, owner, backpack, size);
        for (int i = 0; i < size; i++) {
            String path = slot + i;
            if (!config.contains(path)) {
                continue;
            }
            ItemStack is = config.getItemStack(path);
            if (is != null) {
                inv.setItem(i, is);
            }
        }
        
        return inv;
    }
    
    @Override
    public Map<UUID, Map<String, Inventory>> loadAll() {
        Map<UUID, Map<String, Inventory>> loaded = new HashMap();
        
        for (File file : data_dir.listFiles()) {
            if (!file.isDirectory()) {
                continue;
            }
            UUID owner = UUID.fromString(file.getName());
            Map<String, Inventory> ownerPacks = new HashMap();
            loaded.put(owner, ownerPacks);
            
            for (File packFile : file.listFiles(dotYmlFilter)) {
                String backpack = dotYmlFilter.stripExtension(packFile.getName());
                ownerPacks.put(backpack, loadBackpack(owner, backpack));
            }
        }
        
        return loaded;
    }

    @Override
    public void saveBackpack(UUID owner, String backpack, Inventory inventory) throws IOException {
        YamlConfiguration config = new YamlConfiguration();
        config.set("size", inventory.getSize());
        int i = 0;
        for (ItemStack item : inventory.getContents()) {
            if (item != null) {
                config.set(slot + i, item);
            }
            i++;
        }
        config.save(getBackpackFile(owner, backpack));
    }

    @Override
    public List<String> getBackpackList(UUID owner) {
        File playerFolder = new File(data_dir, owner.toString());
        if (!playerFolder.exists()) {
            return Collections.EMPTY_LIST;
        }
        
        ArrayList<String> list = new ArrayList();
        for (File child : playerFolder.listFiles(dotYmlFilter)) {
            list.add(dotYmlFilter.stripExtension(child.getName()));
        }
        return list;
    }

    @Override
    public void removeBackpack(UUID owner, String backpack) {
        File file = getBackpackFile(owner, backpack);
        if (file.exists()) {
            file.delete();
        }
    }
    
    private File getBackpackFile(UUID owner, String backpack) {
        return new File(data_dir, owner.toString() + File.separator + backpack + ".yml");
    }
    
}
