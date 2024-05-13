package com.tisawesomeness.namehistorian.spigot;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.tisawesomeness.namehistorian.util.ThrowingFunction;
import com.tisawesomeness.namehistorian.util.Util;
import lombok.Value;
import org.jetbrains.annotations.Range;

import javax.annotation.Nonnegative;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class MojangAPI {

    private static final Gson GSON = new Gson();

    private final LoadingCache<String, Optional<UUID>> uuidLookupCache;
    private final LoadingCache<UUID, Optional<String>> usernameLookupCache;
    @Nonnegative
    private final int timeout;

    public MojangAPI(@Nonnegative int timeout, @Range(from = 60, to = Integer.MAX_VALUE) int lifetime) {
        this.timeout = timeout;
        uuidLookupCache = CacheBuilder.newBuilder()
                .expireAfterWrite(lifetime, TimeUnit.SECONDS)
                .build(loader(this::lookupUUID));
        usernameLookupCache = CacheBuilder.newBuilder()
                .expireAfterWrite(lifetime, TimeUnit.SECONDS)
                .build(loader(this::lookupUsername));
    }
    private static <K, V> CacheLoader<K, V> loader(ThrowingFunction<K, V> func) {
        return new CacheLoader<K, V>() {
            @Override
            public V load(K key) throws Exception {
                return func.apply(key);
            }
        };
    }

    public Optional<UUID> getUUID(APICompatibleUsername username) throws IOException {
        try {
            String usernameStr = username.toString();
            Optional<UUID> uuidOpt = uuidLookupCache.get(usernameStr);
            uuidOpt.ifPresent(uuid -> usernameLookupCache.asMap().putIfAbsent(uuid, Optional.of(usernameStr)));
            return uuidOpt;
        } catch (ExecutionException ex) {
            return rethrow(ex);
        }
    }
    private Optional<UUID> lookupUUID(String username) throws IOException {
        URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + username);
        return makeRequest(url)
                .map(Response::getId)
                .flatMap(Util::parseUUID);
    }

    public Optional<String> getUsername(UUID uuid) throws IOException {
        try {
            Optional<String> usernameOpt = usernameLookupCache.get(uuid);
            usernameOpt.ifPresent(username -> uuidLookupCache.asMap().putIfAbsent(username, Optional.of(uuid)));
            return usernameLookupCache.get(uuid);
        } catch (ExecutionException ex) {
            return rethrow(ex);
        }
    }
    private Optional<String> lookupUsername(UUID uuid) throws IOException {
        String undashedUuid = uuid.toString().replace("-", "");
        URL url = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + undashedUuid);
        return makeRequest(url)
                .map(Response::getName);
    }

    private <T> T rethrow(ExecutionException ex) throws IOException {
        Throwable cause = ex.getCause();
        if (cause instanceof IOException) {
            throw (IOException) cause;
        }
        throw new RuntimeException(cause);
    }

    private Optional<Response> makeRequest(URL url) throws IOException {
        HttpURLConnection con = getConnection(url);
        try {
            con.connect();
            if (con.getResponseCode() != 200) {
                return Optional.empty();
            }
            return read(con.getInputStream());
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
    // Shared across both endpoints
    @Value
    private static class Response {
        String id;
        String name;
    }

}
