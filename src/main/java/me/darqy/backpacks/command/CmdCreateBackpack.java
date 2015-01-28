package me.darqy.backpacks.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import me.darqy.backpacks.BackpackManager;
import me.darqy.backpacks.BackpacksPlugin;
import me.darqy.backpacks.BackpacksConfig;
import me.darqy.backpacks.PlayerBackpacks;

public class CmdCreateBackpack implements CommandExecutor {
    
    private BackpacksPlugin plugin;
    
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
        
        String player = s.getName();
        boolean other = Permissions.createBackpackOther(s);
        if (args.length >= 2 && other) {
            player = args[1];
        }
        
        String world = args.length >= 3? args[2] : s instanceof Player? ((Player)s).getWorld().getName() : null;
        if (world == null) {
            s.sendMessage(ChatColor.RED + "Missing world parameter!");
            return true;
        }

        BackpackManager manager = plugin.getBackpackManager(world);
        if (manager == null) {
            s.sendMessage(ChatColor.RED + "Sorry, can't do that in this world.");
            return true;
        }
        
        PlayerBackpacks backpacks = manager.getPlayerBackpacks(player);

        if (backpacks.hasBackpack(backpack)) {
            s.sendMessage(ChatColor.RED + "That backpack already exists.");
            return true;
        }
        
        if (player.equals(s.getName()) && !canCreateMoreBackpacks(manager, s)) {
            s.sendMessage(ChatColor.RED + "Sorry, you've reached your backpack"
                            + " limit of " + pack_limit);
            return true;
        }
        
        backpacks.createBackpack(backpack);
        s.sendMessage(ChatColor.YELLOW + "Created the new backpack: \"" + backpack + "\"");

        return true;
    }
    
    
    public static boolean canCreateMoreBackpacks(BackpackManager manager, CommandSender sender) {
        if (Permissions.createBackpackLimitBypass(sender)) {
            return true;
        }

        int cap = BackpacksConfig.getMaximumBackpacks();
        if (cap <= 0) {
            return true;
        }

        int count = manager.getBackpackCount(sender.getName());
        int max = Permissions.createBackpackLimit(sender, cap);

        return count < max;
    }
    
}