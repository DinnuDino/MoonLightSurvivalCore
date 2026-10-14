package com.moonlight.vanilla.modules.world.commands;

import com.moonlight.vanilla.MoonlightVanillaSurvivalCore;
import com.moonlight.vanilla.utils.MessageUtil;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SetSpawnCommand implements CommandExecutor {

    private final MoonlightVanillaSurvivalCore plugin;

    public SetSpawnCommand(MoonlightVanillaSurvivalCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.format("<red>Only players can execute /setspawn.</red>"));
            return true;
        }

        if (!player.hasPermission("moonlight.setspawn") && !player.hasPermission("moonlight.admin") && !player.isOp()) {
            player.sendMessage(MessageUtil.format("<gradient:#6366f1:#a855f7>[Moonlight]</gradient> <red>You do not have permission to set the server spawn.</red>"));
            return true;
        }

        Location loc = player.getLocation();
        plugin.getSpawnManager().setSpawn(loc);

        // Also update the Bukkit world spawn for completeness
        try {
            loc.getWorld().setSpawnLocation(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), loc.getYaw());
        } catch (Throwable ignored) {}

        player.sendMessage(MessageUtil.format(
                "<gradient:#6366f1:#a855f7>[Moonlight]</gradient> <green>Global server spawn point successfully updated at: " +
                "<yellow>" + loc.getWorld().getName() + "</yellow> (<aqua>" +
                String.format("%.1f", loc.getX()) + ", " +
                String.format("%.1f", loc.getY()) + ", " +
                String.format("%.1f", loc.getZ()) + "</aqua>)!</green>"
        ));

        plugin.getLogger().info("Global spawn set by " + player.getName() + " at " + loc.getWorld().getName() + " (" + loc.getX() + ", " + loc.getY() + ", " + loc.getZ() + ")");
        return true;
    }
}
