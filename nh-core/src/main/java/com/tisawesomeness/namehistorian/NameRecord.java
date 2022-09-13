package com.tisawesomeness.namehistorian;

import lombok.Value;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
    public @Nullable Instant getRawDetectedTime() {
        return detectedTime;
    }

    /**
     * Merges the incoming list of name records with the initial list.
     * This is done by matching records with the same UUID, username, and have overlapping time ranges.
     * The newly returned list has all incoming name records, but with times merged.
     * @param initial The initial list of name records, sorted earliest to latest
     * @param incoming The incoming list of name records, sorted earliest to latest
     * @return The merged list of name records
     */
    public static List<NameRecord> combine(List<NameRecord> initial, List<NameRecord> incoming) {
        List<NameRecord> result = new ArrayList<>(incoming.size());
        int initialIdx = 0;
        int incomingIdx = 0;
        while (initialIdx < initial.size() && incomingIdx < incoming.size()) {
            NameRecord initialRecord = initial.get(initialIdx);
            NameRecord incomingRecord = incoming.get(incomingIdx);

            // if records have the same names, merge
            if (namedPlayerMatches(initialRecord, incomingRecord)) {
                NameRecord combinedRecord = getCombinedRecord(initialRecord, incomingRecord);
                initialIdx++;
                incomingIdx++;
                result.add(combinedRecord);
                continue;
            }

            // if initial is completely before incoming
            if (initialRecord.getLastSeenTime().isBefore(incomingRecord.getFirstSeenTime())) {
                result.add(initialRecord);
                initialIdx++;
                continue;
            }
            // if incoming is completely before initial
            if (incomingRecord.getLastSeenTime().isBefore(initialRecord.getFirstSeenTime())) {
                result.add(incomingRecord);
                incomingIdx++;
                continue;
            }

            // else consume an incoming record
            result.add(incomingRecord);
            incomingIdx++;
        }

        // add remaining records
        while (initialIdx < initial.size()) {
            result.add(initial.get(initialIdx++));
        }
        while (incomingIdx < incoming.size()) {
            result.add(incoming.get(incomingIdx++));
        }
        return result;
    }

    private static boolean namedPlayerMatches(NameRecord initialRecord, NameRecord incomingRecord) {
        return initialRecord.getUuid().equals(incomingRecord.getUuid())
                && initialRecord.getUsername().equals(incomingRecord.getUsername());
    }

    private static NameRecord getCombinedRecord(NameRecord initialRecord, NameRecord incomingRecord) {
        return new NameRecord(
                initialRecord.getUuid(),
                initialRecord.getUsername(),
                initialRecord.getFirstSeenTime().isBefore(incomingRecord.getFirstSeenTime())
                        ? initialRecord.getFirstSeenTime()
                        : incomingRecord.getFirstSeenTime(),
                incomingRecord.getDetectedTime(),
                initialRecord.getLastSeenTime().isAfter(incomingRecord.getLastSeenTime())
                        ? initialRecord.getLastSeenTime()
                        : incomingRecord.getLastSeenTime()
        );
    }

}
