package me.darqy.backpacks.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import me.darqy.backpacks.BackpacksPlugin;

public class CmdBackpacks implements CommandExecutor {
    
    private BackpacksPlugin plugin;
    
    public CmdBackpacks(BackpacksPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] args) {
        s.sendMessage(ChatColor.GREEN + "[Backpacks] - ver. " + plugin.getDescription().getVersion());
        if (!Permissions.backpacksAdmin(s)) return true;
        if (args.length < 1) {
            s.sendMessage(ChatColor.RED + "[Backpacks] Unknown action. Available: reload, backup");
        } else {
            String command = args[0].toLowerCase();
            if ("reload".equals(command)) {
                plugin.reload();
                s.sendMessage(ChatColor.GREEN + "[Backpacks] Reloaded!");
            } else if ("backup".equals(command)) {
                plugin.backupData();
                s.sendMessage(ChatColor.GREEN + "[Backpacks] Backpacks archived!");
            }
        }
        return true;
    }
    
}