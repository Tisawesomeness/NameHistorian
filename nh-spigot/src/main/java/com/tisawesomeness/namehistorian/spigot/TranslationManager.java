package com.tisawesomeness.namehistorian.spigot;

import com.tisawesomeness.namehistorian.Tuple;
import lombok.Cleanup;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationRegistry;
import net.kyori.adventure.translation.Translator;
import net.kyori.adventure.util.UTF8ResourceBundleControl;

import javax.annotation.Nullable;
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

@RequiredArgsConstructor
public class TranslationManager {

    private static final Locale DEFAULT = Locale.ENGLISH;

    private final NameHistorianSpigot plugin;
    private @Nullable TranslationRegistry currentRegistry;

    public void load() {
        TranslationRegistry newRegistry = TranslationRegistry.create(Key.key("namehistorian", "main"));
        newRegistry.defaultLocale(DEFAULT);

        Path translationsDirectory = plugin.getDataFolder().toPath().resolve("translations");
        try {
            Files.createDirectories(translationsDirectory);
            registerFromDirectory(newRegistry, translationsDirectory);
        } catch (IOException ex) {
            ex.printStackTrace();
            // Non-fatal
        }

        registerFromJar(newRegistry);

        if (currentRegistry != null) {
            GlobalTranslator.translator().removeSource(currentRegistry);
        }
        currentRegistry = newRegistry;
        GlobalTranslator.translator().addSource(newRegistry);
    }

    private void registerFromDirectory(TranslationRegistry registry, Path translationsDirectory) throws IOException {
        @Cleanup Stream<Path> stream = Files.list(translationsDirectory);
        stream.map(this::tryReadBundle)
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

    private Optional<Tuple<Locale, ResourceBundle>> tryReadBundle(Path translationFile) {
        try {
            return readBundle(translationFile);
        } catch (IOException ex) {
            plugin.getLogger().warning("Failed to register " + translationFile);
            ex.printStackTrace();
            return Optional.empty();
        }
    }
    private Optional<Tuple<Locale, ResourceBundle>> readBundle(Path translationFile) throws IOException {
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
