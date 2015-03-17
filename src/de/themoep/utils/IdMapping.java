package de.themoep.utils;

import org.apache.commons.lang.WordUtils;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Phoenix616 on 03.03.2015.
 */
public class IdMapping {
    
    Map<Material,String> mcidmap = new HashMap<Material, String>();
    Map<Material,String> aliasmap = new HashMap<Material, String>();

    ConfigAccessor idconfig;

    public IdMapping(JavaPlugin plugin) {
        plugin.getLogger().info("Loading IdMapping...");

        idconfig = new ConfigAccessor(plugin, "idmapping.yml");
        idconfig.reloadConfig();
        idconfig.saveDefaultConfig();
        
        ConfigurationSection section = idconfig.getConfig().getConfigurationSection("idmapping");
        for(String s : section.getKeys(false)) {
            try {
                Material mat = Material.valueOf(s.toUpperCase());
                String mcid = section.getString(s + ".mcid");
                String alias = section.getString(s + ".alias");
                if (mcid != null) {
                    mcidmap.put(mat, mcid);
                }
                if (alias != null) {
                    aliasmap.put(mat, alias);
                }
                //plugin.getLogger().info("[IdMapping] Loaded mapping for Material." + s);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("[IdMapping] " + s + " is not a valid Bukkit material name!");
            }
        }
        plugin.getLogger().info("IdMapping loaded.");
    }

    /**
     * Convert the item id to a more human readable name. Should equal the US translation of MC.
     * @return The alias set int he config or if not existand a capitalized, human friendly id/material name
     */
    public String getHumanName(Material mat) {
        if(aliasmap.containsKey(mat))
            return aliasmap.get(mat);
        else
            return WordUtils.capitalize(getMCid(mat).toLowerCase().replace("_item", "").replace("_", " "));
    }

    public String getMCid(Material mat) {
        if(mcidmap.containsKey(mat))
            return mcidmap.get(mat);
        else
            return mat.toString().toLowerCase().replace("_item", "");
    }
}
