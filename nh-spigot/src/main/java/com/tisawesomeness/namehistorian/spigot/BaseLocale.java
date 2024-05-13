package com.tisawesomeness.namehistorian.spigot;

import lombok.Getter;

import java.util.Locale;

public enum BaseLocale {
    EN("en"),
    ZH("zh");

    public static final BaseLocale DEFAULT = EN;

    @Getter private final Locale locale;

    BaseLocale(String languageCode) {
        locale = new Locale(languageCode);
    }

    @Override
    public String toString() {
        return locale.toString();
    }
}
