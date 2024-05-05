package com.tisawesomeness.namehistorian.spigot;

import lombok.AllArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.sql.SQLException;

@AllArgsConstructor
public final class SeenListener implements Listener {

    private final NameHistorianSpigot plugin;

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        onPlayerEvent(e);
    }
    @EventHandler
    public void onLeave(PlayerQuitEvent e) {
        onPlayerEvent(e);
    }

    private void onPlayerEvent(PlayerEvent e) {
        Player p = e.getPlayer();
        try {
            plugin.getHistorian().recordName(p.getUniqueId(), p.getName());
        } catch (SQLException ex) {
            plugin.err("Could not record name %s for %s", ex, p.getName(), p.getUniqueId());
        }
    }

}
