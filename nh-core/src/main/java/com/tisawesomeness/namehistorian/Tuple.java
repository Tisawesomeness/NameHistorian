package com.tisawesomeness.namehistorian;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

import java.util.function.BiFunction;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode
public final class Tuple<A, B> {
    private final A a;
    private final B b;

    public static <A, B> Tuple<A, B> of(A a, B b) {
        return new Tuple<>(a, b);
    }

    public A get0() {
        return a;
    }
    public B get1() {
        return b;
    }

    public <R> R fold(BiFunction<A, B, R> function) {
        return function.apply(a, b);
    }

    @Override
    public String toString() {
        return "(" + a + ", " + b + ")";
    }
}
