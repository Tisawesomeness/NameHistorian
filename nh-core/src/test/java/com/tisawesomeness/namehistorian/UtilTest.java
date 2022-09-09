package com.tisawesomeness.namehistorian;

import org.junit.jupiter.api.Test;

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

}
