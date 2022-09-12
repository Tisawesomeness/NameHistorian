package com.tisawesomeness.namehistorian;

import java.io.IOException;
import java.util.*;

public interface MojangLookup {

    /**
     * Fetches the list of name changes from the Mojang API. This method will block until the request is complete.
     * BEWARE: this may error or return empty when Mojang disables the API endpoint on September 13th!
     *
     * @param uuid The UUID of the player
     * @return A list of name changes, oldest to newest, or empty if the UUID could not be found
     * @throws IOException If an I/O error occurs or the API returns something other than 200 or 204
     */
    List<NameChange> fetchNameChanges(UUID uuid) throws IOException;

}
