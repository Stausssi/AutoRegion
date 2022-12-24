package io.stausssi.plugins.autoregion;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class AutoRegion extends JavaPlugin {
    // Get Information about the plugin
    private final String name = getDescription().getName();
    private final String version = getDescription().getVersion();
    private final String author = getDescription().getAuthors().get(0);

    private final ConfigHandler configHandler = ConfigHandler.getInstance();
    private final MessageHandler messageHandler = MessageHandler.getInstance();

    List<String> lore = new ArrayList<>();

    private boolean disableRequest;

    public AutoRegion() {
    }

    /**
     * Called when the Plugin is being enabled. Prepares the config files, static variables, etc.
     */
    public void onEnable() {
        configHandler.init(this);
        messageHandler.init(name);

        /*
         Check whether automatic updating is enabled in the config
         IMPORTANT: Auto-Updater is currently disabled
                if (getConfig().getBoolean("auto-update") ) {
                    // Check for updates
                    messageHandler.logServerConsole("Checking for updates...");

                    @SuppressWarnings("unused")
                    Updater updater = new Updater(this, 285923, this.getFile(), Updater.UpdateType.DEFAULT, true);

                    Updater.UpdateResult result = updater.getResult();
                    switch (result) {
                        case SUCCESS:
                            // Success: The updater found an update, and has readied it to be loaded the next time the server restarts/reloads
                            messageHandler.logServerConsole("Update found and ready to install!");
                            break;
                        case NO_UPDATE:
                            // No Update: The updater did not find an update, and nothing was downloaded.
                            messageHandler.logServerConsole("No update found!");
                            break;
                        case DISABLED:
                            // Won't Update: The updater was disabled in its configuration file.
                            messageHandler.logServerConsole("Auto-Updater is disabled in global config!");
                            break;
                        case FAIL_DOWNLOAD:
                            // Download Failed: The updater found an update, but was unable to download it.
                            messageHandler.logServerConsole("Unable to download update!");
                            break;
                        case FAIL_DBO:
                            // dev.bukkit.org Failed: For some reason, the updater was unable to contact DBO to download the file.
                            messageHandler.logServerConsole("Unable to connect to Bukkit!");
                            break;
                        case FAIL_NOVERSION:
                            // No version found: When running the version check, the file on DBO did not contain the a version in the format 'vVersion' such as 'v1.0'.
                            messageHandler.logServerConsole("Versions do not match. No update installed!");
                            break;
                        case FAIL_BADID:
                            // Bad id: The id provided by the plugin running the updater was invalid and doesn't exist on DBO.
                            messageHandler.logServerConsole("PluginID is invalid. Please contact the developer!");
                            break;
                        case FAIL_APIKEY:
                            // Bad API key: The user provided an invalid API key for the updater to use.
                            messageHandler.logServerConsole("API key is invalid. Please contact the developer!");
                            break;
                        case UPDATE_AVAILABLE:
                            // There was an update found, but because you had the UpdateType set to NO_DOWNLOAD, it was not downloaded.
                            messageHandler.logServerConsole("Update found but not downloaded!");
                            break;
                    }
                }
        */

        messageHandler.logServerConsole("Searching Dependencies...");

        // Don't continue without WorldEdit and/or WorldGuard
        if (!dependenciesInstalled()) {
            disablePlugin();
            return;
        }

        // Load ItemLore from config and add it to the List
        messageHandler.logServerConsole("Applying ItemLore...");
        lore.add("");  // Empty line
        lore.add(configHandler.getLore());
        messageHandler.logServerConsole("Lore applied!");

        // Register EventHandler
        messageHandler.logServerConsole("Loading EventHandler...");
        getServer().getPluginManager().registerEvents(new Events(this), this);
        messageHandler.logServerConsole("EventHandler loaded!");

        messageHandler.logServerConsole("Successfully enabled " + name + " v" + version + " by " + author + "!");
    }

    /**
     * Called when the Plugin is being disabled. Persists changes in the config file.
     */
    public void onDisable() {
        // Save configuration files
        configHandler.save();

        messageHandler.logServerConsole("Successfully disabled " + name + " v" + version + " by " + author + "!");
    }


    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        // TODO: Holy remove this insane nesting damn

        // Only react to "/autoregion" Commands
        if (!"autoregion".equalsIgnoreCase(label)) {
            return false;
        }

        // Needed for adding / removing / giving RegionCreators
        // Identifier is UPPER_CASE and name is human-readable
        String blockIdentifier, blockName;
        Material m;

        switch (args.length) {
            case 1:
                break;
            case 2:
                break;
            case 3:
                break;
            default:
                return false;
        }

        // TODO: Switch case with handleXArguments methods
        // One Argument
        if (args.length == 1) {

            // Disable Command
            if (args[0].equalsIgnoreCase("disable")) {
                if (!sender.hasPermission("autoregion.disable")) {
                    messageHandler.noPermission(sender);
                    return true;
                }

                disableRequest = true;
                messageHandler.sendMessage(sender, "disableRequest");
            }

            // Confirm Disable Command
            else if (args[0].equalsIgnoreCase("confirm")) {
                if (!sender.hasPermission("autoregion.confirmdisable")) {
                    messageHandler.noPermission(sender);
                } else if (disableRequest) {
                    disablePlugin();

                    messageHandler.sendMessage(sender, "disabled", Collections.singletonMap("%VERSION%", version));
                } else {
                    messageHandler.sendMessage(sender, "noDisableRequest");
                }
            }

            // Help Command
            else if (args[0].equalsIgnoreCase("help")) {
                if (sender.hasPermission("autoregion.help")) {
                    messageHandler.sendHelp(sender);
                } else {
                    messageHandler.noPermission(sender);
                }
            }

            // List Command
            else if (args[0].equalsIgnoreCase("list")) {
                if (sender.hasPermission("autoregion.list")) {
                    StringBuilder regionCreators = new StringBuilder();

                    // Get all registered RegionCreators
                    for (String regionCreatorName : configHandler.getRegionCreators()) {
                        regionCreators.append("§6");
                        // Add the name of the RegionCreator
                        regionCreators.append(regionCreatorName);
                        // Add the size of the region
                        regionCreators.append(" §7- ").append(configHandler.createItemName(regionCreatorName));
                        // New line
                        regionCreators.append("\n");
                    }

                    messageHandler.sendMessage(
                            sender,
                            configHandler.getMessage("list")
                                    + "\n"
                                    + regionCreators,
                            true,
                            null,
                            false,
                            false,
                            true
                    );
                } else {
                    messageHandler.noPermission(sender);
                }
            } else {
                return false;
            }
        }

        // Two arguments
        else if (args.length == 2) {
            // Remove Block Command
            if (args[0].equalsIgnoreCase("remove")) {
                if (sender.hasPermission("autoregion.remove")) {
                    // Get block from the 2nd argument
                    blockIdentifier = BlockNameConverter.toIdentifier(args[1]);

                    // Convert to Material
                    m = Material.getMaterial(blockIdentifier);

                    // "Restyle" BlockName for better chat appearance
                    blockName = BlockNameConverter.toReadable(blockIdentifier);

                    // Check if userInput is a valid block
                    if (isBlock(m)) {
                        boolean removed = configHandler.deleteRegionCreator(blockIdentifier);

                        messageHandler.sendMessage(sender, removed ? "blockRemoved" : "blockNotAdded", Collections.singletonMap("%BLOCK%", blockName));
                    } else {
                        messageHandler.sendMessage(sender, "blockDoesntExist", Collections.singletonMap("%BLOCK%", blockName));
                    }
                } else {
                    messageHandler.noPermission(sender);
                }
            }

            // Updates enable/disable Command
            else if (args[0].equalsIgnoreCase("updates")) {
                if (args[1].equalsIgnoreCase("disable")) {
                    if (sender.hasPermission("autoregion.updates.disable")) {
                        // Check whether automatic updating is already disabled
                        if (getConfig().getBoolean("auto-update")) {
                            // Disable automatic updating
                            getConfig().set("auto-update", false);
                            saveConfig();

                            messageHandler.sendMessage(sender, "updatesDisabled");
                        } else {
                            messageHandler.sendMessage(sender, "updatesAlreadyDisabled");
                        }
                    } else {
                        messageHandler.noPermission(sender);
                    }
                } else if (args[1].equalsIgnoreCase("enable")) {
                    if (sender.hasPermission("autoregion.updates.enable")) {
                        // Check whether automatic updating is already enabled
                        if (!getConfig().getBoolean("auto-update")) {
                            // Enable automatic updating
                            getConfig().set("auto-update", true);
                            saveConfig();

                            messageHandler.sendMessage(sender, "updatesEnabled");
                        } else {
                            messageHandler.sendMessage(sender, "updatesAlreadyEnabled");
                        }
                    } else {
                        messageHandler.noPermission(sender);
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
            // Get blockName from the 2nd argument
            blockIdentifier = BlockNameConverter.toIdentifier(args[1]);

            // Convert to Material
            m = Material.getMaterial(blockIdentifier);

            // "Restyle" BlockName for better chat appearance
            blockName = BlockNameConverter.toReadable(blockIdentifier);

            // Add Command
            if (args[0].equalsIgnoreCase("add")) {
                if (sender.hasPermission("autoregion.add")) {

                    // Check if userInput is a valid block
                    if (isBlock(m)) {
                        int radius;

                        // Parse String to int
                        try {
                            radius = Integer.parseInt(args[2]);
                        } catch (NumberFormatException e) {
                            // The number entered (3rd argument) can't be parsed to an Integer
                            messageHandler.sendMessage(sender, "noValidNumber", Collections.singletonMap("%NUMBER%", args[2]));
                            return true;
                        }

                        // Try adding the block to the config
                        boolean added = configHandler.addRegionCreator(blockIdentifier, radius);

                        HashMap<String, String> replacements = new HashMap<>();
                        replacements.put("%BLOCK%", blockName);
                        replacements.put("%RADIUS%", String.valueOf(radius));

                        messageHandler.sendMessage(sender, added ? "blockAdded" : "blockAlreadyAdded", replacements);
                    } else {
                        messageHandler.sendMessage(sender, "blockDoesntExist", Collections.singletonMap("%BLOCK%", blockName));
                    }
                } else {
                    messageHandler.noPermission(sender);
                }
            }

            // Give Command
            else if (args[0].equalsIgnoreCase("give")) {
                if (sender.hasPermission("autoregion.give")) {
                    // Check if userInput is a valid block
                    if (isBlock(m)) {
                        if (configHandler.isRegionCreator(blockIdentifier)) {
                            // Get playerName from the 3rd argument
                            String name = args[2];


                            // Search through players in a separate Task
                            Runnable playerSearch = () -> {
                                // Initialize UUID variable
                                UUID UUID = null;

                                messageHandler.sendMessage(sender, "searchingPlayer", Collections.singletonMap("%PLAYER%", name));

                                // Loop through every player on the server and check for the same name
                                for (Player p : getServer().getOnlinePlayers()) {
                                    if (p.getName().equalsIgnoreCase(name)) {
                                        UUID = p.getUniqueId();

                                        messageHandler.sendMessage(sender, "playerFound");
                                        break;
                                    }
                                }

                                // Proceed if a player with the specified name was found
                                if (UUID != null) {
                                    Player p = Bukkit.getPlayer(UUID);

                                    int diameter = configHandler.getDiameter(blockName);

                                    // Create the RegionCreator Item
                                    ItemStack stack = new ItemStack(m);
                                    ItemMeta meta = stack.getItemMeta();

                                    // Set the name with the corresponding diameter
                                    meta.setDisplayName(getItemName(diameter));

                                    // Set the item lore
                                    meta.setLore(lore);

                                    // Add DisplayName and Lore to the Item
                                    stack.setItemMeta(meta);

                                    // Check whether the receiver's inventory is full
                                    if (p.getInventory().firstEmpty() != -1) {
                                        // Add Item to inventory
                                        p.getInventory().addItem(stack);

                                        // Persist the changes to the config
                                        configHandler.addReceivedRegionCreator(p);

                                        // Message both giver and receiver
                                        HashMap<String, String> replacements = new HashMap<>();
                                        replacements.put("%BLOCK%", blockIdentifier);
                                        replacements.put("%PLAYER%", p.getName());
                                        replacements.put("%DIAMETER%", Integer.toString(diameter));

                                        messageHandler.sendMessage(sender, "playerBlockAdded", replacements);
                                        messageHandler.sendMessage(p, "playerBlockReceived", replacements);
                                    } else {
                                        messageHandler.sendMessage(sender, "playerInventoryFull", Collections.singletonMap("%PLAYER%", p.getName()));
                                    }
                                } else {
                                    messageHandler.sendMessage(sender, "playerNotFound", Collections.singletonMap("%PLAYER%", name));
                                }
                            };

                            // Start playerSearch thread
                            Thread thread = new Thread(playerSearch);
                            thread.start();
                        } else {
                            messageHandler.sendMessage(sender, "blockNotSpecified", Collections.singletonMap("%BLOCK%", blockIdentifier));
                            return true;
                        }
                    } else {
                        messageHandler.sendMessage(sender, "blockDoesntExist", Collections.singletonMap("%BLOCK%", blockIdentifier));
                    }
                } else {
                    messageHandler.noPermission(sender);
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
        return true;
    }

    /**
     * Disables this Plugin.
     */
    private void disablePlugin() {
        getServer().getPluginManager().disablePlugin(this);
    }

    /**
     * Checks whether both WorldGuard and WorldEdit are installed.
     *
     * @return True, if both are installed. False otherwise.
     */
    private boolean dependenciesInstalled() {
        PluginManager pluginManager = getServer().getPluginManager();

        // Check whether WorldEdit and WorldGuard are installed
        for (String pluginName : new String[]{"WorldEdit", "WorldGuard"}) {
            Plugin plugin = pluginManager.getPlugin(pluginName);

            if (plugin == null) {
                messageHandler.logServerConsole(pluginName + " not found!");
                return false;
            }

            messageHandler.logServerConsole("Found " + pluginName + " v" + plugin.getDescription().getVersion() + "!");
        }

        return true;
    }

    /**
     * Checks whether the given Material is not Null and a valid block.
     *
     * @param m The material to check.
     * @return True, if the given material is not Null and a block. False otherwise.
     */
    private boolean isBlock(Material m) {
        return m != null && m.isBlock();
    }

    /**
     * Get the lore a RegionCreator must have.
     *
     * @return The lore of a RegionCreator.
     */
    public List<String> getLore() {
        return lore;
    }

    /**
     * Safe, non-nullable, way to retrieve the lore of a given item.
     *
     * @param item The item to get the lore from.
     * @return The lore if it exists, or an empty List.
     */
    public List<String> getLore(ItemStack item) {
        ItemMeta meta = item.getItemMeta();

        return meta != null && meta.hasLore() ? meta.getLore() : new ArrayList<>();
    }

    /**
     * Ensures that the ItemMeta of the given item is not null.
     * Only for AIR a null-meta is possible. AIR is not a valid RegionCreator as well.
     *
     * @param item The item to get the meta from.
     * @return The ItemMeta of the given item.
     */
    public static @NotNull ItemMeta getMetaSafe(ItemStack item) {
        return Objects.requireNonNull(item.getItemMeta());
    }

    // Return the ItemName with the specified diameter
    public String getItemName(int diameter) {
        return "§b" + getConfig().getString("itemName", "").replaceAll("%DIAMETER%", Integer.toString(diameter));
    }
}
