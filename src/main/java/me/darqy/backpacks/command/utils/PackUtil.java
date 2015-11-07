package me.darqy.backpacks.command.utils;

import java.util.UUID;
import me.darqy.backpacks.io.BackpackGroupCache;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;

public interface PackUtil extends Listener {
    
    /**
     * @return the name of this utility
     */
    public String getName();
    
    /**
     * Sends help message to the player
     * @param sender the CommandSender to send to
     * @param alias the alias of the command used
     * @param usage the formatted usage of the command
     */
    public void sendHelpText(CommandSender sender ,String alias, String usage);
    
    /**
     * @return the usage of this utility's command
     */
    public String getUsage();
    
    /**
     * Called when a player uses this utility
     * @param player the player using this utility
     * @param cache the BackpackGroupCache involved
     * @param owner the UUID of the backpack's owner
     * @param inv the backpack's inventory
     * @param backpack the name of the backpack
     * @param usage the documented usage of the utility
     * @param args arguments passed along to the utility
     */
    public void use(Player player, BackpackGroupCache cache, UUID owner, Inventory inv, String backpack, String usage, String[] args);
    
}
