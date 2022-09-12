package com.tisawesomeness.namehistorian;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public final class MojangLookupImpl implements MojangLookup {

    private static final int CONNECT_TIMEOUT = 5000;
    private static final int READ_TIMEOUT = 5000;
    private static final Gson GSON = new Gson();

    @Override
    public List<NameChange> fetchNameChanges(UUID uuid) throws IOException {
        URL url = new URL("https://api.mojang.com/user/profiles/" + uuid + "/names");
        Optional<InputStreamReader> isrOpt = MojangLookupImpl.get(url);
        if (!isrOpt.isPresent()) {
            return Collections.emptyList();
        }
        try (InputStreamReader isr = isrOpt.get()) {
            return Arrays.asList(MojangLookupImpl.GSON.fromJson(isr, NameChange[].class));
        }
    }

    private static Optional<InputStreamReader> get(URL url) throws IOException {
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setConnectTimeout(CONNECT_TIMEOUT);
        con.setReadTimeout(READ_TIMEOUT);
        con.setInstanceFollowRedirects(false);
        con.connect();
        int responseCode = con.getResponseCode();
        if (responseCode == 200) {
            return Optional.of(new InputStreamReader(con.getInputStream()));
        } else if (responseCode == 204) {
            return Optional.empty();
        } else {
            throw new IOException("HTTP " + responseCode);
        }
    }

}
