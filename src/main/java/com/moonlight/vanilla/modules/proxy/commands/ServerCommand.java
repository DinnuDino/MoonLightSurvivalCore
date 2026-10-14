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
package com.moonlight.vanilla.modules.proxy.commands;

import com.moonlight.vanilla.MoonlightVanillaSurvivalCore;
import com.moonlight.vanilla.modules.proxy.ProxyManager;
import com.moonlight.vanilla.utils.MessageUtil;
import java.util.Collections;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ServerCommand
implements CommandExecutor,
TabCompleter {
    private final MoonlightVanillaSurvivalCore plugin;
    private final ProxyManager proxyManager;

    public ServerCommand(MoonlightVanillaSurvivalCore plugin, ProxyManager proxyManager) {
        this.plugin = plugin;
        this.proxyManager = proxyManager;
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (this.proxyManager.isProxyActive()) {
                sender.sendMessage(MessageUtil.format("<gradient:#6366f1:#a855f7>[Moonlight]</gradient> <white>Network Mode: </white><green>" + this.proxyManager.getNetworkMode().name() + "</green> <gray>(Usage: /server <target>)</gray>"));
            } else {
                sender.sendMessage(MessageUtil.format("<gradient:#6366f1:#a855f7>[Moonlight]</gradient> <white>Server: </white><aqua>Moonlight Survival</aqua> <gray>(Normal Paper Standalone - no proxy)</gray>"));
            }
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtil.format("<red>Console cannot switch servers.</red>"));
            return true;
        }
        Player player = (Player)sender;
        String target = args[0];
        if (this.proxyManager.isProxyActive()) {
            this.proxyManager.connectPlayer(player, target);
        } else {
            player.sendMessage(MessageUtil.format("<gradient:#6366f1:#a855f7>[Moonlight]</gradient> <yellow>Standalone Paper Mode: Cannot switch to '" + target + "' because this server is running independently without Velocity.</yellow>"));
        }
        return true;
    }

    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1 && this.proxyManager.isProxyActive()) {
            return List.of("lobby", "survival", "hub", "creative", "minigames");
        }
        return Collections.emptyList();
    }
}

