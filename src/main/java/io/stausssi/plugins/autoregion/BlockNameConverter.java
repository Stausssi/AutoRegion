package io.stausssi.plugins.autoregion;

public class BlockNameConverter {
    public static String toReadable(String blockName) {
        return blockName.toLowerCase().replace("_", "");
    }

    public static String toIdentifier(String blockName) {
        return blockName.toUpperCase().replace(" ", "_");
    }
}
