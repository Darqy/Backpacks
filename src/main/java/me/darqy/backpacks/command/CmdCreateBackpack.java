package me.darqy.backpacks.command;

import java.util.UUID;
import me.darqy.backpacks.BackpackInventoryHolder;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import me.darqy.backpacks.BackpacksPlugin;
import me.darqy.backpacks.BackpacksConfig;
import me.darqy.backpacks.io.BackpackGroupCache;
import org.bukkit.inventory.Inventory;

public class CmdCreateBackpack implements CommandExecutor {
    
    private final BackpacksPlugin plugin;
    
    public CmdCreateBackpack(BackpacksPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] args) {
        final boolean named = Permissions.createBackpackNamed(s);
        final int pack_limit = Permissions.createBackpackLimit(s, BackpacksConfig.getMaximumBackpacks());
        if (pack_limit < 1 && !named) {
            s.sendMessage(ChatColor.RED + "You don't have permission.");
            return true;
        }
        
        String backpack = "default";
        if (args.length >= 1 && named) {
            backpack = args[0].toLowerCase();
            if (backpack.length() > 16) {
                s.sendMessage(ChatColor.YELLOW + "Please choose a backpack name under 16 characters");
                return true;
            }
        }
        
        String playerName = s.getName();
        boolean other = Permissions.createBackpackOther(s);
        if (args.length >= 2 && other) {
            playerName = args[1];
        }
        
        String world = args.length >= 3? args[2] : s instanceof Player? ((Player) s).getWorld().getName() : null;
        if (world == null) {
            s.sendMessage(ChatColor.RED + "Missing world parameter!");
            return true;
        }

        BackpackGroupCache cache = plugin.getGroupCache(world);
        if (cache == null) {
            s.sendMessage(ChatColor.RED + "Sorry, can't do that in this world.");
            return true;
        }
        
        UUID owner = BackpacksPlugin.getOfflinePlayerUUID(playerName);
        if (owner == null) {
            s.sendMessage(ChatColor.RED + "Invalid player.");
            return true;
        }

        if (cache.getBackpackNames(owner).contains(backpack)) {
            s.sendMessage(ChatColor.RED + "That backpack already exists.");
            return true;
        }
        
        if (playerName.equals(s.getName()) && !canCreateMoreBackpacks(cache, owner, Permissions.createBackpackLimit(s, BackpacksConfig.getMaximumBackpacks()), Permissions.createBackpackLimitBypass(s))) {
            s.sendMessage(ChatColor.RED + "Sorry, you've reached your limit of " + pack_limit + " backpacks.");
            return true;
        }
        
        Inventory inv = BackpackInventoryHolder.asEmptyInventory(cache, owner, backpack, 54);
        cache.setBackpack(owner, backpack, inv);
        s.sendMessage(ChatColor.YELLOW + "Created the new backpack: \"" + backpack + "\"");

        return true;
    }
    
    
    public static boolean canCreateMoreBackpacks(BackpackGroupCache cache, UUID owner, int limit, boolean canBypassLimit) {
        int cap = BackpacksConfig.getMaximumBackpacks();
        if (cap <= 0) {
            return true;
        }

        int count = cache.getBackpackCount(owner);
        return count < limit;
    }
    
}