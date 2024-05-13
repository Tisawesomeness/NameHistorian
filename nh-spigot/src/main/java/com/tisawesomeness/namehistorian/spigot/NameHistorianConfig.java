package com.tisawesomeness.namehistorian.spigot;

import com.tisawesomeness.namehistorian.util.Util;
import lombok.Getter;
import net.kyori.adventure.translation.Translator;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.Range;

import javax.annotation.Nonnegative;
import javax.annotation.Nullable;
import java.util.Locale;

@Getter
public class NameHistorianConfig {

    private final boolean perUserTranslations;
    private final Locale defaultLocale;
    private final boolean enableMojangLookups;
    @Nonnegative
    private final int mojangTimeout;
    @Range(from = 60, to = Integer.MAX_VALUE)
    private final int mojangLifetime;

    public NameHistorianConfig(NameHistorianSpigot plugin) {
        FileConfiguration conf = plugin.getConfig();
        perUserTranslations = conf.getBoolean("per-user-translations", false);
        defaultLocale = Util.nullOr(parseLocale(plugin), BaseLocale.DEFAULT.getLocale());
        enableMojangLookups = conf.getBoolean("enable-mojang-lookups", true);
        mojangTimeout = Math.max(0, conf.getInt("mojang-timeout", 5000));
        mojangLifetime = Math.max(60, conf.getInt("mojang-lifetime", 60));
    }

    private static @Nullable Locale parseLocale(NameHistorianSpigot plugin) {
        String localeStr = plugin.getConfig().getString("default-locale", null);
        if (localeStr == null) {
            return null;
        }
        Locale locale = Translator.parseLocale(localeStr);
        if (locale == null) {
            plugin.err("Unknown default-locale %s, using %s instead", localeStr, BaseLocale.DEFAULT);
            return null;
        }
        return locale;
    }

}
