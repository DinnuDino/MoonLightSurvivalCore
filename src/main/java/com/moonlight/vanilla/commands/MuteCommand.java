/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.jetbrains.annotations.NotNull
 */
package com.moonlight.vanilla.commands;

import com.moonlight.vanilla.MoonlightVanillaSurvivalCore;
import com.moonlight.vanilla.modules.punishments.BanManager;
import com.moonlight.vanilla.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class MuteCommand
implements CommandExecutor {
    private final MoonlightVanillaSurvivalCore plugin;

    public MuteCommand(MoonlightVanillaSurvivalCore plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("moonlight.staff.mute")) {
            sender.sendMessage("No permission.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(MessageUtil.format("<red>Usage: /mute <player> [duration]</red>"));
            return true;
        }
        Player target = Bukkit.getPlayer((String)args[0]);
        if (target == null) {
            sender.sendMessage(MessageUtil.format("<red>Player not found online.</red>"));
            return true;
        }
        if (target.hasPermission("moonlight.staff.bypass") && !sender.isOp() && !sender.hasPermission("moonlight.core.admin") && !sender.hasPermission("moonlight.admin")) {
            sender.sendMessage(MessageUtil.format("<red>You cannot mute this player.</red>"));
            return true;
        }
        Long expiresAt = null;
        Object durationStr = "permanently";
        if (args.length > 1) {
            String firstArg = args[1].toLowerCase();
            if (firstArg.equals("perm") || firstArg.equals("permanent") || firstArg.equals("forever") || firstArg.equals("inf")) {
                expiresAt = null;
                durationStr = "permanently";
            } else {
                StringBuilder durationInput = new StringBuilder();
                for (int idx = 1; idx < args.length && this.plugin.getBanManager().isDurationToken(args[idx]); ++idx) {
                    if (!durationInput.isEmpty()) {
                        durationInput.append(" ");
                    }
                    durationInput.append(args[idx]);
                }
                String inputToParse = durationInput.isEmpty() ? args[1] : durationInput.toString();
                try {
                    Long durationMillis = this.plugin.getBanManager().parseDuration(inputToParse);
                    if (durationMillis != null) {
                        expiresAt = System.currentTimeMillis() + durationMillis;
                        durationStr = "for " + BanManager.formatDuration(durationMillis);
                    }
                }
                catch (Exception e) {
                    sender.sendMessage(MessageUtil.format("<red>Invalid duration: " + e.getMessage() + " (Examples: 0h 0m 1s, 30m, 2hr, 1d, perm)</red>"));
                    return true;
                }
            }
        }
        this.plugin.getMuteManager().mute(target, sender.getName(), expiresAt);
        sender.sendMessage(MessageUtil.format("<green>Muted " + target.getName() + " " + (String)durationStr + ".</green>"));
        target.sendMessage(MessageUtil.format("<red>You have been muted " + (String)durationStr + ".</red>"));
        return true;
    }
}

