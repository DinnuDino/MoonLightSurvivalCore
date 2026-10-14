/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.entity.Player
 */
package com.moonlight.vanilla.commands;

import com.moonlight.vanilla.MoonlightVanillaSurvivalCore;
import com.moonlight.vanilla.modules.punishments.BanManager;
import com.moonlight.vanilla.utils.MessageUtil;
import java.util.Collections;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class UnbanCommand
implements CommandExecutor,
TabCompleter {
    private final MoonlightVanillaSurvivalCore plugin;

    public UnbanCommand(MoonlightVanillaSurvivalCore plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        BanManager banManager;
        boolean success;
        if (!sender.hasPermission("moonlight.staff.unban")) {
            sender.sendMessage(MessageUtil.format("<red>Access denied: You do not have permission to unban players (moonlight.staff.unban).</red>"));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(MessageUtil.format("<red>Usage: /unban <player|#BanId> [reason]</red>"));
            return true;
        }
        String targetOrId = args[0];
        StringBuilder reasonSb = new StringBuilder();
        for (int i = 1; i < args.length; ++i) {
            reasonSb.append(args[i]).append(" ");
        }
        String reason = reasonSb.toString().trim();
        if (reason.isEmpty()) {
            reason = "Pardoned by staff";
        }
        if (!(success = (banManager = this.plugin.getBanManager()).unban(targetOrId, sender.getName(), reason))) {
            sender.sendMessage(MessageUtil.format("<red>No active ban found for <yellow>" + targetOrId + "</yellow>.</red>"));
            return true;
        }
        sender.sendMessage(MessageUtil.format("<green>Successfully pardoned ban for <yellow>" + targetOrId + "</yellow>. Reason: <gray>" + reason + "</gray></green>"));
        String staffMsg = "<green>[Punishments] <yellow>" + targetOrId + "</yellow> was unbanned by <gold>" + sender.getName() + "</gold>. Reason: <gray>" + reason + "</gray></green>";
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.hasPermission("moonlight.staff.unban")) continue;
            p.sendMessage(MessageUtil.format(staffMsg));
        }
        Bukkit.getConsoleSender().sendMessage(MessageUtil.format(staffMsg));
        return true;
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}

