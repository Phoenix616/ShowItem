package de.themoep.utils;

/*
 * IconRpMapping utils for mapping of Bukkit materials to WolfieMario's
 * Custom Text Icons Resourcepack (http://imgur.com/a/oHvbX)
 * Copyright (C) 2015 Max Lee (https://github.com/Phoenix616)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Mozilla Public License as published by
 * the Mozilla Foundation, version 2.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Mozilla Public License v2.0 for more details.
 *
 * You should have received a copy of the Mozilla Public License v2.0
 * along with this program. If not, see <http://mozilla.org/MPL/2.0/>.
 */

import org.bukkit.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkEffectMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.plugin.java.JavaPlugin;

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
        iconconfig.saveDefaultConfig();
        iconconfig.reloadConfig();
        
        offset = iconconfig.getConfig().getInt("offset");
        
        List<String> matlist = iconconfig.getConfig().getStringList("map");
        for(String s : matlist) {
            /*if(Material.getMaterial(s.toUpperCase()) == null && !s.equalsIgnoreCase("unused"))
                plugin.getLogger().warning("[IconRpMapping] " + s + " is not a valid Bukkit material name!");*/
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
        String iconstr = "";
        if(meta instanceof LeatherArmorMeta) {
            iconstr = ColorUtils.getNearestChatColor(((LeatherArmorMeta) meta).getColor()) + getIcon("white_" + item.getType().toString(), false) + ChatColor.RESET;
        } else if(item.getType() == Material.POTION) {
            String key = "water_bottle";
            if(item.getDurability() == 0)
                return getIcon(key, escape);
            
            String splash = "";
            
            boolean[] bits = BitUtils.getBits(item.getDurability());
            int id = BitUtils.getRange(item.getDurability(), 0, 3);
            if(id < potionids.length)
                key = potionids[id].toLowerCase();
            
            if(bits[1])
                splash = "splash_";
            iconstr = getIcon(splash + "potion_of_" + key, true);
            if(iconstr.isEmpty())
                iconstr = getIcon(splash + key + "_potion", true);
        } else if(meta instanceof FireworkEffectMeta) {
            FireworkEffect fe = ((FireworkEffectMeta) meta).getEffect();
            Color median = fe.getColors().get(0);
            for(int i = 1; i < fe.getColors().size(); i++)
                median.mixColors(fe.getColors().get(i));

            iconstr = getIcon(item.getType().toString() + "_" + ColorUtils.getNearestChatColor(median).name(), escape);
        } else {
            iconstr = getIcon(item.getType().toString() + ":" + item.getDurability(), escape);
        }
        if(ChatColor.stripColor(iconstr).isEmpty()) {
            iconstr = getIcon(item.getType().toString(), escape);
        }
        return iconstr;
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
