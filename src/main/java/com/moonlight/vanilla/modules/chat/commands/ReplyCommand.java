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
package com.moonlight.vanilla.modules.chat.commands;

import com.moonlight.vanilla.MoonlightVanillaSurvivalCore;
import com.moonlight.vanilla.modules.chat.commands.MessageCommand;
import com.moonlight.vanilla.utils.MessageUtil;
import com.moonlight.vanilla.utils.VanishUtil;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ReplyCommand
implements CommandExecutor,
TabCompleter {
    private final MoonlightVanillaSurvivalCore plugin;
    private final MessageCommand messageCommand;

    public ReplyCommand(MoonlightVanillaSurvivalCore plugin, MessageCommand messageCommand) {
        this.plugin = plugin;
        this.messageCommand = messageCommand;
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtil.format("<red>Only players can reply to private messages.</red>"));
            return true;
        }
        Player player = (Player)sender;
        if (this.plugin.getMuteManager() != null && this.plugin.getMuteManager().isMuted(player)) {
            player.sendMessage(MessageUtil.format("<red>You cannot send private messages while muted.</red>"));
            return true;
        }
        if (args.length < 1) {
            player.sendMessage(MessageUtil.format("<red>Usage: /" + label + " <message></red>"));
            return true;
        }
        UUID targetUuid = this.messageCommand.getLastMessaged(player.getUniqueId());
        if (targetUuid == null) {
            player.sendMessage(MessageUtil.format("<red>You have nobody to reply to.</red>"));
            return true;
        }
        Player target = Bukkit.getPlayer((UUID)targetUuid);
        if (target == null || !target.isOnline() || !VanishUtil.canSee((CommandSender)player, target)) {
            player.sendMessage(MessageUtil.format("<red>That player is no longer online.</red>"));
            return true;
        }
        String messageText = String.join((CharSequence)" ", args);
        this.messageCommand.sendDirectMessage((CommandSender)player, target, messageText);
        return true;
    }

    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return Collections.emptyList();
    }
}

