package me.darqy.backpacks.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import me.darqy.backpacks.Backpack;
import me.darqy.backpacks.BackpackManager;
import me.darqy.backpacks.BackpacksPlugin;

public class CmdBackpack implements CommandExecutor {
    
    private BackpacksPlugin plugin;
    
    public CmdBackpack(BackpacksPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] args) {
        if (!(s instanceof Player)) {
            s.sendMessage(ChatColor.RED + "This command is only available to players");
            return true;
        }
        
        boolean named = Permissions.useBackpackNamed(s);
        if (!Permissions.useBackpack(s) && !named) {
            s.sendMessage(ChatColor.RED + "You don't have permission.");
            return true;
        }
        
        String backpack = args.length >= 1 && named?
                args[0].toLowerCase() : "default";

        BackpackManager manager = plugin.getManager(((Player) s).getWorld());
        if (manager == null) {
            s.sendMessage(ChatColor.RED + "Sorry, can't do that in this world.");
            return true;
        }
        
        Backpack pack = manager.getPlayerBackpacks(s.getName()).getBackpack(backpack);
        if (pack == null) {
            if (backpack.equals("default") && CmdCreateBackpack.canCreateMoreBackpacks(manager, s)) {
                s.sendMessage(ChatColor.YELLOW + "Default backpack not found... creating");
                
                pack = manager.getPlayerBackpacks(s.getName()).createBackpack(backpack);
            } else {
                s.sendMessage(ChatColor.RED + "You don't have that backpack.");
                return true;
            }
        }
        
        final Player player = (Player) s;
        pack.open(player);
        return true;
    }
    
}