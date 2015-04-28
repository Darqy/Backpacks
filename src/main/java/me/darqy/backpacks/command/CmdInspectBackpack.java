package me.darqy.backpacks.command;

import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import me.darqy.backpacks.BackpacksPlugin;
import me.darqy.backpacks.io.BackpackGroupCache;
import me.darqy.backpacks.util.SnooperApi;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;

public class CmdInspectBackpack implements CommandExecutor, Listener {
    
    private final BackpacksPlugin plugin;
    
    public CmdInspectBackpack(BackpacksPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] args) {
        if (!(s instanceof Player)) {
            s.sendMessage(ChatColor.RED + "This command is only available to players");
            return true;
        }
        
        if (!Permissions.inspectBackpack(s)) {
            s.sendMessage(ChatColor.RED + "You don't have permission.");
            return true;
        }
        
        if (args.length < 1) {
            return false;
        }
        
        String player = args[0];
        UUID owner = BackpacksPlugin.getOfflinePlayerUUID(player);
        if (owner == null) {
            s.sendMessage(ChatColor.RED + "Player invalid.");
            return true;
        }
        
        String backpack = "default";
        if (args.length >= 2) {
            backpack = args[1].toLowerCase();
        }
                
        final Player p = (Player) s;
        String world = args.length >= 3? args[2] : p.getWorld().getName();
        if (world == null) {
            s.sendMessage(ChatColor.RED + "Missing world parameter!");
            return true;
        }

        BackpackGroupCache cache = plugin.getGroupCache(world);
        if (cache == null) {
            s.sendMessage(ChatColor.RED + "Sorry, can't do that in this world.");
            return true;
        }
        
        Inventory inv = cache.getBackpack(owner, backpack);
        if (inv == null) {
            s.sendMessage(ChatColor.RED + "That backpack doesn't exist");
            return true;
        }
        
        InventoryView view = p.openInventory(inv);
        if (!Permissions.inspectAndEditBackpack(s)) {
            SnooperApi.registerSnooper(p,
                    SnooperApi.constraintHalf(SnooperApi.InvSection.TOP, view));
        }

        s.sendMessage(ChatColor.YELLOW + "Viewing " + player + "'s \"" + backpack + "\" backpack");
        return true;
    }
    
}
