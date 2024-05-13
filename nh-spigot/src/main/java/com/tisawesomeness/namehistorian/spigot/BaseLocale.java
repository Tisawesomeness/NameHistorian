package com.tisawesomeness.namehistorian.spigot;

import lombok.Getter;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum BaseLocale {
    EN("en"),
    ZH("zh");

    public static final BaseLocale DEFAULT = EN;

    @Getter private final Locale locale;

    BaseLocale(String languageCode) {
        locale = new Locale(languageCode);
    }

    public static Optional<BaseLocale> of(Locale locale) {
        return Arrays.stream(values())
                .filter(bl -> bl.getLocale().equals(locale))
                .findFirst();
    }
    public static boolean isBaseLocale(Locale locale) {
        return of(locale).isPresent();
    }

    @Override
    public String toString() {
        return locale.toString();
    }
}
