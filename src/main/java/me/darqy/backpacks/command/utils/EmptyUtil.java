package me.darqy.backpacks.command.utils;

import java.util.UUID;
import me.darqy.backpacks.BackpackInventoryHolder;
import me.darqy.backpacks.io.BackpackGroupCache;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class EmptyUtil implements PackUtil {

    @Override
    public String getName() {
        return "empty";
    }
    
    @Override
    public void sendHelpText(CommandSender s, String alias, String usage) {
        s.sendMessage("");
        s.sendMessage(ChatColor.YELLOW + "Usage: " + usage.replace("(p:[backpack])", ""));
        s.sendMessage(ChatColor.YELLOW + " Empties a backpack.");
        s.sendMessage(ChatColor.GRAY + "---");
        s.sendMessage(ChatColor.GOLD + "- WARNING: this operation cannot be undone!!"
                + " You will lose the items in the pack forever.");
        s.sendMessage(ChatColor.GRAY + "---");
        s.sendMessage(ChatColor.YELLOW + "Example usage:");
        s.sendMessage(ChatColor.YELLOW + " - " + ChatColor.RED + "/" + alias + " empty default " + ChatColor.YELLOW
                + "- Empties your \"default\" backpack.");
    }

    @Override
    public String getUsage() {
        return "empty [backpack]";
    }

    @Override
    public void use(Player p, BackpackGroupCache cache, UUID owner, Inventory inv, String backpack, String usage, String[] args) {
        if (args.length < 1) {
            p.sendMessage(ChatColor.RED + "Not enough arguments.");
            p.sendMessage(usage);
            return;
        }
        
        inv.clear();
        
        // flag backpack as modified
        BackpackInventoryHolder.of(inv).setUnsavedChanges(true);
        p.sendMessage(ChatColor.YELLOW + "Your \"" + backpack + "\" backpack was emptied.");
    }

}
