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
 */
package com.moonlight.vanilla.commands;

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

public class StaffHelpCommand
implements CommandExecutor,
TabCompleter {
    private final MoonlightVanillaSurvivalCore plugin;

    public StaffHelpCommand(MoonlightVanillaSurvivalCore plugin) {
        this.plugin = plugin;
    }

    public boolean isStaffEligible(CommandSender sender) {
        if (!(sender instanceof Player)) {
            return true;
        }
        Player player = (Player)sender;
        return player.isOp() || player.hasPermission("moonlight.staff.shelp");
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!this.isStaffEligible(sender)) {
            sender.sendMessage(MessageUtil.format("<red>Access denied: Staff commands handbook (/shelp) requires OP or staff permission.</red>"));
            return true;
        }
        String filter = args.length > 0 ? args[0].toLowerCase() : "all";
        sender.sendMessage(MessageUtil.format("<gradient:#6366f1:#a855f7><bold>\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550 Moonlight Staff Handbook & Commands (/shelp) \u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550</bold></gradient>"));
        if (filter.equals("all") || filter.equals("general") || filter.equals("helper")) {
            sender.sendMessage(MessageUtil.format("<yellow><bold>\u25b6 Inspection & Teleport Commands:</bold></yellow>"));
            sender.sendMessage(MessageUtil.format(" <aqua>/shelp [category]</aqua> <gray>- Display this interactive staff command guide</gray>"));
            sender.sendMessage(MessageUtil.format(" <aqua>/inv <player></aqua> <gray>- Live player inventory inspector</gray>"));
            sender.sendMessage(MessageUtil.format(" <aqua>/ec <player></aqua> <gray>- Live player Ender Chest inspector</gray>"));
        }
        if (filter.equals("all") || filter.equals("mod") || filter.equals("moderation")) {
            sender.sendMessage(MessageUtil.format("<yellow><bold>\u25b6 Moderation & Punishment Commands:</bold></yellow>"));
            sender.sendMessage(MessageUtil.format(" <aqua>/mute <player> [duration] [reason]</aqua> <gray>- Silence rule-breaking players</gray>"));
            sender.sendMessage(MessageUtil.format(" <aqua>/unmute <player></aqua> <gray>- Lift player chat mute</gray>"));
            sender.sendMessage(MessageUtil.format(" <aqua>/kick <player> [reason]</aqua> <gray>- Disconnect a player from server</gray>"));
            sender.sendMessage(MessageUtil.format(" <aqua>/ban <player> <duration> [reason]</aqua> <gray>- Timed or permanent player ban with tracking Ban ID</gray>"));
            sender.sendMessage(MessageUtil.format(" <aqua>/unban <player></aqua> <gray>- Pardon a banned player</gray>"));
            sender.sendMessage(MessageUtil.format(" <aqua>/baninfo <player></aqua> <gray>- View active ban details & remaining timer</gray>"));
            sender.sendMessage(MessageUtil.format(" <aqua>/logs <player> [DD/MM/YYYY] [HH:MM:SS]</aqua> <gray>- Lookup deep transaction/death logs</gray>"));
        }
        if (filter.equals("all") || filter.equals("admin") || filter.equals("owner")) {
            sender.sendMessage(MessageUtil.format("<yellow><bold>\u25b6 Server Operator & Admin Commands:</bold></yellow>"));
            sender.sendMessage(MessageUtil.format(" <gold>/changename <player> <name|reset></gold> <gray>- Change or reset player display nickname</gray>"));
            sender.sendMessage(MessageUtil.format(" <gold>/viainfo [player]</gold> <gray>- Display crossplay and client protocol diagnostics</gray>"));
        }
        sender.sendMessage(MessageUtil.format("<gray>Syntax preview: Hover over commands or use TAB for smart argument completion.</gray>"));
        sender.sendMessage(MessageUtil.format("<gradient:#6366f1:#a855f7><bold>\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550</bold></gradient>"));
        return true;
    }

    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!this.isStaffEligible(sender)) {
            return Collections.emptyList();
        }
        if (args.length <= 1) {
            String query = args.length == 0 ? "" : args[0].toLowerCase();
            List<String> categories = List.of("all", "helper", "moderation", "admin");
            ArrayList<String> matches = new ArrayList<String>();
            for (String cat : categories) {
                if (!cat.startsWith(query)) continue;
                matches.add(cat);
            }
            return matches;
        }
        return Collections.emptyList();
    }
}

