package com.tisawesomeness.namehistorian.spigot;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.tisawesomeness.namehistorian.Util;
import lombok.Value;

import javax.annotation.Nonnegative;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class MojangAPI {

    private static final Gson GSON = new Gson();

    private final LoadingCache<String, Optional<UUID>> uuidLookupCache;
    @Nonnegative
    private final int timeout;

    public MojangAPI(@Nonnegative int timeout) {
        this.timeout = timeout;
        uuidLookupCache = CacheBuilder.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(1))
                .build(new CacheLoader<String, Optional<UUID>>() {
                    @Override
                    public Optional<UUID> load(String s) throws IOException {
                        return lookupUUID(s);
                    }
                });
    }

    public Optional<UUID> getUUID(String username) throws IOException {
        try {
            return uuidLookupCache.get(username);
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            }
            throw new RuntimeException(cause);
        }
    }

    private Optional<UUID> lookupUUID(String username) throws IOException {
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

    private HttpURLConnection getConnection(URL url) throws IOException {
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("Accept", "application/json");
        con.setConnectTimeout(timeout);
        con.setReadTimeout(timeout);
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
