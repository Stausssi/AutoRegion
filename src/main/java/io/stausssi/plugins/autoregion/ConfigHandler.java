package io.stausssi.plugins.autoregion;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

/**
 * Provides useful methods to handle the configuration files.
 */
public class ConfigHandler {
    private static final ConfigHandler instance = new ConfigHandler();
    private static boolean initialized = false;

    private static final MessageHandler messageHandler = MessageHandler.getInstance();
    private AutoRegion plugin;

    // Declare Identifiers for the config and blocks file
    private static String messagesIdentifier = "messages";
    private static String groupIdentifier = "groups";
    private static String playerIdentifier = "players";
    private static String nameIdentifier = "name";
    private static String regionsIdentifier = "regions";
    private static String regionCountIdentifier = "regionCount";
    private static String regionCreatorsReceivedIdentifier = "regionCreatorsReceived";
    private static String blocksIdentifier = "Blocks";
    private static String radiusIdentifier = "radius";


    private FileConfiguration blockConfig, pluginConfig;
    private File blockFile, configFile;

    private ConfigHandler() {
    }

    public static ConfigHandler getInstance() {
        return instance;
    }

    /**
     * Init the Handler by loading or creating both configuration files.
     *
     * @param plugin The instance of the AutoRegion plugin.
     * @return Whether the init was successful. False if something went wrong.
     */
    public boolean init(@NotNull AutoRegion plugin) {
        this.plugin = plugin;

        messageHandler.logServerConsole("Loading config...");

        File dataFolder = plugin.getDataFolder();

        if (!dataFolder.exists() && dataFolder.mkdirs()) {
            messageHandler.logServerConsole("Created AutoRegion directory!");
        }

        // Create file instances
        configFile = new File(dataFolder, "config.yml");
        blockFile = new File(dataFolder, "blocks.yml");

        for (File f : new File[]{configFile, blockFile}) {
            // Check whether the file exists
            if (!f.exists()) {
                String fileName = f.getName();

                messageHandler.logServerConsole("Couldn't find " + fileName + "! Creating it...");

                // Create the config file
                plugin.saveResource(fileName, false);

                messageHandler.logServerConsole("Successfully created " + fileName + "!");
            }
        }

        // Create YAML Configuration instances
        pluginConfig = new YamlConfiguration();
        blockConfig = new YamlConfiguration();

        // Try loading the config into the files
        try {
            pluginConfig.load(configFile);
            blockConfig.load(blockFile);
        } catch (InvalidConfigurationException | IOException e) {
            e.printStackTrace();
            return false;
        }

        pluginConfig.options().copyDefaults();

        // Try saving the files
        try {
            pluginConfig.save(configFile);
            blockConfig.save(blockFile);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        messageHandler.logServerConsole("Configs successfully loaded!");

        initialized = true;

        return true;
    }

    /**
     * Saves both configs.
     *
     * @return Whether the save was successful.
     */
    public boolean save() {
        if (!initialized) {
            return false;
        }

        messageHandler.logServerConsole("Saving config...");

        plugin.saveConfig();

        // Try saving the BlockConfig
        try {
            blockConfig.save(blockFile);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        messageHandler.logServerConsole("Configs saved!");
        return true;
    }

    /**
     * Creates a YAML identifier by joining all provided keys with a dot.
     *
     * @param keys The keys to join with a dot in between
     * @return A String representing the YAML identifier.
     */
    private String createIdentifier(String... keys) {
        return String.join(".", keys);
    }

    public String getLore() {
        return pluginConfig.getString("lore", "lore not found!");
    }

    /**
     * Gets a message String with the given key from the plugin config.
     *
     * @param key The key of the message String.
     * @return Either the configured message or a default message telling the user the key is not present in the config.
     */
    public String getMessage(String key) {
        key = createIdentifier(messagesIdentifier, key);
        return pluginConfig.getString(key, String.format("Message with key '%s' missing", key));
    }

    /**
     * Gets the number of regions a player has created with RegionCreators.
     *
     * @param key The UUID of the player (I think).
     * @return The number of regions.
     */
    public int getRegionCount(String key) {
        return getRegionCount(key, false);
    }

    /**
     * Either gets the number of regions a player has created with RegionCreators or the maximum number of regions a
     * group can create.
     *
     * @param key     The UUID of the player or the name of the group.
     * @param isGroup If true, retrieve the region count instead of the player count.
     * @return The number of regions.
     */
    public int getRegionCount(String key, boolean isGroup) {
        key = createIdentifier(isGroup ? groupIdentifier : playerIdentifier, key);
        return pluginConfig.getInt(key, 0);
    }
}

