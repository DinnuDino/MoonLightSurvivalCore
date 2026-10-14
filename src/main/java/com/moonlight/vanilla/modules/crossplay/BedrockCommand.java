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

public class BedrockCommand
implements CommandExecutor,
TabCompleter {
    private final MoonlightVanillaSurvivalCore plugin;

    public BedrockCommand(MoonlightVanillaSurvivalCore plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("info")) {
            Player target = args.length > 1 ? Bukkit.getPlayer(args[1]) : (sender instanceof Player ? (Player) sender : null);
            if (target == null || !VanishUtil.canSee(sender, target)) {
                sender.sendMessage(MessageUtil.format("<red>Player not found.</red>"));
                return true;
            }
            boolean isBedrock = this.plugin.getBedrockManager().isBedrockPlayer(target);
            String device = this.plugin.getBedrockManager().getDeviceName(target);
            sender.sendMessage(MessageUtil.format("<light_purple>=== Crossplay Client Profile ===</light_purple>"));
            sender.sendMessage(MessageUtil.format("<gray>Player: <white>" + target.getName() + "</white>"));
            sender.sendMessage(MessageUtil.format("<gray>Platform: <" + (isBedrock ? "aqua" : "green") + ">" + device + "</" + (isBedrock ? "aqua" : "green") + ">"));
            sender.sendMessage(MessageUtil.format("<gray>Floodgate Auth: <white>" + (isBedrock ? "Active (No Java Account Required)" : "Mojang Java Auth") + "</white>"));
            sender.sendMessage(MessageUtil.format("<gray>Ping: <yellow>" + target.getPing() + "ms</yellow>"));
            return true;
        }
        sender.sendMessage(MessageUtil.format("<light_purple>=== Moonlight Crossplay Engine ===</light_purple>"));
        sender.sendMessage(MessageUtil.format("<gray>Geyser-Spigot: <white>" + (this.plugin.getBedrockManager().isGeyserPresent() ? "<green>ACTIVE</green>" : "<red>STANDALONE</red>") + "</white>"));
        sender.sendMessage(MessageUtil.format("<gray>Floodgate 2.0: <white>" + (this.plugin.getBedrockManager().isFloodgatePresent() ? "<green>ACTIVE</green>" : "<red>STANDALONE</red>") + "</white>"));
        sender.sendMessage(MessageUtil.format("<gray>Supported Bedrock: <yellow>Last 10 updates (1.20.0 to 1.21.60+)</yellow>"));
        sender.sendMessage(MessageUtil.format("<gray>Use <yellow>/bedrock info [player]</yellow> to inspect client platform.</gray>"));
        return true;
    }

    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length <= 1) {
            String query = args.length == 0 ? "" : args[0].toLowerCase();
            ArrayList<String> list = new ArrayList<String>();
            for (String sub : List.of("info", "gui")) {
                if (!sub.startsWith(query)) continue;
                list.add(sub);
            }
            return list;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("info")) {
            String query = args[1].toLowerCase();
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

