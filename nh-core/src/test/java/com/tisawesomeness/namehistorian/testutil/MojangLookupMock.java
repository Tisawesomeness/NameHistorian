package com.tisawesomeness.namehistorian.testutil;

import com.tisawesomeness.namehistorian.MojangLookup;
import com.tisawesomeness.namehistorian.NameChange;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class MojangLookupMock implements MojangLookup {

    private static final UUID TIS_UUID = UUID.fromString("f6489b79-7a9f-49e2-980e-265a05dbc3af");
    private static final long TIS_TIMESTAMP = 1438695830000L;
    private static final UUID JEB_UUID = UUID.fromString("853c80ef-3c37-49fd-aa49-938b674adae6");
    private static final UUID DUMMY_UUID = UUID.fromString("f6489b79-7a9f-49e2-980e-265a05dbc3a0");
    private static final UUID INVALID_UUID = UUID.fromString("f6489b79-7a9f-49e2-980e-265a05dbc3ae");

    @Override
    public List<NameChange> fetchNameChanges(UUID uuid) {
        if (uuid.equals(TIS_UUID)) {
            return Arrays.asList(
                    new NameChange("tis_awesomeness", 0),
                    new NameChange("Tis_awesomeness", TIS_TIMESTAMP)
            );
        } else if (uuid.equals(JEB_UUID)) {
            return Collections.singletonList(new NameChange("jeb_", 0));
        } else if (uuid.equals(DUMMY_UUID)) {
            return Arrays.asList(
                    new NameChange("dummy", TIS_TIMESTAMP),
                    new NameChange("dummy2", TIS_TIMESTAMP + 1000),
                    new NameChange("dummy3", TIS_TIMESTAMP + 2000)
            );
        } else if (uuid.equals(INVALID_UUID)) {
            return Collections.emptyList();
        }
        throw new AssertionError("Unexpected UUID: " + uuid);
    }

}
