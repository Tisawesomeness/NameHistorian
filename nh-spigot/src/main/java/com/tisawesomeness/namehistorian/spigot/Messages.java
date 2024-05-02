package com.tisawesomeness.namehistorian.spigot;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class Messages {

    /** commandLabel */
    public static final A1<String> USAGE = label -> Component.translatable("namehistorian.usage")
            .color(NamedTextColor.RED)
            .arguments(Component.text(label));
    public static final Component UNKNOWN_PLAYER = Component.translatable("namehistorian.unknown_player")
            .color(NamedTextColor.RED);
    public static final Component FETCH_ERROR = Component.translatable("namehistorian.fetch_error")
            .color(NamedTextColor.RED);
    public static final Component NO_HISTORY = Component.translatable("namehistorian.no_history")
            .color(NamedTextColor.RED);
    /** uuid */
    public static final A1<UUID> HISTORY_TITLE = uuid -> Component.translatable("namehistorian.history_title")
            .color(NamedTextColor.GOLD)
            .arguments(Component.text(uuid.toString()).color(NamedTextColor.GREEN));
    /** changeNumber, username */
    public static final A2<Integer, String> USERNAME_LINE = (changeNumber, username) -> Component.translatable("namehistorian.username_line")
            .color(NamedTextColor.BLUE)
            .arguments(
                    Component.text(changeNumber),
                    Component.text(username).color(NamedTextColor.LIGHT_PURPLE)
            );
    /** firstSeen, lastSeen */
    public static final A2<Instant, Instant> DATE_LINE = (firstSeen, lastSeen) -> Component.translatable("namehistorian.date_line")
            .color(NamedTextColor.BLUE)
            .arguments(
                    Component.text(format(firstSeen)).color(NamedTextColor.GREEN),
                    Component.text(format(lastSeen)).color(NamedTextColor.GREEN)
            );

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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
