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

import com.google.common.collect.ImmutableMap;
import de.themoep.utils.IconRpMapping;
import de.themoep.utils.IdMapping;
import de.themoep.utils.TranslationMapping;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.*;
import org.bukkit.block.banner.Pattern;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.*;
import java.util.logging.Level;

public class ShowItem extends JavaPlugin implements CommandExecutor {

    ChatColor msgcolor = ChatColor.GREEN;
    ChatColor msgsecondarycolor = ChatColor.YELLOW;
    
    IdMapping idmap;
    TranslationMapping transmap;
    
    int defaultradius;
    int cooldown;
    boolean useIconRp;
    
    Map<UUID, Long> cooldownmap = new HashMap<UUID, Long>();
    
    IconRpMapping iconrpmap;

    
    ConfigurationSection lang;
    
    boolean spigot;
    
    public void onEnable() {
        try {
            Bukkit.class.getMethod("spigot");
            spigot = true;
            this.getLogger().info("Detected Spigot server. Using Bungee chat api for fancy messages!");
        } catch (NoSuchMethodException e) {
            spigot = false;
            this.getLogger().info("Detected a non-Spigot server. Using vanilla tellraw command for fancy messages!");
        }

        this.loadConfig();
    }

    public void loadConfig() {
        this.saveDefaultConfig();
        this.getLogger().info("Loading Config...");
        this.reloadConfig();
        defaultradius = this.getConfig().getInt("defaultradius");
        cooldown = this.getConfig().getInt("cooldown");
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
            Level debugLevel = Level.FINE;
            for(int i = 0; i < args.length; i++) {
                if(args[i].equalsIgnoreCase("-reload")) {
                    if (sender.hasPermission("showitem.command.reload")) {
                        this.loadConfig();
                        sender.sendMessage(ChatColor.GREEN + "Config reloaded.");
                        return true;
                    } else {
                        sender.sendMessage("You don't have the permission showitem.command.reload");
                    }
                } else if(args[i].equalsIgnoreCase("-debug")) {
                    if(sender.hasPermission("showitem.command.debug")) {
                        debugLevel = Level.INFO;
                        sender.sendMessage(ChatColor.GREEN + "Debug message.");
                    } else {
                        sender.sendMessage("You don't have the permission showitem.command.debug");
                    }
                } else if (args[i].equalsIgnoreCase("-radius") || args[i].equalsIgnoreCase("-r")){
                    if(sender.hasPermission("showitem.command.radius")) {
                        if(i + 1 <args.length ) {
                            try {
                                radius = Integer.parseInt(args[i + 1]);
                            } catch (NumberFormatException e) {
                                sender.sendMessage(ChatColor.RED + "Error: Your input " + args[i + 1] + " is not a valid integer!");
                            }
                        } else {
                            sender.sendMessage(ChatColor.RED + "Error: Please input a number after the radius argument!");
                        }
                    } else {
                        sender.sendMessage("You don't have the permission showitem.command.radius");
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

    private void showInRadius(Player sender, int radius, Level debugLevel) {
        
        if(cooldown > 0 && !sender.hasPermission("showitem.cooldownexempt") && cooldownmap.containsKey(sender.getUniqueId())) {
            long diff = System.currentTimeMillis() - cooldownmap.get(sender.getUniqueId());
            if(diff < cooldown * 1000) {
                tellRaw(sender, getTranslation("error.cooldown", ImmutableMap.of("remaining", Integer.toString((int) (cooldown - diff/1000)))));
                return;
            }
        }
        
        String itemstring = convertItem(sender.getItemInHand(), debugLevel);
        Boolean found = false;
        String msg = getTranslation("radius.self", ImmutableMap.of("player", sender.getName(), "item", itemstring));
        if(debugLevel == Level.INFO) {
            sender.sendMessage(ChatColor.stripColor(itemstring));
        }
        if(radius != defaultradius) {
            msg += " " + getTranslation("radius.custom", ImmutableMap.of("radius", Integer.toString(radius)));
        }
        tellRaw(sender, msg);
        for(Player target : sender.getWorld().getPlayers()) {
            if(target != sender && sender.getLocation().distanceSquared(target.getLocation()) <= (radius*radius)) {
                tellRaw(target, getTranslation("radius.target", ImmutableMap.of("player", sender.getName(), "item", itemstring)), debugLevel);
                found = true;
            }
        }
        if(!found)
            tellRaw(sender, getTranslation("error.noonearound", ImmutableMap.of("player", sender.getName(), "radius", Integer.toString(radius))));
    }

    private void showPlayer(Player sender, Player target, Level debugLevel) {
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

        itemJson.put("id", "minecraft:"+ idmap.getMCid(item.getType()));

        itemJson.put("Damage", item.getDurability());
        
        JSONObject tagJson = new JSONObject();
        
        if(item.hasItemMeta()) {
            boolean hideEnchants = false;
            boolean hideAttributes = false;
            boolean hideUnbreakable = false;
            boolean hideDestroys = false;
            boolean hidePlacedOn = false;
            boolean hideVarious = false;
            
            ItemMeta meta = item.getItemMeta();

            if(meta.getLore() != null || meta instanceof LeatherArmorMeta || meta.getItemFlags().size() > 0) {
                JSONObject displayJson = new JSONObject();

                if(meta.getLore() != null && meta.getLore().size() > 0) {
                    List<String> loreList = new ArrayList<String>();
                    for (String l : meta.getLore()) {
                        loreList.add(l);
                    }
                    displayJson.put("Lore", loreList);
                }

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

                if(meta instanceof LeatherArmorMeta) {
                    displayJson.put("color", ((LeatherArmorMeta) meta).getColor().asRGB());
                }
                
                tagJson.put("display", displayJson);
            }

            if(item.getType().isRecord()) {
                itemcolor = ChatColor.AQUA;
            }

            if(meta.spigot() != null && meta.spigot().isUnbreakable() && !hideUnbreakable) {
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
                    name = idmap.getHumanName(item.getType());
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
                    /*taglist.add("SkullOwner:{Name:\\\\\"" + owner + "\\\\\",},");*/
                    JSONObject ownerJson = new JSONObject();
                    ownerJson.put("Name", owner);
                    tagJson.put("SkullOwner", ownerJson);
                    name = owner + "'";
                    if(!(owner.substring(owner.length() -1).equalsIgnoreCase("s") || owner.substring(owner.length() -1).equalsIgnoreCase("x") || owner.substring(owner.length() -1).equalsIgnoreCase("z")))
                        name += "s";
                    name += " Head";
                    meta.setDisplayName(name);
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
                JSONObject displayJson;
                if(tagJson.containsKey("display")) {
                    displayJson = (JSONObject) tagJson.get("display");
                } else {
                    displayJson = new JSONObject();
                }
                
                if(useIconRp) {
                    displayJson.put("Name", icon + itemcolor + " " + name);
                } else {
                    displayJson.put("Name", itemcolor + name);
                }
                
                tagJson.put("display", displayJson);
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
            if (useIconRp) {
                resultname = icon + resultname;
            }
            nameJson.put("text", resultname);
        }        
        nameJson.put("translate", transmap.getKey(item));
        nameJson.put("hoverEvent", hoverJson);
        
        JSONObject lbracketJson = new JSONObject();
        lbracketJson.put("text", itemcolor + "[");
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
                JSONParser jp = new JSONParser();
                boolean isPlainText = false;
                try {
                    Object o = jp.parse(r);
                    if(!(o instanceof JSONObject)) {
                        isPlainText = true;
                    }                    
                } catch (ParseException e) {
                    isPlainText = true;
                }
                if(isPlainText)
                    r = "{\"text\":\"" + msgsecondarycolor + r + "\"}";
                string = string.replace("%" + variable + "%", "\"}," + r + ",{\"text\":\"" + msgcolor);
            }
        return string;
    }

    private void tellRaw(Player player, String msg) {
        tellRaw(player, msg, Level.FINE);
    }
    
    private void tellRaw(Player player, String msg, Level debugLevel) {
        if(spigot) {
            getLogger().log(debugLevel, "Tellraw " + player.getName() + ": " + msg);
            try {
                player.spigot().sendMessage(new ComponentSerializer().parse(msg));
            } catch (Exception e) {
                getLogger().severe("Exception while using the following json string: " + msg);
                e.printStackTrace();
            }
        } else
            this.getServer().dispatchCommand(this.getServer().getConsoleSender(), "tellraw " + player.getName() + " " + msg);
    }
}
