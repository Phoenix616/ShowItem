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
    
    Map<String, String> transmap = new HashMap<String, String>();
    
    ConfigAccessor langconfig;

    public TranslationMapping(JavaPlugin plugin) {
        plugin.getLogger().info("Loading TranslationMapping...");

        langconfig = new ConfigAccessor(plugin, "transmapping.yml");
        langconfig.reloadConfig();
        langconfig.saveDefaultConfig();

        ConfigurationSection blocksection = langconfig.getConfig().getConfigurationSection("mapping");
        for(String matname : blocksection.getKeys(false)) {
            try {
                String matkey = matname.toUpperCase();
                Material.valueOf(matkey);
                
                if(blocksection.isConfigurationSection(matname)) {
                    ConfigurationSection extrasection = blocksection.getConfigurationSection(matname);
                    String general = blocksection.getString("general");
                    if(general != null) {
                        transmap.put(matkey, general);
                    }
                    String template = blocksection.getString("template");
                    if(template != null) {
                        ConfigurationSection templatesection = langconfig.getConfig().getConfigurationSection("templates." + template);
                        if(templatesection != null) {
                            for(String damage : templatesection.getKeys(false)) {
                                String mckey = templatesection.getString(damage);
                                if (general != null) {
                                    mckey = general + "." + mckey;
                                }
                                transmap.put(matkey + ":" + damage, mckey);
                            }
                        } else {
                            plugin.getLogger().warning("[TranslationMapping] The template " + template + " does not exist!");
                        }
                    }
                    
                    ConfigurationSection damagesection = extrasection.getConfigurationSection("types");
                    if(damagesection != null) {
                        for(String damage : damagesection.getKeys(false)) {
                            String mckey = damagesection.getString(damage);
                            if (general != null) {
                                mckey = general + "." + mckey;
                            }
                            transmap.put(matkey + ":" + damage, mckey);
                        }
                    }
                } else {
                    transmap.put(matkey, blocksection.getString(matname));
                }

                //plugin.getLogger().info("[IdMapping] Loaded mapping for Material." + s);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("[TranslationMapping] " + matname + " is not a valid Bukkit material name!");
            }
        }
        plugin.getLogger().info("TranslationMapping loaded.");
    }

    public String getKey(ItemStack item) {
        Material mat = item.getType();
        if(mat == Material.SKULL_ITEM && item.getItemMeta() instanceof SkullMeta && ((SkullMeta) item.getItemMeta()).getOwner() != null) {
                return "item.skull.player.name";
        }
        String trans = "";
        if(transmap.containsKey(mat.toString() + ":" + item.getDurability())) {
            trans = transmap.get(mat.toString() + ":" + item.getDurability());
        } else if(transmap.containsKey(mat.toString())) {
            trans = transmap.get(mat.toString());
        } else {
            trans = mat.toString().toLowerCase().replace("_block", "").replace("_item", "").replace("_", "");
        }
        if(!trans.startsWith("item.") && !trans.startsWith("tile.")) {
            trans = (mat.isBlock()) ? "tile." : "item." + trans;
        }
        return trans + ".name";
    }
}
