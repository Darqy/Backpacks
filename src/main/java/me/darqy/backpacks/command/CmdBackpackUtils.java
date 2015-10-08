package me.darqy.backpacks.command;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import me.darqy.backpacks.BackpackInventoryHolder;
import me.darqy.backpacks.BackpacksPlugin;
import me.darqy.backpacks.io.BackpackGroupCache;
import me.darqy.backpacks.util.InventoryUtil;
import me.darqy.backpacks.util.NMSUtil;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class CmdBackpackUtils implements CommandExecutor {
    
    private final BackpacksPlugin plugin;
    private final MagnetListener magnet = new MagnetListener();
    
    private static final String[] TOOLS = new String[] {
        "magnet", "chest", "rename", "empty",
    };
    
    private static final String MAGNET_USAGE = "magnet";
    private static final String CHEST_USAGE = "chest [put|take] [item|id|all]";
    private static final String RENAME_USAGE = "rename [new-pack]";
    private static final String EMPTY_USAGE = "empty";
    
    public CmdBackpackUtils(BackpacksPlugin instance) {
        this.plugin = instance;
        
        plugin.getServer().getPluginManager().registerEvents(magnet, plugin);
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] args) {
        if (!(s instanceof Player)) {
            s.sendMessage(ChatColor.RED + "This command is only available to players");
            return true;
        }
        
        final Player p = (Player) s;
                        
        if (args.length < 1) {
            handleHelp(c, p, null, l);
            return true;
        }
        
        if ("help".equalsIgnoreCase(args[0])) {
            handleHelp(c, p, args.length >= 2? args[1].toLowerCase(): null, l);
            return true;
        }

        final String action = getTool(args[0]);
        if (action == null) {
            handleHelp(c, p, args[0], l);
            return true;
        }
        
        if (!Permissions.utilBackpack(s, action)) {
            s.sendMessage(ChatColor.RED + "You don't have permission.");
            return true;
        }
        
        BackpackGroupCache cache = plugin.getGroupCache(p.getWorld());
        if (cache == null) {
            s.sendMessage(ChatColor.RED + "Sorry, can't do that in this world.");
            return true;
        }
        
        String backpack = getExtraArg(args, "p:", "default");
        
        String player = p.getName();
        UUID owner = p.getUniqueId();
        boolean other = Permissions.utilBackpackOther(s);
        if (other) {
            player = getExtraArg(args, "pl:", player);
            owner = BackpacksPlugin.getOfflinePlayerUUID(player);
            if (owner == null) {
                s.sendMessage(ChatColor.RED + "Player invalid.");
                return true;
            }
        }
        
        Inventory inv = cache.getBackpack(owner, backpack);
        if (inv == null) {
            s.sendMessage(ChatColor.RED + "You don't have that backpack.");
            return true;
        }
        
        if ("magnet".equals(action)) {
            if (!magnet.magnetEnabled(p.getName())) {
                magnet.enableMagnet(p.getName(), BackpackInventoryHolder.of(inv));
                p.sendMessage(ChatColor.YELLOW + "Enabled magnet mode on your "
                        + "\"" + backpack + "\" backpack.");
                p.sendMessage(ChatColor.YELLOW + "Redo this command to disable it.");
            } else {
                magnet.disableMagnet(p.getName());
                p.sendMessage(ChatColor.YELLOW + "Magnet mode disabled.");
            }
        } else if ("chest".equals(action)) {
            if (args.length < 3) {
                p.sendMessage(ChatColor.RED + "Not enough arguments.");
                p.sendMessage(getUsage(c, ChatColor.YELLOW, l, CHEST_USAGE));
                return true;
            }
            handleChestTransfer(p, inv, args[1], args[2]);
        } else if ("rename".equals(action)) {
            if (args.length < 3) {
                p.sendMessage(ChatColor.RED + "Not enough arguments.");
                p.sendMessage(getUsage(c, ChatColor.YELLOW, l, RENAME_USAGE)
                        .replace("(p:[backpack])", "[p:old-pack]"));
                return true;
            }
            // handling rename
            String newname = args[1].toLowerCase();
            if (newname.length() > 16) {
                s.sendMessage(ChatColor.YELLOW + "Please choose a backpack name under 16 characters");
                return true;
            }
            
            if (cache.getBackpackNames(owner).contains(newname)) {
                p.sendMessage(ChatColor.RED + "A backpack named \"" + newname + "\", already exists");
                return true;
            }
            
            // do the actual rename
            cache.renameBackpack(owner, backpack, newname);
        
            p.sendMessage(ChatColor.YELLOW + "Your \"" + backpack + "\" backpack is renamed to: \"" + newname + "\"");
        } else if ("empty".equals(action)) {
            if (args.length < 2) {
                p.sendMessage(ChatColor.RED + "Not enough arguments.");
                p.sendMessage(getUsage(c, ChatColor.YELLOW, l, EMPTY_USAGE)
                        .replace("(p:[backpack])", "[p:old-pack]"));
                return true;
            }
            inv.clear();
            p.sendMessage(ChatColor.YELLOW + "Your \"" + backpack + "\" backpack was emptied");
        }

        return true;
    }
    
    private void handleChestTransfer(Player p, Inventory backpack, String action, String item) {
        int direction = 0;
        if (action.equalsIgnoreCase("put")) {
            direction = 1;
        } else if (action.equalsIgnoreCase("take")) {
            direction = -1;
        } else {
            p.sendMessage(ChatColor.RED + "Error: " + action + ". Use \"put\" or \"take\"");
            return;
        }
        
        Block target = p.getTargetBlock(Collections.EMPTY_SET, 5);
        if (!(target.getState() instanceof Chest)) {
            p.sendMessage(ChatColor.RED + "You must be looking at a chest to do that");
            return;
        }
        
        if (!plugin.checkProtection(p, target) && !Permissions.utilBackpack(p, "chest.bypass")) {
            p.sendMessage(ChatColor.RED + "Sorry, you do not have access to that chest.");
            return;
        }
        
        Chest chest = (Chest) target.getState();

        Inventory to = (direction == 1)? chest.getInventory() : backpack;
        Inventory from = (direction == 1)? backpack : chest.getInventory();

        boolean success = InventoryUtil.transferItems(from, to, item);
        if (success) {
            // mark inventory as modified
            BackpackInventoryHolder.of(backpack).setUnsavedChanges(true);
            p.sendMessage(ChatColor.YELLOW + "Items transfered!");
        } else {
            p.sendMessage(ChatColor.RED + "Transfer failed. Invalid item?");
        }
    }
    
    public void handleHelp(Command c, Player p, String action, String l) {
        if (action == null) {
            sendUtils(p, l);
        } else if ("magnet".equals(action)) {
            sendHelpText(p, "Usage: " + getUsage(c, ChatColor.RED, l, MAGNET_USAGE));
            sendHelpText(p, "Toggling magnet mode on a backpack allows it to"
                    + " collect items directly as you pick them up from the ground.");
            sendHelpText(p, ChatColor.GRAY + "---");
            sendHelpText(p, "- In order for an item to be collected by the backpack,"
                    + " your player inventory must also have space for the item");
            sendHelpText(p, "- When your backpack is full, you will be notified and"
                    + " magnet mode will be automatically disabled.");
            sendHelpText(p, ChatColor.GRAY + "---");
            sendHelpText(p, "Example usage:");
            sendHelpText(p, " - /" + l +" magnet - Enables magnet on your default backpack");
            sendHelpText(p, " - /" + l +" magnet p:collecter - Enables magnet on your \"collector\""
                    + " backpack");
            sendHelpText(p, " - /" + l +" magnet (after enabled) - disable magnet mode");
        } else if ("chest".equals(action)) {
            sendHelpText(p, "Usage: " + getUsage(c, ChatColor.RED, l, CHEST_USAGE));
            sendHelpText(p, " Transfers items from your pack into a chest, and"
                    + " vica versa");
            sendHelpText(p, ChatColor.GRAY + "---");
            sendHelpText(p, "- You must be looking at the chest you wish to transfer"
                    + " items with.");
            sendHelpText(p, ChatColor.GRAY + "---");
            sendHelpText(p, "Example usage:");
            sendHelpText(p, " - /" + l +" chest put stone - Transfers as much stone as possible"
                    + " from your \"default\" backpack into the chest");
            sendHelpText(p, " - /" + l +" chest take all p:random - Transfers as manys items as possible"
                    + " can from the chest to your \"random\" backpack");
        } else if ("rename".equals(action)) {
            sendHelpText(p, "Usage: " + getUsage(c, ChatColor.RED, l, RENAME_USAGE)
                    .replace("(p:[backpack])", "[p:old-pack]"));
            sendHelpText(p, " Rename a backpack");
            sendHelpText(p, ChatColor.GRAY + "---");
            sendHelpText(p, "Examples:");
            sendHelpText(p, " - /" + l +" rename diamonds p:mining - Rename your \"mining\" backpack to \"diamonds\"");
        } else if ("empty".equals(action)) {
            sendHelpText(p, "Usage: " + getUsage(c, ChatColor.RED, l, EMPTY_USAGE)
                    .replace("(p:[backpack])", "[p:old-pack]"));
            sendHelpText(p, " Empties a backpack");
            sendHelpText(p, ChatColor.GRAY + "---");
            sendHelpText(p, ChatColor.GOLD + "- WARNING: this operation cannot be undone!!"
                    + " You will lose the items in the pack forever");
            sendHelpText(p, ChatColor.GRAY + "---");
            sendHelpText(p, "Example usage:");
            sendHelpText(p, " - /" + l +" empty p:default - Empties your \"default\" backpack");
        } else {
            sendUtils(p, l);
        }
    }
    
    private static void sendHelpText(Player p, String message) {
        p.sendMessage(ChatColor.YELLOW + message);
    }
    
    private static String getTool(String filter) {
        for (String tool : TOOLS) {
            if (tool.equalsIgnoreCase(filter)) {
                return tool;
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
    
    private static String getUsage(Command c, ChatColor color, String label, String usage) {
        return color + c.getUsage()
                .replace("[action]", usage)
                .replace("<command>", label);
    }
    
    private static void sendUtils(CommandSender sender, String l) {
            sender.sendMessage(ChatColor.YELLOW + "Unknown utility. Available utils: ");
            for (String tool : TOOLS) {
                sender.sendMessage("- " + ChatColor.AQUA + tool);
            }
            sender.sendMessage(ChatColor.YELLOW + "Do " + ChatColor.RED
                    + "/" + l + " help [util] " + ChatColor.YELLOW + "for information and usage");
    }
            
    private final class MagnetListener implements Listener {
        
        private Map<String, BackpackInventoryHolder> magnets = new HashMap();
        
        private static final String backpackFull = 
                "Your backpack is full! Disabled magnet mode.";
        private static final String loginDisable =
                "We disabled the magnet on backpack because you logged out!";
                
        
        public void enableMagnet(String player, BackpackInventoryHolder pack) {
            magnets.put(player, pack);
        }
        
        public void disableMagnet(String player) {
            magnets.remove(player);
        }
        
        public boolean magnetEnabled(String player) {
            return magnets.containsKey(player);
        }
        
        @EventHandler(ignoreCancelled = true)
        public void randomPop(PlayerPickupItemEvent event) {
            final Player p = event.getPlayer();
            final Item item = event.getItem();
            final ItemStack itemstack = item.getItemStack();
            final String player = p.getName();
            
            if (magnetEnabled(player)) {
                final BackpackInventoryHolder pack = magnets.get(player);
                // mark holder as modified
                pack.setUnsavedChanges(true);
                
                Inventory inv = pack.getInventory();
                
                // add item to inventory and check how much remained
                HashMap<Integer, ItemStack> left = inv.addItem(itemstack);
                int remains = left.isEmpty()? 0 : left.get(0).getAmount();
                
                if (remains > 0) {
                    p.sendMessage(ChatColor.RED + backpackFull);
                    disableMagnet(player);
                    
                    ItemStack newStack = itemstack.clone();
                    newStack.setAmount(remains);

                    // spawn new Item at current item's location
                    final Item newItem = item.getWorld().dropItem(item.getLocation(), itemstack);
                    
                    // maintain the previous item velocity
                    newItem.setVelocity(item.getVelocity());
                                        
                    item.remove(); // remove the existing item
                } else {
                    // picked up whole stack; remove item
                    if (plugin.isMinecraftCompatible()) {
                        NMSUtil.simulateItemPickup(p, item);
                    }
                    item.remove();
                }
                
                event.setCancelled(true);
            }
        }
        
        @EventHandler
        public void onWorldChange(PlayerChangedWorldEvent event) {
            final String player = event.getPlayer().getName();
            
            if (magnetEnabled(player)) {
                disableMagnet(player);
            }
        }
        
        @EventHandler
        public void onPlayerJoin(PlayerJoinEvent event) {
            if (magnetEnabled(event.getPlayer().getName())) {
                event.getPlayer().sendMessage(ChatColor.YELLOW + loginDisable);
            }
        }
        
    }
    
}
