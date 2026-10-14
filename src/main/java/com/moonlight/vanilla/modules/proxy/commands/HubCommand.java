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

public class HubCommand
implements CommandExecutor,
TabCompleter {
    private final MoonlightVanillaSurvivalCore plugin;
    private final ProxyManager proxyManager;

    public HubCommand(MoonlightVanillaSurvivalCore plugin, ProxyManager proxyManager) {
        this.plugin = plugin;
        this.proxyManager = proxyManager;
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtil.format("<red>Only in-game players can use /" + label + ".</red>"));
            return true;
        }
        Player player = (Player)sender;
        if (this.proxyManager.isProxyActive()) {
            player.sendMessage(MessageUtil.format("<gradient:#6366f1:#a855f7>[Moonlight]</gradient> <gray>Routing you to <yellow>" + this.proxyManager.getHubServer() + "</yellow> via Velocity proxy...</gray>"));
            this.proxyManager.sendToHub(player);
        } else {
            player.sendMessage(MessageUtil.format("<gradient:#6366f1:#a855f7>[Moonlight]</gradient> <green>Normal Paper Mode: Teleporting to world spawn...</green>"));
            player.teleportAsync(player.getWorld().getSpawnLocation());
        }
        return true;
    }

    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return Collections.emptyList();
    }
}

