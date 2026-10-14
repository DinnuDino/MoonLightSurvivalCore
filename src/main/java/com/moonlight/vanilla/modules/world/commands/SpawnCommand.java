package com.moonlight.vanilla.modules.world.commands;

import com.moonlight.vanilla.MoonlightVanillaSurvivalCore;
import com.moonlight.vanilla.utils.MessageUtil;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SpawnCommand implements CommandExecutor {

    private final MoonlightVanillaSurvivalCore plugin;

    public SpawnCommand(MoonlightVanillaSurvivalCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.format("<red>Only players can execute /spawn.</red>"));
            return true;
        }

        Location spawn = plugin.getSpawnManager().getSpawnLocation();
        if (spawn == null) {
            player.sendMessage(MessageUtil.format("<gradient:#6366f1:#a855f7>[Moonlight]</gradient> <red>Spawn location is not set yet! Ask an admin to use /setspawn.</red>"));
            return true;
        }

        // Check if player has warmup bypass
        if (player.isOp() || player.hasPermission("moonlight.admin") || player.hasPermission("moonlight.teleport.bypass.warmup")) {
            player.teleportAsync(spawn).thenAccept(success -> {
                if (success) {
                    player.sendMessage(MessageUtil.format("<gradient:#6366f1:#a855f7>[Moonlight]</gradient> <green>Teleported to spawn safely!</green>"));
                }
            });
            return true;
        }

        int warmup = plugin.getConfig().getInt("spawn.warmup-seconds", 3);
        player.sendMessage(MessageUtil.format("<gradient:#6366f1:#a855f7>[Moonlight]</gradient> <yellow>Teleporting to spawn in " + warmup + " seconds... Teleport will only cancel on heavy damage!</yellow>"));

        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                player.teleportAsync(spawn).thenAccept(success -> {
                    if (success) {
                        player.sendMessage(MessageUtil.format("<gradient:#6366f1:#a855f7>[Moonlight]</gradient> <green>Teleported to spawn safely!</green>"));
                    }
                });
            }
        }, warmup * 20L);

        return true;
    }
}
