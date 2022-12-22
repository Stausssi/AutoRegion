package io.stausssi.plugins.autoregion;

import jdk.tools.jlink.internal.plugins.PluginsResourceBundle;
import org.bukkit.ChatColor;

/**
 * Handles all kind of messages to both in-game and the server console.
 */
public class MessageHandler {
    private static final MessageHandler instance = new MessageHandler();
    private static boolean initialized = false;
    
    private static final ConfigHandler configHandler = ConfigHandler.getInstance();

    private String sysPrefix, prefix, wideChatPrefix;

    private MessageHandler() {}

    public static MessageHandler getInstance() {
        return instance;
    }

    public void init(String pluginName) {
        sysPrefix = String.format("[%s]", pluginName);
        prefix = ChatColor.translateAlternateColorCodes(
                '&', getConfig().getString("prefix", "PREFIX_MISSING")
        );
        wideChatPrefix = "§8-------- " + prefix + "--------\n";

        initialized = true;
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
     * @param msg The message to send.
     * @param prefix Whether the message should be prefixed with this plugins name.
     */
    public void logServerConsole(String msg, boolean prefix) {
        System.out.println(prefix ? sysPrefix + msg : msg);
    }
}
