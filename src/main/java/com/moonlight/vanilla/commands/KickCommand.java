/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  org.bukkit.Bukkit
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.entity.Player
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package com.moonlight.vanilla.commands;

import com.moonlight.vanilla.MoonlightVanillaSurvivalCore;
import com.moonlight.vanilla.utils.MessageUtil;
import com.moonlight.vanilla.utils.VanishUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class KickCommand
implements CommandExecutor,
TabCompleter {
    private final MoonlightVanillaSurvivalCore plugin;

    public KickCommand(MoonlightVanillaSurvivalCore plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        Player p;
        if (!sender.hasPermission("moonlight.staff.kick")) {
            sender.sendMessage(MessageUtil.format("<red>Access denied: Requires moonlight.staff.kick permission.</red>"));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(MessageUtil.format("<red>Usage: /kick <player> [reason]</red>"));
            return true;
        }
        String targetName = args[0];
        Player target = Bukkit.getPlayer((String)targetName);
        if (target == null || !target.isOnline()) {
            sender.sendMessage(MessageUtil.format("<red>Player '" + targetName + "' is not online.</red>"));
            return true;
        }
        if (target.isOp() && sender instanceof Player && !(p = (Player)sender).isOp() && !p.hasPermission("moonlight.core.admin") && !p.hasPermission("moonlight.admin")) {
            sender.sendMessage(MessageUtil.format("<red>Security violation: You cannot kick a server operator!</red>"));
            return true;
        }
        String reason = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "Warned / Kicked by staff";
        String kickerName = sender instanceof Player ? sender.getName() : "CONSOLE";
        UUID kickerUuid = sender instanceof Player ? ((Player) sender).getUniqueId() : UUID.fromString("00000000-0000-0000-0000-000000000000");
        String kickMessage = "<red><bold>YOU HAVE BEEN KICKED FROM THE SERVER</bold></red>\n\n<gray>Reason: <white>" + reason + "</white></gray>\n<gray>Kicked By: <yellow>" + kickerName + "</yellow></gray>\n\n<yellow>This serves as an official warning. Please review server rules before reconnecting.</yellow>";
        target.kick(MessageUtil.format(kickMessage));
        if (this.plugin.getAuditManager() != null) {
            String correlationId = "kick-" + UUID.randomUUID().toString().substring(0, 8);
            this.plugin.getAuditManager().logAdminAction(kickerUuid, kickerName, target.getUniqueId().toString(), target.getName(), "PLAYER_KICKED", reason, "Command /kick", "Kicked online player", "SUCCESS", correlationId);
        }
        String alertMsg = "<red><bold>[KICK]</bold> " + target.getName() + " was kicked by " + kickerName + " for: " + reason + "</red>";
        Bukkit.broadcast((Component)MessageUtil.format(alertMsg), (String)"moonlight.staff.kick");
        sender.sendMessage(MessageUtil.format("<green>Successfully kicked player " + target.getName() + " for: " + reason + "</green>"));
        return true;
    }

    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            String query = args[0].toLowerCase();
            ArrayList<String> list = new ArrayList<String>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!VanishUtil.canSee(sender, p) || !p.getName().toLowerCase().startsWith(query)) continue;
                list.add(p.getName());
            }
            return list;
        }
        return Collections.emptyList();
    }
}

