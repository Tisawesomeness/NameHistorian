package com.tisawesomeness.namehistorian;

import javax.annotation.Nullable;
import java.util.function.Function;

public class Util {

    public static <T, R> @Nullable R mapNullable(@Nullable T nullable, Function<T, R> mapper) {
        return nullable == null ? null : mapper.apply(nullable);
    }

}
