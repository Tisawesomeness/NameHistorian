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
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class NameHistorianTest {

    private static final UUID TIS_UUID = UUID.fromString("f6489b79-7a9f-49e2-980e-265a05dbc3af");
    private static final UUID JEB_UUID = UUID.fromString("853c80ef-3c37-49fd-aa49-938b674adae6");
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
        assertThat(historian.getNameHistory(TIS_UUID)).isEmpty();
    }

    @Test
    public void testHistory() throws SQLException {
        historian.recordName(TIS_UUID, "test");
        assertThat(historian.getNameHistory(TIS_UUID)).hasSize(1);

        NameRecord nr = historian.getNameHistory(TIS_UUID).get(0);
        assertThat(nr.getUsername()).isEqualTo("test");
        assertThat(nr.getUuid()).isEqualTo(TIS_UUID);

        assertThat(Arrays.asList(nr.getFirstSeenTime(), nr.getDetectedTime(), nr.getLastSeenTime()))
                .allSatisfy(t -> assertThat(t).isBetween(Instant.now().minus(GRACE_PERIOD), Instant.now()));
    }

    @Test
    public void testHistory2() throws SQLException {
        historian.recordName(TIS_UUID, "test");
        historian.recordName(TIS_UUID, "test2");

        assertThat(historian.getNameHistory(TIS_UUID))
                .extracting(NameRecord::getUsername)
                .containsExactly("test2", "test");
    }

    @Test
    public void testSeparateHistory() throws SQLException {
        historian.recordName(TIS_UUID, "test");
        historian.recordName(JEB_UUID, "test2");

        assertThat(historian.getNameHistory(TIS_UUID))
                .extracting(NameRecord::getUsername)
                .containsExactly("test");
        assertThat(historian.getNameHistory(JEB_UUID))
                .extracting(NameRecord::getUsername)
                .containsExactly("test2");
    }

    @Test
    public void testBulkRecord() throws SQLException {
        historian.recordNames(Arrays.asList(
                new NamedPlayer(TIS_UUID, "test"),
                new NamedPlayer(JEB_UUID, "test2")
        ));
        assertThat(historian.getNameHistory(TIS_UUID))
                .extracting(NameRecord::getUsername)
                .containsExactly("test");
        assertThat(historian.getNameHistory(JEB_UUID))
                .extracting(NameRecord::getUsername)
                .containsExactly("test2");
    }

}
