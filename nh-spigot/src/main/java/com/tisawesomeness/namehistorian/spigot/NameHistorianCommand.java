package com.tisawesomeness.namehistorian.spigot;

import lombok.AllArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@AllArgsConstructor
public final class NameHistorianCommand implements CommandExecutor, TabCompleter {

    private final NameHistorianSpigot plugin;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || !args[0].equalsIgnoreCase("reload")) {
            plugin.sendMessage(sender, Messages.NAMEHISTORIAN_USAGE, label);
            return true;
        }
        try {
            plugin.reload();
        } catch (Exception ex) {
            plugin.err("Reload failed", ex);
            plugin.sendMessage(sender, Messages.RELOAD_FAILED);
            return true;
        }
        plugin.sendMessage(sender, Messages.RELOAD_SUCCESS);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            List<String> completions = Collections.singletonList("reload");
            return StringUtil.copyPartialMatches(args[0], completions, new ArrayList<>());
        }
        return null;
    }

}
