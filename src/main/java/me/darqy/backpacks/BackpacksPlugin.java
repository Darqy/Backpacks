package me.darqy.backpacks;

import com.daemitus.deadbolt.Deadbolt;
import me.darqy.backpacks.util.SnooperApi;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import me.darqy.backpacks.command.*;
import me.darqy.backpacks.io.BackpackGroupCache;
import me.darqy.backpacks.io.BackpackIOHandler;
import me.darqy.backpacks.io.NBTIOHandler;
import me.darqy.backpacks.io.YAMLIOHandler;
import me.darqy.backpacks.util.FileUtil;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.event.HandlerList;
import org.yi.acru.bukkit.Lockette.Lockette;

public class BackpacksPlugin extends JavaPlugin {

    private static final String NBT_CLASS = "net.minecraft.server.v1_8_R2.NBTBase";
    /** Map of group names to their cache **/
    private final Map<String, BackpackGroupCache> group_caches = new HashMap();
    
    private GroupConfig groupConfig;
    public static File GROUPS_FOLDER;
    
    private boolean hasLockette;
    private boolean hasDeadbolt;
    
    private boolean isMinecraftCompatible = false;

    public static enum Backend {
        YAML, NBT, ERROR
    }
    
    private Backend backend;

    @Override
    public void onEnable() {
        try {
            reloadConfiguration();
            backend = getBackend(BackpacksConfig.getBackend());
            if (backend == Backend.ERROR) {
                getLogger().warning("Misconfigured backend. Defaulting to yaml.");
                BackpacksConfig.getConfig().set("backend", "yaml");
                BackpacksConfig.save();
                backend = Backend.YAML;
            }

            getServer().getPluginManager().registerEvents(new BackpackListener(), this);
            
            handleConversion();
            initHooks();
            registerCommands();
            SnooperApi.initialize(this);
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Error enabling:", e);
            setEnabled(false);
        }
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
        disableAllCaches();
    }
    
    public void debug (String message, Object... args) {
        if (BackpacksConfig.debugEnabled()) {
            getLogger().log(Level.INFO, "[Debug] " + message, args);
        }
    }

    public void disableAllCaches() {
        for (BackpackGroupCache cache : group_caches.values()) {
            cache.disable();
        }
        
        group_caches.clear(); // clear active caches
    }
    
    public BackpackGroupCache getGroupCache(World world) {
        return getGroupCache(world.getName());
    }

    public BackpackGroupCache getGroupCache(String world) {
        if (BackpacksConfig.getConfiguredWorldsOnly() && !groupConfig.configured(world)) {
            return null;
        }
        String group = groupConfig.getGroup(world);
        
        BackpackGroupCache mngr = group_caches.get(group);
        
        if (mngr == null) {
            mngr = initGroupCache(group, backend, true);
            group_caches.put(group, mngr);
        }

        return mngr;
    }

    private Backend getBackend(String configured) {
        if (configured.equalsIgnoreCase("nbt")) {
            try {
                Class.forName(NBT_CLASS);
                isMinecraftCompatible = true;
                return Backend.NBT;
            } catch (ClassNotFoundException ex) {
                getLogger().log(Level.WARNING, "Attempted to use NBT backend but CraftBukkit versions "
                        + "do not match. Defaulting to YAML.");
                return Backend.YAML;
            }
        } else if (configured.equalsIgnoreCase("yaml")) {
            return Backend.YAML;
        }
        return Backend.ERROR;
    }
    
    private BackpackIOHandler getIOHandler(BackpackGroupCache cache, String group, BackpacksPlugin.Backend backend) {
        switch (backend) {
            case NBT:
                return new NBTIOHandler(cache, new File(BackpacksPlugin.GROUPS_FOLDER, "nbt" + File.separator + group));
            case YAML:
            default:
                return new YAMLIOHandler(cache, new File(BackpacksPlugin.GROUPS_FOLDER, "yaml" + File.separator + group));
        }
    }
    
    private BackpackGroupCache initGroupCache(String group, Backend backend, boolean scheduleAutosave) {
        BackpackGroupCache cache = new BackpackGroupCache(this,
                group,
                BackpacksConfig.getSaveInterval(),
                BackpacksConfig.getSavesPerTick(),
                BackpacksConfig.getSaveTickDelay()
        );
        
        BackpackIOHandler ioHandler = getIOHandler(cache, group, backend);
        cache.init(ioHandler, scheduleAutosave);
        return cache;
    }

