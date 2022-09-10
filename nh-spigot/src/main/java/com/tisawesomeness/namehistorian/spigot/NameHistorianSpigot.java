package com.tisawesomeness.namehistorian.spigot;

import com.tisawesomeness.namehistorian.NameHistorian;
import com.tisawesomeness.namehistorian.NamedPlayer;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class NameHistorianSpigot extends JavaPlugin {

    private @Nullable NameHistorian historian;

    @Override
    public void onEnable() {
        Path dataPath = getDataFolder().toPath();
        try {
            Files.createDirectories(dataPath); // Plugin folder might not exist, create it
            Path dbPath = dataPath.resolve("history.db");
            historian = new NameHistorian(dbPath);
        } catch (IOException | SQLException ex) {
            throw new RuntimeException(ex);
        }

        getServer().getPluginManager().registerEvents(new SeenListener(this), this);
        Objects.requireNonNull(getCommand("history")).setExecutor(new HistoryCommand(this));

        try {
            recordOnlinePlayers();
        } catch (SQLException ex) {
            ex.printStackTrace();
            // Non-fatal, keep plugin running
        }
    }

    @Override
    public void onDisable() {
        try {
            recordOnlinePlayers();
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void recordOnlinePlayers() throws SQLException {
        List<NamedPlayer> players = getServer().getOnlinePlayers().stream()
                .map(this::toNamedPlayer)
                .toList();
        getHistorian().recordNames(players);
    }
    private NamedPlayer toNamedPlayer(Player player) {
        return new NamedPlayer(player.getUniqueId(), player.getName());
    }

    public NameHistorian getHistorian() {
        if (historian == null) {
            throw new IllegalStateException("Historian is not initialized");
        }
        return historian;
    }

    public Optional<OfflinePlayer> getPlayer(String playerNameOrUUID) {
        // Longer usernames are not allowed in (at least) 1.19
        if (playerNameOrUUID.length() <= 16) {
            return Optional.ofNullable(getServer().getPlayer(playerNameOrUUID));
        }
        try {
            OfflinePlayer p = getServer().getOfflinePlayer(UUID.fromString(playerNameOrUUID));
            // OfflinePlayer returned even if player never seen, check if player seen
            if (p.getLastPlayed() == 0) {
                return Optional.empty();
            }
            return Optional.of(p);
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    public void sendMessage(CommandSender sender, String msg, Object... args) {
        sender.sendMessage("§7[§9NH§7]§r " + String.format(msg, args));
    }

}
