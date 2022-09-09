package com.tisawesomeness.namehistorian;

import javax.annotation.Nullable;
import java.util.function.Function;

public class Util {

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

}
