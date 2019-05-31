package com.stausssi.AutoRegion;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class AutoRegion extends JavaPlugin {
    private String name = getDescription().getName();
    private String version = getDescription().getVersion();
    private String sysPrefix;
    private String prefix;
    private FileConfiguration blocks;
    private FileConfiguration config;
    private File blocksf;
    private File configf;
    List<String> lore;
    private boolean disablerequest;

    public AutoRegion() {
        sysPrefix = "[" + name + "] ";
        prefix = ChatColor.translateAlternateColorCodes('&', getConfig().getString("prefix"));
        lore = new ArrayList<String>();
        disablerequest = false;
    }

    public void onEnable() {
        createFiles();
        getConfig().options().copyDefaults(true);
        msgsys("Loading EventHandler...");
        getServer().getPluginManager().registerEvents(new Events(this), this);
        msgsys("EventHandler successfully loaded!");
        msgsys("Applying ItemLore...");
        lore.add("");
        lore.add(getConfig().getString("lore"));
        msgsys("Lore applied!");
        msgsys("Successfully enabled " + name + " v" + version + " by Crotex!");
    }

    public void onDisable() {
        msgsys("Saving configs..");
        saveBlockConfig();
        saveConfig();
        msgsys("Configs successfully saved!");
        msgsys("Successfully disabled " + name + " v" + version + " by Crotex!");
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("autoregion")) {
            if (args.length == 1) {
                if (args[0].equalsIgnoreCase("disable")) {
                    if (sender.hasPermission("autoregion.disable")) {
                        sender.sendMessage(ColorMessage(getConfig().getString("messages.disableRequest")));
                        disablerequest = true;
                        return true;
                    }

                    noPerm(sender);
                    return true;
                }

                if (args[0].equalsIgnoreCase("confirm")) {
                    if (!sender.hasPermission("autoregion.confirmdisable")) {
                        noPerm(sender);
                        return true;
                    }

                    if (disablerequest) {
                        getServer().getPluginManager().disablePlugin(this);
                        sender.sendMessage(ColorMessage(getConfig().getString("messages.disabled").replace("%VERSION%", version)));
                        return true;
                    }

                    smsg(sender, getConfig().getString("messages.noDisableRequest"));
                }

                if (args[0].equalsIgnoreCase("help")) {
                    if (sender.hasPermission("autoregion.help")) {
                        sender.sendMessage("§8-------- " + prefix + "--------\n" + "§6/autoregion disable - §7Disables the plugin\n " + "§6/autoregion add [BlockName] [Radius] - §7Adds a block to the config\n " + "§6/autoregion remove [BlockName] - §7Removes a block from the config\n" + "§6/autoregion give [Player] [BlockName] - §7Gives a player a block to create a region\n\n" + "§4BlockName is for example 'DIAMOND_ORE'");
                        return true;
                    }

                    noPerm(sender);
                    return true;
                }
            }

            String blockName;
            Material m;
            String block;
            if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
                if (sender.hasPermission("autoregion.remove")) {
                    blockName = args[1];
                    m = Material.getMaterial(blockName);
                    block = m.name();
                    if (getBlockConfig().get("Blocks." + block) == null) {
                        sender.sendMessage(ColorMessage(getConfig().getString("messages.blockNotAdded").replaceAll("%BLOCK%", block)));
                        return true;
                    }

                    getBlockConfig().set("Blocks." + block, (Object)null);
                    saveBlockConfig();
                    sender.sendMessage(ColorMessage(getConfig().getString("messages.blockRemoved").replaceAll("%BLOCK%", block)));
                    return true;
                }

                noPerm(sender);
                return true;
            }

            if (args.length == 3) {
                if (args[0].equalsIgnoreCase("add")) {
                    if (sender.hasPermission("autoregion.add")) {
                        blockName = args[1];
                        m = Material.getMaterial(blockName);
                        block = m.name();
                        int radius = Integer.parseInt(args[2]);
                        if (getBlockConfig().get("Blocks." + block) != null) {
                            sender.sendMessage(ColorMessage(getConfig().getString("messages.blockAlreadyAdded").replaceAll("%BLOCK%", block)));
                            return true;
                        }

                        getBlockConfig().set("Blocks." + block, "");
                        getBlockConfig().set("Blocks." + block + ".radius", radius);
                        saveBlockConfig();
                        sender.sendMessage(ColorMessage(getConfig().getString("messages.blockAdded").replaceAll("%BLOCK%", block).replaceAll("%RADIUS%", String.valueOf(radius))));
                        return true;
                    }

                    noPerm(sender);
                    return true;
                }

                if (args[0].equalsIgnoreCase("give") && sender.hasPermission("autoregion.give")) {
                    blockName = args[2].toUpperCase();
                    m = Material.getMaterial(blockName);
                    block = m.name().replace(" ", "_");
                    if (getBlockConfig().get("Blocks." + block) != null) {
                        String name = args[1];
                        UUID UUID = null;

                        // Searching through players
                        for(Player p : getServer().getOnlinePlayers()) {
                            if(p.getName().equals(name)) {
                                UUID = p.getUniqueId();
                            }
                        }

                        if(UUID != null) {
                            OfflinePlayer player = Bukkit.getOfflinePlayer(UUID);

                            if (player.isOnline()) {
                                Player p = (Player)player;
                                ItemStack stack = new ItemStack(m);
                                ItemMeta meta = stack.getItemMeta();
                                meta.setDisplayName("§b" + block.toLowerCase().replace("_", " ") + " - Creates a region of " + (2 * getBlockConfig().getInt("Blocks." + block + ".radius") + 1) + "x" + (2 * getBlockConfig().getInt("Blocks." + block + ".radius") + 1));
                                meta.setLore(lore);
                                stack.setItemMeta(meta);
                                if (p.getInventory().firstEmpty() != -1) {
                                    p.getInventory().addItem(new ItemStack[]{stack});
                                    sender.sendMessage(ColorMessage(getConfig().getString("messages.playerBlockAdded").replaceAll("%BLOCK%", block.toLowerCase().replace("_", " ")).replaceAll("%PLAYER%", p.getName())));
                                    p.sendMessage(ColorMessage(getConfig().getString("messages.playerBlockReceived").replaceAll("%BLOCK%", block.toLowerCase().replace("_", " ")).replaceAll("%SIZE%", String.valueOf(2 * getBlockConfig().getInt("Blocks." + block + ".radius") + 1) + "x" + (2 * getBlockConfig().getInt("Blocks." + block + ".radius") + 1))));
                                    return true;
                                }

                                sender.sendMessage(ColorMessage(getConfig().getString("messages.playerInventoryFull").replaceAll("%PLAYER%", p.getName())));
                                return true;
                            } else {
                                sender.sendMessage(ColorMessage(getConfig().getString("messages.playerNotOnline").replaceAll("%PLAYER%", player.getName())));
                                return true;
                            }
                        } else {
                            return false;
                        }
                    } else {
                        sender.sendMessage(ColorMessage(getConfig().getString("messages.blockNotSpecified").replaceAll("%BLOCK%", block.toLowerCase().replace("_", " "))));
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void msgsys(String msg) {
        System.out.println(sysPrefix + msg);
    }

    public void smsg(CommandSender sender, String msg) {
        sender.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&', msg));
    }

    public String ColorMessage(String msg) {
        msg = prefix + ChatColor.translateAlternateColorCodes('&', msg);
        return msg;
    }

    public void noPerm(CommandSender sender) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.noPermission")));
    }

    private void createFiles() {
        msgsys("Loading configs...");
        configf = new File(getDataFolder(), "config.yml");
        blocksf = new File(getDataFolder(), "blocks.yml");
        if (!configf.exists()) {
            msgsys("Couldn't find config! Creating it...");
            configf.getParentFile().mkdirs();
            saveResource("config.yml", false);
            msgsys("Successfully created config!");
        }

        if (!blocksf.exists()) {
            msgsys("Couldn't find block configuration! Creating it...");
            blocksf.getParentFile().mkdirs();
            saveResource("blocks.yml", false);
            msgsys("Successfully created block configuration!");
        }

        config = new YamlConfiguration();
        blocks = new YamlConfiguration();

        try {
            config.load(configf);
            blocks.load(blocksf);
        } catch (InvalidConfigurationException | IOException var3) {
            var3.printStackTrace();
        }

        try {
            config.save(configf);
            blocks.save(blocksf);
        } catch (IOException var2) {
            var2.printStackTrace();
        }

        msgsys("Configs successfully loaded!");
    }

    public FileConfiguration getBlockConfig() {
        return blocks;
    }

    public void saveBlockConfig() {
        if (blocks != null && blocksf != null) {
            try {
                getBlockConfig().save(blocksf);
            } catch (IOException var2) {
                var2.printStackTrace();
            }

        }
    }
}
