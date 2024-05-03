package com.tisawesomeness.namehistorian.spigot;

import lombok.Getter;
import net.kyori.adventure.translation.Translator;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Locale;
import java.util.Optional;

@Getter
public class NameHistorianConfig {

    private final boolean perUserTranslations;
    private final Locale defaultLocale;

    public NameHistorianConfig(NameHistorianSpigot plugin) {
        FileConfiguration conf = plugin.getConfig();
        this.perUserTranslations = conf.getBoolean("per-user-translations", false);
        this.defaultLocale = parseLocale(plugin).orElse(TranslationManager.PLUGIN_DEFAULT);
    }

    private static Optional<Locale> parseLocale(NameHistorianSpigot plugin) {
        String localeStr = plugin.getConfig().getString("default-locale", null);
        if (localeStr == null) {
            return Optional.empty();
        }
        Locale locale = Translator.parseLocale(localeStr);
        if (locale == null) {
            plugin.getLogger().warning("Unknown locale " + localeStr);
            return Optional.empty();
        }
        return Optional.of(locale);
    }

}
