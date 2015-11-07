package me.darqy.backpacks.command.utils;

import java.util.UUID;
import me.darqy.backpacks.BackpackInventoryHolder;
import me.darqy.backpacks.BackpacksPlugin;
import me.darqy.backpacks.io.BackpackGroupCache;
import me.darqy.backpacks.util.InventoryUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class TransferUtil implements PackUtil {
    
    @Override
    public String getName() {
        return "transfer";
    }

    @Override
    public void sendHelpText(CommandSender s, String alias, String usage) {
        s.sendMessage("");
        s.sendMessage(ChatColor.YELLOW + "Usage: " + usage.replace("(p:[backpack])", "[p:from-backpack]"));
        s.sendMessage(ChatColor.YELLOW + " Similar to the chest utility, this transfers items between two backpacks or your inventory.");
        s.sendMessage(ChatColor.GRAY + "---");
        s.sendMessage(ChatColor.YELLOW + "- Simply enter the command and the transfer is completed instantly.");
        s.sendMessage(ChatColor.YELLOW + "- Items that don't fit will be left behind.");
        s.sendMessage(ChatColor.GRAY + "---");
        s.sendMessage(ChatColor.YELLOW + "Example usage:");
        s.sendMessage(ChatColor.YELLOW + " - " + ChatColor.RED + "/" + alias + " transfer stone stonebox p:magnet " + ChatColor.YELLOW
                    + "- Transfers as much stone as possible to \"stonebox\" backpack from your \"magnet\" backpack.");
        s.sendMessage(ChatColor.YELLOW + " - " + ChatColor.RED + "/" + alias + " transfer * random p:default " + ChatColor.YELLOW
                    + "- Transfers as many items as possible to your \"random\" backback from your default backpack.");
    }

    @Override
    public String getUsage() {
        return "packtransfer [item|id|all] [to-backpack]";
    }

    @Override
    public void use(Player p, BackpackGroupCache cache, UUID owner, Inventory inv, String backpack, String usage, String[] args) {
        if (args.length < 2) {
            p.sendMessage(ChatColor.RED + "Not enough arguments.");
            p.sendMessage(ChatColor.YELLOW + "Usage: " + usage.replace("(p:[backpack])", "[p:from-backpack]"));
            return;
        }

        String item = args[0].toLowerCase();
        
        if (!InventoryUtil.isValidMaterial(item) && !"all".equals(item) && !"*".equals(item)) {
            p.sendMessage(ChatColor.RED + "Invalid item name/ID. Use a valid item or 'all' or '*'.");
            return;
        }
        
        Inventory to = cache.getBackpack(owner, args[1].toLowerCase());
        if (to == null) {
            p.sendMessage(ChatColor.RED + "Invalid backpack.");
            return;
        }
        
        if (to.equals(inv)) {
            p.sendMessage(ChatColor.RED + "Must select two different backpacks.");
            return;
        }
        
        if (InventoryUtil.transferItems(inv, to, item)) {
            // Mark backpacks as modified
            BackpackInventoryHolder.of(inv).setUnsavedChanges(true);
            BackpackInventoryHolder.of(to).setUnsavedChanges(true);
            p.sendMessage(ChatColor.YELLOW + "Backpack items from \"" + backpack + "\" transferred to \"" + args[1] + "\"!");
        } else {
            // this should never be reached unless something really bad happens.
            p.sendMessage(ChatColor.RED + "Transfer failed. Invalid item? Please retry the command.");
        }
    }
    
}
