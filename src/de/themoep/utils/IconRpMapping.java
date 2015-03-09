package de.themoep.utils;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionEffectTypeWrapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Phoenix616 on 07.03.2015.
 */
public class IconRpMapping {

    List<String> encodingNames = new ArrayList<String>();

    String[] potionids = {
            "unused", 
            "regeneration", 
            "swiftness", 
            "fire_resistance", 
            "poison", 
            "healing", 
            "night_vision", 
            "unused", 
            "weakness", 
            "strength", 
            "slowness", 
            "leaping", 
            "harming", 
            "water_breathing", 
            "invisibility" 
    };
    
    int offset;

    ConfigAccessor iconconfig;
    
    public IconRpMapping(JavaPlugin plugin) {
        plugin.getLogger().info("Loading Text Icon Resourcepack mapping...");
        iconconfig = new ConfigAccessor(plugin, "iconrpmapping.yml");
        iconconfig.reloadConfig();
        iconconfig.saveDefaultConfig();
        
        offset = iconconfig.getConfig().getInt("offset");
        
        List<String> matlist = iconconfig.getConfig().getStringList("map");
        for(String s : matlist) {
            if(Material.getMaterial(s.toUpperCase()) == null && !s.equalsIgnoreCase("unused"))
                plugin.getLogger().warning("[IconRpMapping] " + s + " is not a valid Bukkit material name!");
            encodingNames.add(s.toLowerCase());
        }
        
        plugin.getLogger().info("Text Icon Resourcepack mapping loaded.");
    }

    /**
     * Get the unicode character that corresponds with the inputted item
     * @param item The item to get the character of
     * @return A string of the unicode character which represents the item in the icon rp. Empty string if none was found.
     */
    public String getIcon(ItemStack item, boolean escape) {
        ItemMeta meta = item.getItemMeta();
        if(meta instanceof LeatherArmorMeta) {
            return ColorUtils.getNearestChatColor(((LeatherArmorMeta) meta).getColor()) + getIcon("white_" + item.getType().toString(), false) + ChatColor.RESET;
        } else if(item.getType() == Material.SKULL_ITEM) {
            switch(item.getDurability()) {
                case 1: return getIcon("wither_skeleton_skull", true);
                case 2: return getIcon("zombie_head", true);
                case 3: return getIcon("head", true);
                case 4: return getIcon("creeper_head", true);
                default: return getIcon("skeleton_skull", true);
            }
        } else if(item.getType() == Material.POTION) {
            String key = "water_bottle";
            if(item.getDurability() == 0)
                return getIcon(key, true);
            
            String splash = "";
            
            boolean[] bits = BitUtils.getBits(item.getDurability());
            int id = BitUtils.getRange(item.getDurability(), 0, 3);
            if(id < potionids.length)
                key = potionids[id].toLowerCase();
            
            if(bits[1])
                splash = "splash_";
            String icon = getIcon(splash + "potion_of_" + key, true);
            if(icon.isEmpty())
                icon = getIcon(splash + key + "_potion", true);
            return icon;
        }
        return getIcon(item.getType().toString(), escape);
    }

    /**
     * Get the unicode character of the inputed icon
     * @param iconname The name of the icon
     * @return A string of the unicode character which represents the icon in the icon rp. Empty string if none was found.
     */
    public String getIcon(String iconname, boolean escape) {
        if(encodingNames.contains(iconname.toLowerCase())) {
            String symbol = Character.toString((char) (offset + encodingNames.indexOf(iconname.toLowerCase())));
            if(escape)
                symbol = ChatColor.WHITE + symbol + ChatColor.RESET;
            return symbol;
        } else
            return "";
    }
}
