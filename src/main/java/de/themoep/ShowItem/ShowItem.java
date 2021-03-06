package de.themoep.ShowItem;

/**
 * ShowItem - Bukkit plugin to show your items to other players via chat.
 * Copyright (C) 2015 Max Lee (https://github.com/Phoenix616/)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.*
 */

import com.comphenix.attribute.NbtFactory;
import com.comphenix.attribute.NbtFactory.NbtCompound;
import com.comphenix.attribute.NbtFactory.NbtList;

import com.google.common.collect.ImmutableMap;

import de.themoep.utils.IconRpMapping;
import de.themoep.utils.IdMapping;
import de.themoep.utils.TranslationMapping;

import net.md_5.bungee.chat.ComponentSerializer;

import org.apache.commons.lang.StringEscapeUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.block.banner.Pattern;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.FireworkEffectMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.*;
import java.util.logging.Level;

public class ShowItem extends JavaPlugin implements CommandExecutor {

    ChatColor msgcolor = ChatColor.GREEN;
    ChatColor msgsecondarycolor = ChatColor.YELLOW;
    
    IdMapping idmap;
    TranslationMapping transmap;
    
    int defaultradius;
    Map<String, Integer> cooldowns = new HashMap<String, Integer>();
    boolean useIconRp;

    Map<String, Map<UUID, Long>> cooldownMaps = new HashMap<String, Map<UUID, Long>>();
    
    IconRpMapping iconrpmap;

    
    ConfigurationSection lang;
    
    boolean spigot;
    boolean usebungeeapi;
    
    public void onEnable() {

        loadConfig();
        try {
            Bukkit.class.getMethod("spigot");
            spigot = true;
            getLogger().info("Detected Spigot server. " + (usebungeeapi ? "Using Bungee chat api for fancy messages!" : "However it looks like you disabled the usage of the Bungee api in your config! (usefancymsg) Using vanilla tellraw command instead!"));
        } catch (NoSuchMethodException e) {
            spigot = false;
            getLogger().info("Detected a non-Spigot server. Using vanilla tellraw command for fancy messages!");
        }
        for(String s : new String[]{"all", "world", "radius", "direct"}) {
            cooldownMaps.put(s, new HashMap<UUID, Long>());
        }
    }

