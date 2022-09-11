package com.tisawesomeness.namehistorian;

import lombok.Value;

import java.util.UUID;

@Value
public class NamedPlayer {
    UUID uuid;
    String username;
}
