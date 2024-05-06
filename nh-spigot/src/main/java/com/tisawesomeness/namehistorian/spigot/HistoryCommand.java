package com.tisawesomeness.namehistorian.spigot;

import com.tisawesomeness.namehistorian.NameRecord;
import lombok.AllArgsConstructor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@AllArgsConstructor
public final class HistoryCommand implements CommandExecutor, TabCompleter {

    private final NameHistorianSpigot plugin;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            plugin.sendMessage(sender, Messages.HISTORY_USAGE, label);
            return true;
        }
        Optional<OfflinePlayer> playerOpt = plugin.getPlayer(args[0]);
        if (playerOpt.isPresent()) {
            OfflinePlayer player = playerOpt.get();
            fetchNameHistory(sender, player.getUniqueId(), player.isOnline());
        }
        runWithUsername(sender, args[0]);
        return true;
    }

    private void runWithUsername(CommandSender sender, String username) {
        Optional<MojangAPI> apiOpt = plugin.getMojangAPI();
        if (!apiOpt.isPresent() || username.length() > 16) {
            plugin.sendMessage(sender, Messages.UNKNOWN_PLAYER);
            return;
        }
        plugin.log("Fetching UUID for %s from Mojang API", username);
        plugin.sendMessage(sender, Messages.UUID_LOOKUP);
        plugin.scheduleAsync(() -> lookupUUIDFromMojangAsync(sender, username, apiOpt.get()));
    }
    private void lookupUUIDFromMojangAsync(CommandSender sender, String username, MojangAPI api) {
        try {
            Optional<UUID> uuidOpt = api.getUUID(username);
            plugin.scheduleNextTick(() -> {
                if (!uuidOpt.isPresent()) {
                    plugin.sendMessage(sender, Messages.UNKNOWN_PLAYER);
                    return;
                }
                fetchNameHistory(sender, uuidOpt.get(), false);
            });
        } catch (IOException ex) {
            plugin.err("Error fetching name history for %s", ex, username);
            plugin.scheduleNextTick(() -> plugin.sendMessage(sender, Messages.MOJANG_ERROR));
        }
    }

    private void fetchNameHistory(CommandSender sender, UUID uuid, boolean isOnline) {
        try {
            List<NameRecord> history = plugin.getHistorian().getNameHistory(uuid);
            printNameHistory(sender, history, isOnline);
        } catch (SQLException ex) {
            plugin.err("Error fetching name history for %s", ex, uuid);
            plugin.sendMessage(sender, Messages.FETCH_ERROR);
        }
    }

    private void printNameHistory(CommandSender sender, List<NameRecord> nameHistory, boolean isOnline) {
        if (nameHistory.isEmpty()) {
            plugin.sendMessage(sender, Messages.NO_HISTORY);
            return;
        }

        UUID uuid = nameHistory.get(0).getUuid();
        plugin.sendMessage(sender, Messages.HISTORY_TITLE, uuid);

        for (int i = 0; i < nameHistory.size(); i++) {
            NameRecord nr = nameHistory.get(i);

            // History is newest to oldest, changes printed in reverse
            int changeNumber = nameHistory.size() - i;
            plugin.sendMessage(sender, Messages.USERNAME_LINE, changeNumber, nr.getUsername());

            // If the player is online, the player was last seen now
            Instant lastSeen = i == 0 && isOnline ? Instant.now() : nr.getLastSeenTime();
            plugin.sendMessage(sender, Messages.DATE_LINE, nr.getFirstSeenTime(), lastSeen);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            List<String> completions = plugin.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.toList());
            return StringUtil.copyPartialMatches(args[0], completions, new ArrayList<>());
        }
        return null;
    }

}