    public void loadConfig() {
        saveDefaultConfig();
        getLogger().info("Loading Config...");
        reloadConfig();
        usebungeeapi = getConfig().getBoolean("usefancymsg", true);
        defaultradius = this.getConfig().getInt("defaultradius",16);
        cooldowns.put("all", getConfig().getInt("cooldowns.all", 0));
        cooldowns.put("world", getConfig().getInt("cooldowns.world", 0));
        cooldowns.put("radius", getConfig().getInt("cooldowns.radius", getConfig().getInt("cooldown", 0)));
        cooldowns.put("direct", getConfig().getInt("cooldowns.direct", 0));

        lang = this.getConfig().getConfigurationSection("lang");
        useIconRp = this.getConfig().getBoolean("texticonrp");
        idmap = new IdMapping(this);
        transmap = new TranslationMapping(this);
        if(useIconRp) {
            this.iconrpmap = new IconRpMapping(this);
        }
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if(sender.hasPermission("showitem.command")) {
            int radius = this.defaultradius;
            boolean showWorld = false;
            boolean showAll = false;
            Level debugLevel = Level.FINE;
            for(int i = 0; i < args.length; i++) {
                if(args[i].equalsIgnoreCase("-reload")) {
                    if (sender.hasPermission("showitem.command.reload")) {
                        this.loadConfig();
                        sender.sendMessage(ChatColor.GREEN + "Config reloaded.");
                    } else {
                        sender.sendMessage("You don't have the permission showitem.command.reload");
                    }
                    return true;
                } else if(args[i].equalsIgnoreCase("-debug")) {
                    if(sender.hasPermission("showitem.command.debug")) {
                        debugLevel = Level.INFO;
                        sender.sendMessage(ChatColor.GREEN + "Debug message.");
                    } else {
                        sender.sendMessage("You don't have the permission showitem.command.debug");
                        return true;
                    }
                } else if(args[i].equalsIgnoreCase("-radius") || args[i].equalsIgnoreCase("-r")){
                    if(sender.hasPermission("showitem.command.radius")) {
                        i++;
                        try {
                            radius = Integer.parseInt(args[i]);
                        } catch (NumberFormatException e) {
                            sender.sendMessage(ChatColor.RED + "Error: Your input " + args[i] + " is not a valid integer!");
                            return true;
                        } catch(IndexOutOfBoundsException e) {
                            sender.sendMessage(ChatColor.RED + "Error: Please input a number after the radius argument!");
                            return true;
                        }
                    } else {
                        sender.sendMessage("You don't have the permission showitem.command.radius");
                        return true;
                    }
                } else if(args[i].equalsIgnoreCase("-world") || args[i].equalsIgnoreCase("-w")){
                    if(sender.hasPermission("showitem.command.world")) {
                        showWorld = true;
                    } else {
                        sender.sendMessage("You don't have the permission showitem.command.world");
                        return true;
                    }
                } else if(args[i].equalsIgnoreCase("-all") || args[i].equalsIgnoreCase("-a")){
                    if(sender.hasPermission("showitem.command.all")) {
                        showAll = true;
                    } else {
                        sender.sendMessage("You don't have the permission showitem.command.all");
                        return true;
                    }
                }
            }
            
            if(sender instanceof Player) {
                if(((Player) sender).getItemInHand().getType() == Material.AIR) {
                    sender.sendMessage(getTranslation("error.noitem"));
                } else if (args.length > 0 && !args[0].startsWith("-")) {
                    if(sender.hasPermission("showitem.command.player")){
                        for(String name : args) {
                            if(!name.startsWith("-")) {
                                Player target = Bukkit.getPlayer(name);
                                if (target != null && target.isOnline()) {
                                    showPlayer((Player) sender, target, debugLevel);
                                } else {
                                    tellRaw((Player) sender, getTranslation("error.playeroffline", ImmutableMap.of("player", name)));
                                }
                            }
                        }
                    } else {
                        sender.sendMessage("You don't have the permission showitem.command.player");
                    }
                } else if(showAll) {
                    showAll((Player) sender, debugLevel);
                } else if(showWorld) {
                    showWorld((Player) sender, debugLevel);
                } else {
                    showInRadius((Player) sender, radius, debugLevel);
                }
            } else {
                sender.sendMessage("This command can only be run by a player!");
            }
        } else {
            sender.sendMessage("You don't have the permission showitem.command");
        }
        return true;
    }

    private void showAll(Player sender, Level debugLevel) {
        int cooldown = cooldowns.get("all");
        if(cooldown > 0 && !sender.hasPermission("showitem.cooldownexempt")) {
            Map<UUID, Long> cooldownMap = cooldownMaps.get("all");
            if(cooldownMap.containsKey(sender.getUniqueId())) {
                long diff = System.currentTimeMillis() - cooldownMap.get(sender.getUniqueId());
                if(diff < cooldown * 1000) {
                    tellRaw(sender, getTranslation("error.cooldown", ImmutableMap.of("remaining", Integer.toString((int) (cooldown - diff / 1000)))));
                    return;
                }
            }
            cooldownMap.put(sender.getUniqueId(), System.currentTimeMillis());
        }

        String itemstring = convertItem(sender.getItemInHand(), debugLevel);
        Boolean found = false;

        String msg = getTranslation("all.self", ImmutableMap.of("player", sender.getName(), "item", itemstring));
        if(debugLevel == Level.INFO) {
            sender.sendMessage(ChatColor.stripColor(itemstring));
        }
        tellRaw(sender, msg);
        String targetMsg = getTranslation("all.target", ImmutableMap.of("player", sender.getName(), "item", itemstring));
        for(Player target : getServer().getOnlinePlayers()) {
            if(target != sender) {
                tellRaw(target, targetMsg, debugLevel);
                found = true;
            }
        }
        if(!found)
            tellRaw(sender, getTranslation("error.noonearound", ImmutableMap.of("player", sender.getName(), "radius", "everywhere")));
    }

