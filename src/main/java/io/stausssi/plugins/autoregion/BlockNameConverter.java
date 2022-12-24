package io.stausssi.plugins.autoregion;

/**
 * Converts the block names between the two used formats:
 * <p>
 * - BLOCK_IDENTIFIER as a unique identifier for config files.
 * <p>
 * - Block name as a human-readable version.
 */
public class BlockNameConverter {
    /**
     * Convert the given identifier to a human-readable format.
     *
     * @param blockIdentifier The name of the block in IDENTIFIER_FORMAT.
     * @return The human-readable version of the block name.
     */
    public static String toReadable(String blockIdentifier) {
        return blockIdentifier.toLowerCase().replace("_", "");
    }

    /**
     * Convert the given name to an identifier.
     *
     * @param blockName The name of the block human-readable.
     * @return The identifier version of the block name.
     */
    public static String toIdentifier(String blockName) {
        return blockName.toUpperCase().replace(" ", "_");
    }
}
