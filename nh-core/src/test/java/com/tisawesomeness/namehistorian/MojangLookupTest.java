package com.tisawesomeness.namehistorian;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class MojangLookupTest {

    private static final UUID TIS_UUID = UUID.fromString("f6489b79-7a9f-49e2-980e-265a05dbc3af");
    private static final long TIS_TIMESTAMP = 1438695830000L;
    private static final UUID JEB_UUID = UUID.fromString("853c80ef-3c37-49fd-aa49-938b674adae6");
    private static final UUID INVALID_UUID = UUID.fromString("f6489b79-7a9f-49e2-980e-265a05dbc3ae");

    @Test
    @Disabled("Will break on September 13th!")
    public void testFetchNameChanges() throws IOException {
        assertThat(new MojangLookupImpl().fetchNameChanges(TIS_UUID)).containsExactly(
                new NameChange("tis_awesomeness", 0),
                new NameChange("Tis_awesomeness", TIS_TIMESTAMP)
        );
    }
    @Test
    @Disabled("Will break on September 13th!")
    public void testFetchNameChanges2() throws IOException {
        assertThat(new MojangLookupImpl().fetchNameChanges(JEB_UUID)).containsExactly(
                new NameChange("jeb_", 0)
        );
    }
    @Test
    @Disabled("Will break on September 13th!")
    public void testFetchNameChangesDNE() throws IOException {
        assertThat(new MojangLookupImpl().fetchNameChanges(INVALID_UUID)).isEmpty();
    }

}
