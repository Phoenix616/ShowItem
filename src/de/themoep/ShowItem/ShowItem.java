package de.themoep.ShowItem;

import com.google.common.collect.ImmutableMap;
import de.themoep.utils.IdMapping;
import org.bukkit.*;
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
import java.util.List;
import java.util.Map;

public class ShowItem extends JavaPlugin implements CommandExecutor {

    ChatColor msgcolor = ChatColor.GREEN;
    ChatColor msgsecondarycolor = ChatColor.YELLOW;
    
    int defaultradius;
    
    ConfigurationSection lang;
    
    public void onEnable() {
        this.saveDefaultConfig();
        this.loadConfig();
    }

    public void loadConfig() {
        this.reloadConfig();
        defaultradius = this.getConfig().getInt("defaultradius");
        lang = this.getConfig().getConfigurationSection("lang");
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if(sender.hasPermission("showitem.command")) {
            if(args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                if(sender.hasPermission("showitem.command.reload")) {
                    this.loadConfig();
                    sender.sendMessage(ChatColor.GREEN + "Config reloaded.");
                } else {
                    sender.sendMessage("You don't have the permission showitem.command.reload");
                }
            } else if(sender instanceof Player) {
                if(args.length == 0) {
                    showInRadius((Player) sender, this.defaultradius);
                } else if (args.length > 0) {
                    if(args.length > 1 && args[0].equalsIgnoreCase("radius")) {
                        if(sender.hasPermission("showitem.command.radius")) {
                            try {
                                showInRadius((Player) sender, Integer.parseInt(args[1]));
                            } catch(NumberFormatException e) {
                                sender.sendMessage(ChatColor.RED + "Error: Your input " + args[1] + " is not a valid integer!");
                            }
                        } else {
                            sender.sendMessage("You don't have the permission showitem.command.radius");
                        }
                    } else if(sender.hasPermission("showitem.command.player")){
                        for(String name : args) {
                            Player target = Bukkit.getPlayer(name);
                            if(target != null && target.isOnline()) {
                                showPlayer((Player) sender, target);
                            } else {
                                sender.sendMessage(ChatColor.RED + getTranslation("player.offline", ImmutableMap.of("player", name)));
                            }
                        }
                    } else {
                        sender.sendMessage("You don't have the permission showitem.command.player");
                    }
                }
            } else {
                sender.sendMessage("This command can only be run by a player!");
            }
        }
        return true;
    }

    private void showInRadius(Player sender, int radius) {
        String itemstring = convertItem(sender.getItemInHand());
        Boolean found = false;
        tellRaw(sender, getTranslation("radius.self", ImmutableMap.of("player", sender.getName(), "radius", Integer.toString(radius), "item", itemstring)));
        for(Player target : sender.getWorld().getPlayers()) {
            if(target != sender && sender.getLocation().distanceSquared(target.getLocation()) <= (radius*radius)) {
                tellRaw(target, getTranslation("radius.target", ImmutableMap.of("player", sender.getName(), "item", itemstring)));
                found = true;
            }
        }
        if(!found)
            sender.sendMessage(getTranslation("radius.noone", ImmutableMap.of("player", sender.getName(), "radius", Integer.toString(radius))));
    }

    private void showPlayer(Player sender, Player target) {
        String itemstring = convertItem(sender.getItemInHand());
        tellRaw(target, getTranslation("player.target", ImmutableMap.of("player", sender.getName(), "item", itemstring)));
        tellRaw(sender, getTranslation("player.self", ImmutableMap.of("player", target.getName(), "item", itemstring)));
    }

    private String convertItem(ItemStack item) {
        List<String> taglist = new ArrayList<String>();
        ChatColor itemcolor = ChatColor.WHITE;
        IdMapping.getHumanName(item.getType());
            
        String name = IdMapping.getHumanName(item.getType());

        String msg = "id:minecraft:" + IdMapping.getMCid(item.getType()) + ",";

        msg += "Durability:" + ((item.getDurability() < item.getType().getMaxDurability()) ? item.getDurability() : item.getData().getData() )+ ",";
        
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
            
            if(meta.getLore() != null || meta.getDisplayName() != null || meta instanceof LeatherArmorMeta) {
                String displaytag = "display:{";
                if(meta.getLore() != null && meta.getLore().size() > 0) {
                    displaytag += "Lore:[";
                    for (String l : meta.getLore()) {
                        displaytag += "\\\"" + l + "\\\",";
                    }
                    displaytag = "],";
                }
                if(meta.getDisplayName() != null) {
                    displaytag += "Name:\\\\\"" + meta.getDisplayName() + "\\\\\",";
                    name = ChatColor.ITALIC + meta.getDisplayName();
                }
                if(meta instanceof LeatherArmorMeta) {
                    displaytag += "color:" + ((LeatherArmorMeta) meta).getColor().asRGB() + ",";
                }
                displaytag += "},";
                taglist.add(displaytag);
            }
            
            if(meta instanceof BookMeta) {
                BookMeta bm = (BookMeta) meta;
                if(bm.getTitle() != null) {
                    taglist.add("title:\\\\\"" + bm.getTitle() + "\\\\\",");
                    name += ": " + bm.getTitle();
                }
                if(bm.getAuthor() != null) {
                    taglist.add("author:\\\\\"" + bm.getAuthor() + "\\\\\",");
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
                }
            }
            
            if(meta instanceof FireworkMeta) {
                FireworkMeta fm = (FireworkMeta) meta;
                String fireworktag = "Explosion:{";
                for(FireworkEffect fe : fm.getEffects()) {
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
                            fireworktag += "3";
                            break;
                        case CREEPER:
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
                }
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

        name = itemcolor + "[" + itemcolor + name + ChatColor.RESET + "" +  itemcolor + "]";
        
        return "{\"text\":\"" + name + "\",\"hoverEvent\":{\"action\":\"show_item\",\"value\":\"{" + msg + "}\"}}";
    }

    private String getTranslation(String key) {
        if (lang.getString(key, "").isEmpty()) {
            return ChatColor.RED + "Unknown language key: " + ChatColor.YELLOW + key;
        } else {
            return ChatColor.translateAlternateColorCodes('&', msgcolor + lang.getString(key));
        }
    }
    
    private String getTranslation(String key, Map<String,String> replacements) {
        String string = getTranslation(key);

        if (replacements != null)
            for (String variable: replacements.keySet()) {
                if(variable.equalsIgnoreCase("item")) {
                    string = "[\"\",{\"text\":\"" + string.replaceAll("%" + variable + "%", "\"}," + replacements.get(variable) + ",{\"text\":\"" + msgcolor) + "\"}]";
                } else {
                    string = string.replaceAll("%" + variable + "%", msgsecondarycolor + replacements.get(variable) + msgcolor);
                }
            }
        return string;
    }
    
    private void tellRaw(Player player, String msg) {
        this.getLogger().info("tellraw: " + msg);
        this.getServer().dispatchCommand(this.getServer().getConsoleSender(), "tellraw " + player.getName() + " " + msg);
    }
}
