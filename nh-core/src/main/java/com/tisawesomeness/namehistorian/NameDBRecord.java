package com.tisawesomeness.namehistorian;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.UUID;

public record NameDBRecord(
        int id,
        String uuid,
        String username,
        long firstSeenTime,
        @Nullable Long detectedTime,
        long lastSeenTime
) {

    public NameRecord toNameRecord() {
        return new NameRecord(
                UUID.fromString(uuid),
                username,
                Instant.ofEpochMilli(firstSeenTime),
                Util.mapNullable(detectedTime, Instant::ofEpochMilli),
                Instant.ofEpochMilli(lastSeenTime)
        );
    }

}