    private void showWorld(Player sender, Level debugLevel) {
        int cooldown = cooldowns.get("world");
        if(cooldown > 0 && !sender.hasPermission("showitem.cooldownexempt")) {
            Map<UUID, Long> cooldownMap = cooldownMaps.get("world");
            if(cooldownMap.containsKey(sender.getUniqueId())) {
                long diff = System.currentTimeMillis() - cooldownMap.get(sender.getUniqueId());
                if(diff < cooldown * 1000) {
                    tellRaw(sender, getTranslation("error.cooldown", ImmutableMap.of("remaining", Integer.toString((int) (cooldown - diff / 1000)))));
                    return;
                }
            }
            cooldownMap.put(sender.getUniqueId(), System.currentTimeMillis());
        }

        String itemstring = convertItem(sender.getItemInHand(), debugLevel);
        Boolean found = false;

        String msg = getTranslation("world.self", ImmutableMap.of("player", sender.getName(), "item", itemstring, "world", sender.getWorld().getName()));
        if(debugLevel == Level.INFO) {
            sender.sendMessage(ChatColor.stripColor(itemstring));
        }
        tellRaw(sender, msg);
        String targetMsg = getTranslation("world.target", ImmutableMap.of("player", sender.getName(), "item", itemstring, "world", sender.getWorld().getName()));
        for(Player target : sender.getWorld().getPlayers()) {
            if(target != sender) {
                tellRaw(target, targetMsg, debugLevel);
                found = true;
            }
        }
        if(!found)
            tellRaw(sender, getTranslation("error.noonearound", ImmutableMap.of("player", sender.getName(), "radius", sender.getWorld().getName())));
    }

    private void showInRadius(Player sender, int radius, Level debugLevel) {
        int cooldown = cooldowns.get("radius");
        if(cooldown > 0 && !sender.hasPermission("showitem.cooldownexempt")) {
            Map<UUID, Long> cooldownMap = cooldownMaps.get("radius");
            if(cooldownMap.containsKey(sender.getUniqueId())) {
                long diff = System.currentTimeMillis() - cooldownMap.get(sender.getUniqueId());
                if(diff < cooldown * 1000) {
                    tellRaw(sender, getTranslation("error.cooldown", ImmutableMap.of("remaining", Integer.toString((int) (cooldown - diff / 1000)))));
                    return;
                }
            }
            cooldownMap.put(sender.getUniqueId(), System.currentTimeMillis());
        }
        
        String itemstring = convertItem(sender.getItemInHand(), debugLevel);
        Boolean found = false;

        String radiusMsg = "";
        if(radius != defaultradius) {
            radiusMsg += " " + getTranslation("radius.custom");
            radiusMsg = radiusMsg.replace("%radius%", Integer.toString(radius));
        }
        String msg = getTranslation("radius.self", ImmutableMap.of("player", sender.getName(), "item", itemstring, "customradius", radiusMsg));
        if(debugLevel == Level.INFO) {
            sender.sendMessage(ChatColor.stripColor(itemstring));
        }
        tellRaw(sender, msg);
        String targetMsg = getTranslation("radius.target", ImmutableMap.of("player", sender.getName(), "item", itemstring, "customradius", radiusMsg));
        for(Player target : sender.getWorld().getPlayers()) {
            if(target != sender && sender.getLocation().distanceSquared(target.getLocation()) <= (radius*radius)) {
                tellRaw(target, targetMsg, debugLevel);
                found = true;
            }
        }
        if(!found)
            tellRaw(sender, getTranslation("error.noonearound", ImmutableMap.of("player", sender.getName(), "radius", Integer.toString(radius))));
    }

    private void showPlayer(Player sender, Player target, Level debugLevel) {
        int cooldown = cooldowns.get("direct");
        if(cooldown > 0 && !sender.hasPermission("showitem.cooldownexempt")) {
            Map<UUID, Long> cooldownMap = cooldownMaps.get("direct");
            if(cooldownMap.containsKey(sender.getUniqueId())) {
                long diff = System.currentTimeMillis() - cooldownMap.get(sender.getUniqueId());
                if(diff < cooldown * 1000) {
                    tellRaw(sender, getTranslation("error.cooldown", ImmutableMap.of("remaining", Integer.toString((int) (cooldown - diff / 1000)))));
                    return;
                }
            }
            cooldownMap.put(sender.getUniqueId(), System.currentTimeMillis());
        }
        
        String itemstring = convertItem(sender.getItemInHand(), debugLevel);
        if(debugLevel == Level.INFO) {
            sender.sendMessage(ChatColor.stripColor(itemstring));
        }
        tellRaw(target, getTranslation("player.target", ImmutableMap.of("player", sender.getName(), "item", itemstring)));
        tellRaw(sender, getTranslation("player.self", ImmutableMap.of("player", target.getName(), "item", itemstring)), debugLevel);
    }

