package com.tisawesomeness.namehistorian;

import lombok.Value;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.UUID;

/**
 * Represents a recording of a player's name from first seen to last seen.
 */
@Value
public class NameRecord {
    /** The player's UUID */
    UUID uuid;
    /** The player's username */
    String username;
    /**
     * The time the player was first seen with this name,
     * may be earlier than first join date if the name was retrieved from an external source
     */
    Instant firstSeenTime;
    /** The time the server detected that the player changed their name, or null if same as first seen */
    @Nullable Instant detectedTime;
    /** The time the player was last seen with this name */
    Instant lastSeenTime;

    public Instant getDetectedTime() {
        return detectedTime == null ? firstSeenTime : detectedTime;
    }

}
