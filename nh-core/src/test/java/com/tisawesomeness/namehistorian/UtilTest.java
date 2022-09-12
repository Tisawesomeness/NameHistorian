package com.tisawesomeness.namehistorian;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

public class UtilTest {

    @Test
    public void testMapNullable() {
        assertThat(Util.mapNullable(null, String::length)).isNull();
    }
    @Test
    public void testMapNullable2() {
        assertThat(Util.mapNullable("abc", String::length)).isEqualTo(3);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "  ", "\n"})
    public void testBlank(String str) {
        assertThat(Util.isBlank(str)).isTrue();
    }
    @Test
    public void testNotBlank() {
        assertThat(Util.isBlank("a")).isFalse();
    }

}