    private String convertItem(ItemStack item, Level debugLevel) {
        ChatColor itemcolor = ChatColor.WHITE;

        JSONObject itemJson = new JSONObject();

        String icon = "";
        if(useIconRp)
            icon = iconrpmap.getIcon(item, true);
        //String name = idmap.getHumanName(item.getType());
        String name = "";
        List<String> translateWith = new ArrayList<String>();

        itemJson.put("id", "minecraft:"+ idmap.getMCid(item.getType()));

        itemJson.put("Damage", item.getDurability());
        
        JSONObject tagJson = new JSONObject();

        boolean hideEnchants = false;
        boolean hideAttributes = false;
        boolean hideUnbreakable = false;
        boolean hideDestroys = false;
        boolean hidePlacedOn = false;
        boolean hideVarious = false;
        
        if(item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();

            JSONObject displayJson = new JSONObject();

            if(meta.getLore() != null && meta.getLore().size() > 0) {
                List<String> loreList = new ArrayList<String>();
                for (String l : meta.getLore()) {
                    loreList.add(l);
                }
                displayJson.put("Lore", loreList);
            }

            try {
                if(meta.getItemFlags().size() > 0) {
                    int flagBits = 0;
                    for(ItemFlag flag : meta.getItemFlags())
                        switch(flag) {
                            case HIDE_ENCHANTS:
                                hideEnchants = true;
                                flagBits += 1;
                                break;
                            case HIDE_ATTRIBUTES:
                                hideAttributes = true;
                                flagBits += 2;
                                break;
                            case HIDE_UNBREAKABLE:
                                hideUnbreakable = true;
                                flagBits += 4;
                                break;
                            case HIDE_DESTROYS:
                                hideDestroys = true;
                                flagBits += 8;
                                break;
                            case HIDE_PLACED_ON:
                                hidePlacedOn = true;
                                flagBits += 16;
                                break;
                            case HIDE_POTION_EFFECTS:
                                hideVarious = true;
                                flagBits += 32;
                                break;
                        }
                    displayJson.put("HideFlags", flagBits);
                }
            } catch (NoSuchMethodError e) {
                // Catch for 1.7.3-1.7.10 servers, there are no item flags previous to 1.8!
            }

            if(meta instanceof LeatherArmorMeta) {
                displayJson.put("color", ((LeatherArmorMeta) meta).getColor().asRGB());
            }

            if(item.getType().isRecord()) {
                itemcolor = ChatColor.AQUA;
            }

            if(spigot && meta.spigot().isUnbreakable() && !hideUnbreakable) {
                tagJson.put("Unbreakable", 1);
            }
            
            if (meta.getEnchants() != null && meta.getEnchants().size() > 0) {
                if(!hideEnchants) {
                    List<JSONObject> enchList = new ArrayList<JSONObject>();

                    for (Map.Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
                        JSONObject enchJson = new JSONObject();
                        enchJson.put("id", entry.getKey().hashCode());
                        enchJson.put("lvl", entry.getValue());
                        enchList.add(enchJson);
                    }
                    tagJson.put("ench", enchList);
                }
                itemcolor = ChatColor.AQUA;
            }

            if(meta instanceof EnchantmentStorageMeta) {
                EnchantmentStorageMeta esm = (EnchantmentStorageMeta) meta;
                
                if(esm.getStoredEnchants() != null) {
                    if(!hideVarious) {
                        List<JSONObject> enchList = new ArrayList<JSONObject>();

                        for (Map.Entry<Enchantment, Integer> entry : esm.getStoredEnchants().entrySet()) {
                            JSONObject enchJson = new JSONObject();
                            enchJson.put("id", entry.getKey().hashCode());
                            enchJson.put("lvl", entry.getValue());
                            enchList.add(enchJson);
                        }
                        tagJson.put("StoredEnchantments", enchList);
                    }
                    itemcolor = ChatColor.YELLOW;
                }
            }

            if(meta instanceof PotionMeta) {
                if(!hideVarious) {
                    PotionMeta pm = (PotionMeta) meta;
                    List<JSONObject> potionList = new ArrayList<JSONObject>();
                    for (PotionEffect potion : pm.getCustomEffects()) {
                        JSONObject potionJson = new JSONObject();
                        potionJson.put("Id", potion.getType().hashCode());
                        potionJson.put("Amplifier", potion.getAmplifier());
                        potionJson.put("Duration", potion.getDuration());
                        if(potion.isAmbient()) {
                            potionJson.put("Ambient", (byte) 1);
                        }
                        potionList.add(potionJson);
                    }
                    tagJson.put("CustomPotionEffects", potionList);
                }
                itemcolor = ChatColor.YELLOW;
            }

            if(meta instanceof BookMeta) {
                if(!hideVarious) {
                    BookMeta bm = (BookMeta) meta;
                    name = "Book";
                    if (bm.getTitle() != null) {
                        tagJson.put("title", bm.getTitle());
                        name += ": " + bm.getTitle();
                    }
                    if (bm.getAuthor() != null) {
                        tagJson.put("author", bm.getAuthor());
                        if (bm.getTitle() == null) {
                            name += " by " + bm.getAuthor();
                        }
                    }
                }
            }

            if(meta instanceof SkullMeta) {
                SkullMeta sm = (SkullMeta) meta;
                if(sm.hasOwner()) {
                    String owner = sm.getOwner();
                    if(owner != null) {
                        translateWith.add(0, owner);
                    /*taglist.add("SkullOwner:{Name:\\\\\"" + owner + "\\\\\",},");*/
                        JSONObject ownerJson = new JSONObject();
                        ownerJson.put("Name", owner);
                        tagJson.put("SkullOwner", ownerJson);
                    }
                    /*name = owner + "'";
                    if(!(owner.substring(owner.length() -1).equalsIgnoreCase("s") || owner.substring(owner.length() -1).equalsIgnoreCase("x") || owner.substring(owner.length() -1).equalsIgnoreCase("z")))
                        name += "s";
                    name += " Head";
                    meta.setDisplayName(name);*/
                }
            }
            
            if(meta instanceof FireworkMeta) {
                if(!hideVarious) {
                    FireworkMeta fm = (FireworkMeta) meta;

                    JSONObject fireworkJson = new JSONObject();

                    fireworkJson.put("Flight", fm.getPower());

                    List<JSONObject> explList = new ArrayList<JSONObject>();
                    for (FireworkEffect fe : fm.getEffects()) {
                        JSONObject explJson = new JSONObject();

                        if (fe.hasFlicker()) {
                            explJson.put("Flicker", (byte) 1);
                        }
                        if (fe.hasTrail()) {
                            explJson.put("Trail", (byte) 1);
                        }
                        byte type = 42;
                        switch (fe.getType()) {
                            case BALL:
                                type = 0;
                                break;
                            case BALL_LARGE:
                                type = 1;
                                break;
                            case STAR:
                                type = 2;
                                break;
                            case CREEPER:
                                type = 3;
                                break;
                            case BURST:
                                type = 4;
                                break;
                        }
                        explJson.put("Type", type);
                        JSONArray colorArray = new JSONArray();
                        for (Color c : fe.getColors()) {
                            colorArray.add(c.asRGB());
                        }
                        explJson.put("Colors", colorArray);
                        JSONArray fadeArray = new JSONArray();
                        for (Color c : fe.getFadeColors()) {
                            fadeArray.add(c.asRGB());
                        }
                        explJson.put("FadeColors", fadeArray);
                        explList.add(explJson);
                    }
                    fireworkJson.put("Explosions", explList);

                    tagJson.put("Fireworks", fireworkJson);
                }
            }
            
            if(meta instanceof FireworkEffectMeta) {
                if(!hideVarious) {
                    FireworkEffect fe = ((FireworkEffectMeta) meta).getEffect();
                    JSONObject explJson = new JSONObject();

                    if (fe.hasFlicker()) {
                        explJson.put("Flicker", (byte) 1);
                    }
                    if (fe.hasTrail()) {
                        explJson.put("Trail", (byte) 1);
                    }
                    byte type = 42;
                    switch (fe.getType()) {
                        case BALL:
                            type = 0;
                            break;
                        case BALL_LARGE:
                            type = 1;
                            break;
                        case STAR:
                            type = 2;
                            break;
                        case CREEPER:
                            type = 3;
                            break;
                        case BURST:
                            type = 4;
                            break;
                    }
                    explJson.put("Type", type);
                    JSONArray colorArray = new JSONArray();
                    for (Color c : fe.getColors()) {
                        colorArray.add(c.asRGB());
                    }
                    explJson.put("Colors", colorArray);
                    JSONArray fadeArray = new JSONArray();
                    for (Color c : fe.getFadeColors()) {
                        fadeArray.add(c.asRGB());
                    }
                    explJson.put("FadeColors", fadeArray);
                    tagJson.put("Explosion", explJson);
                }
            }
            
            if(meta instanceof MapMeta && ((MapMeta) meta).isScaling() && !hideVarious) {
                tagJson.put("map_is_scaling", (byte) 1);
            }
            
            if(meta instanceof BannerMeta) {
                BannerMeta bm = (BannerMeta) meta;
                JSONObject blockEntityJson = new JSONObject();
                DyeColor baseColor = bm.getBaseColor();
                int base;
                if(baseColor != null) {
                    base = baseColor.getDyeData();
                } else {
                    base = (int) item.getDurability();
                }
                blockEntityJson.put("Base", base);
                
                List<JSONObject> patternList = new ArrayList<JSONObject>();
                for(Pattern p : bm.getPatterns()) {
                    JSONObject patternJson = new JSONObject();
                    patternJson.put("Pattern", p.getPattern().getIdentifier());
                    patternJson.put("Color", p.getColor().getDyeData());
                    patternList.add(patternJson);
                }
                blockEntityJson.put("Patterns", patternList);
                
                tagJson.put("BlockEntityTag", blockEntityJson);
            }

            if(meta.getDisplayName() != null) {
                name = ChatColor.ITALIC + meta.getDisplayName();
                if(useIconRp) {
                    displayJson.put("Name", icon + itemcolor + " " + name);
                } else {
                    displayJson.put("Name", itemcolor + name);
                }
            }

            if(!displayJson.isEmpty()) {
                tagJson.put("display", displayJson);
            }
        }

        NbtCompound itemNbt = NbtFactory.fromItemTag(item);
        getLogger().log(debugLevel, "Item-Nbt: " + itemNbt.toString());

        if (!itemNbt.isEmpty()) {
            if(!hideUnbreakable) {
                Byte unbreakable = itemNbt.getByte("Unbreakable", (byte) 0);
                if(unbreakable != 0) {
                    tagJson.put("Unbreakable", unbreakable);
                }
            }
            
            if(!hideDestroys) {
                NbtList destroyNbtList = itemNbt.getList("CanDestroy", false);
                if(destroyNbtList != null) {
                    List<String> destroyList = new ArrayList<String>();
                    for(Object destroyObj : destroyNbtList) {
                        if(destroyObj instanceof String) {
                            destroyList.add((String) destroyObj);
                        }
                    }
                    if(destroyList.size() > 0) {
                        tagJson.put("CanDestroy", destroyList);
                    }
                }
            }

            if(!hidePlacedOn) {
                NbtList placeNbtList = itemNbt.getList("CanPlaceOn", false);
                if(placeNbtList != null) {
                    List<String> placeList = new ArrayList<String>();
                    for(Object destroyObj : placeNbtList) {
                        if(destroyObj instanceof String) {
                            placeList.add((String) destroyObj);
                        }
                    }
                    if(placeList.size() > 0) {
                        tagJson.put("CanPlaceOn", placeList);
                    }
                }
            }

            if(!hideAttributes) {
                NbtList attrNbtList = itemNbt.getList("AttributeModifiers", false);
                if (attrNbtList != null) {
                    List<JSONObject> attrList = new ArrayList<JSONObject>();
                    for (Object attrObj : attrNbtList) {
                        if (attrObj instanceof NbtCompound) {
                            JSONObject attrJson = new JSONObject();
                            NbtCompound attrNbt = (NbtCompound) attrObj;
                            attrJson.put("AttributeName", attrNbt.getString("AttributeName", "ERROR"));
                            attrJson.put("Name", attrNbt.getString("Name", "ERROR"));
                            String tags[] = {"Amount", "Operation", "UUIDLeast", "UUIDMost"};
                            for(String tag : tags) {
                                Object value = attrNbt.get(tag);
                                if(value == null || !(value instanceof Number)) {
                                    attrJson.put(tag, -1);
                                } else {
                                    attrJson.put(tag, value);
                                }
                            }
                            attrList.add(attrJson);
                        }
                    }
                    if (attrList.size() > 0) {
                        tagJson.put("AttributeModifiers", attrList);
                    }
                }
            }
        }
        
        if(!tagJson.isEmpty()) {
            itemJson.put("tag", tagJson);
        }


        JSONObject hoverJson = new JSONObject();
        hoverJson.put("action", "show_item");
        String mojangItemJson = toMojangJsonString(itemJson.toJSONString());
        getLogger().log(debugLevel, "toMojangJsonString: " + mojangItemJson);

        hoverJson.put("value", mojangItemJson);
        
        JSONObject nameJson = new JSONObject ();        
        if(!name.isEmpty()) {
            String resultname = itemcolor + name + ChatColor.RESET;
            nameJson.put("text", resultname);
        } else {
            nameJson.put("translate", transmap.getKey(item));
            if(!translateWith.isEmpty()) {
                nameJson.put("with", translateWith);
            }
        }
        nameJson.put("hoverEvent", hoverJson);
        nameJson.put("color", itemcolor.name().toLowerCase());

        String lbracket = itemcolor + "[";
        if (useIconRp) {
            lbracket += icon;
        }
        
        JSONObject lbracketJson = new JSONObject();
        lbracketJson.put("text", lbracket);
        lbracketJson.put("hoverEvent", hoverJson);
        
        JSONObject rbracketJson = new JSONObject();
        rbracketJson.put("text", itemcolor + "]");
        rbracketJson.put("hoverEvent", hoverJson);

        getLogger().log(debugLevel, "Json string: " + nameJson.toJSONString());
        return lbracketJson.toJSONString() + "," + nameJson.toJSONString() + "," + rbracketJson.toJSONString();
    }

