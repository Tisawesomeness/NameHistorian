package com.tisawesomeness.namehistorian.spigot;

import com.tisawesomeness.namehistorian.NameHistorian;

import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;

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
        getServer().getPluginManager().registerEvents(new JoinListener(this), this);
    }

    public NameHistorian getHistorian() {
        if (historian == null) {
            throw new IllegalStateException("Historian is not initialized");
        }
        return historian;
    }

}
