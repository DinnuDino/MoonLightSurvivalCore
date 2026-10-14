/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.OfflinePlayer
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
import java.util.Iterator;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class DelHomeCommand
implements CommandExecutor,
TabCompleter {
    private final MoonlightVanillaSurvivalCore plugin;

    public DelHomeCommand(MoonlightVanillaSurvivalCore plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        Player player;
        boolean isPlayer = sender instanceof Player;
        Player player2 = player = isPlayer ? (Player)sender : null;
        if (isPlayer && !SetHomeCommand.canUseHomes(player)) {
            sender.sendMessage(MessageUtil.format("&9MoonLight &8\u2759 &cAccess denied: You do not have permission to remove homes."));
            return true;
        }
        if (args.length >= 2) {
            boolean isAdmin;
            boolean bl = isAdmin = !isPlayer || player.isOp() || player.hasPermission("moonlight.admin") || player.hasPermission("moonlight.staff");
            if (!isAdmin) {
                sender.sendMessage(MessageUtil.format("&9MoonLight &8\u2759 &cYou do not have permission to remove homes for other players."));
                return true;
            }
            String targetPlayerName = args[0];
            String homeName = args[1];
            OfflinePlayer target = Bukkit.getPlayer(targetPlayerName);
            if (target == null) {
                target = Bukkit.getOfflinePlayer(targetPlayerName);
            }
            if (target.getName() == null && !target.hasPlayedBefore() && !target.isOnline()) {
                sender.sendMessage(MessageUtil.format("&9MoonLight &8\u2759 &cPlayer '&e" + targetPlayerName + "&c' not found."));
                return true;
            }
            OfflinePlayer finalTarget = target;
            if (homeName.equalsIgnoreCase("all")) {
                this.plugin.getHomeManager().deleteAllHomes(target.getUniqueId()).thenAccept(arg_0 -> this.lambda$onCommand$0(sender, (OfflinePlayer)finalTarget, arg_0));
            } else {
                this.plugin.getHomeManager().deleteHome(target.getUniqueId(), homeName).thenAccept(arg_0 -> this.lambda$onCommand$2(sender, homeName, (OfflinePlayer)finalTarget, arg_0));
            }
            return true;
        }
        if (!isPlayer) {
            sender.sendMessage("Console usage: /delhome <player> <homeName|all>");
            return true;
        }
        if (args.length == 0) {
            this.plugin.getHomeManager().getHomes(player.getUniqueId()).thenAccept(homes -> Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
                if (!player.isOnline()) {
                    return;
                }
                if (homes.isEmpty()) {
                    player.sendMessage(MessageUtil.format("&9MoonLight &8\u2759 &eYou do not have any saved homes to remove."));
                    return;
                }
                if (homes.size() == 1) {
                    String singleName = ((Home)homes.get(0)).getName();
                    this.plugin.getHomeManager().deleteHome(player.getUniqueId(), singleName).thenAccept(deleted -> Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
                        if (player.isOnline()) {
                            player.sendMessage(MessageUtil.format("&9MoonLight &8\u2759 &aSuccessfully removed home '&e" + singleName + "&a'."));
                        }
                    }));
                    return;
                }
                player.sendMessage(MessageUtil.format("<gradient:#ef4444:#f59e0b><bold>=== Remove a Home (Click to Delete) ===</bold></gradient>"));
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < homes.size(); ++i) {
                    Home h = (Home)homes.get(i);
                    sb.append("<click:run_command:'/delhome ").append(h.getName()).append("'>").append("<hover:show_text:'<red>Click to permanently delete home <yellow>").append(h.getName()).append("</yellow>!</red>'>").append("<red><bold>[\u2716 ").append(h.getName()).append("]</bold></red></hover></click>");
                    if (i >= homes.size() - 1) continue;
                    sb.append("<gray>, </gray>");
                }
                player.sendMessage(MessageUtil.format(sb.toString()));
                player.sendMessage(MessageUtil.format("&7Tip: Click any &c[\u2716]&7 above, or type &e/delhome <name>&7 or &e/delhome all&7."));
            }));
            return true;
        }
        String homeArg = args[0];
        if (homeArg.equalsIgnoreCase("all")) {
            this.plugin.getHomeManager().deleteAllHomes(player.getUniqueId()).thenAccept(count -> Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
                if (player.isOnline()) {
                    if (count > 0) {
                        player.sendMessage(MessageUtil.format("&9MoonLight &8\u2759 &aSuccessfully removed all &e" + count + "&a of your saved homes."));
                    } else {
                        player.sendMessage(MessageUtil.format("&9MoonLight &8\u2759 &eYou do not have any saved homes to remove."));
                    }
                }
            }));
            return true;
        }
        this.plugin.getHomeManager().deleteHome(player.getUniqueId(), homeArg).thenAccept(deleted -> Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
            if (player.isOnline()) {
                if (deleted.booleanValue()) {
                    player.sendMessage(MessageUtil.format("&9MoonLight &8\u2759 &aSuccessfully removed home '&e" + homeArg + "&a'."));
                } else {
                    player.sendMessage(MessageUtil.format("&9MoonLight &8\u2759 &cHome '&e" + homeArg + "&c' does not exist. Use &e/homes&c to view your homes."));
                }
            }
        }));
        return true;
    }

    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        Player p;
        if (args.length == 1) {
            Player p2;
            String query = args[0].toLowerCase();
            ArrayList<String> list = new ArrayList<String>();
            if ("all".startsWith(query)) {
                list.add("all");
            }
            if (sender instanceof Player) {
                Player player = (Player)sender;
                List<String> homeNames = this.plugin.getHomeManager().getHomeNamesFast(player.getUniqueId());
                Iterator iterator = homeNames.iterator();
                while (iterator.hasNext()) {
                    String name = (String)iterator.next();
                    if (!name.toLowerCase().startsWith(query)) continue;
                    list.add(name);
                }
            }
            if (!(sender instanceof Player) || (p2 = (Player)sender).isOp() || p2.hasPermission("moonlight.admin") || p2.hasPermission("moonlight.staff")) {
                for (Player target : Bukkit.getOnlinePlayers()) {
                    if (!target.getName().toLowerCase().startsWith(query) || list.contains(target.getName())) continue;
                    list.add(target.getName());
                }
            }
            return list;
        }
        if (args.length == 2 && (!(sender instanceof Player) || (p = (Player)sender).isOp() || p.hasPermission("moonlight.admin") || p.hasPermission("moonlight.staff"))) {
            Player target;
            String query = args[1].toLowerCase();
            ArrayList<String> list = new ArrayList<String>();
            if ("all".startsWith(query)) {
                list.add("all");
            }
            if ((target = Bukkit.getPlayer((String)args[0])) != null) {
                List<String> homeNames = this.plugin.getHomeManager().getHomeNamesFast(target.getUniqueId());
                for (String name : homeNames) {
                    if (!name.toLowerCase().startsWith(query)) continue;
                    list.add(name);
                }
            }
            return list;
        }
        return Collections.emptyList();
    }

    private /* synthetic */ void lambda$onCommand$2(CommandSender sender, String homeName, OfflinePlayer finalTarget, Boolean deleted) {
        Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
            if (deleted.booleanValue()) {
                sender.sendMessage(MessageUtil.format("&9MoonLight &8\u2759 &aSuccessfully removed home '&e" + homeName + "&a' for player &e" + finalTarget.getName() + "&a."));
            } else {
                sender.sendMessage(MessageUtil.format("&9MoonLight &8\u2759 &cHome '&e" + homeName + "&c' not found for player &e" + finalTarget.getName() + "&c."));
            }
        });
    }

    private /* synthetic */ void lambda$onCommand$0(CommandSender sender, OfflinePlayer finalTarget, Integer count) {
        Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> sender.sendMessage(MessageUtil.format("&9MoonLight &8\u2759 &aSuccessfully removed all &e" + count + "&a homes for player &e" + finalTarget.getName() + "&a.")));
    }
}

