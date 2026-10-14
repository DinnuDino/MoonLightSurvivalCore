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
 *  org.bukkit.permissions.PermissionAttachmentInfo
 *  org.bukkit.plugin.Plugin
 *  org.jetbrains.annotations.NotNull
 */
package com.moonlight.vanilla.modules.homes.commands;

import com.moonlight.vanilla.MoonlightVanillaSurvivalCore;
import com.moonlight.vanilla.utils.MessageUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class SetHomeCommand
implements CommandExecutor,
TabCompleter {
    private final MoonlightVanillaSurvivalCore plugin;

    public SetHomeCommand(MoonlightVanillaSurvivalCore plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        String homeName;
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can set homes.");
            return true;
        }
        Player player = (Player)sender;
        if (!SetHomeCommand.canUseHomes(player)) {
            player.sendMessage(MessageUtil.format("&9MoonLight &8\u2759 &cAccess denied: You do not have permission to set homes."));
            return true;
        }
        boolean preventAir = this.plugin.getConfig().getBoolean("homes.prevent-sethome-in-air", false);
        if (preventAir && player.isFlying()) {
            player.sendMessage(MessageUtil.format("&9MoonLight &8\u2759 &cCannot set home while flying in mid-air!"));
            return true;
        }
        String string = homeName = args.length > 0 ? args[0] : "home";
        if (!homeName.matches("^[a-zA-Z0-9_-]{1,16}$")) {
            player.sendMessage(MessageUtil.format("&9MoonLight &8\u2759 &cInvalid home name. Use 1-16 alphanumeric characters."));
            return true;
        }
        int limit = this.plugin.getHomeManager().getMaxHomes(player);
        this.plugin.getHomeManager().saveHome(player, homeName, player.getLocation()).thenAccept(success -> Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            if (success.booleanValue()) {
                player.sendMessage(MessageUtil.format("&9MoonLight &8\u2759 &aHome '&e" + homeName + "&a' saved successfully!"));
            } else {
                player.sendMessage(MessageUtil.format("&9MoonLight &8\u2759 &cCannot set home '&e" + homeName + "&c' \u2014 you have reached your limit of &e" + String.valueOf(limit == -1 ? "unlimited" : Integer.valueOf(limit)) + " homes&c."));
                player.sendMessage(MessageUtil.format("&7Remove an existing home with &e/delhome <name>&7 or &e/removehome <name>&7."));
            }
        }));
        return true;
    }

    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length <= 1) {
            String query = args.length == 0 ? "" : args[0].toLowerCase();
            ArrayList<String> suggestions = new ArrayList<String>();
            for (String tag : List.of("home", "base", "farm", "shop", "mine", "nether", "end")) {
                if (!tag.startsWith(query)) continue;
                suggestions.add(tag);
            }
            return suggestions;
        }
        return Collections.emptyList();
    }

    public static boolean canUseHomes(Player player) {
        if (player.isOp() || player.hasPermission("moonlight.admin") || player.hasPermission("moonlight.player") || player.hasPermission("moonlight.homes.use") || player.hasPermission("moonlight.homes.*") || player.hasPermission("moonlight.sethome") || player.hasPermission("moonlight.home") || player.hasPermission("moonlight.delhome") || player.hasPermission("moonlight.homes") || player.hasPermission("moonlight.homes.sethome") || player.hasPermission("moonlight.homes.set") || player.hasPermission("moonlight.homes.tp") || player.hasPermission("essentials.sethome") || player.hasPermission("essentials.home") || player.hasPermission("essentials.delhome") || player.hasPermission("essentials.homes") || player.hasPermission("essentials.home.set") || player.hasPermission("cmi.command.sethome") || player.hasPermission("cmi.command.home")) {
            return true;
        }
        for (PermissionAttachmentInfo pai : player.getEffectivePermissions()) {
            if (!pai.getValue() || !pai.getPermission().toLowerCase(Locale.ROOT).startsWith("moonlight.homes.")) continue;
            return true;
        }
        if (player.isPermissionSet("moonlight.homes.use") && !player.hasPermission("moonlight.homes.use")) {
            return false;
        }
        if (player.isPermissionSet("moonlight.sethome") && !player.hasPermission("moonlight.sethome")) {
            return false;
        }
        return !player.isPermissionSet("moonlight.home") || player.hasPermission("moonlight.home");
    }
}

