package me.darqy.backpacks.util;

import net.minecraft.server.v1_8_R1.PacketPlayOutCollect;
import net.minecraft.server.v1_8_R1.EntityTracker;
import org.bukkit.craftbukkit.v1_8_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R1.entity.CraftPlayer;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;

public class NMSUtil {
    
    public static void simulateItemPickup(Player player, Item item) {
        simulateItemPickup(player, item.getEntityId());
    }
    
    public static void simulateItemPickup(Player player, int entity) {
        CraftPlayer p = (CraftPlayer) player;
        EntityTracker tracker = ((CraftWorld) p.getWorld()).getHandle().getTracker();
        tracker.sendPacketToEntity(p.getHandle(),
                new PacketPlayOutCollect(entity, p.getEntityId()));
    }
    
}
