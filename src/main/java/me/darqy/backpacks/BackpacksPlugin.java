package me.darqy.backpacks;

import com.daemitus.deadbolt.Deadbolt;
import me.darqy.backpacks.util.SnooperApi;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import me.darqy.backpacks.command.*;
import me.darqy.backpacks.util.FileUtil;
import org.bukkit.event.HandlerList;
import org.bukkit.scheduler.BukkitRunnable;
import org.yi.acru.bukkit.Lockette.Lockette;

public class BackpacksPlugin extends JavaPlugin {

    private static final String NBT_CLASS = "net.minecraft.server.v1_8_R1.NBTBase";
    
    private final Map<String, BackpackManager> managers = new HashMap();
    
    private GroupConfig groupConfig;
    private File groupsFolder;
    
    private boolean hasLockette;
    private boolean hasDeadbolt;

    private enum Backend {

        YAML, NBT, NONE
    }
    private Backend backend;

    @Override
    public void onEnable() {
        try {
            reloadConfiguration();
            backend = getBackend(BackpacksConfig.getBackend());
            if (backend == Backend.NONE) {
                getLogger().warning("Misconfigured backend. Defaulting to yaml.");
                BackpacksConfig.getConfig().set("backend", "yaml");
                BackpacksConfig.save();
                backend = Backend.YAML;
            }
            handleConversion();
            initHooks();
            registerCommands();
            scheduleBackpackSaver();
            SnooperApi.initialize(this);
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Error enabling:", e);
            setEnabled(false);
        }
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
        closeAllBackpacks();
        saveAllBackpacks();
    }

    public void saveAllBackpacks() {
        for (BackpackManager mngr : managers.values()) {
            try {
                mngr.saveBackpacks();
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, "Error while saving backpack!", e);
            }
        }
    }

    public void closeAllBackpacks() {
        for (BackpackManager mngr : managers.values()) {
            mngr.closeBackpacks();
        }
    }

    public BackpackManager getManager(World world) {
        return getBackpackManager(world.getName());
    }

    public BackpackManager getBackpackManager(String world) {
        if (BackpacksConfig.getConfiguredWorldsOnly() && !groupConfig.configured(world)) {
            return null;
        }
        String group = groupConfig.getGroup(world);
        
        BackpackManager mngr = managers.get(group);
        
        if (mngr == null) {
            mngr = newBackpackManager(group, backend);
            managers.put(group, mngr);
        }

        return mngr;
    }

    private Backend getBackend(String configured) {
        if (configured.equalsIgnoreCase("nbt")) {
            try {
                Class.forName(NBT_CLASS);
                return Backend.NBT;
            } catch (ClassNotFoundException ex) {
                getLogger().log(Level.WARNING, "Attempted to use NBT backend but CraftBukkit versions "
                        + "do not match. Defaulting to YAML.");
                return Backend.YAML;
            }
        } else if (configured.equalsIgnoreCase("yaml")) {
            return Backend.YAML;
        }
        return Backend.NONE;
    }

    private BackpackManager newBackpackManager(String group, Backend backend) {
        BackpackManager manager;
        switch (backend) {
            case NBT:
                manager = new NBTBackpackManager(new File(groupsFolder, "nbt" + File.separator + group));
                break;
            case YAML:
            default:
                manager = new YamlBackpackManager(new File(groupsFolder, "yaml" + File.separator + group));
        }
        return manager;
    }

    private void handleConversion() {
        Backend converting = getBackend(BackpacksConfig.getConverter());
        if (converting == Backend.NONE || converting == backend) {
            return;
        }
        
        File data_dir;
        switch (converting) {
            case NBT:
                data_dir = new File(groupsFolder, "nbt");
                break;
            case YAML:
                data_dir = new File(groupsFolder, "yaml");
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
                try {
                    String group = file.getName();
                    long start = System.currentTimeMillis();
                    BackpackManager manager = newBackpackManager(group, converting);
                    // load all that we wish to convert
                    manager.loadAll();
                    // get manager to save to
                    BackpackManager man = newBackpackManager(group, backend);
                    // transfer backpacks to new manager
                    man.setBackpacks(manager.getBackpacks());
                    // save, converting them to new format
                    man.saveBackpacks();
                    long finish = System.currentTimeMillis() - start;
                    getLogger().log(Level.INFO, "Converted group " + group + " to " + backend + " from " + converting + " in " + finish + "ms.");
                    managers.put(group, man);
                } catch (IOException ex) {
                    getLogger().log(Level.SEVERE, null, ex);
                }
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
                folder = new File(groupsFolder, "nbt");
                output = new File(getDataFolder(), "nbt-" + FileUtil.getFileTimestamp() + ".zip");
                break;
            case YAML:
                folder = new File(groupsFolder, "yaml");
                output = new File(getDataFolder(), "yaml-" + FileUtil.getFileTimestamp() + ".zip");
                break;
            default:
                return;
        }
        
        for (BackpackManager manager : managers.values()) {
            try {
                manager.saveBackpacks();
            } catch (IOException ex) {
                getLogger().log(Level.SEVERE, null, ex);
            }
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
        groupsFolder = new File(getDataFolder(), "groups/");

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

    private void scheduleBackpackSaver() {
        new BackpackSaver(this).runTaskTimer(this, 10L, BackpacksConfig.getSaveInterval());
    }

    private static class BackpackSaver extends BukkitRunnable {

        private BackpacksPlugin plugin;

        public BackpackSaver(BackpacksPlugin instance) {
            this.plugin = instance;
        }

        @Override
        public void run() {
            plugin.saveAllBackpacks();
        }
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
    
}
