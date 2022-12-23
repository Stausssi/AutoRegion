package io.stausssi.plugins.autoregion;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Map;

/**
 * Handles all kind of messages to both in-game and the server console.
 */
public class MessageHandler {
    private static final MessageHandler instance = new MessageHandler();
    private static final ConfigHandler configHandler = ConfigHandler.getInstance();
    private static boolean initialized = false;
    private String sysPrefix, prefix, wideChatPrefix;

    private MessageHandler() {
    }

    public static MessageHandler getInstance() {
        return instance;
    }

    /**
     * Initialize the prefixes for chat messages depending on the name of the plugin.
     *
     * @param pluginName The name of the plugin. Should be retrieved via the plugin.yml to have a consistent name.
     */
    public void init(String pluginName) {
        sysPrefix = String.format("[%s]", pluginName);
        prefix = ChatColor.translateAlternateColorCodes('&', getConfig().getString("prefix", "PREFIX_MISSING"));
        wideChatPrefix = "§8-------- " + prefix + "--------\n";

        initialized = true;
    }

    /**
     * Checks whether this class has been initialized. If not, a message will be printed to the console.
     *
     * @return Whether the init method was called.
     */
    private boolean checkInit() {
        if (!initialized) {
            System.out.println("MessageHandler has not been initialized yet. Please call init() first!");
        }

        return initialized;
    }

    /**
     * Colors the given string.
     *
     * @param str The string to color
     * @return The colored string
     */
    private String convertColors(String str) {
        return ChatColor.translateAlternateColorCodes('&', str);
    }

    /**
     * Log a message to the server console with a prefix.
     *
     * @param msg The message to send.
     */
    public void logServerConsole(String msg) {
        logServerConsole(msg, true);
    }

    /**
     * Log a message to the server console.
     *
     * @param msg    The message to send.
     * @param prefix Whether the message should be prefixed with this plugins name.
     */
    public void logServerConsole(String msg, boolean prefix) {
        if (!prefix || checkInit()) {
            System.out.println(prefix ? sysPrefix + msg : msg);
        }
    }

    /**
     * Sends a colored message retrieved from the config to the given player. Mostly used as a response to a command.
     * Will automatically prefix the message.
     *
     * @param player     The player to send the message to.
     * @param messageKey The key of the message in the plugin config. The messages prefixed will be prepended
     *                   automatically.
     */
    public void sendMessage(CommandSender player, String messageKey) {
        sendMessage(player, messageKey, true);
    }


    /**
     * Sends a message retrieved from the config to the given player. Mostly used as a response to a command. Will
     * automatically prefix the message.
     *
     * @param player     The player to send the message to.
     * @param messageKey The key of the message in the plugin config. The messages prefixed will be prepended
     *                   automatically.
     * @param color      Whether the color codes in the message should be converted.
     */
    public void sendMessage(CommandSender player, String messageKey, boolean color) {
        sendMessage(player, messageKey, color, null);
    }

    /**
     * Sends a colored message from the config to the player. Will also replace placeholders with their given values.
     *
     * @param player       The player to send the message to.
     * @param messageKey   The key of the message in the plugin config. The messages prefixed will be prepended
     *                     automatically.
     * @param replacements The keys contain the placeholders which will be replaced with the corresponding values.
     */
    public void sendMessage(CommandSender player, String messageKey, Map<String, String> replacements) {
        sendMessage(player, messageKey, true, replacements);
    }

    /**
     * Sends a message from the config to the player. Will also replace placeholders with their given values.
     *
     * @param player       The player to send the message to.
     * @param messageKey   The key of the message in the plugin config. The messages prefixed will be prepended
     *                     automatically.
     * @param color        Whether the color codes in the message should be converted.
     * @param replacements The keys contain the placeholders which will be replaced with the corresponding values.
     */
    public void sendMessage(CommandSender player, String messageKey, boolean color, Map<String, String> replacements) {
        sendMessage(player, messageKey, color, replacements, true);
    }

    /**
     * Sends a message retrieved from the config to the given player. Mostly used as a response to a command. Will
     * automatically prefix the message.
     *
     * @param player       The player to send the message to.
     * @param message      Either the key of the message or the message text.
     * @param color        Whether the color codes in the message should be converted.
     * @param replacements The keys contain the placeholders which will be replaced with the corresponding values.
     * @param fromConfig   True, if the given message is a key and should be retrieved from the config. If false, just the
     *                     given message will be sent.
     */
    public void sendMessage(CommandSender player, String message, boolean color, Map<String, String> replacements, boolean fromConfig) {
        sendMessage(player, message, color, replacements, fromConfig, true);
    }

    /**
     * Sends a message retrieved to the given player. Mostly used as a response to a command.
     *
     * @param player       The player to send the message to.
     * @param message      Either the key of the message or the message text.
     * @param color        Whether the color codes in the message should be converted.
     * @param replacements The keys contain the placeholders which will be replaced with the corresponding values.
     * @param fromConfig   True, if the given message is a key and should be retrieved from the config. If false, just the
     *                     given message will be sent.
     * @param prefix       Whether the message should be prefixed.
     */
    public void sendMessage(CommandSender player, String message, boolean color, Map<String, String> replacements, boolean fromConfig, boolean prefix) {
        sendMessage(player, message, color, replacements, fromConfig, prefix, false);
    }

    /**
     * Sends a message retrieved to the given player. Mostly used as a response to a command.
     *
     * @param player       The player to send the message to.
     * @param message      Either the key of the message or the message text.
     * @param color        Whether the color codes in the message should be converted.
     * @param replacements The keys contain the placeholders which will be replaced with the corresponding values.
     * @param fromConfig   True, if the given message is a key and should be retrieved from the config. If false, just the
     *                     given message will be sent.
     * @param prefix       Whether the message should be prefixed.
     * @param widePrefix   Whether the wide prefix spanning an entire chat line should be used.
     */
    public void sendMessage(CommandSender player, String message, boolean color, Map<String, String> replacements, boolean fromConfig, boolean prefix, boolean widePrefix) {
        // Force the class to be initialized if prefixes are used
        if ((prefix || widePrefix) && !checkInit()) {
            return;
        }

        if (fromConfig) {
            message = configHandler.getMessage(message);
        }

        if (color) {
            message = convertColors(message);
        }

        if (prefix) {
            message = this.prefix + message;
        }

        if (widePrefix) {
            message = this.wideChatPrefix + message;
        }

        if (replacements != null) {
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                message = message.replace(entry.getKey(), entry.getValue());
            }
        }

        player.sendMessage(message);
    }

    /**
     * Informs the player that they don't have sufficient privilege to execute the requested action.
     *
     * @param sender The player to inform.
     */
    public void noPermission(CommandSender sender) {
        sendMessage(sender, "noPermission");
    }

    public void sendHelp(CommandSender sender) {
        sendMessage(
                sender,
                "§6/autoregion disable §7- Disables the plugin\n"
                        + "§6/autoregion updates [enable/disable] §7- Enables/Disables automatic updating\n"
                        + "§6/autoregion list §7- Gives you a list of all RegionCreators\n"
                        + "§6/autoregion help §7- Displays a list with all commands\n"
                        + "§6/autoregion add [BlockName] [Radius] §7- Adds a RegionCreator to the config\n"
                        + "§6/autoregion remove [BlockName] §7- Removes a RegionCreator from the config\n"
                        + "§6/autoregion give [BlockName] [Player] §7- Gives a player a RegionCreator to create a region\n"
                        + "\n"
                        + "§4[BlockName] has to be a valid RegionCreator, for instance: 'DIAMOND_ORE'",
                true, null, false, false, true
        );
    }
}
