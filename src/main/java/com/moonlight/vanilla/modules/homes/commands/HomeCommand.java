/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 *  org.jetbrains.annotations.NotNull
 */
package com.moonlight.vanilla.modules.homes.commands;

import com.moonlight.vanilla.MoonlightVanillaSurvivalCore;
import com.moonlight.vanilla.modules.homes.Home;
import com.moonlight.vanilla.modules.homes.commands.SetHomeCommand;
import com.moonlight.vanilla.utils.MessageUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class HomeCommand
implements CommandExecutor,
TabCompleter {
    private final MoonlightVanillaSurvivalCore plugin;

    public HomeCommand(MoonlightVanillaSurvivalCore plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by players.");
            return true;
        }
        Player player = (Player)sender;
        if (!SetHomeCommand.canUseHomes(player)) {
            player.sendMessage(MessageUtil.format("&9MoonLight &8\u2759 &cAccess denied: You do not have permission to use /home."));
            return true;
        }
        this.plugin.getHomeManager().getHomes(player.getUniqueId()).thenAccept(homes -> Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            if (homes.isEmpty()) {
                player.sendMessage(MessageUtil.format("&9MoonLight &8\u2759 &eYou do not have any homes set. Use &a/sethome [name]&e to create one."));
                return;
            }
            if (label.equalsIgnoreCase("homes")) {
                this.showHomeList(player, (List<Home>)homes);
                return;
            }
            if (args.length == 0) {
                if (homes.size() == 1) {
                    this.teleportToHome(player, (Home)homes.get(0));
                    return;
                }
                Home defaultHome = homes.stream().filter(h -> h.getName().equalsIgnoreCase("home")).findFirst().orElse(null);
                if (defaultHome != null) {
                    this.teleportToHome(player, defaultHome);
                    return;
                }
                this.showHomeList(player, (List<Home>)homes);
                return;
            }
            String targetHome = args[0];
            Home home = homes.stream().filter(h -> h.getName().equalsIgnoreCase(targetHome)).findFirst().orElse(null);
            if (home == null) {
                player.sendMessage(MessageUtil.format("&9MoonLight &8\u2759 &cHome '&e" + targetHome + "&c' was not found!"));
                this.showHomeList(player, (List<Home>)homes);
                return;
            }
            this.teleportToHome(player, home);
        }));
        return true;
    }

    public void teleportToHome(Player player, Home home) {
        Location targetLoc = this.plugin.getHomeManager().resolveHomeDestination(home);
        if (targetLoc == null) {
            player.sendMessage(MessageUtil.format("&9MoonLight &8\u2759 &cCannot teleport to home '&e" + home.getName() + "&c' due to suffocation hazard!"));
            player.sendMessage(MessageUtil.format("&7The location is completely encased in solid blocks (5x5x5 obstruction). Clear the space or relocate your home."));
            return;
        }
        if (targetLoc.getBlockY() > (int)home.getY()) {
            player.sendMessage(MessageUtil.format("&9MoonLight &8\u2759 &eNotice: Obstruction detected. Teleporting to topmost block to avoid suffocation."));
        }
        this.plugin.getTeleportManager().startWarmup(player, targetLoc, "home '&e" + home.getName() + "&7'");
    }

    public void showHomeList(Player player, List<Home> homes) {
        player.sendMessage(MessageUtil.format("<gradient:#6366f1:#a855f7><bold>=== Your Homes (" + homes.size() + ") ===</bold></gradient>"));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < homes.size(); ++i) {
            Home h = homes.get(i);
            sb.append("<click:run_command:'/home ").append(h.getName()).append("'>").append("<hover:show_text:'<gradient:#6366f1:#a855f7><bold>Home: ").append(h.getName()).append("</bold></gradient><newline><gray>World: <white>").append(h.getWorldName()).append("</white><newline>Coords: <yellow>").append(String.format("%.1f", h.getX())).append(", ").append(String.format("%.1f", h.getY())).append(", ").append(String.format("%.1f", h.getZ())).append("</yellow><newline><green>Click to teleport!</green>'>").append("<aqua><bold>[").append(h.getName()).append("]</bold></aqua></hover></click>");
            if (i >= homes.size() - 1) continue;
            sb.append("<gray>, </gray>");
        }
        player.sendMessage(MessageUtil.format(sb.toString()));
        player.sendMessage(MessageUtil.format("<gray>Tip: Click any home above or type <yellow>/home <press TAB></yellow> to preview and autocomplete.</gray>"));
    }

    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        Player player;
        block4: {
            block3: {
                if (!(sender instanceof Player)) break block3;
                player = (Player)sender;
                if (args.length <= 1) break block4;
            }
            return Collections.emptyList();
        }
        List<String> homeNames = this.plugin.getHomeManager().getHomeNamesFast(player.getUniqueId());
        String query = args.length == 0 ? "" : args[0].toLowerCase();
        ArrayList<String> matches = new ArrayList<String>();
        for (String name : homeNames) {
            if (!name.toLowerCase().startsWith(query)) continue;
            matches.add(name);
        }
        return matches;
    }
}

