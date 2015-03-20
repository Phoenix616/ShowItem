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
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ShowItem extends JavaPlugin implements CommandExecutor {

    ChatColor msgcolor = ChatColor.GREEN;
    ChatColor msgsecondarycolor = ChatColor.YELLOW;
    
    IdMapping idmap;
    
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
        if(useIconRp)
            this.iconrpmap = new IconRpMapping(this);
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if(sender.hasPermission("showitem.command")) {
            boolean debug = false;
            int radius = this.defaultradius;
            for(int i = 0; i < args.length; i++) {
                if(args[i].equalsIgnoreCase("-reload")) {
                    if (sender.hasPermission("showitem.command.reload")) {
                        this.loadConfig();
                        sender.sendMessage(ChatColor.GREEN + "Config reloaded.");
                    } else {
                        sender.sendMessage("You don't have the permission showitem.command.reload");
                    }
                } else if(args[i].equalsIgnoreCase("-debug")) {
                    if(sender.hasPermission("showitem.command.debug")) {
                        debug = true;
                        sender.sendMessage(ChatColor.GREEN + "Debug message. Look into the console!");
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
                } else if (args.length > 0) {
                    if(sender.hasPermission("showitem.command.player")){
                        for(String name : args) {
                            Player target = Bukkit.getPlayer(name);
                            if(target != null && target.isOnline()) {
                                showPlayer((Player) sender, target, debug);
                            } else {
                                sender.sendMessage(ChatColor.RED + getTranslation("error.playeroffline", ImmutableMap.of("player", name)));
                            }
                        }
                    } else {
                        sender.sendMessage("You don't have the permission showitem.command.player");
                    }
                } else {
                    showInRadius((Player) sender, radius, debug);                    
                }
            } else {
                sender.sendMessage("This command can only be run by a player!");
            }
        }
        sender.sendMessage("You don't have the permission showitem.command");
        return true;
    }

    private void showInRadius(Player sender, int radius, boolean debug) {
        
        if(cooldown > 0 && !sender.hasPermission("showitem.cooldownexempt") && cooldownmap.containsKey(sender.getUniqueId())) {
            long diff = System.currentTimeMillis() - cooldownmap.get(sender.getUniqueId());
            if(diff < cooldown * 1000) {
                tellRaw(sender, getTranslation("error.cooldown", ImmutableMap.of("remaining", Integer.toString((int) (cooldown - diff/1000)))));
                return;
            }
        }
        
        String itemstring = convertItem(sender.getItemInHand(), debug);
        Boolean found = false;
        String msg = getTranslation("radius.self", ImmutableMap.of("player", sender.getName(), "item", itemstring));
        if(radius != defaultradius)
            msg += " " + getTranslation("radius.custom", ImmutableMap.of("radius", Integer.toString(radius)));
        tellRaw(sender, msg);
        for(Player target : sender.getWorld().getPlayers()) {
            if(target != sender && sender.getLocation().distanceSquared(target.getLocation()) <= (radius*radius)) {
                tellRaw(target, getTranslation("radius.target", ImmutableMap.of("player", sender.getName(), "item", itemstring)));
                found = true;
            }
        }
        if(!found)
            tellRaw(sender, getTranslation("error.noonearound", ImmutableMap.of("player", sender.getName(), "radius", Integer.toString(radius))));
    }

    private void showPlayer(Player sender, Player target, boolean debug) {
        String itemstring = convertItem(sender.getItemInHand(), debug);
        tellRaw(target, getTranslation("player.target", ImmutableMap.of("player", sender.getName(), "item", itemstring)));
        tellRaw(sender, getTranslation("player.self", ImmutableMap.of("player", target.getName(), "item", itemstring)));
    }

    private String convertItem(ItemStack item, boolean debug) {
        List<String> taglist = new ArrayList<String>();
        ChatColor itemcolor = ChatColor.WHITE;
            
        String icon = "";
        if(useIconRp)
            icon = iconrpmap.getIcon(item, true);
        String name = idmap.getHumanName(item.getType());

        String msg = "id:minecraft:" + idmap.getMCid(item.getType()) + ",";

        msg += "Damage:" + item.getDurability() + ",";
        
        if(item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            
            if(meta.spigot() != null && meta.spigot().isUnbreakable())
                taglist.add("Unbreakable:1,");
            
            if (meta.getEnchants() != null && meta.getEnchants().size() > 0) {
                String enchtag = "ench:[";
                for (Map.Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet())
                    enchtag += "{id:" + entry.getKey().hashCode() + ",lvl:" + entry.getValue() + "},";

                enchtag += "],";
                taglist.add(enchtag);
                itemcolor = ChatColor.AQUA;
            }

            if(meta instanceof EnchantmentStorageMeta) {
                EnchantmentStorageMeta esm = (EnchantmentStorageMeta) meta;
                if(esm.getStoredEnchants() != null) {
                    String storedenchtag = "StoredEnchantments:[";
                    for (Map.Entry<Enchantment, Integer> entry : esm.getStoredEnchants().entrySet())
                        storedenchtag += "{id:" + entry.getKey().hashCode() + ",lvl:" + entry.getValue() + "},";

                    storedenchtag += "],";
                    taglist.add(storedenchtag);
                    itemcolor = ChatColor.YELLOW;
                }
            }

            if(meta instanceof PotionMeta) {
                PotionMeta pm = (PotionMeta) meta;
                String potiontag = "CustomPotionEffects:[";
                for (PotionEffect potion : pm.getCustomEffects())
                    potiontag += "{Id:" + potion.getType().hashCode() + 
                            ",Amplifier:" + potion.getAmplifier() + 
                            ",Duration:" + potion.getDuration() + 
                            ((potion.isAmbient()) ? "Ambient:1," : "") + "},";

                potiontag += "],";
                taglist.add(potiontag);
                itemcolor = ChatColor.YELLOW;
            }

            if(meta instanceof BookMeta) {
                BookMeta bm = (BookMeta) meta;
                if(bm.getTitle() != null) {
                    taglist.add("title:\\\"" + bm.getTitle() + "\\\",");
                    name += ": " + bm.getTitle();
                }
                if(bm.getAuthor() != null) {
                    taglist.add("author:\\\"" + bm.getAuthor() + "\\\",");
                    if(bm.getTitle() == null)
                        name += " by " + bm.getAuthor();
                }

            }

            if(meta instanceof SkullMeta) {
                SkullMeta sm = (SkullMeta) meta;
                if(sm.hasOwner()) {
                    String owner = sm.getOwner();
                    /*taglist.add("SkullOwner:{Name:\\\\\"" + owner + "\\\\\",},");*/
                    name = owner + "'";
                    if(!(owner.substring(owner.length() -1).equalsIgnoreCase("s") || owner.substring(owner.length() -1).equalsIgnoreCase("x") || owner.substring(owner.length() -1).equalsIgnoreCase("z")))
                        name += "s";
                    name += " Head";
                    meta.setDisplayName(name);
                }
            }
            
            if(meta.getLore() != null || meta.getDisplayName() != null || meta instanceof LeatherArmorMeta) {
                String displaytag = "display:{";
                if(meta.getLore() != null && meta.getLore().size() > 0) {
                    displaytag += "Lore:[";
                    for (String l : meta.getLore()) {
                        displaytag += "\\\"" + l + "\\\",";
                    }
                    displaytag = "],";
                }
                if(meta instanceof LeatherArmorMeta) {
                    displaytag += "color:" + ((LeatherArmorMeta) meta).getColor().asRGB() + ",";
                }
                if(meta.getDisplayName() != null) {
                    displaytag += "Name:\\\"";
                    if(useIconRp)
                        displaytag += icon + " ";
                    name = ChatColor.ITALIC + meta.getDisplayName();
                    displaytag += name + "\\\",";
                }
                displaytag += "},";
                taglist.add(displaytag);
            }

            if(meta instanceof FireworkMeta) {
                FireworkMeta fm = (FireworkMeta) meta;
                String fireworktag = "Fireworks:{";
                fireworktag += "Flight:" + fm.getPower() + "b,";
                fireworktag += "Explosions:[";
                for(FireworkEffect fe : fm.getEffects()) {
                    fireworktag += "{";
                    fireworktag += (fe.hasFlicker()) ? "Flicker:1," : "";
                    fireworktag += (fe.hasTrail()) ? "Trail:1," : "";
                    fireworktag += "Type:";
                    switch(fe.getType()) {
                        case BALL:
                            fireworktag += "0";
                            break;
                        case BALL_LARGE:
                            fireworktag += "1";
                            break;
                        case STAR:
                            fireworktag += "2";
                            break;
                        case CREEPER:
                            fireworktag += "3";
                            break;
                        case BURST:
                            fireworktag += "4";
                            break;
                        default:
                            fireworktag += "42";
                            break;
                    }
                    fireworktag += ",";
                    fireworktag += "Colors:[";
                    for(Color c : fe.getColors()) {
                        fireworktag += c.asRGB() + ",";
                    }
                    fireworktag += "],";
                    fireworktag += "FadeColors:[";
                    for(Color c : fe.getFadeColors()) {
                        fireworktag += c.asRGB() + ",";
                    }
                    fireworktag += "]";
                    fireworktag += "},";
                }
                fireworktag += "]},";
                taglist.add(fireworktag);
            }
            
            if(meta instanceof FireworkEffectMeta) {
                FireworkEffect fe = ((FireworkEffectMeta) meta).getEffect();
                String fireworktag = "Explosion:{";
                fireworktag += (fe.hasFlicker()) ? "Flicker:1," : "";
                fireworktag += (fe.hasTrail()) ? "Trail:1," : "";
                fireworktag += "Type:";
                switch(fe.getType()) {
                    case BALL:
                        fireworktag += "0";
                        break;
                    case BALL_LARGE:
                        fireworktag += "1";
                        break;
                    case STAR:
                        fireworktag += "2";
                        break;
                    case CREEPER:
                        fireworktag += "3";
                        break;
                    case BURST:
                        fireworktag += "4";
                        break;
                    default:
                        fireworktag += "42";
                        break;
                }
                fireworktag += ",";
                fireworktag += "Colors:[";
                for(Color c : fe.getColors()) {
                    fireworktag += c.asRGB() + ",";
                }
                fireworktag += "],";
                fireworktag += "FadeColors:[";
                for(Color c : fe.getFadeColors()) {
                    fireworktag += c.asRGB() + ",";
                }
                fireworktag += "],";
                fireworktag += "},";
                taglist.add(fireworktag);
            }
            
            if(meta instanceof MapMeta && ((MapMeta) meta).isScaling())
                taglist.add("map_is_scaling:1");
        }


        if(taglist.size() > 0) {
            msg += "tag:{";
            for(String tag : taglist)
                msg += tag;
            msg += "}";            
        }
        
        if(item.getType().isRecord())
            itemcolor = ChatColor.AQUA;

        String resultname = itemcolor + "[";
        if(useIconRp) {
            resultname += icon;
        }
        resultname += itemcolor + name + ChatColor.RESET + "" +  itemcolor + "]";
        
        String itemstring = "{\"text\":\"" + resultname + "\",\"hoverEvent\":{\"action\":\"show_item\",\"value\":\"{" + msg + "}\"}}";

        if(debug)
            getLogger().info("Debug: " + itemstring);
        return itemstring;
    }

    private String getTranslation(String key) {
        if (lang.getString(key, "").isEmpty()) {
            return ChatColor.RED + "Unknown language key: " + ChatColor.YELLOW + key;
        } else {
            return ChatColor.translateAlternateColorCodes('&', msgcolor + lang.getString(key));
        }
    }
    
    private String getTranslation(String key, Map<String,String> replacements) {
        String string = "[\"\",{\"text\":\"" + getTranslation(key).replace("\"", "\\\"") + "\"}]";

        if (replacements != null)
            for (String variable : replacements.keySet()) {
                String r = replacements.get(variable);
                if(!r.startsWith("{\"text\":\""))
                    r = "{\"text\":\"" + msgsecondarycolor + r + "\"}";
                string = string.replace("%" + variable + "%", "\"}," + r + ",{\"text\":\"" + msgcolor);
            }
        return string;
    }
    
    private void tellRaw(Player player, String msg) {
        if(spigot)
            player.spigot().sendMessage(new ComponentSerializer().parse(msg));
        else
            this.getServer().dispatchCommand(this.getServer().getConsoleSender(), "tellraw " + player.getName() + " " + msg);
    }
}