    private void handleConversion() {
        Backend converting = getBackend(BackpacksConfig.getConverter());
        if (converting == Backend.ERROR || converting == backend) {
            return;
        }
        
        File data_dir;
        switch (converting) {
            case NBT:
                data_dir = new File(GROUPS_FOLDER, "nbt");
                break;
            case YAML:
                data_dir = new File(GROUPS_FOLDER, "yaml");
                break;
            default: return;
        }
        
        if (!data_dir.exists()) {
            getLogger().warning("Trying to convert from a format that doesn't exist.. aborting.");
            return;
        }
        
        backup(backend, true); // backup and delete the backend we're converting to
        for (File file : data_dir.listFiles()) {
            if (file.isDirectory()) {
                String group = file.getName();
                long start = System.currentTimeMillis();
                BackpackGroupCache converFrom = initGroupCache(group, converting, false);
                // load all that we wish to convert
                converFrom.loadAll();
                // get manager to save to
                BackpackGroupCache convertTo = initGroupCache(group, backend, true);
                // transfer backpacks to new cache
                convertTo.loadAll(converFrom.getAllCached());
                // save all backpacks on convertTo cache
                convertTo.saveAllBackpacks();
                long finish = System.currentTimeMillis() - start;
                getLogger().log(Level.INFO, "Converted group " + group + " to " + backend + " from " + converting + " in " + finish + "ms.");
                group_caches.put(group, convertTo);
            }
        }
        backup(converting, true); // backup converted files and delete
        BackpacksConfig.getConfig().set("convert", "false");
        BackpacksConfig.save();
    }
    
    public void reload() {
        setEnabled(false);
        setEnabled(true);
    }
    
    public void backupData() {
        backup(backend, false);
    }
 
    private void backup(Backend backend, boolean removeExisting) {
        File folder;
        File output;
        switch(backend) {
            case NBT:
                folder = new File(GROUPS_FOLDER, "nbt");
                output = new File(getDataFolder(), "nbt-" + FileUtil.getFileTimestamp() + ".zip");
                break;
            case YAML:
                folder = new File(GROUPS_FOLDER, "yaml");
                output = new File(getDataFolder(), "yaml-" + FileUtil.getFileTimestamp() + ".zip");
                break;
            default:
                return;
        }
        
        for (BackpackGroupCache cache : group_caches.values()) {
            cache.saveAllBackpacks();
        }
        
        FileUtil.zip(folder, output);
        if (removeExisting) {
            FileUtil.delete(folder);
        }
    }

    private void reloadConfiguration() throws IOException {
        File groupsFile = new File(getDataFolder(), "groups.yml");

        if (!groupsFile.exists()) {
            saveResource("groups.yml", false);
        }

        groupConfig = new GroupConfig(YamlConfiguration.loadConfiguration(groupsFile));
        GROUPS_FOLDER = new File(getDataFolder(), "groups/");

        File configFile = new File(getDataFolder(), "config.yml");

        if (!configFile.exists()) {
            saveResource("config.yml", false);
        }

        BackpacksConfig.load(configFile);
    }

    private void initHooks() {
        hasLockette = getServer().getPluginManager().isPluginEnabled("Lockette");
        hasDeadbolt = getServer().getPluginManager().isPluginEnabled("Deadbolt");
    }

    private void registerCommands() {
        getCommand("createpack").setExecutor(new CmdCreateBackpack(this));
        getCommand("backpack").setExecutor(new CmdBackpack(this));
        getCommand("inspectpack").setExecutor(new CmdInspectBackpack(this));
        getCommand("listpacks").setExecutor(new CmdListBackpacks(this));
        getCommand("packutils").setExecutor(new CmdBackpackUtils(this));
        getCommand("backpacks").setExecutor(new CmdBackpacks(this));
    }
    
    public boolean isMinecraftCompatible() {
        return isMinecraftCompatible;
    }

    public boolean checkProtection(Player p, org.bukkit.block.Block b) {
        if (hasLockette) {
            return Lockette.isProtected(b) ? Lockette.isUser(b, p.getName(), true) : true;
        }
        if (hasDeadbolt) {
            return Deadbolt.isProtected(b) ? Deadbolt.isAuthorized(p, b) : true;
        }
        return true;
    }

    public static Player matchPlayer(CommandSender sender, String target) {
        Player player = Bukkit.getPlayer(target);
        if (player == null) {
            sender.sendMessage(ChatColor.RED + target + " not found online");
        }
        return player;
    }
    
    public static UUID getOfflinePlayerUUID(String playerName) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(playerName);
        return op.hasPlayedBefore()? op.getUniqueId() : null;
    }
    
}
