package com.tisawesomeness.namehistorian.spigot;

import com.tisawesomeness.namehistorian.util.Tuple;
import lombok.Cleanup;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationRegistry;
import net.kyori.adventure.translation.Translator;
import net.kyori.adventure.util.UTF8ResourceBundleControl;
import nu.studer.java.util.OrderedProperties;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class TranslationManager {

    private static final int LOCALE_METHOD_CHANGE_VERSION = 12;

    private final NameHistorianSpigot plugin;
    // Null until first loadTranslations()
    private @Nullable RegistryAdapter currentRegistry;

    /**
     * Initializes the translation manager by:
     * <ul>
     *     <li>Creating the translations directory</li>
     *     <li>Updating existing translation files by filling in missing keys</li>
     *     <li>Writing the default translation file if it doesn't exist</li>
     *     <li>Loading translations from file (and from jar if necessary) into the plugin</li>
     * </ul>
     * @throws IllegalStateException if the plugin is not initialized with a config
     */
    public void init() {
        Path translationsDirectory = tryCreateTranslationsDirectory();
        if (translationsDirectory != null) {
            try {
                updateTranslationFiles(translationsDirectory);
            } catch (IOException ex) {
                plugin.err("Could not update translation files", ex);
                // Non-fatal
            }
            try {
                writeDefaultTranslationFileIfNotExists(translationsDirectory);
            } catch (IOException ex) {
                plugin.err("Could not write default translations file", ex);
                // Non-fatal
            }
        }
        loadTranslations(translationsDirectory);
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

    private void updateTranslationFiles(Path translationsDirectory) throws IOException {
        @Cleanup Stream<Path> translationFiles = Files.list(translationsDirectory);
        translationFiles.forEach(this::updateTranslationFile);
    }
    private void updateTranslationFile(Path translationFile) {
        String fileName = translationFile.getFileName().toString();
        String resourceName = "lang/namehistorian_" + fileName;
        try (InputStream jarInputStream = plugin.getResource(resourceName)) {
            if (jarInputStream == null) {
                return;
            }
            tryFillMissingKeys(jarInputStream, translationFile);
        } catch (IOException ignore) {
            // only occurs on close(), very rare
        }
    }
    private void tryFillMissingKeys(InputStream jarInputStream, Path translationFile) {
        try {
            fillMissingKeys(jarInputStream, translationFile);
        } catch (IOException ex) {
            plugin.err("Could not update translation file %s", ex, translationFile.getFileName());
        }
    }
    private static void fillMissingKeys(InputStream jarInputStream, Path translationsFile) throws IOException {
        // Must always use readers or writers with UTF-8, or we'll get nasty encoding errors
        OrderedProperties jarProperties = dateSuppressingProperties();
        Reader jarInputReader = new InputStreamReader(jarInputStream, StandardCharsets.UTF_8);
        jarProperties.load(jarInputReader);

        OrderedProperties fileProperties = dateSuppressingProperties();
        @Cleanup Reader fileInputReader = Files.newBufferedReader(translationsFile, StandardCharsets.UTF_8);
        fileProperties.load(fileInputReader);

        // Takes default translations and replaces them with any modifications from the custom folder
        // Effectively the same as adding missing keys to the custom translations file
        for (Map.Entry<String, String> fileProperty : fileProperties.entrySet()) {
            jarProperties.setProperty(fileProperty.getKey(), fileProperty.getValue());
        }

        @Cleanup Writer fileOutputWriter = Files.newBufferedWriter(translationsFile, StandardCharsets.UTF_8);
        jarProperties.store(fileOutputWriter, null);
    }
    private static OrderedProperties dateSuppressingProperties() {
        return new OrderedProperties.OrderedPropertiesBuilder()
                .withSuppressDateInComment(true)
                .build();
    }

    private void writeDefaultTranslationFileIfNotExists(Path translationsDirectory) throws IOException {
        Path translationsFile = translationsDirectory.resolve("en.properties");
        if (!Files.exists(translationsFile)) {
            @Cleanup InputStream jarInputStream = plugin.getResource("lang/namehistorian_en.properties");
            assert jarInputStream != null;
            Files.copy(jarInputStream, translationsFile);
        }
    }

    private void loadTranslations(@Nullable Path translationsDirectory) {
        NameHistorianConfig config = plugin.getNHConfig();
        RegistryAdapter newRegistry = new RegistryAdapter();
        newRegistry.setDefaultLocale(config.getDefaultLocale());

        boolean anyLanguageRegistered = tryRegisterFromDirectory(translationsDirectory, newRegistry);
        // If per-user-translations enabled, there may be languages in jar that are not in file
        if (config.isPerUserTranslations()) {
            registerAllFromJar(newRegistry);
        // If per-user-translations disabled, only need to fallback to jar if the default wasn't registered earlier
        } else if (!anyLanguageRegistered) {
            registerFromJar(newRegistry, BaseLocale.DEFAULT);
        }

        if (currentRegistry != null) {
            GlobalTranslator.translator().removeSource(currentRegistry.getRegistry());
        }
        currentRegistry = newRegistry;
        GlobalTranslator.translator().addSource(newRegistry.getRegistry());
    }

    private boolean tryRegisterFromDirectory(@Nullable Path translationsDirectory, RegistryAdapter registry) {
        if (translationsDirectory == null) {
            return false;
        }
        try {
            return registerFromDirectory(translationsDirectory, registry);
        } catch (IOException ex) {
            plugin.err("Failed to read translations from plugin directory", ex);
            return false;
        }
    }
    private boolean registerFromDirectory(Path translationsDirectory, RegistryAdapter registry) throws IOException {
        NameHistorianConfig config = plugin.getNHConfig();
        if (config.isPerUserTranslations()) {
            return registerAllFromDirectory(registry, translationsDirectory);
        }

        String defaultTranslationFileName = config.getDefaultLocale() + ".properties";
        Path defaultTranslationFile = translationsDirectory.resolve(defaultTranslationFileName);
        if (registerFile(registry, defaultTranslationFile)) {
            return true;
        }
        if (BaseLocale.isBaseLocale(config.getDefaultLocale())) {
            plugin.log("Default translation file %s could not be loaded, loading from jar instead",
                    defaultTranslationFileName, BaseLocale.DEFAULT);
            return false;
        }
        plugin.warn("Default translation file %s could not be loaded, using %s locale instead",
                defaultTranslationFileName, BaseLocale.DEFAULT);

        String pluginDefaultTranslationFileName = BaseLocale.DEFAULT + ".properties";
        Path pluginDefaultTranslationFile = translationsDirectory.resolve(pluginDefaultTranslationFileName);
        if (registerFile(registry, pluginDefaultTranslationFile)) {
            return true;
        }
        plugin.warn("%s.properties translation file could not be loaded, loading from jar instead", BaseLocale.DEFAULT);
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
            plugin.err("Failed to read translation file %s", ex, translationFile.getFileName());
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

    private static void registerAllFromJar(RegistryAdapter registry) {
        Arrays.stream(BaseLocale.values()).forEach(bl -> registerFromJar(registry, bl));
    }
    private static void registerFromJar(RegistryAdapter registry, BaseLocale baseLocale) {
        Locale locale = baseLocale.getLocale();
        ResourceBundle bundle = ResourceBundle.getBundle("lang/namehistorian", locale, UTF8ResourceBundleControl.get());
        registry.tryRegister(locale, bundle);
    }

    /**
     * Translates the input component using the target's locale.
     * First the full locale is used, then the locale with language only, then the default locale.
     * If the target is a player, the player's locale is used, otherwise the locale is the default locale.
     * Note that if per-player-translations is disabled, only one locale will be registered so all translations
     * will be in that language.
     * @param target the target the message will be sent to
     * @param msg the input component
     * @return the translated component
     */
    public Component render(CommandSender target, Component msg) {
        return GlobalTranslator.render(msg, getLocale(target));
    }
    private Locale getLocale(CommandSender sendTo) {
        if (sendTo instanceof Player) {
            String localeStr = getLocale((Player) sendTo);
            Locale locale = Translator.parseLocale(localeStr);
            if (locale != null) {
                return locale;
            }
        }
        return plugin.getNHConfig().getDefaultLocale();
    }
    private static String getLocale(Player player) {
        if (NameHistorianSpigot.MAJOR_SPIGOT_VERSION < LOCALE_METHOD_CHANGE_VERSION) {
            @SuppressWarnings("deprecation") // not deprecated in old versions
            String locale = player.spigot().getLocale();
            return locale;
        } else {
            return player.getLocale();
        }
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
            boolean languageOnlyRegistered = registerLanguageOnlyLocaleIfExists(locale, bundle);
            return registered || languageOnlyRegistered;
        }
        private boolean registerLanguageOnlyLocaleIfExists(Locale locale, ResourceBundle bundle) {
            Locale languageOnlyLocale = new Locale(locale.getLanguage());
            if (languageOnlyLocale.equals(locale) || languageOnlyLocale.equals(BaseLocale.DEFAULT.getLocale())) {
                return false;
            }
            return tryRegister(locale, bundle);
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

    }

}
