package com.tisawesomeness.namehistorian.spigot;

import com.tisawesomeness.namehistorian.NameRecord;
import com.tisawesomeness.namehistorian.util.Util;
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
        Optional<UUID> uuidOpt = Util.parseUUID(args[0]);
        if (uuidOpt.isPresent()) {
            runWithUuid(sender, uuidOpt.get());
            return true;
        }
        Optional<APICompatibleUsername> usernameOpt = APICompatibleUsername.of(args[0]);
        if (usernameOpt.isPresent()) {
            runWithUsername(sender, usernameOpt.get());
            return true;
        }
        plugin.sendMessage(sender, Messages.INVALID_PLAYER, args[0]);
        return true;
    }

    private void runWithUuid(CommandSender sender, UUID uuid) {
        Optional<OfflinePlayer> playerOpt = plugin.getPlayer(uuid);
        if (!playerOpt.isPresent()) {
            plugin.sendMessage(sender, Messages.UNKNOWN_PLAYER);
            return;
        }
        OfflinePlayer player = playerOpt.get();
        Optional<MojangAPI> apiOpt = plugin.getMojangAPI();
        if (player.isOnline() || !apiOpt.isPresent()) {
            fetchNameHistory(sender, uuid, player.isOnline());
            return;
        }
        plugin.log("Fetching username for %s from Mojang API", uuid);
        plugin.sendMessage(sender, Messages.MOJANG_LOOKUP);
        plugin.scheduleAsync(() -> lookupUsernameFromMojangAsync(sender, uuid, apiOpt.get()));
    }
    private void lookupUsernameFromMojangAsync(CommandSender sender, UUID uuid, MojangAPI api) {
        try {
            Optional<String> usernameOpt = api.getUsername(uuid);
            plugin.scheduleNextTick(() -> {
                if (!usernameOpt.isPresent()) {
                    plugin.sendMessage(sender, Messages.UNKNOWN_PLAYER);
                    return;
                }
                String username = usernameOpt.get();
                tryRecordName(uuid, username);
                fetchNameHistory(sender, uuid, false);
            });
        } catch (IOException ex) {
            plugin.err("Error fetching name history for %s", ex, uuid);
            plugin.scheduleNextTick(() -> plugin.sendMessage(sender, Messages.MOJANG_ERROR));
        }
    }

    private void runWithUsername(CommandSender sender, APICompatibleUsername username) {
        Optional<Player> playerOpt = plugin.getPlayer(username.toString());
        if (playerOpt.isPresent()) {
            Player player = playerOpt.get();
            fetchNameHistory(sender, player.getUniqueId(), true);
            return;
        }
        Optional<MojangAPI> apiOpt = plugin.getMojangAPI();
        if (!apiOpt.isPresent()) {
            plugin.sendMessage(sender, Messages.UNKNOWN_PLAYER);
            return;
        }
        plugin.log("Fetching UUID for %s from Mojang API", username);
        plugin.sendMessage(sender, Messages.MOJANG_LOOKUP);
        plugin.scheduleAsync(() -> lookupUUIDFromMojangAsync(sender, username, apiOpt.get()));
    }
    private void lookupUUIDFromMojangAsync(CommandSender sender, APICompatibleUsername username, MojangAPI api) {
        try {
            Optional<UUID> uuidOpt = api.getUUID(username);
            plugin.scheduleNextTick(() -> {
                if (!uuidOpt.isPresent()) {
                    plugin.sendMessage(sender, Messages.UNKNOWN_PLAYER);
                    return;
                }
                UUID uuid = uuidOpt.get();
                tryRecordName(uuid, username.toString());
                fetchNameHistory(sender, uuid, false);
            });
        } catch (IOException ex) {
            plugin.err("Error fetching name history for %s", ex, username);
            plugin.scheduleNextTick(() -> plugin.sendMessage(sender, Messages.MOJANG_ERROR));
        }
    }

    private void tryRecordName(UUID uuid, String username) {
        try {
            plugin.getHistorian().recordName(uuid, username);
        } catch (SQLException ex) {
            plugin.err("Error recording name for %s - %s", ex, username, uuid);
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
