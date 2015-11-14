package me.darqy.backpacks.command.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import me.darqy.backpacks.BackpackInventoryHolder;
import me.darqy.backpacks.BackpacksPlugin;
import me.darqy.backpacks.io.BackpackGroupCache;
import me.darqy.backpacks.util.InventoryUtil;
import me.darqy.backpacks.util.NMSUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class MagnetUtil implements PackUtil {
    
    private static final Map<String, BackpackInventoryHolder> magnets = new HashMap();

    private static final String backpackFull
            = "Your backpack is full! Disabled magnet mode.";
    private static final String loginDisable
            = "We disabled the magnet on backpack because you logged out!";

    private final BackpacksPlugin plugin;
    
    public MagnetUtil(BackpacksPlugin plugin) {
        this.plugin = plugin;
    }
    
    private void enableMagnet(String player, BackpackInventoryHolder pack) {
        magnets.put(player, pack);
    }

    private void disableMagnet(String player) {
        magnets.remove(player);
    }

    private boolean magnetEnabled(String player) {
        return magnets.containsKey(player);
    }
    
    @Override
    public String getName() {
        return "magnet";
    }    

    @Override
    public void sendHelpText(CommandSender s, String alias, String usage) {
        s.sendMessage("");
        s.sendMessage(ChatColor.YELLOW + "Usage: " + usage);
        s.sendMessage(ChatColor.YELLOW + " Enabling magnet mode causes all items you pick up "
                + " to be put into your backpack.");
        s.sendMessage(ChatColor.GRAY + "---");
        s.sendMessage(ChatColor.YELLOW + "- Won't work if your player inventory is full.");
        s.sendMessage(ChatColor.YELLOW + "- Automatically disables when backpack is full.");
        s.sendMessage(ChatColor.GRAY + "---");
        s.sendMessage(ChatColor.YELLOW + "Example usage:");
        s.sendMessage(ChatColor.YELLOW + " - " + ChatColor.RED + "/" + alias +" magnet " + ChatColor.YELLOW
                + "- Enables magnet on your default backpack.");
        s.sendMessage(ChatColor.YELLOW + " - " + ChatColor.RED + "/" + alias +" magnet p:collecter " + ChatColor.YELLOW
                + "- Enables magnet on your \"collector\" backpack.");
        s.sendMessage(ChatColor.YELLOW + " - " + ChatColor.RED + "/" + alias +" magnet " + ChatColor.YELLOW
                + "- disables magnet mode if already enabled.");
    }

    @Override
    public String getUsage() {
        return "magnet";
    }

    @Override
    public void use(Player p, BackpackGroupCache cache, UUID owner, Inventory inv, String backpack, String usage, String[] args) {
        if (!magnetEnabled(p.getName())) {
            enableMagnet(p.getName(), BackpackInventoryHolder.of(inv));
            p.sendMessage(ChatColor.YELLOW + "Enabled magnet mode on your "
                    + "\"" + backpack + "\" backpack.");
            p.sendMessage(ChatColor.YELLOW + "Redo this command to disable it.");
        } else {
            disableMagnet(p.getName());
            p.sendMessage(ChatColor.YELLOW + "Magnet mode disabled.");
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
        final Player player = event.getPlayer();
        if (magnetEnabled(player.getName())) {
            player.sendMessage(ChatColor.YELLOW + loginDisable);

            disableMagnet(player.getName());
        }
    }
    
    @EventHandler(ignoreCancelled = true)
    public void randomPop(PlayerPickupItemEvent event) {
        Player p = event.getPlayer();
        Item item = event.getItem();
        ItemStack itemstack = item.getItemStack();
        String player = p.getName();

        if (magnetEnabled(player)) {
            final BackpackInventoryHolder pack = magnets.get(player);  
            Inventory inv = pack.getInventory();
            // the real amount of the item being picked up
            // not the amount the player has room for in their inventory.
            int stackAmount = itemstack.getAmount() + event.getRemaining();     
            itemstack = itemstack.clone();
            itemstack.setAmount(stackAmount);
            
            int space = InventoryUtil.spaceForItem(itemstack, inv);
            if (space == 0) {
                p.sendMessage(ChatColor.RED + backpackFull);
                disableMagnet(player);
                return;
            }
            
            // not enough room for the entire itemstack
            if (space < stackAmount) {
                // set itemstack to pickup amount to the amount of space available
                itemstack.setAmount(space);
                // new itemstack to replace the item on the ground
                ItemStack remaining = itemstack.clone();
                // set amount to the previous amount minus what we took
                remaining.setAmount(stackAmount - space);
                // set the remaining item's new amount
                item.setItemStack(remaining);
            } else {
                // enough room for entire stack; remove it, simulate pickup
                item.remove();
                if (plugin.isMinecraftCompatible()) {
                    NMSUtil.simulateItemPickup(p, item);
                }
            }
            
            // Add what we can fit to the backpack
            inv.addItem(itemstack);
            pack.setUnsavedChanges(true);
            
            event.setCancelled(true);
        }
    }
    
}
