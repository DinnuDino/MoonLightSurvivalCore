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
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package com.moonlight.vanilla.modules.crossplay;

import com.moonlight.vanilla.MoonlightVanillaSurvivalCore;
import com.moonlight.vanilla.modules.crossplay.BedrockManager;
import com.moonlight.vanilla.modules.crossplay.ViaBackwardsManager;
import com.moonlight.vanilla.utils.MessageUtil;
import com.moonlight.vanilla.utils.VanishUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ViaInfoCommand
implements CommandExecutor,
TabCompleter {
    private final MoonlightVanillaSurvivalCore plugin;

    public ViaInfoCommand(MoonlightVanillaSurvivalCore plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        Player target;
        if (args.length > 0) {
            target = Bukkit.getPlayer((String)args[0]);
            if (target == null || !VanishUtil.canSee(sender, target)) {
                sender.sendMessage(MessageUtil.format("<red>Player '<yellow>" + args[0] + "</yellow>' is not online.</red>"));
                return true;
            }
        } else if (sender instanceof Player) {
            Player player;
            target = player = (Player)sender;
        } else {
            sender.sendMessage(MessageUtil.format("<red>Console usage: /viainfo <player></red>"));
            return true;
        }
        ViaBackwardsManager viaManager = this.plugin.getViaBackwardsManager();
        BedrockManager bedrockManager = this.plugin.getBedrockManager();
        int protocol = viaManager != null ? viaManager.getPlayerProtocol(target.getUniqueId()) : 769;
        String versionString = viaManager != null ? viaManager.getProtocolName(protocol) : "Paper 26.2 Native";
        boolean isBedrock = bedrockManager != null && bedrockManager.isBedrockPlayer(target);
        String device = bedrockManager != null ? bedrockManager.getDeviceName(target) : "Java Edition";
        sender.sendMessage(MessageUtil.format("<gold>=== Multi-Version Client Inspection: <yellow>" + target.getName() + "</yellow> ===</gold>"));
        sender.sendMessage(MessageUtil.format("<gray>Client Platform:</gray> <aqua>" + (String)(isBedrock ? "Bedrock Crossplay (" + device + ")" : "Java Edition") + "</aqua>"));
        sender.sendMessage(MessageUtil.format("<gray>Protocol Version:</gray> <light_purple>" + protocol + "</light_purple> (<green>" + versionString + "</green>)"));
        sender.sendMessage(MessageUtil.format("<gray>Paper Native Target:</gray> <white>1.21.4 / Paper 26.2 (Protocol 769)</white>"));
        if (protocol < 769) {
            sender.sendMessage(MessageUtil.format("<gray>Translation Bridge:</gray> <gold>ViaBackwards Active (No disconnects or profile errors)</gold>"));
        } else {
            sender.sendMessage(MessageUtil.format("<gray>Translation Bridge:</gray> <green>Native Unconverted Session</green>"));
        }
        return true;
    }

    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            ArrayList<String> list = new ArrayList<String>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!VanishUtil.canSee(sender, p) || !p.getName().toLowerCase().startsWith(args[0].toLowerCase())) continue;
                list.add(p.getName());
            }
            return list;
        }
        return Collections.emptyList();
    }
}

