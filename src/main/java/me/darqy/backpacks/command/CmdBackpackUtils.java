package me.darqy.backpacks.command;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.UUID;
import me.darqy.backpacks.BackpacksPlugin;
import me.darqy.backpacks.command.utils.*;
import me.darqy.backpacks.io.BackpackGroupCache;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class CmdBackpackUtils implements CommandExecutor {
    
    private final BackpacksPlugin plugin;
    
    private static final HashMap<String, PackUtil> backpackUtils = new HashMap();

    public CmdBackpackUtils(BackpacksPlugin instance) {
        this.plugin = instance;
        
        backpackUtils.put("magnet", new MagnetUtil(plugin));
        backpackUtils.put("chest", new ChestUtil(plugin));
        backpackUtils.put("rename", new RenameUtil());
        backpackUtils.put("empty", new EmptyUtil());
        backpackUtils.put("transfer", new TransferUtil());
     
        for (PackUtil util : backpackUtils.values()) {
            instance.getServer().getPluginManager().registerEvents(util, plugin);
        }
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String alias, String[] args) {
        if (!(s instanceof Player)) {
            s.sendMessage(ChatColor.RED + "This command is only available to players.");
            return true;
        }
        
        final Player p = (Player) s;
                        
        if (args.length < 1) {
            handleHelp(c, p, null, alias);
            return true;
        }
        
        if ("help".equalsIgnoreCase(args[0])) {
            handleHelp(c, p, args.length >= 2? getTool(args[1]): null, alias);
            return true;
        }

        final PackUtil action = getTool(args[0]);
        if (action == null) {
            handleHelp(c, p, null, alias);
            return true;
        }
        
        if (!Permissions.utilBackpack(s, action.getName())) {
            s.sendMessage(ChatColor.RED + "You don't have permission.");
            return true;
        }
        
        BackpackGroupCache cache = plugin.getGroupCache(p.getWorld());
        if (cache == null) {
            s.sendMessage(ChatColor.RED + "Sorry, you can't do that in this world.");
            return true;
        }
        
        String backpack = getExtraArg(args, "p:", "default");
        
        UUID owner = p.getUniqueId();
        if (Permissions.utilBackpackOther(s)) {
            String player = getExtraArg(args, "pl:", null);
            if (player != null) {
                owner = BackpacksPlugin.getOfflinePlayerUUID(player);
                if (owner == null) {
                    s.sendMessage(ChatColor.RED + "Player invalid.");
                    return true;
                }
            }
        }
        
        Inventory inv = cache.getBackpack(owner, backpack);
        if (inv == null) {
            s.sendMessage(ChatColor.RED + "That backpack doesn't exist.");
            return true;
        }
        
        String[] stripped = stripExtraArgs(args);
        String[] utilArgs = new String[stripped.length - 1];
        // cut first two elements from array
        System.arraycopy(stripped, 1, utilArgs, 0, stripped.length - 1);
        
        // format util's usage
        String utilUsage = getUsage(c, ChatColor.RED, alias, action.getUsage());
        // call util
        action.use(p, cache, owner, inv, backpack, utilUsage, utilArgs);
        
        return true;
    }
    
    public void handleHelp(Command c, Player p, PackUtil action, String l) {
        if (action != null) {
            action.sendHelpText(p, l, getUsage(c, ChatColor.RED, l, action.getUsage()));
        } else {
            sendUtils(p, l);
        }
    }
    
    private static PackUtil getTool(String filter) {
        for (Entry<String, PackUtil> ent : backpackUtils.entrySet()) {
            if (ent.getKey().equalsIgnoreCase(filter)) {
                return ent.getValue();
            }
        }  
        return null;
    }
    
    private static String getExtraArg(String[] args, String prefix, String def) {
        for (int i = args.length - 1; i > 0; i--) {
            String arg = args[i];
            if (arg.startsWith(prefix)) {
                String str = arg.substring(prefix.length());
                if (!str.isEmpty()) {
                    return str;
                }
            }
        }
        return def;
    }

    private static String[] stripExtraArgs(String[] args) {
        String[] stripped = new String[args.length];
        int count = 0;
        for (String arg : args) {
            if (!arg.contains(":")) {
                stripped[count++] = arg;
            }
        }
        String[] dest = new String[count];
        System.arraycopy(stripped, 0, dest, 0, count);
        return dest;
    }
    
    private static String getUsage(Command c, ChatColor color, String label, String usage) {
        return color + c.getUsage()
                .replace("[action]", usage)
                .replace("<command>", label);
    }
    
    private static void sendUtils(CommandSender sender, String l) {
            sender.sendMessage(ChatColor.YELLOW + "Unknown utility. Available utils: ");
            for (String tool : backpackUtils.keySet()) {
                sender.sendMessage("- " + ChatColor.AQUA + tool);
            }
            sender.sendMessage(ChatColor.YELLOW + "Do " + ChatColor.RED
                    + "/" + l + " help [util] " + ChatColor.YELLOW + "for information and usage");
    }
}
