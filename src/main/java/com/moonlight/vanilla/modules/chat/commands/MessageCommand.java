/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.TextComponent
 *  net.kyori.adventure.text.event.ClickEvent
 *  net.kyori.adventure.text.event.HoverEvent
 *  net.kyori.adventure.text.event.HoverEventSource
 *  net.kyori.adventure.text.format.NamedTextColor
 *  net.kyori.adventure.text.format.TextColor
 *  org.bukkit.Bukkit
 *  org.bukkit.Sound
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.entity.Player
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package com.moonlight.vanilla.modules.chat.commands;

import com.moonlight.vanilla.MoonlightVanillaSurvivalCore;
import com.moonlight.vanilla.modules.chat.listeners.ChatAndJoinListener;
import com.moonlight.vanilla.utils.MessageUtil;
import com.moonlight.vanilla.utils.VanishUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.event.HoverEventSource;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MessageCommand
implements CommandExecutor,
TabCompleter {
    private final MoonlightVanillaSurvivalCore plugin;
    private final Map<UUID, UUID> lastMessaged = new ConcurrentHashMap<UUID, UUID>();

    public MessageCommand(MoonlightVanillaSurvivalCore plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player) {
            Player p = (Player)sender;
            if (this.plugin.getMuteManager() != null && this.plugin.getMuteManager().isMuted(p)) {
                sender.sendMessage(MessageUtil.format("<red>You cannot send private messages while muted.</red>"));
                return true;
            }
        }
        if (args.length < 2) {
            sender.sendMessage(MessageUtil.format("<red>Usage: /" + label + " <player> <message></red>"));
            return true;
        }
        Player target = Bukkit.getPlayer((String)args[0]);
        if (target == null || !target.isOnline() || !VanishUtil.canSee(sender, target)) {
            sender.sendMessage(MessageUtil.format("<red>Player '<yellow>" + args[0] + "</yellow>' is not online.</red>"));
            return true;
        }
        String messageText = String.join((CharSequence)" ", Arrays.copyOfRange(args, 1, args.length));
        this.sendDirectMessage(sender, target, messageText);
        return true;
    }

    public void sendDirectMessage(CommandSender sender, Player target, String text) {
        Component component;
        String senderName;
        Player p;
        if (sender instanceof Player) {
            p = (Player)sender;
            senderName = p.getName();
        } else {
            senderName = "CONSOLE";
        }
        if (sender instanceof Player && (p = (Player)sender).getUniqueId().equals(target.getUniqueId())) {
            Component selfClickable = ((TextComponent)Component.text((String)p.getName(), (TextColor)NamedTextColor.GREEN).clickEvent(ClickEvent.suggestCommand((String)("/msg " + p.getName() + " ")))).hoverEvent((HoverEventSource)HoverEvent.showText((Component)ChatAndJoinListener.createPlayerHoverCard(p)));
            Component selfMsg = ((TextComponent)((TextComponent)((TextComponent)((TextComponent)Component.text((String)"[", (TextColor)NamedTextColor.GRAY).append((Component)Component.text((String)"You", (TextColor)NamedTextColor.LIGHT_PURPLE))).append((Component)Component.text((String)" -> ", (TextColor)NamedTextColor.DARK_GRAY))).append(selfClickable)).append((Component)Component.text((String)"] ", (TextColor)NamedTextColor.GRAY))).append((Component)Component.text((String)text, (TextColor)NamedTextColor.GOLD));
            p.sendMessage(selfMsg);
            p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.2f);
            this.lastMessaged.put(p.getUniqueId(), p.getUniqueId());
            return;
        }
        Component targetHover = ChatAndJoinListener.createPlayerHoverCard(target);
        Component targetClickable = ((TextComponent)Component.text((String)target.getName(), (TextColor)NamedTextColor.GREEN).clickEvent(ClickEvent.suggestCommand((String)("/msg " + target.getName() + " ")))).hoverEvent((HoverEventSource)HoverEvent.showText((Component)targetHover));
        Component toSender = ((TextComponent)((TextComponent)((TextComponent)((TextComponent)Component.text((String)"[", (TextColor)NamedTextColor.GRAY).append((Component)Component.text((String)"You", (TextColor)NamedTextColor.LIGHT_PURPLE))).append((Component)Component.text((String)" -> ", (TextColor)NamedTextColor.DARK_GRAY))).append(targetClickable)).append((Component)Component.text((String)"] ", (TextColor)NamedTextColor.GRAY))).append((Component)Component.text((String)text, (TextColor)NamedTextColor.GOLD));
        if (sender instanceof Player) {
            Player p2 = (Player)sender;
            component = ChatAndJoinListener.createPlayerHoverCard(p2);
        } else {
            component = MessageUtil.format("<white>Server Console</white>");
        }
        Component senderHover = component;
        Component senderClickable = ((TextComponent)Component.text((String)senderName, (TextColor)NamedTextColor.GREEN).clickEvent(ClickEvent.suggestCommand((String)("/msg " + senderName + " ")))).hoverEvent((HoverEventSource)HoverEvent.showText((Component)senderHover));
        Component toReceiver = ((TextComponent)((TextComponent)((TextComponent)((TextComponent)Component.text((String)"[", (TextColor)NamedTextColor.GRAY).append(senderClickable)).append((Component)Component.text((String)" -> ", (TextColor)NamedTextColor.DARK_GRAY))).append((Component)Component.text((String)"You", (TextColor)NamedTextColor.LIGHT_PURPLE))).append((Component)Component.text((String)"] ", (TextColor)NamedTextColor.GRAY))).append((Component)Component.text((String)text, (TextColor)NamedTextColor.GOLD));
        sender.sendMessage(toSender);
        target.sendMessage(toReceiver);
        target.playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.2f);
        if (sender instanceof Player) {
            Player p3 = (Player)sender;
            this.lastMessaged.put(p3.getUniqueId(), target.getUniqueId());
            this.lastMessaged.put(target.getUniqueId(), p3.getUniqueId());
        }
    }

    public UUID getLastMessaged(UUID uuid) {
        return this.lastMessaged.get(uuid);
    }

    public void clearHistory(UUID uuid) {
        this.lastMessaged.remove(uuid);
        this.lastMessaged.values().removeIf(id -> id.equals(uuid));
    }

    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            String query = args[0].toLowerCase();
            ArrayList<String> matches = new ArrayList<String>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!VanishUtil.canSee(sender, p) || !p.getName().toLowerCase().startsWith(query)) continue;
                matches.add(p.getName());
            }
            return matches;
        }
        return Collections.emptyList();
    }
}

