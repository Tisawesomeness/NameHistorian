package com.tisawesomeness.namehistorian.spigot;

import com.tisawesomeness.namehistorian.NameRecord;

import lombok.AllArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@AllArgsConstructor
public class HistoryCommand implements CommandExecutor, TabCompleter {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final NameHistorianSpigot plugin;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            plugin.sendMessage(sender, "%sUsage: /%s <player>", ChatColor.RED, label);
            return true;
        }
        Optional<OfflinePlayer> playerOpt = plugin.getPlayer(args[0]);
        if (!playerOpt.isPresent()) {
            plugin.sendMessage(sender, ChatColor.RED + "Player not found.");
            return true;
        }
        OfflinePlayer player = playerOpt.get();
        try {
            printNameHistory(sender, plugin.getHistorian().getNameHistory(player.getUniqueId()));
        } catch (SQLException ex) {
            ex.printStackTrace();
            plugin.sendMessage(sender, ChatColor.RED + "An error occurred while fetching the name history.");
        }
        return true;
    }

    private void printNameHistory(CommandSender sender, List<NameRecord> nameHistory) {
        if (nameHistory.isEmpty()) {
            plugin.sendMessage(sender, ChatColor.RED + "No name history found.");
            return;
        }
        UUID uuid = nameHistory.get(0).getUuid();
        plugin.sendMessage(sender, "%sName history for %s%s", ChatColor.GOLD, ChatColor.GREEN, uuid);
        for (int i = 0; i < nameHistory.size(); i++) {
            NameRecord nr = nameHistory.get(i);
            int changeNumber = nameHistory.size() - i;
            plugin.sendMessage(sender, "%s%d. %s%s", ChatColor.BLUE, changeNumber, ChatColor.LIGHT_PURPLE, nr.getUsername());
            plugin.sendMessage(sender, "%sFrom: %s%s, %sTo: %s%s",
                    ChatColor.BLUE, ChatColor.GREEN, format(nr.getFirstSeenTime()),
                    ChatColor.BLUE, ChatColor.GREEN, format(nr.getLastSeenTime())
            );
        }
    }
    private static String format(Instant time) {
        return FORMATTER.format(time.atZone(ZoneOffset.systemDefault()));
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
