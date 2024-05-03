package com.tisawesomeness.namehistorian;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TupleTest {

    @Test
    public void testContent() {
        assertThat(Tuple.of("a", 1))
                .extracting(Tuple::get0, Tuple::get1)
                .containsExactly("a", 1);
    }

    @Test
    public void testFold() {
        assertThat(Tuple.of(2, 1).fold(Integer::sum)).isEqualTo(3);
    }

    @Test
    public void testToString() {
        assertThat(Tuple.of("a", 1)).hasToString("(a, 1)");
    }

}
