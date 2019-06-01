package com.stausssi.AutoRegion;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
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
    private String author = getDescription().getAuthors().get(0);
    
    private String sysPrefix, prefix;
    
    private FileConfiguration blocks, config;
    private File blocksf, configf;
    
    private CommandSender cmdSender;
    List<String> lore;
    
    private boolean disablerequest;
    
    private int radius;
    
    UUID UUID;
    
    
    public AutoRegion() {
        sysPrefix = "[" + name + "] ";
        prefix = ChatColor.translateAlternateColorCodes('&', getConfig().getString("prefix"));
        lore = new ArrayList<String>();
        disablerequest = false;
    }

    public void onEnable() {
        createFiles();
        getConfig().options().copyDefaults(true);
        msgServer("Loading EventHandler...");
        getServer().getPluginManager().registerEvents(new Events(this), this);
        msgServer("EventHandler loaded!");
        msgServer("Applying ItemLore...");
        lore.add("");
        lore.add(getConfig().getString("lore"));
        msgServer("Lore applied!");
        msgServer("Successfully enabled " + name + " v" + version + " by " + author + "!");
    }

    public void onDisable() {
        msgServer("Saving config...");
        saveBlockConfig();
        saveConfig();
        msgServer("Configs saved!");
        msgServer("Successfully disabled " + name + " v" + version + " by " + author + "!");
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    	// Only react to "AutoRegion" Commands
        if (label.equalsIgnoreCase("autoregion")) {
        	cmdSender = sender;
        	String block, blockName;
            Material m;
            
        	// One Argument
            if (args.length == 1) {
            	// Disable Command
                if (args[0].equalsIgnoreCase("disable")) {
                    if (sender.hasPermission("autoregion.disable")) {
                        sender.sendMessage(ColorMessage(getConfig().getString("messages.disableRequest")));
                        disablerequest = true;
                    } else {
                    	noPerm();
                    }
                } 
                
                // Confirm Disable Command
                if (args[0].equalsIgnoreCase("confirm")) {
                    if (!sender.hasPermission("autoregion.confirmdisable")) {
                        noPerm();
                    } else if (disablerequest) {
                        getServer().getPluginManager().disablePlugin(this);
                        sender.sendMessage(ColorMessage(getConfig().getString("messages.disabled").replace("%VERSION%", version)));
                    } else {
                    	sender.sendMessage(ColorMessage(getConfig().getString("messages.noDisableRequest")));
                    }
                }

                // Help Command
                if (args[0].equalsIgnoreCase("help")) {
                    if (sender.hasPermission("autoregion.help")) {
                        sender.sendMessage("§8-------- " + prefix + "--------\n"
                        		+ "§6/autoregion disable - §7Disables the plugin\n"
                        		+ "§6/autoregion add [BlockName] [Radius] - §7Adds a block to the config\n"
                        		+ "§6/autoregion remove [BlockName] - §7Removes a block from the config\n"
                        		+ "§6/autoregion give [BlockName] [Player]- §7Gives a player a block to create a region\n"
                        		+ "\n"
                        		+ "§4For instance, BlockName: 'DIAMOND_ORE'");
                    } else {
                    	noPerm();
                    }
                }
            } else if (args.length == 2) {             
            	// Remove Block Command
            	if(args[0].equalsIgnoreCase("remove")) {
	                if (sender.hasPermission("autoregion.remove")) {
	                	blockName = args[1].toUpperCase();
	                    m = Material.getMaterial(blockName);
	                    block = blockName.toLowerCase().replace("_", "");
	                    
	                    // Check if userInput is a valid block
	                    if(isBlock(m)) {
		                    if (getBlockConfig().get("Blocks." + blockName) == null) {
		                        sender.sendMessage(ColorMessage(getConfig().getString("messages.blockNotAdded").replaceAll("%BLOCK%", block)));
		                    } else {
		                    	getBlockConfig().set("Blocks." + blockName, null);
			                    saveBlockConfig();
			                    sender.sendMessage(ColorMessage(getConfig().getString("messages.blockRemoved").replaceAll("%BLOCK%", block)));
		                    }
	                    } else {
	                    	sender.sendMessage(ColorMessage(getConfig().getString("messages.blockDoesntExist").replaceAll("%BLOCK%", block)));
	                    }
	                } else {
	                	noPerm();
	                }
	            } else { 
	            	return false;
	            }
            } else if (args.length == 3) {
            	blockName = args[1].toUpperCase();
                m = Material.getMaterial(blockName);
                block = blockName.toLowerCase().replace("_", " ");
                
            	// Add Command
                if (args[0].equalsIgnoreCase("add")) {
                    if (sender.hasPermission("autoregion.add")) {
                    	
                        // Check if userInput is a valid block
                        if(isBlock(m)) {
                        	
                        	// Parse String to int
                           try {
                        	   radius = Integer.parseInt(args[2]);	
                           } catch (NumberFormatException e) {
                           		sender.sendMessage(ColorMessage(getConfig().getString("messages.noValidNumber").replaceAll("%NUMBER%", args[2])));
                           		return true;
                           }
                           
                           if (getBlockConfig().get("Blocks." + blockName) != null) {
                               sender.sendMessage(ColorMessage(getConfig().getString("messages.blockAlreadyAdded").replaceAll("%BLOCK%", block)));
                           } else {
	                           getBlockConfig().set("Blocks." + blockName, "");
	                           getBlockConfig().set("Blocks." + blockName + ".radius", radius);
	                           saveBlockConfig();
	                           sender.sendMessage(ColorMessage(getConfig().getString("messages.blockAdded").replaceAll("%BLOCK%", block).replaceAll("%RADIUS%", String.valueOf(radius))));
                           }
                       } else {
                    	   sender.sendMessage(ColorMessage(getConfig().getString("messages.blockDoesntExist").replaceAll("%BLOCK%", block)));
                       }
                    } else {
                    	noPerm();
                    }
                } else if (args[0].equalsIgnoreCase("give")) {
                	if(sender.hasPermission("autoregion.give")) { 
                		// Check if userInput is a valid block
	                    if(isBlock(m)) {
	                    	if (getBlockConfig().get("Blocks." + blockName) != null) {
		                        String name = args[2];
		                        UUID = null;
		                        
		                        // Search through players in a Task
		                        Runnable playerSearch = () -> {
		                        	sender.sendMessage(ColorMessage(getConfig().getString("messages.searchingPlayer").replaceAll("%PLAYER%", name)));
		                        	
		                        	// Search for player
		                        	for(Player p : getServer().getOnlinePlayers()) {
		                        	    if(p.getName().equals(name)) {
		                        	    	UUID = p.getUniqueId();
			                                sender.sendMessage(ColorMessage(getConfig().getString("messages.playerFound")));
			                            }			                            		                            
			                        }
			                        	
		                        	// Proceed
		                        	if(UUID != null) {
			                            Player p = Bukkit.getPlayer(UUID);
			                            
			                            radius = 2 * getBlockConfig().getInt("Blocks." + blockName + ".radius") + 1;
			                            
			                            ItemStack stack = new ItemStack(m);
			                            ItemMeta meta = stack.getItemMeta();
			                            meta.setDisplayName(getItemName());
			                            meta.setLore(lore);
			                            stack.setItemMeta(meta);
			                            
			                            if (p.getInventory().firstEmpty() != -1) {
			                            	p.getInventory().addItem(new ItemStack[]{stack});
			                                sender.sendMessage(ColorMessage(getConfig().getString("messages.playerBlockAdded").replaceAll("%BLOCK%", block.toLowerCase().replace("_", " ")).replaceAll("%PLAYER%", p.getName())));
			                                p.sendMessage(ColorMessage(getConfig().getString("messages.playerBlockReceived").replaceAll("%BLOCK%", block.toLowerCase().replace("_", " ")).replaceAll("%RADIUS%", Integer.toString(radius))));
			                            } else {
				                            sender.sendMessage(ColorMessage(getConfig().getString("messages.playerInventoryFull").replaceAll("%PLAYER%", p.getName())));
			                            }
			                        } else {
			                            sender.sendMessage(ColorMessage(getConfig().getString("messages.playerNotFound").replaceAll("%PLAYER%", name)));
			                        }
		                        };
		                        
		                        Thread thread = new Thread(playerSearch);
		                        thread.start();
		                    } else {
		                        sender.sendMessage(ColorMessage(getConfig().getString("messages.blockNotSpecified").replaceAll("%BLOCK%", block.toLowerCase().replace("_", " "))));
		                        return true;
		                    }
	                    } else {
	                    	sender.sendMessage(ColorMessage(getConfig().getString("messages.blockDoesntExist").replaceAll("%BLOCK%", block)));
	                    }
	                } else {
	                	noPerm();
	                }
                } else {
                	return false;
                }
            } else {
            	return false;
            }
            return true;
        }
		return false;
    }

    private void msgServer(String msg) {
        System.out.println(sysPrefix + msg);
    }

    public String ColorMessage(String msg) {
        msg = prefix + ChatColor.translateAlternateColorCodes('&', msg);
        return msg;
    }

    public void noPerm() {
        cmdSender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.noPermission")));
    }

    private void createFiles() {
        msgServer("Loading config...");
        
        configf = new File(getDataFolder(), "config.yml");
        blocksf = new File(getDataFolder(), "blocks.yml");
        
        if (!configf.exists()) {
            msgServer("Couldn't find config! Creating it...");
            configf.getParentFile().mkdirs();
            saveResource("config.yml", false);
            msgServer("Successfully created config!");
        }

        if (!blocksf.exists()) {
            msgServer("Couldn't find block configuration! Creating it...");
            blocksf.getParentFile().mkdirs();
            saveResource("blocks.yml", false);
            msgServer("Successfully created block configuration!");
        }

        config = new YamlConfiguration();
        blocks = new YamlConfiguration();

        try {
            config.load(configf);
            blocks.load(blocksf);
        } catch (InvalidConfigurationException | IOException e) {
            e.printStackTrace();
        }

        try {
            config.save(configf);
            blocks.save(blocksf);
        } catch (IOException e) {
            e.printStackTrace();
        }

        msgServer("Configs successfully loaded!");
    }

    public FileConfiguration getBlockConfig() {
        return blocks;
    }

    public void saveBlockConfig() {
        if (blocks != null && blocksf != null) {
            try {
                getBlockConfig().save(blocksf);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
    
    private boolean isBlock(Material m) {
    	if(m != null) {
    		return m.isBlock();
    	} else {
    		return false;
    	}
    }
    
    public List<String> getLore() {
    	return lore;
    }
    
    public String getItemName() {
    	return "§b" + getConfig().getString("itemName").replaceAll("%RADIUS%", Integer.toString(radius));
    }
}
