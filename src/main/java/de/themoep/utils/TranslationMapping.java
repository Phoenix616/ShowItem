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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class TranslationMapping {
    
    Map<Material, String> transmap = new HashMap<Material, String>();
    
    ConfigAccessor langconfig;

    public TranslationMapping(JavaPlugin plugin) {
        plugin.getLogger().info("Loading TranslationMapping...");

        langconfig = new ConfigAccessor(plugin, "transmapping.yml");
        langconfig.reloadConfig();
        langconfig.saveDefaultConfig();

        ConfigurationSection blocksection = langconfig.getConfig().getConfigurationSection("mapping");
        for(String s : blocksection.getKeys(false)) {
            try {
                Material mat = Material.valueOf(s.toUpperCase());
                String mckey = blocksection.getString(s);
                transmap.put(mat, mckey);
                
                //plugin.getLogger().info("[IdMapping] Loaded mapping for Material." + s);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("[TranslationMapping] " + s + " is not a valid Bukkit material name!");
            }
        }
        plugin.getLogger().info("TranslationMapping loaded.");
    }

    public String getKey(ItemStack item) {
        Material mat = item.getType();
        if(mat == Material.SKULL_ITEM) {
            String t = "item.skull.";
            if(item.getItemMeta() instanceof SkullMeta && ((SkullMeta) item.getItemMeta()).getOwner() != null) {
                t += "player";
            } else {
                switch(item.getDurability()) {
                    case 0:
                        t += "skeleton";
                        break;
                    case 1:
                        t += "wither";
                        break;
                    case 2:
                        t += "zombie";
                        break;
                    case 4:
                        t += "creeper";
                        break;
                    default:
                        t += "char";
                        break;
                }
            }
            return t + ".name";
        }
        if(transmap.containsKey(mat)) {
            String t = transmap.get(mat) + ".name";
            if(!t.startsWith("item.") && !t.startsWith("tile.")) {
                t = (mat.isBlock()) ? "tile." : "item." + t;
            }
            return t;
        } else {
            if(mat.isBlock()) {
                return "tile." + mat.toString().toLowerCase().replace("_block", "").replace("_", "") + ".name";
            } else {
                return "item." + mat.toString().toLowerCase().replace("_item", "").replace("_", "") + ".name";
            }
        }
    }
}
