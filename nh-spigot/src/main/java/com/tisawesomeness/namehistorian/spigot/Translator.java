package com.tisawesomeness.namehistorian.spigot;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationRegistry;
import net.kyori.adventure.util.UTF8ResourceBundleControl;

import java.util.Locale;
import java.util.ResourceBundle;

public class Translator {
    public static void load() {
        ResourceBundle bundle = ResourceBundle.getBundle("lang/namehistorian", Locale.ENGLISH, UTF8ResourceBundleControl.get());
        TranslationRegistry registry = TranslationRegistry.create(Key.key("namehistorian", "main"));
        registry.registerAll(Locale.ENGLISH, bundle, false);
        GlobalTranslator.translator().addSource(registry);
    }
}
