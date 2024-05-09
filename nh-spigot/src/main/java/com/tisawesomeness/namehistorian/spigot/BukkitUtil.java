package com.tisawesomeness.namehistorian.spigot;

import java.util.OptionalInt;

public final class BukkitUtil {
    private BukkitUtil() { }

    /**
     * Parses the major version of a Bukkit version from {@code Bukkit.getBukkitVersion()}.
     * As an example, both "1.20" and "1.20.5" have major version 20.
     * @param bukkitVersion the version
     * @return the major version, or empty if it could not be parsed
     */
    public static OptionalInt parseVersion(String bukkitVersion) {
        int dashIdx = bukkitVersion.indexOf('-');
        if (dashIdx == -1 || !bukkitVersion.startsWith("1.")) {
            return OptionalInt.empty();
        }
        // "1.20.5-R0.1-SNAPSHOT" --> "20.5"
        // "1.20-R0.1-SNAPSHOT" --> "20"
        String shortVersion = bukkitVersion.substring(2, dashIdx);
        int dotIdx = shortVersion.indexOf(".");
        // "20.5" --> "20"
        // "20" --> "20"
        String majorVersionStr = dotIdx == -1 ? shortVersion : shortVersion.substring(0, dotIdx);
        try {
            return OptionalInt.of(Integer.parseInt(majorVersionStr));
        } catch (IllegalArgumentException ignore) {
            return OptionalInt.empty();
        }
    }

}