    /**
     * We have to remove alls quotes around key strings... why? Because Mojang!
     * @param json The json string to convert
     * @return The Mojang item compatible json string
     */
    private String toMojangJsonString(String json) {
        json = json.replace("\\\"", "{ESCAPED_QUOTE}");
        json = json.replaceAll("\"([a-zA-Z]*)\":", "$1:");
        json = json.replace("{ESCAPED_QUOTE}", "\\\"");
        json = StringEscapeUtils.unescapeJava(json);
        return json;
    }

    private String getTranslation(String key) {
        if (lang.getString(key, "").isEmpty()) {
            return ChatColor.RED + "Unknown language key: " + ChatColor.YELLOW + key;
        } else {
            return ChatColor.translateAlternateColorCodes('&', msgcolor + lang.getString(key));
        }
    }
    
    private String getTranslation(String key, Map<String,String> replacements) {
        String string = "[{\"text\":\"" + getTranslation(key).replace("\"", "\\\"") + "\"}]";

        if (replacements != null)
            for (String variable : replacements.keySet()) {
                String r = replacements.get(variable);
                boolean isJson = r.startsWith("{") && r.endsWith("}");
                if(isJson) {
                    string = string.replace("%" + variable + "%", "\"}," + r + ",{\"text\":\"" + msgcolor);
                } else {
                    string = string.replace("%" + variable + "%", r + msgcolor);
                }
                
            }
        return string;
    }

    private void tellRaw(Player player, String msg) {
        tellRaw(player, msg, Level.FINE);
    }
    
    private void tellRaw(Player player, String msg, Level debugLevel) {
        if(spigot && usebungeeapi) {
            getLogger().log(debugLevel, "Tellraw " + player.getName() + ": " + msg);
            try {
                player.spigot().sendMessage(ComponentSerializer.parse(msg));
            } catch (Exception e) {
                getLogger().severe("Exception while using the following json string: " + msg);
                e.printStackTrace();
            }
        } else
            this.getServer().dispatchCommand(this.getServer().getConsoleSender(), "tellraw " + player.getName() + " " + msg);
    }
}
