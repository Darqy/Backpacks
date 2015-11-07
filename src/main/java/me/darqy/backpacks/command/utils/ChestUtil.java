package me.darqy.backpacks.command.utils;

import java.util.HashMap;
import java.util.UUID;
import me.darqy.backpacks.BackpackInventoryHolder;
import me.darqy.backpacks.BackpacksPlugin;
import me.darqy.backpacks.command.Permissions;
import me.darqy.backpacks.io.BackpackGroupCache;
import me.darqy.backpacks.util.InventoryUtil;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;

public class ChestUtil implements PackUtil {
    
    private static final HashMap<String, ChestTransfer> chestTransferers = new HashMap();
    
    private final BackpacksPlugin plugin;
    
    public ChestUtil(BackpacksPlugin plugin) {
        this.plugin = plugin;
    }
    
    private boolean isChestTransfering(String player) {
        return chestTransferers.containsKey(player);
    }
    
    private void disableChestTransfering(String player) {
        chestTransferers.remove(player);
    }
    
    private void enableChestTransfering(String player, ChestTransfer transfer) {
        chestTransferers.put(player, transfer);
    }
    
    @Override
    public String getName() {
        return "chest";
    }

    @Override
    public void sendHelpText(CommandSender s, String alias, String usage) {
        s.sendMessage("");
        s.sendMessage(ChatColor.YELLOW + "Usage: " + usage);
        s.sendMessage(ChatColor.YELLOW + " Transfers items between your backpacks and chests.");
        s.sendMessage(ChatColor.GRAY + "---");
        s.sendMessage(ChatColor.YELLOW + "- After entering the command, click the chest you "
                + "wish to transfer items with.");
        s.sendMessage(ChatColor.GRAY + "---");
        s.sendMessage(ChatColor.YELLOW + "Example usage:");
        s.sendMessage(ChatColor.YELLOW + " - " + ChatColor.RED + "/" + alias + " chest put stone " + ChatColor.YELLOW
                    + "- Transfers as much stone as possible from your \"default\" backpack into a chest.");
        s.sendMessage(ChatColor.YELLOW + " - " + ChatColor.RED + "/" + alias + " chest take all p:random " + ChatColor.YELLOW
                    + "- Transfers as many items as possible from a chest to your \"random\" backpack.");
    }

    @Override
    public String getUsage() {
        return "chest [put|take] [item|id|all]";
    }

    @Override
    public void use(Player p, BackpackGroupCache cache, UUID owner, Inventory inv, String backpack, String usage, String[] args) {
        if (isChestTransfering(p.getName())) {
            disableChestTransfering(p.getName());
            p.sendMessage("Chest transfering disabled.");
            return;
        }
        if (args.length < 2) {
            p.sendMessage(ChatColor.RED + "Not enough arguments.");
            p.sendMessage(ChatColor.YELLOW + "Usage: " + usage);
            return;
        }
        int direction = 0;
        if (args[0].equalsIgnoreCase("put")) {
            direction = 1;
        } else if (args[0].equalsIgnoreCase("take")) {
            direction = -1;
        } else {
            p.sendMessage(ChatColor.RED + "Error: invalid chest action \"" + args[0] + "\". Use \"put\" or \"take\".");
            return;
        }
        
        String item = args[1].toLowerCase();
        
        if (!InventoryUtil.isValidMaterial(item) && !"all".equals(item) && !"*".equals(item)) {
            p.sendMessage(ChatColor.RED + "Invalid item name/ID. Use a valid item or 'all' or '*'.");
            return;
        }

        enableChestTransfering(p.getName(), new ChestTransfer(inv, direction, item));
        p.sendMessage(ChatColor.YELLOW + "Now click a chest to transfer items.");
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!isChestTransfering(p.getName()) || e.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        
        Block interacted = e.getClickedBlock();
        BlockState state = interacted.getState();
        
        if (!(state instanceof Chest)) {
            return; // didn't click a chest.
        }
       
        if (!plugin.checkProtection(p, interacted) && !Permissions.utilBackpack(p, "chest.bypass")) {
            p.sendMessage(ChatColor.RED + "You do not have access to that chest.");
            return;
        }
        
        ChestTransfer transfer = chestTransferers.get(p.getName());        
        Chest chest = (Chest) state;

        Inventory to = (transfer.direction == 1)? chest.getInventory() : transfer.inv;
        Inventory from = (transfer.direction == 1)? transfer.inv : chest.getInventory();

        if (InventoryUtil.transferItems(from, to, transfer.item)) {
            chest.update();
            // mark backpack as modified
            BackpackInventoryHolder.of(transfer.inv).setUnsavedChanges(true);
            p.sendMessage(ChatColor.YELLOW + "Items transfered!");
        } else {
            // this should never be reached unless something really bad happens.
            p.sendMessage(ChatColor.RED + "Transfer failed. Invalid item? Please retry the command.");
        }
        
        disableChestTransfering(p.getName());
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        final Player p = e.getPlayer();
        if (isChestTransfering(p.getName())) {
            disableChestTransfering(p.getName());
        }
    }
    
    @EventHandler
    public void onPlayerChangeWorld(PlayerChangedWorldEvent e) {
        final Player p = e.getPlayer();
        if (isChestTransfering(p.getName())) {
            disableChestTransfering(p.getName());
        }
    }
     
    private static final class ChestTransfer {
        private final Inventory inv;
        private final int direction;
        private final String item;
        public ChestTransfer(Inventory inv, int direction, String item) {
            this.inv = inv;
            this.direction = direction;
            this.item = item;
        }
    }

    
}
