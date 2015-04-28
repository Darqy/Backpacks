package me.darqy.backpacks;

import java.io.File;
import java.io.IOException;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class BackpacksConfig {
    
    private static File file;
    private static FileConfiguration config;
    
    private static long saveInterval = (long) 2.5 * 20 * 60;
    private static long savesPerTick = 3;
    private static long saveTickDelay = 5;
    private static boolean configuredOnly = true;
    private static int maximumBackpacks = 8;
    private static String inventoryTitleFormat;
    private static String backend = "nbt";
    private static String convert = "false";
    private static boolean debug = false;
    
    public static void load(File config_file) {
        file = config_file;
        config = YamlConfiguration.loadConfiguration(file);

        saveInterval = config.getLong("save-interval");
        savesPerTick = config.getLong("save-per-tick");
        saveTickDelay = config.getLong("save-tick-delay");
        configuredOnly = config.getBoolean("configured-world-only");
        maximumBackpacks = config.getInt("maximum-backpacks-per-group");
        inventoryTitleFormat = config.getString("inventory-title-format");
        backend = config.getString("backend");
        convert = config.getString("convert");
        debug = config.getBoolean("debug");
    }
    
    
    /**
     * The interval, in ticks (1/20 second), between backpack saves to disk
     */
    public static long getSaveInterval() {
        return saveInterval;
    }
    
    /**
     * How many backpacks to save per tick
     */
    public static long getSavesPerTick() {
        return savesPerTick;
    }
    
    /**
     * Ticks delay between each run of save-per-tick
     */
    public static long getSaveTickDelay() {
        return saveTickDelay;
    }
    
    /**
     * If true, a player will not be able to use backpack commands
     * if the world isn't properly configured in groups.yml
     */
    public static boolean getConfiguredWorldsOnly() {
        return configuredOnly;
    }
    
    /**
     * The limit of backpacks a player may have per configured group
     * of worlds
     */
    public static int getMaximumBackpacks() {
        return maximumBackpacks;
    }
    
    /**
     * Formats the string for use as an inventory title
     */
    public static String formatInventoryTitle(String s) {
        return ChatColor.translateAlternateColorCodes('&', String.format(inventoryTitleFormat, s));
    }
    
    /**
     * The backend used to save backpacks
     */
    public static String getBackend() {
        return backend;
    }
    
    /**
     * Which backend to convert from
     */
    public static String getConverter() {
        return convert;
    }
    
    /**
     * Debug enabled
     */
    public static boolean debugEnabled() {
        return debug;
    }
    
    public static void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static FileConfiguration getConfig() {
        return config;
    }
    
}
