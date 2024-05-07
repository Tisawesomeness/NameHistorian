package com.tisawesomeness.namehistorian.spigot;

import com.tchristofferson.configupdater.ConfigUpdater;
import com.tisawesomeness.namehistorian.NameHistorian;
import com.tisawesomeness.namehistorian.NamedPlayer;
import com.tisawesomeness.namehistorian.Util;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.formatter.qual.FormatMethod;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public final class NameHistorianSpigot extends JavaPlugin {

    private static final Component PREFIX = Component.join(JoinConfiguration.noSeparators(),
            Component.text("[").color(NamedTextColor.GRAY),
            Component.text("NH").color(NamedTextColor.BLUE),
            Component.text("]").color(NamedTextColor.GRAY),
            Component.text(" ")
    );

    // Null until plugin enabled
    private @Nullable BukkitAudiences adventure;
    private @Nullable NameHistorianConfig config;
    private @Nullable TranslationManager translationManager;
    private @Nullable MojangAPI mojangAPI; // null if disabled in config
    private @Nullable NameHistorian historian;

    @Override
    public void onEnable() {
        adventure = BukkitAudiences.create(this);

        File configFile = new File(getDataFolder(), "config.yml");
        if (configFile.exists()) {
            getConfig().options().copyDefaults(true);
            try {
                ConfigUpdater.update(this, "config.yml", configFile);
            } catch (IOException ex) {
                err("Error while trying to update config", ex);
                // Non-fatal
            }
            reloadConfig();
        } else {
            saveDefaultConfig();
        }
        config = new NameHistorianConfig(this);

        Path dataPath = getDataFolder().toPath();
        try {
            Files.createDirectories(dataPath); // Plugin folder might not exist, create it
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        translationManager = new TranslationManager(this);
        assert config != null;
        translationManager.init(config);

        if (config.isEnableMojangLookups()) {
            mojangAPI = new MojangAPI(config.getMojangTimeout());
        }

        Path dbPath = dataPath.resolve("history.db");
        try {
            historian = new NameHistorian(dbPath);
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }

        getServer().getPluginManager().registerEvents(new SeenListener(this), this);
        Objects.requireNonNull(getCommand("history")).setExecutor(new HistoryCommand(this));
        Objects.requireNonNull(getCommand("namehistorian")).setExecutor(new NameHistorianCommand(this));

        try {
            recordOnlinePlayers();
        } catch (SQLException ex) {
            err("Could not record online players", ex);
            // Non-fatal, keep plugin running
        }
    }

    public void reload() {
        reloadConfig();
        config = new NameHistorianConfig(this);
        if (translationManager != null) {
            translationManager.init(config);
        }
        if (config.isEnableMojangLookups()) {
            mojangAPI = new MojangAPI(config.getMojangTimeout());
        } else {
            mojangAPI = null;
        }
    }

    @Override
    public void onDisable() {
        if (adventure != null) {
            adventure.close();
            adventure = null;
        }
        if (historian != null) {
            try {
                recordOnlinePlayers();
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private void recordOnlinePlayers() throws SQLException {
        List<NamedPlayer> players = getServer().getOnlinePlayers().stream()
                .map(NameHistorianSpigot::toNamedPlayer)
                .collect(Collectors.toList());
        getHistorian().recordNames(players);
    }
    private static NamedPlayer toNamedPlayer(Player player) {
        return new NamedPlayer(player.getUniqueId(), player.getName());
    }

    public BukkitAudiences getAdventure() {
        if (adventure == null) {
            throw new IllegalStateException("Tried to get adventure instance before plugin enabled");
        }
        return adventure;
    }
    public Optional<MojangAPI> getMojangAPI() {
        return Optional.ofNullable(mojangAPI);
    }
    public NameHistorian getHistorian() {
        if (historian == null) {
            throw new IllegalStateException("Tried to get historian instance before plugin enabled");
        }
        return historian;
    }

    public Optional<OfflinePlayer> getPlayer(String playerNameOrUUID) {
        // Longer usernames are not allowed in (at least) 1.19
        if (playerNameOrUUID.length() <= 16) {
            return Optional.ofNullable(getServer().getPlayer(playerNameOrUUID));
        }
        Optional<UUID> uuidOpt = Util.parseUUID(playerNameOrUUID);
        if (!uuidOpt.isPresent()) {
            return Optional.empty();
        }
        OfflinePlayer p = getServer().getOfflinePlayer(uuidOpt.get());
        // OfflinePlayer returned even if player never seen, check if player seen
        if (p.getLastPlayed() == 0) {
            return Optional.empty();
        }
        return Optional.of(p);
    }

    public void scheduleNextTick(Runnable runnable) {
        getServer().getScheduler().runTask(this, runnable);
    }
    public void scheduleAsync(Runnable runnable) {
        getServer().getScheduler().runTaskAsynchronously(this, runnable);
    }

    public void sendMessage(CommandSender sendTo, Component msg) {
        getAdventure().sender(sendTo).sendMessage(PREFIX.append(msg));
    }
    public <A0> void sendMessage(CommandSender sendTo, Messages.A1<A0> msg, A0 a0) {
        sendMessage(sendTo, msg.build(a0));
    }
    public <A0, A1> void sendMessage(CommandSender sendTo, Messages.A2<A0, A1> msg, A0 a0, A1 a1) {
        sendMessage(sendTo, msg.build(a0, a1));
    }

    @FormatMethod
    public void log(String msg, Object... args) {
        getLogger().info(String.format(msg, args));
    }
    @FormatMethod
    public void warn(String msg, Object... args) {
        getLogger().warning(String.format(msg, args));
    }
    @FormatMethod
    public void err(String msg, Object... args) {
        getLogger().severe(String.format(msg, args));
    }
    public void err(String msg, Throwable ex, Object... args) {
        getLogger().severe(String.format(msg, args));
        err(ex);
    }
    public void err(Throwable ex) {
        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));
        for (String line : sw.toString().split("\n")) {
            getLogger().severe(line);
        }
    }

}
