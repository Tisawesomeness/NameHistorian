package com.tisawesomeness.namehistorian;

import com.tisawesomeness.namehistorian.spigot.Version;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

public class VersionTest {
    @ParameterizedTest
    @CsvSource({
            "26.2.build.55-alpha, 26, 2, 0",
            "26.1.2.build.23-alpha, 26, 1, 2",
            "1.20.5-R0.1-SNAPSHOT, 1, 20, 5",
            "1.20-R0.1-SNAPSHOT, 1, 20, 0",
            "1.19.2-R0.1-SNAPSHOT, 1, 19, 2",
            "1.8.9-R0.1-SNAPSHOT, 1, 8, 9"
    })
    public void testParseBukkitVersion(String version, int major, int minor, int patch) {
        assertThat(Version.parseBukkitVersion(version))
                .hasValue(new Version(major, minor, patch));
    }
}
