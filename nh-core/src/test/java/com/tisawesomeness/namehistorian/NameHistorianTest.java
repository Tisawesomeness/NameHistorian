package com.tisawesomeness.namehistorian;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class NameHistorianTest {

    private static final UUID TEST_UUID = UUID.fromString("f6489b79-7a9f-49e2-980e-265a05dbc3af");
    private static final UUID TEST_UUID_2 = UUID.fromString("853c80ef-3c37-49fd-aa49-938b674adae6");
    private static final Duration GRACE_PERIOD = Duration.ofMinutes(1);

    private NameHistorian historian;

    @BeforeEach
    public void setUp() throws SQLException, IOException {
        Path parent = Paths.get("target");
        Files.createDirectories(parent);
        Path dbPath = parent.resolve("test.db");
        Files.deleteIfExists(dbPath);
        historian = new NameHistorian(dbPath);
    }

    @Test
    public void testBlank() throws SQLException {
        assertThat(historian.getNameHistory(TEST_UUID)).isEmpty();
    }

    @Test
    public void testHistory() throws SQLException {
        historian.recordName(TEST_UUID, "test");
        assertThat(historian.getNameHistory(TEST_UUID)).hasSize(1);

        NameRecord nr = historian.getNameHistory(TEST_UUID).get(0);
        assertThat(nr.username()).isEqualTo("test");
        assertThat(nr.uuid()).isEqualTo(TEST_UUID);

        assertThat(List.of(nr.firstSeenTime(), nr.detectedTime(), nr.lastSeenTime()))
                .allSatisfy(t -> assertThat(t).isBetween(Instant.now().minus(GRACE_PERIOD), Instant.now()));
    }

    @Test
    public void testHistory2() throws SQLException {
        historian.recordName(TEST_UUID, "test");
        historian.recordName(TEST_UUID, "test2");

        assertThat(historian.getNameHistory(TEST_UUID))
                .extracting(NameRecord::username)
                .containsExactly("test2", "test");
    }

    @Test
    public void testSeparateHistory() throws SQLException {
        historian.recordName(TEST_UUID, "test");
        historian.recordName(TEST_UUID_2, "test2");

        assertThat(historian.getNameHistory(TEST_UUID))
                .extracting(NameRecord::username)
                .containsExactly("test");
        assertThat(historian.getNameHistory(TEST_UUID_2))
                .extracting(NameRecord::username)
                .containsExactly("test2");
    }

}
