package me.darqy.backpacks.command;

import org.bukkit.command.CommandSender;

public class Permissions {
    
    private static final String PREFIX = "backpacks.";

    private static boolean check(CommandSender s, String perm) {
        return s.hasPermission(PREFIX + perm);
    }

    public static boolean useBackpack(CommandSender sender) {
        return check(sender, "use");
    }

    public static boolean useBackpackNamed(CommandSender sender) {
        return check(sender, "use.named");
    }

    public static boolean createBackpack(CommandSender sender) {
        return check(sender, "create");
    }

    public static boolean createBackpackNamed(CommandSender sender) {
        return check(sender, "create.named");
    }
    
    public static boolean createBackpackOther(CommandSender sender) {
        return check(sender, "create.other");
    }
    
    public static boolean createBackpackLimitBypass(CommandSender sender) {
        return check(sender, "create.limit.bypass");
    }
    
    public static int createBackpackLimit(CommandSender sender, int max) {
        if (check(sender, "create.limit.maximum")) {
            return max;
        }
        
        for (int i = max; i > 0; i--) {
            if (check(sender, "create.limit." + i)) {
                return i;
            }
        }
        
        return createBackpack(sender)? 1 : 0; // without createBackpack permission, limit is 0
    }

    public static boolean inspectBackpack(CommandSender sender) {
        return check(sender, "inspect");
    }

    public static boolean inspectAndEditBackpack(CommandSender sender) {
        return check(sender, "inspect.edit");
    }

    public static boolean listBackpacks(CommandSender sender) {
        return check(sender, "list");
    }
    
    public static boolean listBackpacksOther(CommandSender sender) {
        return check(sender, "list.other");
    }
    
    public static boolean utilBackpack(CommandSender sender, String util) {
        return check(sender, "util." + util);
    }
    
    public static boolean utilBackpackOther(CommandSender sender) {
        return check(sender, "util.other");
    }
    
    public static boolean backpacksAdmin(CommandSender sender) {
        return check(sender, "admin");
    }
    
}
