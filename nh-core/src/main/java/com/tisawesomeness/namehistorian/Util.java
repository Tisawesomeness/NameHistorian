package com.tisawesomeness.namehistorian;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class Util {
    private Util() { }

    /**
     * Applies a function to a value if it is not null.
     * @param nullable The nullable value
     * @param mapper The function to apply
     * @return The result of the function, or null if the value is null.
     * @param <T> The type of the value
     * @param <R> The type of the result
     */
    public static <T, R> @Nullable R mapNullable(@Nullable T nullable, Function<T, R> mapper) {
        return nullable == null ? null : mapper.apply(nullable);
    }

    /**
     * Checks if a string contains only whitespace
     * @param str the string
     * @return true if the string contains only whitespace
     */
    public static boolean isBlank(String str) {
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Reads an embedded resource file as a string.
     * @param name The name of the resource file
     * @return The file contents
     */
    public static String loadResource(String name) {
        InputStream is = openResource(name);
        try (InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(isr)) {
            return br.lines().collect(Collectors.joining("\n"));
        } catch (IOException ex) {
            throw new AssertionError("An IOException when closing a resource stream should never happen.");
        }
    }
    private static InputStream openResource(String name) {
        InputStream is = Util.class.getClassLoader().getResourceAsStream(name);
        if (is == null) {
            throw new IllegalArgumentException("The resource was not found!");
        }
        return is;
    }

}
