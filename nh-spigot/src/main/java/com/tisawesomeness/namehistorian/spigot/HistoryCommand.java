package com.tisawesomeness.namehistorian.spigot;

import com.tisawesomeness.namehistorian.NameRecord;
import com.tisawesomeness.namehistorian.util.Util;
import lombok.AllArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import javax.annotation.Nullable;
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
        JoinStatus joinStatus = plugin.getPlayer(uuid)
                .map(p -> p.isOnline() ? JoinStatus.ONLINE : JoinStatus.OFFLINE)
                .orElse(JoinStatus.NEVER_JOINED);
        Optional<MojangAPI> apiOpt = plugin.getMojangAPI();
        if (joinStatus == JoinStatus.ONLINE || !apiOpt.isPresent()) {
            fetchNameHistory(sender, uuid, joinStatus);
            return;
        }
        plugin.log("Fetching username for %s from Mojang API", uuid);
        plugin.sendMessage(sender, Messages.MOJANG_LOOKUP);
        plugin.scheduleAsync(() -> lookupUsernameFromMojangAsync(sender, uuid, joinStatus, apiOpt.get()));
    }
    private void lookupUsernameFromMojangAsync(CommandSender sender, UUID uuid, JoinStatus joinStatus, MojangAPI api) {
        try {
            String username = api.getUsername(uuid).orElse(null);
            plugin.scheduleNextTick(() -> processUsernameSync(sender, uuid, username, joinStatus));
        } catch (IOException ex) {
            plugin.err("Error fetching name history for %s", ex, uuid);
            plugin.scheduleNextTick(() -> plugin.sendMessage(sender, Messages.MOJANG_ERROR));
        }
    }
    private void processUsernameSync(CommandSender sender, UUID uuid, @Nullable String username, JoinStatus joinStatus) {
        if (username == null) {
            plugin.sendMessage(sender, Messages.UNKNOWN_PLAYER);
            return;
        }
        tryRecordName(uuid, username);
        fetchNameHistory(sender, uuid, joinStatus);
    }

    private void runWithUsername(CommandSender sender, APICompatibleUsername username) {
        Optional<Player> playerOpt = plugin.getOnlinePlayer(username.toString());
        if (playerOpt.isPresent()) {
            Player player = playerOpt.get();
            fetchNameHistory(sender, player.getUniqueId(), JoinStatus.ONLINE);
            return;
        }
        Optional<MojangAPI> apiOpt = plugin.getMojangAPI();
        if (!apiOpt.isPresent()) {
            lookupLatestByUsername(sender, username);
            return;
        }
        plugin.log("Fetching UUID for %s from Mojang API", username);
        plugin.sendMessage(sender, Messages.MOJANG_LOOKUP);
        plugin.scheduleAsync(() -> lookupUUIDFromMojangAsync(sender, username, apiOpt.get()));
    }
    private void lookupLatestByUsername(CommandSender sender, APICompatibleUsername username) {
        try {
            UUID uuid = plugin.getHistorian().getLatestByUsername(username.toString())
                    .map(NameRecord::getUuid)
                    .orElse(null);
            processUUIDSync(sender, username, uuid, false);
        } catch (SQLException ex) {
            plugin.err("Error fetching latest name for %s", ex, username);
            plugin.sendMessage(sender, Messages.FETCH_ERROR);
        }
    }
    private void lookupUUIDFromMojangAsync(CommandSender sender, APICompatibleUsername username, MojangAPI api) {
        try {
            UUID uuid = api.getUUID(username).orElse(null);
            plugin.scheduleNextTick(() -> processUUIDSync(sender, username, uuid, true));
        } catch (IOException ex) {
            plugin.err("Error fetching name history for %s", ex, username);
            plugin.scheduleNextTick(() -> plugin.sendMessage(sender, Messages.MOJANG_ERROR));
        }
    }
    private void processUUIDSync(CommandSender sender, APICompatibleUsername username, @Nullable UUID uuid, boolean shouldRecord) {
        if (uuid == null) {
            plugin.sendMessage(sender, Messages.UNKNOWN_PLAYER);
            return;
        }
        if (shouldRecord) {
            tryRecordName(uuid, username.toString());
        }
        boolean hasJoined = plugin.getPlayer(uuid).isPresent();
        JoinStatus joinStatus = hasJoined ? JoinStatus.OFFLINE : JoinStatus.NEVER_JOINED;
        fetchNameHistory(sender, uuid, joinStatus);
    }

    private void tryRecordName(UUID uuid, String username) {
        try {
            plugin.getHistorian().recordName(uuid, username);
        } catch (SQLException ex) {
            plugin.err("Error recording name for %s - %s", ex, username, uuid);
        }
    }

    private void fetchNameHistory(CommandSender sender, UUID uuid, JoinStatus joinStatus) {
        try {
            List<NameRecord> history = plugin.getHistorian().getNameHistory(uuid);
            printNameHistory(sender, history, joinStatus);
        } catch (SQLException ex) {
            plugin.err("Error fetching name history for %s", ex, uuid);
            plugin.sendMessage(sender, Messages.FETCH_ERROR);
        }
    }

    private void printNameHistory(CommandSender sender, List<NameRecord> nameHistory, JoinStatus joinStatus) {
        if (nameHistory.isEmpty()) {
            plugin.sendMessage(sender, Messages.NO_HISTORY);
            return;
        }

        UUID uuid = nameHistory.get(0).getUuid();
        plugin.sendMessage(sender, Messages.HISTORY_TITLE, uuid);

        if (joinStatus == JoinStatus.NEVER_JOINED) {
            plugin.sendMessage(sender, Messages.NEVER_JOINED);
        }

        for (int i = 0; i < nameHistory.size(); i++) {
            NameRecord nr = nameHistory.get(i);

            // History is newest to oldest, changes printed in reverse
            int changeNumber = nameHistory.size() - i;
            plugin.sendMessage(sender, Messages.USERNAME_LINE, changeNumber, nr.getUsername());

            // If the player is online, the player was last seen now
            Instant lastSeen = i == 0 && joinStatus == JoinStatus.ONLINE ? Instant.now() : nr.getLastSeenTime();
            plugin.sendMessage(sender, Messages.DATE_LINE, nr.getFirstSeenTime(), lastSeen);
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            List<String> completions = plugin.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.toList());
            return StringUtil.copyPartialMatches(args[0], completions, new ArrayList<>());
        }
        return null;
    }

    private enum JoinStatus {
        ONLINE, OFFLINE, NEVER_JOINED
    }

}
