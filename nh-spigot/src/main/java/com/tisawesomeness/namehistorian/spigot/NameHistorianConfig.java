package com.tisawesomeness.namehistorian.spigot;

import lombok.Getter;
import net.kyori.adventure.translation.Translator;
import org.bukkit.configuration.file.FileConfiguration;

import javax.annotation.Nonnegative;
import java.util.Locale;
import java.util.Optional;

@Getter
public class NameHistorianConfig {

    private final boolean perUserTranslations;
    private final Locale defaultLocale;
    private final boolean enableMojangLookups;
    @Nonnegative
    private final int mojangTimeout;

    public NameHistorianConfig(NameHistorianSpigot plugin) {
        FileConfiguration conf = plugin.getConfig();
        perUserTranslations = conf.getBoolean("per-user-translations", false);
        defaultLocale = parseLocale(plugin).orElse(BaseLocale.DEFAULT.getLocale());
        enableMojangLookups = conf.getBoolean("enable-mojang-lookups", true);
        mojangTimeout = Math.max(0, conf.getInt("mojang-timeout", 5000));
    }

    private static Optional<Locale> parseLocale(NameHistorianSpigot plugin) {
        String localeStr = plugin.getConfig().getString("default-locale", null);
        if (localeStr == null) {
            return Optional.empty();
        }
        Locale locale = Translator.parseLocale(localeStr);
        if (locale == null) {
            plugin.err("Unknown default-locale %s, using %s instead", localeStr, BaseLocale.DEFAULT);
            return Optional.empty();
        }
        return Optional.of(locale);
    }

}
