package com.moonlight.vanilla.commands;

import com.moonlight.vanilla.MoonlightVanillaSurvivalCore;
import com.moonlight.vanilla.modules.audit.gui.EnderChestInspectionGui;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * /ec [player] (alias: /enderchest)
 * If no args: opens player's own Ender Chest.
 * If target specified: opens live Ender Chest inspector with take-only protection and live sync.
 */
public class EnderChestCommand implements CommandExecutor, TabCompleter {
    private final MoonlightVanillaSurvivalCore plugin;

    public EnderChestCommand(MoonlightVanillaSurvivalCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (args.length == 0) {
            if (player.hasPermission("moonlight.ec") || player.isOp()) {
                player.openInventory(player.getEnderChest());
                return true;
            } else {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Usage: /ec <player></red>"));
                return true;
            }
        }

        if (!player.hasPermission("moonlight.audit.enderchest") && !player.hasPermission("moonlight.core.admin") && !player.hasPermission("moonlight.admin") && !player.isOp()) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>No permission to inspect other players' ender chests.</red>"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<red>Player <white>" + args[0] + "</white> is not online. Use <yellow>/logs ec " + args[0] + "</yellow> to view historical snapshots.</red>"));
            return true;
        }

        EnderChestInspectionGui gui = new EnderChestInspectionGui(
                plugin,
                target.getName(),
                "live",
                "Now (Live)",
                target.getEnderChest().getContents(),
                true,
                target
        );
        player.openInventory(gui.getInventory());
        player.sendMessage(MiniMessage.miniMessage().deserialize("<green>Opened live Ender Chest of <white>" + target.getName() + "</white>.</green>"));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            String q = args[0].toLowerCase();
            List<String> r = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(q)) r.add(p.getName());
            }
            return r;
        }
        return Collections.emptyList();
    }
}
