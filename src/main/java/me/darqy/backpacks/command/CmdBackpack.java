package me.darqy.backpacks.command;

import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import me.darqy.backpacks.BackpackInventoryHolder;
import me.darqy.backpacks.BackpacksConfig;
import me.darqy.backpacks.BackpacksPlugin;
import me.darqy.backpacks.io.BackpackGroupCache;
import org.bukkit.inventory.Inventory;

public class CmdBackpack implements CommandExecutor {
    
    private final BackpacksPlugin plugin;
    
    public CmdBackpack(BackpacksPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] args) {
        if (!(s instanceof Player)) {
            s.sendMessage(ChatColor.RED + "This command is only available to players");
            return true;
        }
        
        Player player = (Player) s;
        UUID owner = player.getUniqueId();
        
        boolean named = Permissions.useBackpackNamed(s);
        if (!Permissions.useBackpack(s) && !named) {
            s.sendMessage(ChatColor.RED + "You don't have permission.");
            return true;
        }
        
        String backpack = args.length >= 1 && named?
                args[0].toLowerCase() : "default";

        BackpackGroupCache cache = plugin.getGroupCache(player.getWorld());
        if (cache == null) {
            s.sendMessage(ChatColor.RED + "You can't use this command in this world.");
            return true;
        }
        
        Inventory inv = cache.getBackpack(player.getUniqueId(), backpack);
        if (inv == null) {
            if (backpack.equals("default") && CmdCreateBackpack.canCreateMoreBackpacks(cache, player.getUniqueId(), Permissions.createBackpackLimit(s, BackpacksConfig.getMaximumBackpacks()), Permissions.createBackpackLimitBypass(s))) {
                s.sendMessage(ChatColor.YELLOW + "Default backpack not found... creating");
                
                // TODO: something for backpacks smaller than full size?
                inv = BackpackInventoryHolder.asEmptyInventory(cache, owner, backpack, 54);
                cache.setBackpack(owner, backpack, inv);
            } else {
                s.sendMessage(ChatColor.RED + "You don't have a backpack with that name. ");
                return true;
            }
        }
        
        player.openInventory(inv);
        return true;
    }
    
}