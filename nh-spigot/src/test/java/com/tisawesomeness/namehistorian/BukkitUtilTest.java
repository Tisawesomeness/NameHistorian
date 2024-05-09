package com.tisawesomeness.namehistorian;

import com.tisawesomeness.namehistorian.spigot.BukkitUtil;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

public class BukkitUtilTest {
    @ParameterizedTest
    @CsvSource({
            "1.20.5-R0.1-SNAPSHOT, 20",
            "1.20-R0.1-SNAPSHOT, 20",
            "1.19.2-R0.1-SNAPSHOT, 19",
            "1.8.9-R0.1-SNAPSHOT, 8"
    })
    public void testParseVersion(String version, int expected) {
        assertThat(BukkitUtil.parseVersion(version))
                .hasValue(expected);
    }
}
