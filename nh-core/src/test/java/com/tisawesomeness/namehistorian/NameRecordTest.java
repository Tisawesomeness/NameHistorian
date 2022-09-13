package com.tisawesomeness.namehistorian;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class NameRecordTest {

    private static final UUID TIS_UUID = UUID.fromString("f6489b79-7a9f-49e2-980e-265a05dbc3af");
    private static final Instant TIS_TIME = Instant.ofEpochMilli(1438695830000L);

    @Test
    public void testCombine() {
        assertThat(NameRecord.combine(Collections.emptyList(), Collections.emptyList()))
                .isEmpty();
    }
    @Test
    public void testCombine2() {
        NameRecord nr = new NameRecord(TIS_UUID, "test", Instant.now(), Instant.now(), Instant.now());
        assertThat(NameRecord.combine(Collections.singletonList(nr), Collections.emptyList()))
                .containsExactly(nr);
    }
    @Test
    public void testCombine3() {
        NameRecord nr = new NameRecord(TIS_UUID, "test", Instant.now(), Instant.now(), Instant.now());
        assertThat(NameRecord.combine(Collections.emptyList(), Collections.singletonList(nr)))
                .containsExactly(nr);
    }
    @Test
    public void testCombine4() {
        NameRecord nr = new NameRecord(TIS_UUID, "test", Instant.now(), Instant.now(), Instant.now());
        assertThat(NameRecord.combine(Collections.singletonList(nr), Collections.singletonList(nr)))
                .containsExactly(nr);
    }
    @Test
    public void testCombine5() {
        NameRecord nrDb = new NameRecord(TIS_UUID, "test", TIS_TIME.minusSeconds(5), Instant.now(), TIS_TIME);
        NameRecord nrMoj = new NameRecord(TIS_UUID, "test", TIS_TIME.minusSeconds(10), Instant.now(), TIS_TIME);
        assertThat(NameRecord.combine(Collections.singletonList(nrDb), Collections.singletonList(nrMoj)))
                .hasSize(1)
                .first()
                .extracting(NameRecord::getFirstSeenTime, NameRecord::getLastSeenTime)
                .containsExactly(TIS_TIME.minusSeconds(10), TIS_TIME);
    }
    @Test
    public void testCombine6() {
        NameRecord nrDb = new NameRecord(TIS_UUID, "test", TIS_TIME, Instant.now(), TIS_TIME.plusSeconds(5));
        NameRecord nrMoj = new NameRecord(TIS_UUID, "test", TIS_TIME, Instant.now(), TIS_TIME.plusSeconds(10));
        assertThat(NameRecord.combine(Collections.singletonList(nrDb), Collections.singletonList(nrMoj)))
                .hasSize(1)
                .first()
                .extracting(NameRecord::getFirstSeenTime, NameRecord::getLastSeenTime)
                .containsExactly(TIS_TIME, TIS_TIME.plusSeconds(10));
    }
    @Test
    public void testCombine7() {
        NameRecord nrDb = new NameRecord(TIS_UUID, "test", TIS_TIME.minusSeconds(5), Instant.now(), TIS_TIME.minusSeconds(5));
        NameRecord nrMoj = new NameRecord(TIS_UUID, "test2", TIS_TIME, Instant.now(), TIS_TIME);
        assertThat(NameRecord.combine(Collections.singletonList(nrDb), Collections.singletonList(nrMoj)))
                .containsExactly(nrDb, nrMoj);
    }
    @Test
    public void testCombine8() {
        NameRecord nrDb = new NameRecord(TIS_UUID, "test", TIS_TIME, Instant.now(), TIS_TIME);
        NameRecord nrMoj = new NameRecord(TIS_UUID, "test2", TIS_TIME.minusSeconds(5), Instant.now(), TIS_TIME.minusSeconds(5));
        assertThat(NameRecord.combine(Collections.singletonList(nrDb), Collections.singletonList(nrMoj)))
                .containsExactly(nrMoj, nrDb);
    }
    @Test
    public void testCombine9() {
        List<NameRecord> dbRecords = Collections.singletonList(
                new NameRecord(TIS_UUID, "test2", TIS_TIME, Instant.now(), TIS_TIME.plusSeconds(5))
        );
        List<NameRecord> mojRecords = Arrays.asList(
                new NameRecord(TIS_UUID, "test", TIS_TIME, Instant.now(), TIS_TIME),
                new NameRecord(TIS_UUID, "test2", TIS_TIME, Instant.now(), TIS_TIME.plusSeconds(10))
        );
        assertThat(NameRecord.combine(dbRecords, mojRecords))
                .isEqualTo(mojRecords);
    }
    @Test
    public void testCombine10() {
        Instant now = Instant.now();
        List<NameRecord> dbRecords = Arrays.asList(
                new NameRecord(TIS_UUID, "test", TIS_TIME.minusSeconds(10), now, TIS_TIME.minusSeconds(5)),
                new NameRecord(TIS_UUID, "test2", TIS_TIME.plusSeconds(3), now, TIS_TIME.plusSeconds(6)),
                new NameRecord(TIS_UUID, "test3", TIS_TIME.plusSeconds(11), now, TIS_TIME.plusSeconds(19))
        );
        List<NameRecord> mojRecords = Arrays.asList(
                new NameRecord(TIS_UUID, "test", TIS_TIME, now, TIS_TIME),
                new NameRecord(TIS_UUID, "test2", TIS_TIME, now, TIS_TIME.plusSeconds(10)),
                new NameRecord(TIS_UUID, "test3", TIS_TIME.plusSeconds(10), now, TIS_TIME.plusSeconds(20))
        );
        assertThat(NameRecord.combine(dbRecords, mojRecords))
                .containsExactly(
                        new NameRecord(TIS_UUID, "test", TIS_TIME.minusSeconds(10), now, TIS_TIME),
                        new NameRecord(TIS_UUID, "test2", TIS_TIME, now, TIS_TIME.plusSeconds(10)),
                        new NameRecord(TIS_UUID, "test3", TIS_TIME.plusSeconds(10), now, TIS_TIME.plusSeconds(20))
                );
    }
    @Test
    public void testCombine11() {
        Instant now = Instant.now();
        List<NameRecord> dbRecords = Arrays.asList(
                new NameRecord(TIS_UUID, "test2", TIS_TIME.plusSeconds(3), now, TIS_TIME.plusSeconds(6)),
                new NameRecord(TIS_UUID, "test3", TIS_TIME.plusSeconds(11), now, TIS_TIME.plusSeconds(19))
        );
        List<NameRecord> mojRecords = Arrays.asList(
                new NameRecord(TIS_UUID, "test", TIS_TIME, now, TIS_TIME),
                new NameRecord(TIS_UUID, "test2", TIS_TIME, now, TIS_TIME.plusSeconds(10)),
                new NameRecord(TIS_UUID, "test3", TIS_TIME.plusSeconds(10), now, TIS_TIME.plusSeconds(20))
        );
        assertThat(NameRecord.combine(dbRecords, mojRecords))
                .containsExactly(
                        new NameRecord(TIS_UUID, "test", TIS_TIME, now, TIS_TIME),
                        new NameRecord(TIS_UUID, "test2", TIS_TIME, now, TIS_TIME.plusSeconds(10)),
                        new NameRecord(TIS_UUID, "test3", TIS_TIME.plusSeconds(10), now, TIS_TIME.plusSeconds(20))
                );
    }
    @Test
    public void testCombine12() {
        Instant now = Instant.now();
        List<NameRecord> dbRecords = Arrays.asList(
                new NameRecord(TIS_UUID, "test", TIS_TIME.minusSeconds(10), now, TIS_TIME.minusSeconds(5)),
                new NameRecord(TIS_UUID, "test2", TIS_TIME.plusSeconds(3), now, TIS_TIME.plusSeconds(6)),
                new NameRecord(TIS_UUID, "test", TIS_TIME.plusSeconds(11), now, TIS_TIME.plusSeconds(19))
        );
        List<NameRecord> mojRecords = Arrays.asList(
                new NameRecord(TIS_UUID, "test", TIS_TIME, now, TIS_TIME),
                new NameRecord(TIS_UUID, "test2", TIS_TIME, now, TIS_TIME.plusSeconds(10)),
                new NameRecord(TIS_UUID, "test", TIS_TIME.plusSeconds(10), now, TIS_TIME.plusSeconds(20))
        );
        assertThat(NameRecord.combine(dbRecords, mojRecords))
                .containsExactly(
                        new NameRecord(TIS_UUID, "test", TIS_TIME.minusSeconds(10), now, TIS_TIME),
                        new NameRecord(TIS_UUID, "test2", TIS_TIME, now, TIS_TIME.plusSeconds(10)),
                        new NameRecord(TIS_UUID, "test", TIS_TIME.plusSeconds(10), now, TIS_TIME.plusSeconds(20))
                );
    }
    @Test
    public void testCombine13() {
        Instant now = Instant.now();
        List<NameRecord> dbRecords = Collections.singletonList(
                new NameRecord(TIS_UUID, "tis_awesomeness", TIS_TIME.minusSeconds(10), Instant.now(), TIS_TIME.minusSeconds(5))
        );
        List<NameRecord> mojRecords = Arrays.asList(
                new NameRecord(TIS_UUID, "tis_awesomeness", TIS_TIME, now, TIS_TIME),
                new NameRecord(TIS_UUID, "Tis_awesomeness", TIS_TIME, now, TIS_TIME.plusSeconds(10))
        );
        assertThat(NameRecord.combine(dbRecords, mojRecords))
                .containsExactly(
                        new NameRecord(TIS_UUID, "tis_awesomeness", TIS_TIME.minusSeconds(10), now, TIS_TIME),
                        new NameRecord(TIS_UUID, "Tis_awesomeness", TIS_TIME, now, TIS_TIME.plusSeconds(10))
                );
    }

}
