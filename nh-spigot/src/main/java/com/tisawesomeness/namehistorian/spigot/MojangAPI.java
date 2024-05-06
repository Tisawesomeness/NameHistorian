package com.tisawesomeness.namehistorian.spigot;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.tisawesomeness.namehistorian.Util;
import lombok.Value;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Optional;
import java.util.UUID;

public final class MojangAPI {
    private MojangAPI() { }

    private static final Gson GSON = new Gson();
    private static final int TIMEOUT = 5000;

    public static Optional<UUID> getUUID(String username) throws IOException {
        URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + username);
        HttpURLConnection con = getConnection(url);
        try {
            con.connect();
            if (con.getResponseCode() != 200) {
                return Optional.empty();
            }
            return read(con.getInputStream())
                    .map(Response::getId)
                    .flatMap(Util::parseUUID);
        } finally {
            con.disconnect();
        }
    }

    private static HttpURLConnection getConnection(URL url) throws IOException {
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("Accept", "application/json");
        con.setConnectTimeout(TIMEOUT);
        con.setReadTimeout(TIMEOUT);
        return con;
    }

    private static Optional<Response> read(InputStream is) throws IOException {
        try (InputStreamReader isr = new InputStreamReader(is)) {
            return Optional.of(GSON.fromJson(isr, Response.class));
        } catch (JsonSyntaxException ex) {
            return Optional.empty();
        }
    }

    @Value
    private static class Response {
        String id;
    }

}
