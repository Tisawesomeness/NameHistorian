package com.tisawesomeness.namehistorian.spigot;

import lombok.AllArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.sql.SQLException;

@AllArgsConstructor
public class JoinListener implements Listener {

    private final NameHistorianSpigot plugin;

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        try {
            plugin.getHistorian().recordName(p.getUniqueId(), p.getName());
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

}
