package com.tisawesomeness.namehistorian.spigot;

import lombok.Value;

import java.util.Comparator;
import java.util.Optional;

@Value
public class Version implements Comparable<Version> {
    private static final Comparator<Version> COMPARATOR = Comparator
            .comparingInt((Version v) -> v.major)
            .thenComparingInt(v -> v.minor)
            .thenComparingInt(v -> v.patch);

    int major;
    int minor;
    int patch;

    public static Optional<Version> parseBukkitVersion(String bukkitVersion) {
        int idx = bukkitVersion.indexOf('-');
        if (idx == -1) {
            return Optional.empty();
        }
        String shortVersion = bukkitVersion.substring(0, idx);
        return parse(shortVersion);
    }

    public static Optional<Version> parse(String str) {
        String[] split = str.split("\\.");
        if (split.length < 2) {
            return Optional.empty();
        }
        try {
            int major = Integer.parseInt(split[0]);
            int minor = Integer.parseInt(split[1]);
            int patch = split.length > 2 && isDigits(split[2]) ? Integer.parseInt(split[2]) : 0;
            return Optional.of(new Version(major, minor, patch));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }
    private static boolean isDigits(String str) {
        return !str.isEmpty() && str.chars().allMatch(c -> '0' <= c && c <= '9');
    }

    @Override
    public int compareTo(Version version) {
        return COMPARATOR.compare(this, version);
    }

    @Override
    public String toString() {
        if (patch == 0) {
            return major + "." + minor;
        }
        return major + "." + minor + "." + patch;
    }
}
