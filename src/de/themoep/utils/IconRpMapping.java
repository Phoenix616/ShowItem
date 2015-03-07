package de.themoep.utils;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Phoenix616 on 07.03.2015.
 */
public class IconRpMapping {
    
    List<Material> encoding = new ArrayList<Material>();
    
    int offset;

    ConfigAccessor iconconfig;
    
    public IconRpMapping(JavaPlugin plugin) {
        plugin.getLogger().info("Loading Text Icon Resourcepack mapping...");
        iconconfig = new ConfigAccessor(plugin, "iconrpmapping.yml");
        iconconfig.saveDefaultConfig();
        iconconfig.reloadConfig();
        
        offset = iconconfig.getConfig().getInt("offset");
        
        List<String> matlist = iconconfig.getConfig().getStringList("map");
        for(String s : matlist) {
            Material mat = Material.getMaterial(s.toUpperCase());
            encoding.add(mat);
            if(mat == null)
                plugin.getLogger().warning("[IconRpMapping] " + s + " is not a valid Bukkit material name!");
        }
        
        plugin.getLogger().info("Text Icon Resourcepack mapping loaded.");
    }

    /**
     * Get the unicode character that corresponds with the inputted item
     * @param item The item to get the character of
     * @return A string of the unicode character which represents the item in the icon rp. Empty string if none was found.
     */
    public String getIcon(ItemStack item) {
        if(encoding.contains(item.getType())) {
            String symbol = Character.toString((char) (offset + encoding.indexOf(item.getType())));
            return ChatColor.WHITE + symbol + ChatColor.RESET;
        } else
            return "";
    }
}
