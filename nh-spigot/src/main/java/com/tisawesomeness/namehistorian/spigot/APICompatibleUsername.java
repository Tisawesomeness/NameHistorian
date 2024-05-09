package com.tisawesomeness.namehistorian.spigot;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * A username that is supported by both the Mojang API and modern Spigot servers.
 */
@EqualsAndHashCode
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class APICompatibleUsername implements CharSequence, Comparable<APICompatibleUsername> {

    private static final Pattern MOJANG_SUPPORTED_PATTERN = Pattern.compile("^[\\w!@$\\-.?]{1,16}$");

    private final String name;

    /**
     * Creates a username, checking if the name is between 1-16 characters and does not contain characters that
     * break the Mojang API. While "valid" usernames are a more restricted subset, "invalid" names such as "8"
     * or "Din-ex" have existed before and work in the Mojang API. Other "invalid" names that have been registered,
     * such as "Will Wall", break servers and therefore will return empty when passed to this method.
     * @param name the username as a string
     * @return the username, or empty if the string is not supported
     */
    public static Optional<APICompatibleUsername> of(String name) {
        if (!MOJANG_SUPPORTED_PATTERN.matcher(name).matches()) {
            return Optional.empty();
        }
        // Breaks Mojang API, I'm sure you can guess why
        if (name.equals(".") || name.equals("..")) {
            return Optional.empty();
        }
        return Optional.of(new APICompatibleUsername(name));
    }

    @Override
    public int length() {
        return name.length();
    }
    @Override
    public char charAt(int i) {
        return name.charAt(i);
    }
    @Override
    public CharSequence subSequence(int start, int end) {
        return name.subSequence(start, end);
    }
    @Override
    public int compareTo(APICompatibleUsername username) {
        return name.compareTo(username.name);
    }
    @Override
    public String toString() {
        return name;
    }

}
