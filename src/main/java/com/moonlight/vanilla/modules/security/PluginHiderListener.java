/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerCommandPreprocessEvent
 *  org.bukkit.event.player.PlayerCommandSendEvent
 *  org.bukkit.event.server.TabCompleteEvent
 */
package com.moonlight.vanilla.modules.security;

import com.moonlight.vanilla.MoonlightVanillaSurvivalCore;
import com.moonlight.vanilla.utils.MessageUtil;
import java.util.List;
import java.util.Locale;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.event.server.TabCompleteEvent;

public class PluginHiderListener
implements Listener {
    private final MoonlightVanillaSurvivalCore plugin;
    private static final List<String> HIDDEN_PLUGINS = List.of("viaversion", "viabackwards", "viarewind", "geyser-spigot", "geyser", "floodgate", "viaversion-api");

    public PluginHiderListener(MoonlightVanillaSurvivalCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (player.isOp() || player.hasPermission("moonlight.admin") || player.hasPermission("bukkit.command.plugins")) {
            return;
        }
        if (!this.plugin.getConfig().getBoolean("security.plugin-hider.enabled", false)) {
            return;
        }
        String msg = event.getMessage().toLowerCase(Locale.ROOT).trim();
        String command = msg.startsWith("/") ? msg.substring(1) : msg;
        String[] parts = command.split(" ");
        String label = parts[0].toLowerCase(Locale.ROOT);
        if (label.equals("plugins") || label.equals("pl") || label.equals("bukkit:plugins") || label.equals("bukkit:pl") || label.equals("icanhasbukkit") || label.equals("bukkit:about") || label.equals("about") || label.equals("paper") && parts.length > 1 && parts[1].equalsIgnoreCase("plugins")) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(MessageUtil.format("<white>Plugins (1): </white><green>MoonlightVanillaSurvivalCore</green>"));
            return;
        }
        if (label.equals("version") || label.equals("ver") || label.equals("bukkit:version") || label.equals("bukkit:ver")) {
            if (parts.length > 1) {
                String targetPlugin = parts[1].toLowerCase(Locale.ROOT);
                for (String hidden : HIDDEN_PLUGINS) {
                    if (!targetPlugin.contains(hidden)) continue;
                    event.setCancelled(true);
                    event.getPlayer().sendMessage(MessageUtil.format("<red>Unknown plugin. Type '/help' for help.</red>"));
                    return;
                }
            } else {
                event.setCancelled(true);
                event.getPlayer().sendMessage(MessageUtil.format("<white>This server is running Paper version 1.21.4 (Git: Paper-26.2)</white>"));
                return;
            }
        }
        for (String hidden : HIDDEN_PLUGINS) {
            if (!label.equals(hidden) && !label.startsWith(hidden + ":")) continue;
            event.setCancelled(true);
            event.getPlayer().sendMessage(MessageUtil.format("<red>Unknown command. Type '/help' for help.</red>"));
            return;
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onCommandSend(PlayerCommandSendEvent event) {
        Player player = event.getPlayer();
        if (player.isOp() || player.hasPermission("moonlight.admin")) {
            return;
        }
        if (!this.plugin.getConfig().getBoolean("security.plugin-hider.enabled", false)) {
            return;
        }
        event.getCommands().removeIf(cmd -> {
            String lower = cmd.toLowerCase(Locale.ROOT);
            for (String hidden : HIDDEN_PLUGINS) {
                if (!lower.equals(hidden) && !lower.startsWith(hidden + ":")) continue;
                return true;
            }
            return false;
        });
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onTabComplete(TabCompleteEvent event) {
        if (event.getSender().isOp() || event.getSender().hasPermission("moonlight.admin")) {
            return;
        }
        if (!this.plugin.getConfig().getBoolean("security.plugin-hider.enabled", false)) {
            return;
        }
        String buffer = event.getBuffer().toLowerCase(Locale.ROOT).trim();
        if (buffer.startsWith("/ver ") || buffer.startsWith("/version ") || buffer.startsWith("/bukkit:ver ") || buffer.startsWith("/bukkit:version ")) {
            event.getCompletions().removeIf(completion -> {
                String lower = completion.toLowerCase(Locale.ROOT);
                for (String hidden : HIDDEN_PLUGINS) {
                    if (!lower.contains(hidden)) continue;
                    return true;
                }
                return false;
            });
        }
    }
}

