package com.tisawesomeness.namehistorian.spigot;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

import java.util.Optional;
import java.util.regex.Pattern;

@EqualsAndHashCode
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class APICompatibleUsername implements CharSequence, Comparable<APICompatibleUsername> {

    // Represents all usernames that are supported by both the Mojang API and modern servers.
    // Modern servers restrict usernames to 16 or fewer characters and no spaces
    // Mojang API allows some but not all invalid characters
    private static final Pattern MOJANG_SUPPORTED_PATTERN = Pattern.compile("^[\\w!@$\\-.?]{1,16}$");

    private final String name;

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
