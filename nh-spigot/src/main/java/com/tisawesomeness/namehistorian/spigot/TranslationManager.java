package com.tisawesomeness.namehistorian.spigot;

import com.tisawesomeness.namehistorian.Tuple;
import lombok.Cleanup;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationRegistry;
import net.kyori.adventure.translation.Translator;
import net.kyori.adventure.util.UTF8ResourceBundleControl;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class TranslationManager {

    public static final Locale PLUGIN_DEFAULT = Locale.ENGLISH;

    private final NameHistorianSpigot plugin;
    // Null until first loadTranslations()
    private @Nullable RegistryAdapter currentRegistry;

    public void init(NameHistorianConfig config) {
        Path translationsDirectory = tryCreateTranslationsDirectory();
        if (translationsDirectory != null) {
            try {
                writeTranslationsFromJarIfMissing(translationsDirectory);
            } catch (IOException ex) {
                plugin.err("Could not write default translations file", ex);
                // Non-fatal
            }
        }
        loadTranslations(config, translationsDirectory);
    }

    private @Nullable Path tryCreateTranslationsDirectory() {
        Path translationsDirectory = plugin.getDataFolder().toPath().resolve("translations");
        try {
            Files.createDirectories(translationsDirectory);
            return translationsDirectory;
        } catch (IOException ex) {
            plugin.err("Could not create translations directory", ex);
            return null;
        }
    }

    private void writeTranslationsFromJarIfMissing(Path translationsDirectory) throws IOException {
        Path translationsFile = translationsDirectory.resolve("en.properties");
        if (!Files.exists(translationsFile)) {
            @Cleanup InputStream is = plugin.getResource("lang/namehistorian_en.properties");
            if (is == null) {
                throw new AssertionError("Some classloader trickery is going on");
            }
            Files.copy(is, translationsFile);
        }
    }

    private void loadTranslations(NameHistorianConfig config, @Nullable Path translationsDirectory) {
        RegistryAdapter newRegistry = new RegistryAdapter();
        newRegistry.setDefaultLocale(config.getDefaultLocale());

        boolean anyLanguageRegistered = tryRegisterFromDirectory(config, translationsDirectory, newRegistry);
        if (config.isPerUserTranslations() || !anyLanguageRegistered) {
            registerFromJar(newRegistry);
        }

        if (currentRegistry != null) {
            GlobalTranslator.translator().removeSource(currentRegistry.getRegistry());
        }
        currentRegistry = newRegistry;
        GlobalTranslator.translator().addSource(newRegistry.getRegistry());
    }

    private boolean tryRegisterFromDirectory(NameHistorianConfig config, @Nullable Path translationsDirectory,
                RegistryAdapter registry) {
        if (translationsDirectory == null) {
            return false;
        }
        try {
            return registerFromDirectory(config, translationsDirectory, registry);
        } catch (IOException ex) {
            plugin.err("Failed to read translations from plugin directory", ex);
            return false;
        }
    }
    private boolean registerFromDirectory(NameHistorianConfig config, Path translationsDirectory, RegistryAdapter registry) throws IOException {
        if (config.isPerUserTranslations()) {
            return registerAllFromDirectory(registry, translationsDirectory);
        }

        String defaultTranslationFileName = config.getDefaultLocale() + ".properties";
        Path defaultTranslationFile = translationsDirectory.resolve(defaultTranslationFileName);
        if (registerFile(registry, defaultTranslationFile)) {
            return true;
        }
        plugin.warn("Default translation file could not be loaded, using %s locale instead", PLUGIN_DEFAULT);

        String pluginDefaultTranslationFileName = PLUGIN_DEFAULT.getLanguage() + ".properties";
        Path pluginDefaultTranslationFile = translationsDirectory.resolve(pluginDefaultTranslationFileName);
        if (registerFile(registry, pluginDefaultTranslationFile)) {
            return true;
        }
        plugin.warn("%s.properties translation file could not be loaded, loading from jar instead", PLUGIN_DEFAULT);
        return false;
    }

    private boolean registerFile(RegistryAdapter registry, Path translationFile) {
        if (!Files.exists(translationFile)) {
            plugin.warn("Translation file %s does not exist", translationFile.getFileName());
            return false;
        }
        return tryReadBundle(translationFile)
                .map(t -> t.fold(registry::register))
                .orElse(false);
    }

    private boolean registerAllFromDirectory(RegistryAdapter registry, Path translationsDirectory) throws IOException {
        @Cleanup Stream<Path> stream = Files.list(translationsDirectory);
        return stream.map(this::tryReadBundle)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(t -> t.fold(registry::register))
                .reduce((a, b) -> a || b) // Reducing with OR instead of using anyMatch() so stream doesn't short-circuit
                .orElse(false);
    }

    private Optional<Tuple<Locale, ResourceBundle>> tryReadBundle(Path translationFile) {
        try {
            return readBundle(translationFile);
        } catch (IOException ex) {
            plugin.err("Failed to register translation file %s", ex, translationFile);
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
            plugin.err("Translation file %s is not a valid locale", translationFile.getFileName());
            return Optional.empty();
        }

        @Cleanup BufferedReader reader = Files.newBufferedReader(translationFile, StandardCharsets.UTF_8);
        return Optional.of(Tuple.of(locale, new PropertyResourceBundle(reader)));
    }

    private static void registerFromJar(RegistryAdapter registry) {
        ResourceBundle bundle = ResourceBundle.getBundle("lang/namehistorian", PLUGIN_DEFAULT, UTF8ResourceBundleControl.get());
        registry.register(PLUGIN_DEFAULT, bundle);
    }

    private static class RegistryAdapter {

        @Getter private final TranslationRegistry registry;
        private final Set<Locale> registeredLocales = new HashSet<>();

        public RegistryAdapter() {
            registry = TranslationRegistry.create(Key.key("namehistorian", "main"));
        }

        public void setDefaultLocale(Locale locale) {
            registry.defaultLocale(locale);
        }

        public boolean register(Locale locale, ResourceBundle bundle) {
            boolean registered = tryRegister(locale, bundle);
            boolean languageOnlyRegistered = languageOnlyLocale(locale)
                    .map(l -> tryRegister(l, bundle))
                    .orElse(false);
            return registered || languageOnlyRegistered;
        }
        private boolean tryRegister(Locale locale, ResourceBundle bundle) {
            if (registeredLocales.contains(locale)) {
                return false;
            }
            try {
                registry.registerAll(locale, bundle, false);
            } catch (IllegalArgumentException ignore) {
                return false;
            }
            registeredLocales.add(locale);
            return true;
        }

        private static Optional<Locale> languageOnlyLocale(Locale locale) {
            Locale languageOnlyLocale = new Locale(locale.getLanguage());
            if (languageOnlyLocale.equals(locale) || languageOnlyLocale.equals(PLUGIN_DEFAULT)) {
                return Optional.empty();
            }
            return Optional.of(languageOnlyLocale);
        }

    }

}
