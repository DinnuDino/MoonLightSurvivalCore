/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.entity.Player
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package com.moonlight.vanilla.modules.scoreboard.commands;

import com.moonlight.vanilla.MoonlightVanillaSurvivalCore;
import com.moonlight.vanilla.utils.MessageUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ScoreboardCommand
implements CommandExecutor,
TabCompleter {
    private final MoonlightVanillaSurvivalCore plugin;

    public ScoreboardCommand(MoonlightVanillaSurvivalCore plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtil.format("&9MoonLight &8\u2759 &cOnly in-game players can toggle their scoreboard."));
            return true;
        }
        Player player = (Player)sender;
        if (args.length == 0 || args[0].equalsIgnoreCase("toggle")) {
            boolean visible = this.plugin.getScoreboardManager().toggleScoreboard(player);
            if (visible) {
                player.sendMessage(MessageUtil.format("&9MoonLight &8\u2759 &aScoreboard has been &2enabled&a."));
            } else {
                player.sendMessage(MessageUtil.format("&9MoonLight &8\u2759 &cScoreboard has been &4disabled&c."));
            }
            return true;
        }
        if (args[0].equalsIgnoreCase("on") || args[0].equalsIgnoreCase("show")) {
            boolean changed = this.plugin.getScoreboardManager().showScoreboard(player);
            if (changed) {
                player.sendMessage(MessageUtil.format("&9MoonLight &8\u2759 &aScoreboard has been &2enabled&a."));
            } else {
                player.sendMessage(MessageUtil.format("&9MoonLight &8\u2759 &eScoreboard is already enabled."));
            }
            return true;
        }
        if (args[0].equalsIgnoreCase("off") || args[0].equalsIgnoreCase("hide")) {
            boolean changed = this.plugin.getScoreboardManager().hideScoreboard(player);
            if (changed) {
                player.sendMessage(MessageUtil.format("&9MoonLight &8\u2759 &cScoreboard has been &4disabled&c."));
            } else {
                player.sendMessage(MessageUtil.format("&9MoonLight &8\u2759 &eScoreboard is already disabled."));
            }
            return true;
        }
        if (args[0].equalsIgnoreCase("logo") || args[0].equalsIgnoreCase("title") || args[0].equalsIgnoreCase("mode")) {
            boolean logoMode = this.plugin.getScoreboardManager().toggleTitleMode(player);
            if (logoMode) {
                player.sendMessage(MessageUtil.format("&9MoonLight &8\u2759 &aScoreboard header set to &bGraphic Logo Mode &a(Requires pack)."));
            } else {
                player.sendMessage(MessageUtil.format("&9MoonLight &8\u2759 &aScoreboard header set to &eClean Text Mode &a(Works on all clients)."));
            }
            return true;
        }
        player.sendMessage(MessageUtil.format("&9MoonLight &8\u2759 &7Usage: &b/" + label + " <toggle|on|off|title>"));
        return true;
    }

    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length <= 1) {
            String query = args.length == 0 ? "" : args[0].toLowerCase();
            ArrayList<String> list = new ArrayList<String>();
            for (String sub : List.of("toggle", "title", "on", "off")) {
                if (!sub.startsWith(query)) continue;
                list.add(sub);
            }
            return list;
        }
        return Collections.emptyList();
    }
}

