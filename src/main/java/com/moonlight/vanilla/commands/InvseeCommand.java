package com.moonlight.vanilla.commands;

import com.moonlight.vanilla.MoonlightVanillaSurvivalCore;
import com.moonlight.vanilla.modules.audit.gui.SnapshotInspectionGui;
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
 * /inv <player> (alias: /invsee)
 * Opens an InteractiveChat-style live inventory inspection menu with real-time equipment, XP levels, and player head.
 * Allows server staff to inspect and take items out with live sync to the target player.
 */
public class InvseeCommand implements CommandExecutor, TabCompleter {
    private final MoonlightVanillaSurvivalCore plugin;

    public InvseeCommand(MoonlightVanillaSurvivalCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (!player.hasPermission("moonlight.audit.invsee") && !player.hasPermission("moonlight.admin") && !player.hasPermission("moonlight.core.admin") && !player.isOp()) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>No permission to inspect inventories.</red>"));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Usage: /inv <player></red>"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<red>Player <white>" + args[0] + "</white> is not online. Use <yellow>/logs " + args[0] + "</yellow> to view historical snapshots.</red>"));
            return true;
        }

        // Open InteractiveChat-style live inspection GUI
        SnapshotInspectionGui gui = new SnapshotInspectionGui(
                plugin,
                target.getName(),
                "live",
                "Now (Live)",
                target.getInventory().getStorageContents(),
                target.getInventory().getArmorContents(),
                target.getInventory().getItemInOffHand(),
                target.getLevel(),
                target.getTotalExperience(),
                target.getHealth(),
                target.getFoodLevel(),
                target.getGameMode().name(),
                true,
                target
        );

        player.openInventory(gui.getInventory());
        player.sendMessage(MiniMessage.miniMessage().deserialize("<green>Opened live inventory of <white>" + target.getName() + "</white>.</green>"));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            String q = args[0].toLowerCase();
            List<String> r = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(q)) {
                    r.add(p.getName());
                }
            }
            return r;
        }
        return Collections.emptyList();
    }
}
