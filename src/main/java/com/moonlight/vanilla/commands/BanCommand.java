/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.entity.Player
 */
package com.moonlight.vanilla.commands;

import com.moonlight.vanilla.MoonlightVanillaSurvivalCore;
import com.moonlight.vanilla.modules.punishments.BanManager;
import com.moonlight.vanilla.modules.punishments.BanRecord;
import com.moonlight.vanilla.utils.MessageUtil;
import com.moonlight.vanilla.utils.VanishUtil;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class BanCommand
implements CommandExecutor,
TabCompleter {
    private final MoonlightVanillaSurvivalCore plugin;

    public BanCommand(MoonlightVanillaSurvivalCore plugin) {
        this.plugin = plugin;
    }

    public static int getSenderStaffPriority(MoonlightVanillaSurvivalCore plugin, CommandSender sender) {
        if (!(sender instanceof Player)) {
            return 100;
        }
        Player player = (Player)sender;
        if (player.isOp() || player.hasPermission("moonlight.core.admin") || player.hasPermission("moonlight.admin")) {
            return 100;
        }
        if (player.hasPermission("moonlight.staff.ban.permanent")) {
            return 12;
        }
        if (player.hasPermission("moonlight.staff.ban")) {
            return 8;
        }
        return 0;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        int priority = BanCommand.getSenderStaffPriority(this.plugin, sender);
        if (priority < 8 && !sender.hasPermission("moonlight.staff.ban")) {
            sender.sendMessage(MessageUtil.format("<red>Access denied: Ban authority requires operator or moonlight.staff.ban permission.</red>"));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(MessageUtil.format("<red>Usage: /ban <player> [duration] <reason></red>"));
            sender.sendMessage(MessageUtil.format("<gray>Examples: <aqua>/ban CheaterX 2hr Hacks</aqua> or <aqua>/ban Spammer 3d Toxicity</aqua></gray>"));
            if (priority >= 12) {
                sender.sendMessage(MessageUtil.format("<gray>Permanent Ban Example: <gold>/ban CheaterX Severe Exploits</gold></gray>"));
            }
            return true;
        }
        String targetName = args[0];
        OfflinePlayer target = Bukkit.getOfflinePlayer((String)targetName);
        UUID targetUuid = target.getUniqueId();
        BanManager banManager = this.plugin.getBanManager();
        if (banManager.getActiveBan(targetUuid) != null) {
            sender.sendMessage(MessageUtil.format("<red>Player <yellow>" + targetName + "</yellow> is already actively banned! Use /baninfo " + targetName + " to inspect.</red>"));
            return true;
        }
        Long durationMillis = null;
        String durationStr = "permanent";
        int reasonStartIndex = 1;
        if (args.length >= 2) {
            String firstArg = args[1].toLowerCase();
            if (firstArg.equals("perm") || firstArg.equals("permanent") || firstArg.equals("forever") || firstArg.equals("inf")) {
                durationMillis = null;
                durationStr = "permanent";
                reasonStartIndex = 2;
            } else {
                int idx;
                StringBuilder durationInput = new StringBuilder();
                for (idx = 1; idx < args.length && banManager.isDurationToken(args[idx]); ++idx) {
                    if (!durationInput.isEmpty()) {
                        durationInput.append(" ");
                    }
                    durationInput.append(args[idx]);
                }
                if (!durationInput.isEmpty()) {
                    try {
                        durationMillis = banManager.parseDuration(durationInput.toString());
                        durationStr = BanManager.formatDuration(durationMillis);
                        reasonStartIndex = idx;
                    }
                    catch (IllegalArgumentException e) {
                        sender.sendMessage(MessageUtil.format("<red>Invalid duration: " + e.getMessage() + "</red>"));
                        return true;
                    }
                } else {
                    durationMillis = null;
                    durationStr = "permanent";
                    reasonStartIndex = 1;
                }
            }
        }
        StringBuilder reasonSb = new StringBuilder();
        for (int i = reasonStartIndex; i < args.length; ++i) {
            reasonSb.append(args[i]).append(" ");
        }
        String reason = reasonSb.toString().trim();
        if (reason.isEmpty()) {
            reason = "Suspended by staff";
        }
        long maxAllowedMillis = 259200000L;
        if (priority < 12) {
            if (durationMillis == null) {
                sender.sendMessage(MessageUtil.format("<red>Access denied: Permanent bans require operator status or moonlight.staff.ban.permanent permission. Maximum allowed duration is 3 days (e.g. /ban " + targetName + " 3d " + reason + ").</red>"));
                return true;
            }
            if (durationMillis > maxAllowedMillis) {
                sender.sendMessage(MessageUtil.format("<red>Access denied: Your permission limits bans to a maximum of 3 days (3d).</red>"));
                return true;
            }
        }
        UUID bannerUuid = sender instanceof Player ? ((Player)sender).getUniqueId() : UUID.fromString("00000000-0000-0000-0000-000000000000");
        String bannerName = sender.getName();
        String ip = target.isOnline() && target.getPlayer() != null ? target.getPlayer().getAddress().getAddress().getHostAddress() : null;
        BanRecord ban = banManager.issueBan(targetName, targetUuid, bannerName, bannerUuid, durationMillis, durationStr, reason, ip);
        sender.sendMessage(MessageUtil.format("<green>Successfully banned <yellow>" + targetName + "</yellow> for <aqua>" + durationStr + "</aqua>. Ban ID: <light_purple>" + ban.getBanId() + "</light_purple></green>"));
        String staffMsg = "<red>[Punishments] <yellow>" + targetName + "</yellow> was banned by <gold>" + bannerName + "</gold> for <aqua>" + durationStr + "</aqua>. Reason: <gray>" + reason + "</gray> [Ban ID: <light_purple>" + ban.getBanId() + "</light_purple>]</red>";
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.hasPermission("moonlight.staff.ban") && !p.isOp()) continue;
            p.sendMessage(MessageUtil.format(staffMsg));
        }
        Bukkit.getConsoleSender().sendMessage(MessageUtil.format(staffMsg));
        return true;
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream().filter(p -> VanishUtil.canSee(sender, p)).map(Player::getName).filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        if (args.length == 2) {
            int priority = BanCommand.getSenderStaffPriority(this.plugin, sender);
            if (priority < 12) {
                return List.of("30m", "1hr", "2hr", "6hr", "12hr", "1d", "2d", "3d");
            }
            return List.of("30m", "1hr", "2hr", "6hr", "12hr", "1d", "3d", "7d", "30d", "perm");
        }
        if (args.length == 3) {
            return List.of("Hacks/Fly", "KillAura/Combat", "X-Ray_Mining", "Toxicity/Chat", "Duping/Exploits");
        }
        return Collections.emptyList();
    }
}

