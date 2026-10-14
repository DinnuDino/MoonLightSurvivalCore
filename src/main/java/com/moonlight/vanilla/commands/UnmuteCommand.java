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
import com.moonlight.vanilla.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class UnmuteCommand
implements CommandExecutor {
    private final MoonlightVanillaSurvivalCore plugin;

    public UnmuteCommand(MoonlightVanillaSurvivalCore plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("moonlight.staff.mute")) {
            sender.sendMessage("No permission.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(MessageUtil.format("<red>Usage: /unmute <player></red>"));
            return true;
        }
        Player target = Bukkit.getPlayer((String)args[0]);
        if (target != null) {
            this.plugin.getMuteManager().unmute(target.getUniqueId());
            sender.sendMessage(MessageUtil.format("<green>Unmuted " + target.getName() + ".</green>"));
            target.sendMessage(MessageUtil.format("<green>You have been unmuted.</green>"));
        } else {
            sender.sendMessage(MessageUtil.format("<red>Player not found online.</red>"));
        }
        return true;
    }
}

