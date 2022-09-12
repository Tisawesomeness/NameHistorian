package com.tisawesomeness.namehistorian;

import lombok.Value;

import java.time.Instant;

/**
 * A name change, as returned by the Mojang API.
 */
@Value
public class NameChange {
    /** The name the player changed to */
    String name;
    /** The time the player changed their name, or 0 if this is the original name */
    long changedToAt;

    /**
     * Whether this name change is the original name, and does not have a timestamp.
     * @return True if this is the original name, false otherwise
     */
    public boolean isOriginal() {
        return changedToAt == 0;
    }

    /**
     * Gets the time the player changed their name.
     * @return The time
     * @throws IllegalStateException If {@link #isOriginal()} is true
     */
    public Instant getChangeTime() {
        if (isOriginal()) {
            throw new IllegalStateException("Cannot get change time of original name");
        }
        return Instant.ofEpochMilli(changedToAt);
    }
}
