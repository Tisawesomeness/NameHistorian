package com.tisawesomeness.namehistorian.spigot;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class Messages {

    private static final int COPY_EVENT_VERSION = 15;

    /** commandLabel */
    public static final A1<String> HISTORY_USAGE = label -> Component.translatable("namehistorian.history_usage")
            .color(NamedTextColor.RED)
            .arguments(Component.text(label));
    /** input ({@code <player>} argument) */
    public static final A1<String> INVALID_PLAYER = input -> Component.translatable("namehistorian.invalid_player")
            .color(NamedTextColor.RED)
            .arguments(Component.text(input));
    public static final Component UNKNOWN_PLAYER = Component.translatable("namehistorian.unknown_player")
            .color(NamedTextColor.RED);
    public static final Component FETCH_ERROR = Component.translatable("namehistorian.fetch_error")
            .color(NamedTextColor.RED);
    public static final Component MOJANG_LOOKUP = Component.translatable("namehistorian.mojang_lookup")
            .color(NamedTextColor.GRAY);
    public static final Component MOJANG_ERROR = Component.translatable("namehistorian.mojang_error")
            .color(NamedTextColor.RED);
    public static final Component NO_HISTORY = Component.translatable("namehistorian.no_history")
            .color(NamedTextColor.RED);
    /** uuid */
    public static final A1<UUID> HISTORY_TITLE = uuid -> Component.translatable("namehistorian.history_title")
            .color(NamedTextColor.GOLD)
            .arguments(copyableText(uuid.toString()).color(NamedTextColor.GREEN));
    public static final Component NEVER_JOINED = Component.translatable("namehistorian.never_joined")
            .color(NamedTextColor.GRAY);
    /** changeNumber, username */
    public static final A2<Integer, String> USERNAME_LINE = (changeNumber, username) -> Component.translatable("namehistorian.username_line")
            .color(NamedTextColor.BLUE)
            .arguments(
                    Component.text(changeNumber),
                    copyableText(username).color(NamedTextColor.LIGHT_PURPLE)
            );
    /** firstSeen, lastSeen */
    public static final A2<Instant, Instant> DATE_LINE = (firstSeen, lastSeen) -> Component.translatable("namehistorian.date_line")
            .color(NamedTextColor.BLUE)
            .arguments(
                    Component.text(format(firstSeen)).color(NamedTextColor.GREEN),
                    Component.text(format(lastSeen)).color(NamedTextColor.GREEN)
            );
    /** commandLabel */
    public static final A1<String> NAMEHISTORIAN_USAGE = label -> Component.translatable("namehistorian.namehistorian_usage")
            .color(NamedTextColor.RED)
            .arguments(Component.text(label));
    public static final Component RELOAD_FAILED = Component.translatable("namehistorian.reload_failed")
            .color(NamedTextColor.RED);
    public static final Component RELOAD_SUCCESS = Component.translatable("namehistorian.reload_success");

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static Component copyableText(String str) {
        // Versions below 1.15 don't support copy event, gracefully degrade functionality
        if (NameHistorianSpigot.MAJOR_SPIGOT_VERSION < COPY_EVENT_VERSION) {
            return Component.text(str);
        }
        return Component.text(str)
                .clickEvent(ClickEvent.copyToClipboard(str))
                .hoverEvent(Component.translatable("namehistorian.click_to_copy").asHoverEvent());
    }
    private static String format(Instant time) {
        return FORMATTER.format(time.atZone(ZoneOffset.systemDefault()));
    }

    public interface A1<A0> {
        Component build(A0 a0);
    }
    public interface A2<A0, A1> {
        Component build(A0 a0, A1 a1);
    }

}
