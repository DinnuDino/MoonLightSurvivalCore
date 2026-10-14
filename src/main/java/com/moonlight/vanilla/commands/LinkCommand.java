package com.moonlight.vanilla.commands;

import com.moonlight.vanilla.MoonlightVanillaSurvivalCore;
import com.moonlight.vanilla.utils.MessageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * LinkCommand
 * Provides interactive clickable chat links for /weblink and /discord.
 * Automatically prompts to open the URL in the player's web browser when clicked.
 */
public class LinkCommand implements CommandExecutor, TabCompleter {

    private final MoonlightVanillaSurvivalCore plugin;

    public LinkCommand(MoonlightVanillaSurvivalCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        String name = command.getName().toLowerCase();
        String lbl = label.toLowerCase();

        if (name.equals("link") || name.equals("weblink") || name.equals("website") || name.equals("site") || name.equals("web")
                || lbl.equals("link") || lbl.equals("weblink") || lbl.equals("website") || lbl.equals("site") || lbl.equals("web")) {
            // If someone is using /link <code> for DiscordSRV linking, forward to DiscordSRV
            if (name.equals("link") || lbl.equals("link")) {
                if (args.length > 0) {
                    Command dsLink = org.bukkit.Bukkit.getCommandMap().getCommand("discordsrv:link");
                    if (dsLink != null) {
                        return dsLink.execute(sender, label, args);
                    }
                }
            }

            if (sender.isPermissionSet("moonlight.link") && !sender.hasPermission("moonlight.link")) {
                sender.sendMessage(MessageUtil.format("&9MoonLight &8❙ &cYou are forbidden to use this."));
                return true;
            }
            sendWebsiteLink(sender);
            return true;
        }

        if (name.equals("discord") || name.equals("dc") || name.equals("dclink") || name.equals("discordlink")
                || lbl.equals("discord") || lbl.equals("dc") || lbl.equals("dclink") || lbl.equals("discordlink")) {
            // If subcommands are passed (e.g. /discord link 1234), forward to DiscordSRV
            if (args.length > 0) {
                Command dsCmd = org.bukkit.Bukkit.getCommandMap().getCommand("discordsrv:discord");
                if (dsCmd != null) {
                    return dsCmd.execute(sender, label, args);
                }
            }

            if (sender.isPermissionSet("moonlight.discord") && !sender.hasPermission("moonlight.discord")) {
                sender.sendMessage(MessageUtil.format("&9MoonLight &8❙ &cYou are forbidden to use this."));
                return true;
            }
            sendDiscordLink(sender);
            return true;
        }

        return true;
    }

    public void sendWebsiteLink(CommandSender sender) {
        String url = plugin.getConfig().getString("links.website", "https://www.moonlightsmp.info/");

        Component header = MessageUtil.format("<gradient:#38bdf8:#6366f1><bold>▶ Moonlight Website</bold></gradient>");
        Component desc = MessageUtil.format("<gray>Click the link below to visit our official website:</gray>");

        Component linkComp = MessageUtil.format("<aqua><underlined>" + url + "</underlined></aqua>")
                .clickEvent(ClickEvent.openUrl(url))
                .hoverEvent(HoverEvent.showText(MessageUtil.format("<yellow>Click to open: </yellow><aqua>" + url + "</aqua>")));

        Component button = MessageUtil.format(" <gradient:#38bdf8:#6366f1><bold>[VISIT WEBSITE]</bold></gradient>")
                .clickEvent(ClickEvent.openUrl(url))
                .hoverEvent(HoverEvent.showText(MessageUtil.format("<green>Click to open in your browser!</green>")));

        sender.sendMessage(Component.empty());
        sender.sendMessage(header);
        sender.sendMessage(desc);
        sender.sendMessage(linkComp.append(button));
        sender.sendMessage(Component.empty());

        if (sender instanceof Player player) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
        }
    }

    public void sendDiscordLink(CommandSender sender) {
        String url = plugin.getConfig().getString("links.discord", "https://discord.gg/BVEa5UbCHg");

        Component header = MessageUtil.format("<gradient:#5865f2:#818cf8><bold>▶ Moonlight Discord</bold></gradient>");
        Component desc = MessageUtil.format("<gray>Click the link below to join our official Discord community:</gray>");

        Component linkComp = MessageUtil.format("<aqua><underlined>" + url + "</underlined></aqua>")
                .clickEvent(ClickEvent.openUrl(url))
                .hoverEvent(HoverEvent.showText(MessageUtil.format("<yellow>Click to join: </yellow><aqua>" + url + "</aqua>")));

        Component button = MessageUtil.format(" <gradient:#5865f2:#818cf8><bold>[JOIN DISCORD]</bold></gradient>")
                .clickEvent(ClickEvent.openUrl(url))
                .hoverEvent(HoverEvent.showText(MessageUtil.format("<green>Click to join our Discord server!</green>")));

        sender.sendMessage(Component.empty());
        sender.sendMessage(header);
        sender.sendMessage(desc);
        sender.sendMessage(linkComp.append(button));
        sender.sendMessage(Component.empty());

        if (sender instanceof Player player) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
