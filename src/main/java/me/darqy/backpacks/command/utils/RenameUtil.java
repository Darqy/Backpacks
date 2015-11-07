package me.darqy.backpacks.command.utils;

import java.util.UUID;
import me.darqy.backpacks.io.BackpackGroupCache;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class RenameUtil implements PackUtil {

    @Override
    public String getName() {
        return "rename";
    }    
    
    @Override
    public void sendHelpText(CommandSender s, String alias, String usage) {
        s.sendMessage("");
        s.sendMessage(ChatColor.YELLOW + "Usage: " + usage.replace("(p:[backpack])", "[p:old-pack]"));
        s.sendMessage(ChatColor.YELLOW + " Rename a backpack.");
        s.sendMessage(ChatColor.GRAY + "---");
        s.sendMessage(ChatColor.YELLOW + "Examples:");
        s.sendMessage(ChatColor.YELLOW + " - " + ChatColor.RED + "/" + alias +" rename diamonds p:mining " + ChatColor.YELLOW
                + "- Rename your \"mining\" backpack to \"diamonds\".");
    }

    @Override
    public String getUsage() {
        return "rename [new-pack]";
    }

    @Override
    public void use(Player p, BackpackGroupCache cache, UUID owner, Inventory inv, String backpack, String usage, String[] args) {
            if (args.length < 3) {
                p.sendMessage(ChatColor.RED + "Not enough arguments.");
                p.sendMessage(usage.replace("(p:[backpack])", "[p:old-pack]"));
                return;
            }
            // handling rename, strips special characters
            String newname = args[0].toLowerCase().replaceAll("[^\\w\\s-]", "");
            if (newname.length() > 16) {
                p.sendMessage(ChatColor.YELLOW + "Please choose a backpack name under 16 characters.");
                return;
            }
            
            if (cache.getBackpackNames(owner).contains(newname)) {
                p.sendMessage(ChatColor.RED + "A backpack named \"" + newname + "\", already exists.");
                return;
            }
            
            // do the actual rename
            cache.renameBackpack(owner, backpack, newname);
        
            p.sendMessage(ChatColor.YELLOW + "Your \"" + backpack + "\" backpack is renamed to: \"" + newname + "\".");
    }

}
