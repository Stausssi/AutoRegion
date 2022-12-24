package io.stausssi.plugins.autoregion;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Provides useful methods to handle the configuration files.
 */
public class ConfigHandler {
    private static final ConfigHandler instance = new ConfigHandler();
    private static boolean initialized = false;

    private static final MessageHandler messageHandler = MessageHandler.getInstance();

    // Declare Identifiers for the config and blocks file
    private static final String messagesIdentifier = "messages";
    private static final String groupIdentifier = "groups";
    private static final String playerIdentifier = "players";
    private static final String nameIdentifier = "name";
    private static final String regionsIdentifier = "regions";
    private static final String regionCountIdentifier = "regionCount";
    private static final String regionCreatorsReceivedIdentifier = "regionCreatorsReceived";
    private static final String blocksIdentifier = "Blocks";
    private static final String radiusIdentifier = "radius";


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

        // Try saving the configs
        try {
            pluginConfig.save(configFile);
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

    public String getString(String key) {
        return getString(key, null);
    }

    public String getString(String key, String defaultValue) {
        if (defaultValue == null) {
            defaultValue = String.format("String with '%s' not present in config!", key);
        }

        return pluginConfig.getString(key, defaultValue);
    }

    /**
     * Gets the String representing the RegionCreator lore from the config.
     *
     * @return The item lore configured in the file.
     */
    public String getLore() {
        return pluginConfig.getString("lore", "Lore not found!");
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
     * Update the config if a player received a RegionCreator.
     *
     * @param player The player that received the RegionCreator.
     */
    public void addReceivedRegionCreator(Player player) {
        UUID playerID = player.getUniqueId();
        String UUID = String.valueOf(playerID);

        int currentCount = getRegionCreatorCount(UUID);

        // Add 1 to received RegionCreators
        pluginConfig.set(createIdentifier(playerIdentifier, UUID, regionsIdentifier, regionCreatorsReceivedIdentifier), currentCount + 1);

        // Update the players name
        pluginConfig.set(createIdentifier(playerIdentifier, UUID, nameIdentifier), player.getName());

        save();
    }

    /**
     * Update the config if a player created a region.
     *
     * @param player The player that created a region with a RegionCreator.
     */
    public void addRegion(Player player) {
        UUID playerID = player.getUniqueId();
        String UUID = String.valueOf(playerID);

        int currentCount = getRegionCount(UUID);

        // Add 1 to the RegionCount
        pluginConfig.set(createIdentifier(playerIdentifier, UUID, regionsIdentifier, regionCountIdentifier), currentCount + 1);

        // Update the players name
        pluginConfig.set(createIdentifier(playerIdentifier, UUID, nameIdentifier), player.getName());

        save();
    }

    /**
     * Gets the number of regions a player has created with RegionCreators.
     *
     * @param UUID The UUID of the player.
     * @return The number of regions.
     */
    public int getRegionCount(String UUID) {
        return getRegionCount(UUID, false);
    }

    /**
     * Gets the number of regions a player has created with RegionCreators.
     *
     * @param UUID The UUID of the player.
     * @return The number of regions.
     */
    public int getRegionCount(UUID UUID) {
        return getRegionCount(String.valueOf(UUID), false);
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
        key = createIdentifier(isGroup ? groupIdentifier : playerIdentifier, key, regionsIdentifier, regionCountIdentifier);
        return pluginConfig.getInt(key, 0);
    }


    /**
     * The amount of RegionCreators the given player has received.
     *
     * @param UUID The UUID of the player.
     * @return The number of received RegionCreators.
     */
    public int getRegionCreatorCount(UUID UUID) {
        return getRegionCreatorCount(String.valueOf(UUID));
    }

    /**
     * The amount of RegionCreators the given player has received.
     *
     * @param UUID The UUID of the player.
     * @return The number of received RegionCreators.
     */
    public int getRegionCreatorCount(String UUID) {
        return pluginConfig.getInt(createIdentifier(playerIdentifier, UUID, regionsIdentifier, regionCountIdentifier), 0);
    }

    /**
     * Gets all registered RegionCreators from the config.
     *
     * @return A set of all registered Region Creators.
     */
    public Set<String> getRegionCreators() {
        return Objects.requireNonNull(blockConfig.getConfigurationSection(blocksIdentifier)).getKeys(false);
    }

    /**
     * Get the configured radius of the RegionCreator with the given name.
     *
     * @param blockIdentifier The name of the block in IDENTIFIER_FORMAT.
     * @return The radius if found, 0 as default.
     */
    public int getRadius(String blockIdentifier) {
        return blockConfig.getInt(createIdentifier(createBlockConfigIdentifier(blockIdentifier), radiusIdentifier), 0);
    }

    /**
     * Get the configured diameter of the RegionCreator with the given name. This is the radius * 2 + 1
     *
     * @param blockIdentifier The name of the block in IDENTIFIER_FORMAT.
     * @return The diameter.
     */
    public int getDiameter(String blockIdentifier) {
        return 2 * getRadius(blockIdentifier) + 1;
    }

    /**
     * Create the item name of the RegionCreator.
     *
     * @param blockIdentifier The name of the block in IDENTIFIER_FORMAT.
     * @return The item name.
     */
    public String createItemName(String blockIdentifier) {
        return Objects.requireNonNull(pluginConfig.getString("itemName")).replace("%DIAMETER%", String.valueOf(getDiameter(blockIdentifier)));
    }

    /**
     * Create the identifier of the block in the config
     *
     * @param blockIdentifier The name of the block in IDENTIFIER_FORMAT.
     * @return The key to retrieve the block config values with.
     */
    private String createBlockConfigIdentifier(String blockIdentifier) {
        return createIdentifier(blocksIdentifier, BlockNameConverter.toIdentifier(blockIdentifier));
    }

    /**
     * Returns whether the given block is registered as a RegionCreator.
     *
     * @param blockIdentifier The name of the block in IDENTIFIER_FORMAT.
     * @return true, if there is an entry in the config for the block. False if no entry is present.
     */
    public boolean isRegionCreator(String blockIdentifier) {
        return blockConfig.get(createBlockConfigIdentifier(blockIdentifier)) != null;
    }

    /**
     * Tries to delete a RegionCreator by removing it from the config.
     *
     * @param blockIdentifier The name of the block in IDENTIFIER_FORMAT.
     * @return true, if the block was removed. False, if the block is not a registered RegionCreator.
     */
    public boolean deleteRegionCreator(String blockIdentifier) {
        if (!isRegionCreator(blockIdentifier)) {
            return false;
        }

        blockConfig.set(createBlockConfigIdentifier(blockIdentifier), null);
        save();

        return true;

    }

    /**
     * Tries to add a RegionCreator by adding it to the config.
     *
     * @param blockIdentifier The name of the block in IDENTIFIER_FORMAT.
     * @return true, if the block was added. False, if the block is already a registered RegionCreator.
     */
    public boolean addRegionCreator(String blockIdentifier, int radius) {
        if (isRegionCreator(blockIdentifier)) {
            return false;
        }

        blockIdentifier = createBlockConfigIdentifier(blockIdentifier);
        blockConfig.set(blockIdentifier, "");
        blockConfig.set(createIdentifier(blockIdentifier, radiusIdentifier), radius);
        save();

        return true;

    }

    /**
     * Get all registered groups from the config.
     *
     * @return A set containing all group identifiers.
     */
    public Set<String> getGroups() {
        return Objects.requireNonNull(pluginConfig.getConfigurationSection(groupIdentifier)).getKeys(false);
    }

    /**
     * Get the block a player should receive on the first join.
     *
     * @return The name of the block in IDENTIFIER_FORMAT or null, if blockOnFirstJoin is false.
     */
    public String blockOnFirstJoin() {
        if (!pluginConfig.getBoolean("blockOnFirstJoin")) {
            return null;
        }

        return BlockNameConverter.toIdentifier(getString("block"));
    }
}

