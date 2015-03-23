package de.themoep.utils;

/*
 * TranslationMapping is an util for mapping Bukkit materials the language 
 * keys of minecraft items in the language files of the client
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

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class TranslationMapping {
    
    Map<Material, String> blockmap = new HashMap<Material, String>();
    Map<Material, String> itemmap = new HashMap<Material, String>();
    
    ConfigAccessor langconfig;

    public TranslationMapping(JavaPlugin plugin) {
        plugin.getLogger().info("Loading TranslationMapping...");

        langconfig = new ConfigAccessor(plugin, "transmapping.yml");
        langconfig.reloadConfig();
        langconfig.saveDefaultConfig();

        ConfigurationSection blocksection = langconfig.getConfig().getConfigurationSection("mapping.blocks");
        for(String s : blocksection.getKeys(false)) {
            try {
                Material mat = Material.valueOf(s.toUpperCase());
                String mckey = blocksection.getString(s);
                blockmap.put(mat, mckey);
                
                //plugin.getLogger().info("[IdMapping] Loaded mapping for Material." + s);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("[TranslationMapping] " + s + " is not a valid Bukkit material name!");
            }
        }
        ConfigurationSection itemsection = langconfig.getConfig().getConfigurationSection("mapping.items");
        for(String s : itemsection.getKeys(false)) {
            try {
                Material mat = Material.valueOf(s.toUpperCase());
                String mckey = itemsection.getString(s);
                itemmap.put(mat, mckey);

                //plugin.getLogger().info("[IdMapping] Loaded mapping for Material." + s);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("[TranslationMapping] " + s + " is not a valid Bukkit material name!");
            }
        }
        plugin.getLogger().info("TranslationMapping loaded.");
    }

    public String getKey(Material mat) {
        if(blockmap.containsKey(mat)) {
            return "tile." + blockmap.get(mat) + ".name";
        } else if(itemmap.containsKey(mat)) {
            return "item." + itemmap.get(mat) + ".name";
        } else {
            if(mat.isBlock()) {
                return "tile." + mat.toString().toLowerCase().replace("_block", "").replace("_", "") + ".name";
            } else {
                return "item." + mat.toString().toLowerCase().replace("_item", "").replace("_", "") + ".name";
            }
        }
    }
}