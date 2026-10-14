/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.TabCompleter
 */
package com.moonlight.vanilla.commands;

import com.moonlight.vanilla.MoonlightVanillaSurvivalCore;
import com.moonlight.vanilla.modules.punishments.BanManager;
import com.moonlight.vanilla.modules.punishments.BanRecord;
import com.moonlight.vanilla.utils.MessageUtil;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

public class BanInfoCommand
implements CommandExecutor,
TabCompleter {
    private final MoonlightVanillaSurvivalCore plugin;

    public BanInfoCommand(MoonlightVanillaSurvivalCore plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("moonlight.staff.baninfo")) {
            sender.sendMessage(MessageUtil.format("<red>Access denied: You do not have permission to view ban information.</red>"));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(MessageUtil.format("<red>Usage: /baninfo <player|#BanId></red>"));
            return true;
        }
        String query = args[0];
        BanManager banManager = this.plugin.getBanManager();
        BanRecord ban = null;
        if (query.startsWith("#")) {
            ban = banManager.getBanById(query);
        }
        if (ban == null) {
            ban = banManager.getActiveBanByName(query);
        }
        if (ban == null) {
            ban = banManager.getBanById(query);
        }
        if (ban == null) {
            sender.sendMessage(MessageUtil.format("<yellow>No active ban record found for '" + query + "'.</yellow>"));
            return true;
        }
        sender.sendMessage(MessageUtil.format("<gold><bold>\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501 BAN INFORMATION \u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501</bold></gold>"));
        sender.sendMessage(MessageUtil.format("<white>Player:</white> <yellow>" + ban.getTargetName() + "</yellow> <gray>(" + String.valueOf(ban.getTargetUuid()) + ")</gray>"));
        sender.sendMessage(MessageUtil.format("<white>Ban ID:</white> <light_purple>" + ban.getBanId() + "</light_purple>"));
        sender.sendMessage(MessageUtil.format("<white>Banned By:</white> <gold>" + ban.getBannedByName() + "</gold>"));
        sender.sendMessage(MessageUtil.format("<white>Reason:</white> <red>" + ban.getReason() + "</red>"));
        sender.sendMessage(MessageUtil.format("<white>Status:</white> " + (ban.isActive() ? "<green>ACTIVE</green>" : "<gray>INACTIVE</gray>")));
        sender.sendMessage(MessageUtil.format("<white>Duration:</white> <aqua>" + ban.getDurationString() + "</aqua>"));
        sender.sendMessage(MessageUtil.format("<white>Time Remaining:</white> <aqua><bold>" + ban.getFormattedRemaining() + "</bold></aqua>"));
        sender.sendMessage(MessageUtil.format("<white>Expires On:</white> <gray>" + ban.getFormattedExpiryDate(ZoneId.of("UTC")) + "</gray>"));
        sender.sendMessage(MessageUtil.format("<white>Correlation ID:</white> <gray>" + ban.getCorrelationId() + "</gray>"));
        sender.sendMessage(MessageUtil.format("<gold><bold>\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501</bold></gold>"));
        return true;
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}

