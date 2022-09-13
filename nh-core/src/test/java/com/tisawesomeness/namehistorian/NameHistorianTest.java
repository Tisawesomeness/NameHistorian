package com.tisawesomeness.namehistorian;

import com.tisawesomeness.namehistorian.testutil.MojangLookupMock;

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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class NameHistorianTest {

    private static final UUID TIS_UUID = UUID.fromString("f6489b79-7a9f-49e2-980e-265a05dbc3af");
    private static final Instant TIS_TIME = Instant.ofEpochMilli(1438695830000L);
    private static final UUID JEB_UUID = UUID.fromString("853c80ef-3c37-49fd-aa49-938b674adae6");
    private static final UUID DUMMY_UUID = UUID.fromString("f6489b79-7a9f-49e2-980e-265a05dbc3a0");
    private static final UUID INVALID_UUID = UUID.fromString("f6489b79-7a9f-49e2-980e-265a05dbc3ae");
    private static final UUID THROWING_UUID = UUID.fromString("e6489b79-7a9f-49e2-980e-265a05dbc3af");
    private static final Duration GRACE_PERIOD = Duration.ofMinutes(1);
    private NameHistorian historian;

    @BeforeEach
    public void setUp() throws SQLException, IOException {
        Path parent = Paths.get("target");
        Files.createDirectories(parent);
        Path dbPath = parent.resolve("test.db");
        Files.deleteIfExists(dbPath);
        historian = new NameHistorian(dbPath, new MojangLookupMock());
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

    @Test
    public void testSaveHistory() throws SQLException, IOException {
        assertThat(historian.saveMojangHistory(TIS_UUID)).isTrue();

        List<NameRecord> history = historian.getNameHistory(TIS_UUID);
        checkHistory(history);
    }

    @Test
    public void testSaveHistoryAlreadyExists() throws SQLException, IOException {
        historian.recordName(TIS_UUID, "Tis_awesomeness");
        assertThat(historian.saveMojangHistory(TIS_UUID)).isTrue();

        List<NameRecord> history = historian.getNameHistory(TIS_UUID);
        checkHistory(history);
    }

    @Test
    public void testSaveHistoryPreviouslyExists() throws SQLException, IOException {
        NameRecord nr = new NameRecord(TIS_UUID, "tis_awesomeness", TIS_TIME.minusSeconds(10), Instant.now(), TIS_TIME.minusSeconds(5));
        historian.recordName(nr);
        assertThat(historian.saveMojangHistory(TIS_UUID)).isTrue();

        List<NameRecord> history = historian.getNameHistory(TIS_UUID);
        checkHistory(history);
    }

    @Test
    public void testSaveHistoryDuplicate() throws SQLException, IOException {
        assertThat(historian.saveMojangHistory(TIS_UUID)).isTrue();
        assertThat(historian.saveMojangHistory(TIS_UUID)).isTrue();

        List<NameRecord> history = historian.getNameHistory(TIS_UUID);
        checkHistory(history);
    }

    private static void checkHistory(List<NameRecord> history) {
        assertThat(history).hasSize(2);

        NameRecord latest = history.get(0);
        assertThat(latest)
                .extracting(NameRecord::getUsername, NameRecord::getUuid, NameRecord::getFirstSeenTime)
                .containsExactly("Tis_awesomeness", TIS_UUID, TIS_TIME);
        assertThat(Arrays.asList(latest.getDetectedTime(), latest.getLastSeenTime()))
                .allSatisfy(t -> assertThat(t).isBetween(Instant.now().minus(GRACE_PERIOD), Instant.now()));

        NameRecord original = history.get(1);
        assertThat(original)
                .extracting(NameRecord::getUsername, NameRecord::getUuid, NameRecord::getFirstSeenTime, NameRecord::getLastSeenTime)
                .containsExactly("tis_awesomeness", TIS_UUID, NameHistorianTest.TIS_TIME, TIS_TIME);
        assertThat(original.getDetectedTime())
                .isBetween(Instant.now().minus(GRACE_PERIOD), Instant.now());
    }

    @Test
    public void testSaveHistory1() throws IOException, SQLException {
        assertThat(historian.saveMojangHistory(JEB_UUID)).isTrue();
        List<NameRecord> history = historian.getNameHistory(JEB_UUID);

        assertThat(history).hasSize(1);

        NameRecord original = history.get(0);
        assertThat(original)
                .extracting(NameRecord::getUsername, NameRecord::getUuid)
                .containsExactly("jeb_", JEB_UUID);
        assertThat(Arrays.asList(original.getFirstSeenTime(), original.getDetectedTime(), original.getLastSeenTime()))
                .allSatisfy(t -> assertThat(t).isBetween(Instant.now().minus(GRACE_PERIOD), Instant.now()));
    }

    @Test
    public void testSaveHistory2() throws IOException, SQLException {
        assertThat(historian.saveMojangHistory(DUMMY_UUID)).isTrue();
        List<NameRecord> history = historian.getNameHistory(DUMMY_UUID);

        assertThat(history).hasSize(3);

        NameRecord middle = history.get(1);
        assertThat(middle)
                .extracting(NameRecord::getUsername, NameRecord::getUuid, NameRecord::getFirstSeenTime, NameRecord::getLastSeenTime)
                .containsExactly("dummy2", DUMMY_UUID, TIS_TIME.plusSeconds(1), TIS_TIME.plusSeconds(2));
        assertThat(middle.getDetectedTime())
                .isBetween(Instant.now().minus(GRACE_PERIOD), Instant.now());
    }

    @Test
    public void testSaveHistoryInvalid() throws SQLException, IOException {
        assertThat(historian.saveMojangHistory(INVALID_UUID)).isFalse();
    }
    @Test
    public void testSaveHistoryIOE() {
        assertThatThrownBy(() -> historian.saveMojangHistory(THROWING_UUID))
                .isInstanceOf(IOException.class);
    }

}
