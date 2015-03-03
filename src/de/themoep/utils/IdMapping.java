package de.themoep.utils;

import org.apache.commons.lang.WordUtils;
import org.bukkit.Material;

/**
 * Created by Phoenix616 on 03.03.2015.
 */
public enum IdMapping {
    SKULL_ITEM          ("skull",               true),
    WOOD_DOOR           ("wooden_door",         true),
    FIREWORK            ("fireworks"),
    GOLD_HELMET         ("golden_helmet",       true),
    GOLD_CHESTPLATE     ("golden_chestplate",   true),
    GOLD_LEGGINGS       ("golden_leggings",     true),
    GOLD_BOOTS          ("golden_boots",        true),
    MONSTER_EGG         ("spawn_egg",           true),
    EXP_BOTTLE          ("experience_bottle",   "Bottle o' Enchanting"),
    BOOK_AND_QUILL      ("writable_book",       "Book and Quill"),
    EYE_OF_ENDER        ("ender_eye",           "Eye of Ender"),
    SPECKLED_MELON      ("speckled_melon",      "Glistering Melon"),
    FLINT_AND_STEEL     ("flint_and_steel",     "Flint and Steel"),
    EMPTY_MAP           ("map"),
    GOLD_RECORD         ("record_13",           true),
    GREEN_RECORD        ("record_cat",          true),
    RECORD_3            ("record_blocks",       true),
    RECORD_4            ("record_chirp",        true),
    RECORD_5            ("record_far",          true),
    RECORD_6            ("record_mall",         true),
    RECORD_7            ("record_mellohi",      true),
    RECORD_8            ("record_stal",         true),
    RECORD_9            ("record_srad",         true),
    RECORD_10           ("record_ward",         true),
    RECORD_12           ("record_wait",         true),
    ENDER_PORTAL_FRAME  ("end_portal_frame",    true),
    RAW_FISH            ("fish"),
    ;

    // The id minecraft uses for the item
    private final String mcid;
    
    // If the name should get generated from the mcid (true) or the bukkit material name (false, default)
    private final boolean useMcid;
    
    private final String alias;
    
    IdMapping(String mcid) {
        this.mcid = mcid;
        this.useMcid = false;
        this.alias = null;
    }
    
    IdMapping(String mcid, boolean replace) {
        this.mcid = mcid;
        this.useMcid = replace;
        this.alias = null;
    }

    IdMapping(String mcid, String alias) {
        this.mcid = mcid;
        this.useMcid = false;
        this.alias = alias;
    }
    
    public static IdMapping valueOf(Material mat) throws IllegalArgumentException {
        IdMapping idmap = valueOf(mat.toString());
        return idmap;
    }

    /**
     * Convert the item id to a more human readable name. Should equal the US translation of MC.*
     * @return A capitalized, human friendly material/id name
     */
    public static String getHumanName(Material mat) {
        try {
            IdMapping idmap = valueOf(mat);
            return (idmap.alias != null) ? idmap.alias : WordUtils.capitalize(((idmap.useMcid) ? idmap.mcid : idmap.toString().toLowerCase()).replace("_", " "));
        } catch(IllegalArgumentException e) {
            return WordUtils.capitalize(mat.toString().toLowerCase().replace("_item", "").replace("_", " "));
        }
    }

    public static String getMCid(Material mat) {
        try {
            IdMapping idmap = valueOf(mat);
            return idmap.mcid;
        } catch(IllegalArgumentException e) {
            return mat.toString().toLowerCase().replace("_item", "");
        }
    }
}
