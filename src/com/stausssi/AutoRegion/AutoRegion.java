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
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class AutoRegion extends JavaPlugin {
	// Get Information about the plugin
    private String name = getDescription().getName();
    private String version = getDescription().getVersion();
    private String author = getDescription().getAuthors().get(0);
    
    // Create prefixes
    private String sysPrefix, prefix;
    
    // Declare Identifiers for the config and blocks file
    public static String messagesIdentifier = "messages";
    
    public static String groupIdentifier = "groups";
    
    public static String playerIdentifier = "players";
    public static String nameIdentifier = "name";
    public static String regionsIdentifier = "regions";
    public static String regionCountIdentifier = "regionCount";
    public static String regionCreatorsReceivedIdentifier = "regionCreatorsReceived";
    
    public static String blocksIdentifier = "Blocks";
    public static String radiusIdentifier = "radius";
    
    // Create the file instances for the configuration and blocks file
    private FileConfiguration blockConfig, config;
    private File blockFile, configFile;
    
    private CommandSender cmdSender;
    List<String> lore;
    
    private boolean disablerequest;
    
    UUID UUID;
    
    public AutoRegion() {
        sysPrefix = "[" + name + "] ";
        prefix = ChatColor.translateAlternateColorCodes('&', getConfig().getString("prefix"));
        lore = new ArrayList<String>();
        disablerequest = false;
    }

    public void onEnable() {
    	initializeFiles();
        getConfig().options().copyDefaults(true);
        
    	if(getConfig().getBoolean("auto-update")) {
    		msgServer("Checking for updates...");
    		
    		@SuppressWarnings("unused")
			Updater updater = new Updater(this, 285923, this.getFile(), Updater.UpdateType.DEFAULT, true);
    		
    		Updater.UpdateResult result = updater.getResult();
    		switch(result) {
    		case SUCCESS:
                // Success: The updater found an update, and has readied it to be loaded the next time the server restarts/reloads
    			msgServer("Update found and ready to install!");
                break;
            case NO_UPDATE:
                // No Update: The updater did not find an update, and nothing was downloaded.
            	msgServer("No update found!");
                break;
            case DISABLED:
                // Won't Update: The updater was disabled in its configuration file.
            	msgServer("Auto-Updater is disabled in global config!");
                break;
            case FAIL_DOWNLOAD:
                // Download Failed: The updater found an update, but was unable to download it.
            	msgServer("Unable to download update!");
                break;
            case FAIL_DBO:
                // dev.bukkit.org Failed: For some reason, the updater was unable to contact DBO to download the file.
            	msgServer("Unable to connect to Bukkit!");
                break;
            case FAIL_NOVERSION:
                // No version found: When running the version check, the file on DBO did not contain the a version in the format 'vVersion' such as 'v1.0'.
            	msgServer("Versions do not match. No update installed!");
                break;
            case FAIL_BADID:
                // Bad id: The id provided by the plugin running the updater was invalid and doesn't exist on DBO.
            	msgServer("PluginID is invalid. Please contact the developer!");
                break;
            case FAIL_APIKEY:
                // Bad API key: The user provided an invalid API key for the updater to use.
            	msgServer("API key is invalid. Please contact the developer!");
                break;
            case UPDATE_AVAILABLE:
                // There was an update found, but because you had the UpdateType set to NO_DOWNLOAD, it was not downloaded.
            	msgServer("Update found but not downloaded!");
            	break;
    		}
    	}
		
        msgServer("Searching Dependencies...");
        if(dependenciesInstalled()) {
        	msgServer("Applying ItemLore...");
            lore.add("");
            lore.add(getConfig().getString("lore"));
            msgServer("Lore applied!");
            msgServer("Loading EventHandler...");
            getServer().getPluginManager().registerEvents(new Events(this), this);
            msgServer("EventHandler loaded!");
            msgServer("Successfully enabled " + name + " v" + version + " by " + author + "!");
        } else {
        	disablePlugin();
        }
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
                        sender.sendMessage(ColorMessage(getConfig().getString(messagesIdentifier + "." + "disableRequest")));
                        disablerequest = true;
                    } else {
                    	noPerm();
                    }
                } 
                
                // Confirm Disable Command
                else if (args[0].equalsIgnoreCase("confirm")) {
                    if (!sender.hasPermission("autoregion.confirmdisable")) {
                        noPerm();
                    } else if (disablerequest) {
                        disablePlugin();
                        sender.sendMessage(ColorMessage(getConfig().getString(messagesIdentifier + "." + "disabled").replace("%VERSION%", version)));
                    } else {
                    	sender.sendMessage(ColorMessage(getConfig().getString(messagesIdentifier + "." + "noDisableRequest")));
                    }
                }

                // Help Command
                else if (args[0].equalsIgnoreCase("help")) {
                    if (sender.hasPermission("autoregion.help")) {
                        sender.sendMessage("§8-------- " + prefix + "--------\n"
                        		+ "§6/autoregion disable §7- Disables the plugin\n"
                        		+ "§6/autoregion updates [enable/disable] §7- Enables/Disables automatic updating\n"
                        		+ "§6/autoregion list §7- Gives you a list of all RegionCreators\n"
                        		+ "§6/autoregion help §7- Displays a list with all commands\n"
                        		+ "§6/autoregion add [BlockName] [Radius] §7- Adds a RegionCreator to the config\n"
                        		+ "§6/autoregion remove [BlockName] §7- Removes a RegionCreator from the config\n"
                        		+ "§6/autoregion give [BlockName] [Player] §7- Gives a player a RegionCreator to create a region\n"
                        		+ "\n"
                        		+ "§4[BlockName] has to be a valid RegionCreator, for instance: 'DIAMOND_ORE'");
                    } else {
                    	noPerm();
                    }
                }
                
                // List Command
                else if(args[0].equalsIgnoreCase("list")) {
                	if(sender.hasPermission("autoregion.list")) {
                		String regionCreators = "";
                		
                		// Get all registered RegionCreators
                		for(String key : getBlockConfig().getConfigurationSection(blocksIdentifier).getKeys(false)) {
                			regionCreators += "§6";
                			// Add the name of the RegionCreator
                			regionCreators += key;
                			// Add the size of the region
                			regionCreators += " §7- " + getConfig().getString("itemName").replaceAll("%DIAMETER%",  Integer.toString(getDiameter(key)));
                			// New line
                			regionCreators += "\n";
                		}
                		
                		sender.sendMessage("§8-------- " + prefix + "--------\n"
                				+ ChatColor.translateAlternateColorCodes('&', getConfig().getString(messagesIdentifier + "." + "list"))
                				+ "\n"
                				+ regionCreators);
                	} else {
                		noPerm();
                	}
                }else {
                	return false;
                }
            }
            
            // Two arguments
            else if (args.length == 2) {             
            	// Remove Block Command
            	if(args[0].equalsIgnoreCase("remove")) {
	                if (sender.hasPermission("autoregion.remove")) {
	                	blockName = args[1].toUpperCase();
	                    m = Material.getMaterial(blockName);
	                    block = blockName.toLowerCase().replace("_", "");
	                    
	                    // Check if userInput is a valid block
	                    if(isBlock(m)) {
		                    if (getBlockConfig().get(blocksIdentifier + "." + blockName) == null) {
		                        sender.sendMessage(ColorMessage(getConfig().getString(messagesIdentifier + "." + "blockNotAdded").replaceAll("%BLOCK%", block)));
		                    } else {
		                    	getBlockConfig().set(blocksIdentifier + "." + blockName, null);
			                    saveBlockConfig();
			                    sender.sendMessage(ColorMessage(getConfig().getString(messagesIdentifier + "." + "blockRemoved").replaceAll("%BLOCK%", block)));
		                    }
	                    } else {
	                    	sender.sendMessage(ColorMessage(getConfig().getString(messagesIdentifier + "." + "blockDoesntExist").replaceAll("%BLOCK%", block)));
	                    }
	                } else {
	                	noPerm();
	                }
	            }
            	
            	// Updates enable/disable Command
            	else if(args[0].equalsIgnoreCase("updates")) {
                	if(args[1].equalsIgnoreCase("disable")) {
                		if(sender.hasPermission("autoregion.updates.disable")) {
                			if(getConfig().getBoolean("auto-update")) {
                				getConfig().set("auto-update", false);
                        		saveConfig();
                        		sender.sendMessage(ColorMessage(getConfig().getString(messagesIdentifier + "." + "updatesDisabled")));
                			} else {
                				sender.sendMessage(ColorMessage(getConfig().getString(messagesIdentifier + "." + "updatesAlreadyDisabled")));
                			}
                    	} else {
                    		noPerm();
                    	}
                	} else if (args[1].equalsIgnoreCase("enable")) {
                		if(sender.hasPermission("autoregion.updates.enable")) {
                			if(!getConfig().getBoolean("auto-update")) {
                				getConfig().set("auto-update", true);
                        		saveConfig();
                        		sender.sendMessage(ColorMessage(getConfig().getString(messagesIdentifier + "." + "updatesEnabled")));
                			} else {
                				sender.sendMessage(ColorMessage(getConfig().getString(messagesIdentifier + "." + "updatesAlreadyEnabled")));
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
            } 
            
            // Three arguments
            else if (args.length == 3) {
            	blockName = args[1].toUpperCase();
                m = Material.getMaterial(blockName);
                block = blockName.toLowerCase().replace("_", " ");
                
            	// Add Command
                if (args[0].equalsIgnoreCase("add")) {
                    if (sender.hasPermission("autoregion.add")) {
                    	
                        // Check if userInput is a valid block
                        if(isBlock(m)) {
                        	int radius;
                        	
                        	// Parse String to int
                           try {
                        	   radius = Integer.parseInt(args[2]);	
                           } catch (NumberFormatException e) {
                           		sender.sendMessage(ColorMessage(getConfig().getString(messagesIdentifier + "." + "noValidNumber").replaceAll("%NUMBER%", args[2])));
                           		return true;
                           }
                           
                           if (getBlockConfig().get(blocksIdentifier + "." + blockName) != null) {
                               sender.sendMessage(ColorMessage(getConfig().getString(messagesIdentifier + "." + "blockAlreadyAdded").replaceAll("%BLOCK%", block)));
                           } else {
	                           getBlockConfig().set(blocksIdentifier + "." + blockName, "");
	                           getBlockConfig().set(blocksIdentifier + "." + blockName + "." + radiusIdentifier, radius);
	                           saveBlockConfig();
	                           sender.sendMessage(ColorMessage(getConfig().getString(messagesIdentifier + "." + "blockAdded").replaceAll("%BLOCK%", block).replaceAll("%RADIUS%", String.valueOf(radius))));
                           }
                       } else {
                    	   sender.sendMessage(ColorMessage(getConfig().getString(messagesIdentifier + "." + "blockDoesntExist").replaceAll("%BLOCK%", block)));
                       }
                    } else {
                    	noPerm();
                    }
                } 
                
                // Give Command
                else if (args[0].equalsIgnoreCase("give")) {
                	if(sender.hasPermission("autoregion.give")) { 
                		// Check if userInput is a valid block
	                    if(isBlock(m)) {
	                    	if (getBlockConfig().get(blocksIdentifier + "." + blockName) != null) {
		                        String name = args[2];
		                        UUID = null;
		                        
		                        // Search through players in a Task
		                        Runnable playerSearch = () -> {
		                        	sender.sendMessage(ColorMessage(getConfig().getString(messagesIdentifier + "." + "searchingPlayer").replaceAll("%PLAYER%", name)));
		                        	
		                        	// Search for player
		                        	for(Player p : getServer().getOnlinePlayers()) {
		                        	    if(p.getName().toLowerCase().equals(name.toLowerCase())) {
		                        	    	UUID = p.getUniqueId();
			                                sender.sendMessage(ColorMessage(getConfig().getString(messagesIdentifier + "." + "playerFound")));
			                            }			                            		                            
			                        }
			                        	
		                        	// Proceed
		                        	if(UUID != null) {
			                            Player p = Bukkit.getPlayer(UUID);
			                            
			                            int diameter = getDiameter(blockName);
			                            
			                            ItemStack stack = new ItemStack(m);
			                            ItemMeta meta = stack.getItemMeta();
			                            meta.setDisplayName(getItemName(diameter));
			                            meta.setLore(lore);
			                            stack.setItemMeta(meta);
			                            
			                            if (p.getInventory().firstEmpty() != -1) {
			                            	p.getInventory().addItem(new ItemStack[]{stack});
			                            	getConfig().set(playerIdentifier + "." + UUID + "." + regionsIdentifier + "." + regionCreatorsReceivedIdentifier, getRegionCreatorsReceived(UUID) + 1);
			                                sender.sendMessage(ColorMessage(getConfig().getString(messagesIdentifier + "." + "playerBlockAdded").replaceAll("%BLOCK%", block.toLowerCase().replace("_", " ")).replaceAll("%PLAYER%", p.getName())));
			                                p.sendMessage(ColorMessage(getConfig().getString(messagesIdentifier + "." + "playerBlockReceived").replaceAll("%BLOCK%", block.toLowerCase().replace("_", " ")).replaceAll("%DIAMETER%", Integer.toString(diameter))));
			                            } else {
				                            sender.sendMessage(ColorMessage(getConfig().getString(messagesIdentifier + "." + "playerInventoryFull").replaceAll("%PLAYER%", p.getName())));
			                            }
			                        } else {
			                            sender.sendMessage(ColorMessage(getConfig().getString(messagesIdentifier + "." + "playerNotFound").replaceAll("%PLAYER%", name)));
			                        }
		                        };
		                        
		                        Thread thread = new Thread(playerSearch);
		                        thread.start();
		                    } else {
		                        sender.sendMessage(ColorMessage(getConfig().getString(messagesIdentifier + "." + "blockNotSpecified").replaceAll("%BLOCK%", block.toLowerCase().replace("_", " "))));
		                        return true;
		                    }
	                    } else {
	                    	sender.sendMessage(ColorMessage(getConfig().getString(messagesIdentifier + "." + "blockDoesntExist").replaceAll("%BLOCK%", block)));
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
        cmdSender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString(messagesIdentifier + "." + "noPermission")));
    }

    private void initializeFiles() {
        msgServer("Loading config...");
        
        configFile = new File(getDataFolder(), "config.yml");
        blockFile = new File(getDataFolder(), "blocks.yml");
        
        if (!configFile.exists()) {
            msgServer("Couldn't find config! Creating it...");
            configFile.getParentFile().mkdirs();
            saveResource("config.yml", false);
            msgServer("Successfully created config!");
        }

        if (!blockFile.exists()) {
            msgServer("Couldn't find block configuration! Creating it...");
            blockFile.getParentFile().mkdirs();
            saveResource("blocks.yml", false);
            msgServer("Successfully created block configuration!");
        }

        config = new YamlConfiguration();
        blockConfig = new YamlConfiguration();

        try {
            config.load(configFile);
            blockConfig.load(blockFile);
        } catch (InvalidConfigurationException | IOException e) {
            e.printStackTrace();
        }

        try {
            config.save(configFile);
            blockConfig.save(blockFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        msgServer("Configs successfully loaded!");
    }

    public FileConfiguration getBlockConfig() {
        return blockConfig;
    }

    public void saveBlockConfig() {
        if (blockConfig != null && blockFile != null) {
            try {
                getBlockConfig().save(blockFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private boolean dependenciesInstalled() {
    	PluginManager pluginManager = getServer().getPluginManager();
    	
    	// Check for WorldEdit
    	if(pluginManager.getPlugin("WorldEdit") != null) {
    		msgServer("Found WorldEdit v" + pluginManager.getPlugin("WorldEdit").getDescription().getVersion() + "!");
    	} else {
    		msgServer("WorldEdit not found!");
    		return false;
    	}
    	
    	//Check for WorldGuard
    	if(pluginManager.getPlugin("WorldGuard") != null) {
    		msgServer("Found WorldGuard v" + pluginManager.getPlugin("WorldGuard").getDescription().getVersion() + "!");
    	} else {
    		msgServer("WorldGuard not found!");
    		return false;
    	}
    	
    	return true;
    }
    
    private boolean isBlock(Material m) {
    	if(m != null) {
    		return m.isBlock();
    	} else {
    		return false;
    	}
    }
    
    private void disablePlugin() {
    	getServer().getPluginManager().disablePlugin(this);
    }
    
    public List<String> getLore() {
    	return lore;
    }
    
    public int getRadius(String blockName) {
    	return getBlockConfig().getInt(blocksIdentifier + "." + blockName + "." + radiusIdentifier);
    }
    
    public int getDiameter(String blockName) {
    	return 2 * getRadius(blockName) +1;
    }
    
    public String getItemName(int radius) {
    	return "§b" + getConfig().getString("itemName").replaceAll("%DIAMETER%", Integer.toString(radius));
    }
    
    public int getRegionCount(UUID UUID) {
    	return getConfig().getInt(playerIdentifier + "." + UUID.toString() + "." + regionsIdentifier + "." + regionCountIdentifier);
    }
    
    public int getRegionCreatorsReceived(UUID UUID) {
    	return getConfig().getInt(playerIdentifier + "." + UUID.toString() + "." + regionsIdentifier + "." + regionCreatorsReceivedIdentifier);
    }
}
