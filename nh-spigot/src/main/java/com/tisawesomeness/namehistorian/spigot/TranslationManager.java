package com.tisawesomeness.namehistorian.spigot;

import com.tisawesomeness.namehistorian.Tuple;
import lombok.Cleanup;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationRegistry;
import net.kyori.adventure.translation.Translator;
import net.kyori.adventure.util.UTF8ResourceBundleControl;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.stream.Stream;

public class TranslationManager {

    private static final Locale DEFAULT = Locale.ENGLISH;

    public static void load(NameHistorianSpigot plugin) {
        TranslationRegistry registry = TranslationRegistry.create(Key.key("namehistorian", "main"));
        registry.defaultLocale(DEFAULT);

        Path translationsDirectory = plugin.getDataFolder().toPath().resolve("translations");
        try {
            Files.createDirectories(translationsDirectory);
            registerFromDirectory(plugin, registry, translationsDirectory);
        } catch (IOException ex) {
            ex.printStackTrace();
            // Non-fatal
        }

        registerFromJar(registry);

        GlobalTranslator.translator().addSource(registry);
    }

    private static void registerFromDirectory(NameHistorianSpigot plugin, TranslationRegistry registry, Path translationsDirectory) throws IOException {
        @Cleanup Stream<Path> stream = Files.list(translationsDirectory);
        stream.map(p -> tryReadBundle(plugin, p))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(t -> t.run((locale, bundle) -> {
                    tryRegisterBundle(registry, locale, bundle);
                    languageOnlyLocale(locale).ifPresent(l -> tryRegisterBundle(registry, l, bundle));
                }));
    }
    private static Optional<Locale> languageOnlyLocale(Locale locale) {
        Locale languageOnlyLocale = new Locale(locale.getLanguage());
        if (languageOnlyLocale.equals(locale) || languageOnlyLocale.equals(DEFAULT)) {
            return Optional.empty();
        }
        return Optional.of(languageOnlyLocale);
    }

    private static Optional<Tuple<Locale, ResourceBundle>> tryReadBundle(NameHistorianSpigot plugin, Path translationFile) {
        try {
            return readBundle(plugin, translationFile);
        } catch (IOException ex) {
            plugin.getLogger().warning("Failed to register " + translationFile);
            ex.printStackTrace();
            return Optional.empty();
        }
    }
    private static Optional<Tuple<Locale, ResourceBundle>> readBundle(NameHistorianSpigot plugin, Path translationFile) throws IOException {
        String fileName = translationFile.getFileName().toString();
        if (!fileName.endsWith(".properties")) {
            return Optional.empty();
        }

        String localeStr = fileName.substring(0, fileName.length() - ".properties".length());
        Locale locale = Translator.parseLocale(localeStr);
        if (locale == null) {
            plugin.getLogger().warning("Unknown locale " + localeStr);
            return Optional.empty();
        }

        @Cleanup BufferedReader reader = Files.newBufferedReader(translationFile, StandardCharsets.UTF_8);
        return Optional.of(Tuple.of(locale, new PropertyResourceBundle(reader)));
    }

    private static void registerFromJar(TranslationRegistry registry) {
        ResourceBundle bundle = ResourceBundle.getBundle("lang/namehistorian", DEFAULT, UTF8ResourceBundleControl.get());
        tryRegisterBundle(registry, DEFAULT, bundle);
    }

    private static void tryRegisterBundle(TranslationRegistry registry, Locale locale, ResourceBundle bundle) {
        try {
            registry.registerAll(locale, bundle, false);
        } catch (IllegalArgumentException ignore) { }
    }

}
