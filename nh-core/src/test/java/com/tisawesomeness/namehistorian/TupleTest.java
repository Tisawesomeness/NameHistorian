package com.tisawesomeness.namehistorian;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class TupleTest {

    @Test
    public void testContent() {
        assertThat(Tuple.of("a", 1))
                .extracting(Tuple::get0, Tuple::get1)
                .containsExactly("a", 1);
    }

    @Test
    public void testRun() {
        AtomicInteger i = new AtomicInteger(0);
        Tuple.of(2, 1).run((a, b) -> {
            i.addAndGet(a);
            i.addAndGet(b);
        });
        assertThat(i).hasValue(3);
    }

    @Test
    public void testToString() {
        assertThat(Tuple.of("a", 1)).hasToString("(a, 1)");
    }

}
