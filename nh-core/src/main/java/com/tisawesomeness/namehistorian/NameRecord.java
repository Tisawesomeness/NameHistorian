package com.tisawesomeness.namehistorian;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.UUID;

/**
 * Represents a recording of a player's name from first seen to last seen.
 * @param uuid The player's UUID
 * @param username The player's username
 * @param firstSeenTime The time the player was first seen with this name,
 *                      may be earlier than first join date if the name was retrieved from an external source
 * @param detectedTime The time the server detected that the player changed their name, or null if same as first seen
 * @param lastSeenTime The time the player was last seen with this name
 */
public record NameRecord(
        UUID uuid,
        String username,
        Instant firstSeenTime,
        @Nullable Instant detectedTime,
        Instant lastSeenTime
) {

    @Override
    public Instant detectedTime() {
        return detectedTime == null ? firstSeenTime : detectedTime;
    }

}
