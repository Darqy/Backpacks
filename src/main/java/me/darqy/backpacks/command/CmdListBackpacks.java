package me.darqy.backpacks.command;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import me.darqy.backpacks.BackpacksPlugin;
import me.darqy.backpacks.io.BackpackGroupCache;

public class CmdListBackpacks implements CommandExecutor {

    private final BackpacksPlugin plugin;

    public CmdListBackpacks(BackpacksPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] args) {
        boolean other = Permissions.listBackpacksOther(s);
        if (!Permissions.listBackpacks(s) && !other) {
            s.sendMessage(ChatColor.RED + "You don't have permission.");
            return true;
        }
        
        String world = args.length >= 1 ? args[0] : s instanceof Player? ((Player)s).getWorld().getName() : null;
        if (world == null) {
            s.sendMessage(ChatColor.RED + "Missing world parameter!");
            return true;
        }
        
        String player = s.getName();
        if (args.length >= 2 && other) {
            player = args[1];
        }
        
        UUID owner = BackpacksPlugin.getOfflinePlayerUUID(player);
        if (owner == null) {
            s.sendMessage(ChatColor.RED + "Invalid player.");
            return true;
        }

        BackpackGroupCache cache = plugin.getGroupCache(world);
        if (cache == null) {
            s.sendMessage(ChatColor.RED + "Sorry, can't do that in this world.");
            return true;
        }
        
        List<String> packs = cache.getBackpackNames(owner);
        
        if (packs.isEmpty()) {
            s.sendMessage(ChatColor.YELLOW + "No backpacks found");
            return true;
        }
        
        Collections.sort(packs);
        
        s.sendMessage(ChatColor.YELLOW + "Backpacks:");
        for (String name : packs) {
           s.sendMessage(ChatColor.YELLOW + "- " + name);
        }
        return true;
    }
}
