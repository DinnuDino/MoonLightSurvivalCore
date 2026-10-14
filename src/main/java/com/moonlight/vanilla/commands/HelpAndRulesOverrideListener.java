package com.moonlight.vanilla.commands;

import com.destroystokyo.paper.event.server.AsyncTabCompleteEvent;
import com.moonlight.vanilla.MoonlightVanillaSurvivalCore;
import com.moonlight.vanilla.modules.wraps.Wrap;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.TabCompleteEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * HelpAndRulesOverrideListener
 * Intercepts /help, /rules, /discord, /website, /youtube, /changename, /itemwarps, and /sb
 * at the lowest event priority to eliminate third-party plugin collisions (DiscordSRV, DeluxeHub, Essentials).
 * Also powers instant Brigadier command previews and real-time tab autocompletions.
 */
public class HelpAndRulesOverrideListener implements Listener {

    private final MoonlightVanillaSurvivalCore plugin;
    private final HelpCommand helpCommand;
    private final RulesCommand rulesCommand;
    private final LinkCommand linkCommand;
    private final YouTubeCommand youTubeCommand;
    private final com.moonlight.vanilla.modules.ranks.RanksCommand ranksCommand;
    private final InvseeCommand invseeCommand;
    private final com.moonlight.vanilla.modules.wraps.WrapCommand wrapCommand;
    private final com.moonlight.vanilla.modules.scoreboard.commands.ScoreboardCommand scoreboardCommand;
    private final ChangeNameCommand changeNameCommand;
    private final com.moonlight.vanilla.modules.homes.commands.HomeCommand homeCommand;
    private final com.moonlight.vanilla.modules.homes.commands.SetHomeCommand setHomeCommand;
    private final com.moonlight.vanilla.modules.homes.commands.DelHomeCommand delHomeCommand;
    private final com.moonlight.vanilla.modules.audit.commands.LogsCommand logsCommand;
    private final com.moonlight.vanilla.modules.chat.commands.MessageCommand messageCommand;
    private final com.moonlight.vanilla.modules.chat.commands.ReplyCommand replyCommand;
    private final com.moonlight.vanilla.modules.vault.PlayerVaultCommand playerVaultCommand;

    public HelpAndRulesOverrideListener(MoonlightVanillaSurvivalCore plugin,
                                       HelpCommand helpCommand,
                                       RulesCommand rulesCommand,
                                       LinkCommand linkCommand,
                                       YouTubeCommand youTubeCommand,
                                       com.moonlight.vanilla.modules.ranks.RanksCommand ranksCommand,
                                       InvseeCommand invseeCommand,
                                       com.moonlight.vanilla.modules.wraps.WrapCommand wrapCommand,
                                       com.moonlight.vanilla.modules.scoreboard.commands.ScoreboardCommand scoreboardCommand,
                                       ChangeNameCommand changeNameCommand,
                                       com.moonlight.vanilla.modules.homes.commands.HomeCommand homeCommand,
                                       com.moonlight.vanilla.modules.homes.commands.SetHomeCommand setHomeCommand,
                                       com.moonlight.vanilla.modules.homes.commands.DelHomeCommand delHomeCommand,
                                       com.moonlight.vanilla.modules.audit.commands.LogsCommand logsCommand,
                                       com.moonlight.vanilla.modules.chat.commands.MessageCommand messageCommand,
                                       com.moonlight.vanilla.modules.chat.commands.ReplyCommand replyCommand,
                                       com.moonlight.vanilla.modules.vault.PlayerVaultCommand playerVaultCommand) {
        this.plugin = plugin;
        this.helpCommand = helpCommand;
        this.rulesCommand = rulesCommand;
        this.linkCommand = linkCommand;
        this.youTubeCommand = youTubeCommand;
        this.ranksCommand = ranksCommand;
        this.invseeCommand = invseeCommand;
        this.wrapCommand = wrapCommand;
        this.scoreboardCommand = scoreboardCommand;
        this.changeNameCommand = changeNameCommand;
        this.homeCommand = homeCommand;
        this.setHomeCommand = setHomeCommand;
        this.delHomeCommand = delHomeCommand;
        this.logsCommand = logsCommand;
        this.messageCommand = messageCommand;
        this.replyCommand = replyCommand;
        this.playerVaultCommand = playerVaultCommand;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String raw = event.getMessage().trim();
        if (!raw.startsWith("/")) return;

        String withoutSlash = raw.substring(1).trim();
        String[] parts = withoutSlash.split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) return;

        String cmd = parts[0].toLowerCase(Locale.ROOT);

        // 1. /help or any prefix variation (e.g. /bukkit:help, /minecraft:help, /paper:help, /deluxehub:help)
        if (cmd.equals("help") || cmd.equals("bukkit:help") || cmd.equals("minecraft:help")
                || cmd.equals("paper:help") || cmd.equals("deluxehub:help") || cmd.equals("mhelp") || cmd.equals("guide") || cmd.equals("playerguide")) {
            event.setCancelled(true);
            String[] args = parts.length > 1 ? Arrays.copyOfRange(parts, 1, parts.length) : new String[0];
            helpCommand.executeHelp(event.getPlayer(), args);
            return;
        }

        // 2. /rules or any prefix variation
        if (cmd.equals("rules") || cmd.equals("rulebook") || cmd.equals("mrules") || cmd.equals("mrulebook")
                || cmd.equals("bukkit:rules") || cmd.equals("essentials:rules") || cmd.equals("essentials:rulebook")
                || cmd.equals("paper:rules") || cmd.equals("moonlight:rules") || cmd.equals("moonlight:rulebook")
                || cmd.endsWith(":rules") || cmd.endsWith(":rulebook")) {
            event.setCancelled(true);
            String[] args = parts.length > 1 ? Arrays.copyOfRange(parts, 1, parts.length) : new String[0];
            rulesCommand.executeRules(event.getPlayer(), args);
            return;
        }

        // 3. /discord, /dc, /dclink, /discordlink, /discordsrv:discord, /mdiscord
        if (cmd.equals("discord") || cmd.equals("dc") || cmd.equals("dclink") || cmd.equals("discordlink") || cmd.equals("discordsrv:discord") || cmd.equals("mdiscord")) {
            if (parts.length > 1) {
                String sub = parts[1].toLowerCase(Locale.ROOT);
                if (sub.equals("link") || sub.equals("unlink") || sub.equals("bcast") || sub.equals("broadcast") || sub.equals("toggle")) {
                    return;
                }
            }
            event.setCancelled(true);
            if (linkCommand != null) {
                linkCommand.sendDiscordLink(event.getPlayer());
            }
            return;
        }

        // 4. /website, /link, /weblink, /site, /web, /mwebsite, /mweblink
        if (cmd.equals("website") || cmd.equals("link") || cmd.equals("weblink") || cmd.equals("site") || cmd.equals("web") || cmd.equals("mwebsite") || cmd.equals("mweblink")) {
            if (cmd.equals("link") && parts.length > 1) {
                return;
            }
            event.setCancelled(true);
            if (linkCommand != null) {
                linkCommand.sendWebsiteLink(event.getPlayer());
            }
            return;
        }

        // 5. /youtube, /yt, /videos, /youtubers, /myoutube, /addyoutube, /addyt, /removeyt, /removeyoutube, /delyt, /removelink, /ytpriority
        if (cmd.equals("youtube") || cmd.equals("yt") || cmd.equals("videos") || cmd.equals("youtubelinks")
                || cmd.equals("youtubers") || cmd.equals("myoutube") || cmd.equals("addyoutube") || cmd.equals("addyt")
                || cmd.equals("removeyoutube") || cmd.equals("removeyt") || cmd.equals("delyoutube") || cmd.equals("delyt")
                || cmd.equals("removelink") || cmd.equals("dellink") || cmd.equals("ytpriority") || cmd.equals("setytpriority")
                || cmd.equals("moonlight:yt") || cmd.equals("moonlight:youtube") || cmd.equals("moonlight:ytpriority")) {
            event.setCancelled(true);
            if (youTubeCommand != null) {
                String[] args = parts.length > 1 ? Arrays.copyOfRange(parts, 1, parts.length) : new String[0];
                youTubeCommand.onCommand(event.getPlayer(), null, cmd, args);
            }
            return;
        }

        // 6. /ranks, /rank, /store, /buy, /paidranks, /donateranks, /donorranks, /webstore
        if (cmd.equals("ranks") || cmd.equals("rank") || cmd.equals("store") || cmd.equals("buy")
                || cmd.equals("paidranks") || cmd.equals("donateranks") || cmd.equals("donorranks") || cmd.equals("webstore")
                || cmd.equals("moonlight:ranks") || cmd.equals("moonlight:store") || cmd.equals("moonlight:buy")) {
            event.setCancelled(true);
            if (ranksCommand != null) {
                String[] args = parts.length > 1 ? Arrays.copyOfRange(parts, 1, parts.length) : new String[0];
                ranksCommand.onCommand(event.getPlayer(), null, cmd, args);
            }
            return;
        }

        // 7. /inv, /invsee, /openinv
        if (cmd.equals("inv") || cmd.equals("invsee") || cmd.equals("openinv")
                || cmd.equals("moonlight:inv") || cmd.equals("moonlight:invsee")
                || cmd.equals("essentials:invsee") || cmd.equals("openinv:openinv")) {
            event.setCancelled(true);
            if (invseeCommand != null) {
                String[] args = parts.length > 1 ? Arrays.copyOfRange(parts, 1, parts.length) : new String[0];
                invseeCommand.onCommand(event.getPlayer(), null, cmd, args);
            }
            return;
        }

        // 8. /item warps, /item wraps, /item warp, /item wrap, /itemwarps, /itemwarp, /itemwraps, /itemwrap, /wraps, /wrap
        if (cmd.equals("item") && parts.length > 1 && (
                parts[1].equalsIgnoreCase("warps") || parts[1].equalsIgnoreCase("wraps")
                || parts[1].equalsIgnoreCase("warp") || parts[1].equalsIgnoreCase("wrap"))) {
            event.setCancelled(true);
            if (wrapCommand != null) {
                String[] args = parts.length > 2 ? Arrays.copyOfRange(parts, 2, parts.length) : new String[0];
                wrapCommand.onCommand(event.getPlayer(), null, "itemwarps", args);
            }
            return;
        }

        if (cmd.equals("itemwarps") || cmd.equals("itemwraps") || cmd.equals("itemwarp") || cmd.equals("itemwrap")
                || cmd.equals("wraps") || cmd.equals("wrap")
                || cmd.equals("mitemwarps") || cmd.equals("mitemwarp") || cmd.equals("mwraps") || cmd.equals("mwrap")
                || cmd.equals("moonlight:itemwarps") || cmd.equals("moonlight:itemwarp") || cmd.equals("moonlight:wraps") || cmd.equals("moonlight:wrap")) {
            event.setCancelled(true);
            if (wrapCommand != null) {
                String[] args = parts.length > 1 ? Arrays.copyOfRange(parts, 1, parts.length) : new String[0];
                wrapCommand.onCommand(event.getPlayer(), null, cmd, args);
            }
            return;
        }

        // 9. /changename, /mchangename, /setname, /nickplayer
        if (cmd.equals("changename") || cmd.equals("mchangename") || cmd.equals("setname") || cmd.equals("nickplayer")
                || cmd.equals("moonlight:changename") || cmd.equals("moonlight:mchangename") || cmd.equals("moonlight:setname")) {
            event.setCancelled(true);
            if (changeNameCommand != null) {
                String[] args = parts.length > 1 ? Arrays.copyOfRange(parts, 1, parts.length) : new String[0];
                changeNameCommand.onCommand(event.getPlayer(), null, cmd, args);
            }
            return;
        }

        // 10. Scoreboard commands: /sb, /sbtoggle, /scoreboard
        if (cmd.equals("sb") || cmd.equals("sbtoggle") || cmd.equals("scoreboard") || cmd.equals("msb") || cmd.equals("msbtoggle")
                || cmd.equals("moonlight:sb") || cmd.equals("moonlight:scoreboard")) {
            event.setCancelled(true);
            if (scoreboardCommand != null) {
                String[] args = parts.length > 1 ? Arrays.copyOfRange(parts, 1, parts.length) : new String[0];
                scoreboardCommand.onCommand(event.getPlayer(), null, cmd, args);
            }
            return;
        }

        // 11. /home, /homes, /mhome, /mhomes
        if (cmd.equals("home") || cmd.equals("homes") || cmd.equals("mhome") || cmd.equals("mhomes")
                || cmd.equals("moonlight:home") || cmd.equals("moonlight:homes")) {
            event.setCancelled(true);
            if (homeCommand != null) {
                String[] args = parts.length > 1 ? Arrays.copyOfRange(parts, 1, parts.length) : new String[0];
                homeCommand.onCommand(event.getPlayer(), null, cmd, args);
            }
            return;
        }

        // 12. /sethome, /msethome
        if (cmd.equals("sethome") || cmd.equals("msethome") || cmd.equals("moonlight:sethome")) {
            event.setCancelled(true);
            if (setHomeCommand != null) {
                String[] args = parts.length > 1 ? Arrays.copyOfRange(parts, 1, parts.length) : new String[0];
                setHomeCommand.onCommand(event.getPlayer(), null, cmd, args);
            }
            return;
        }

        // 13. /delhome, /removehome, /deletehome, /rmhome, /remhome, /removehomes, /delhomes
        if (cmd.equals("delhome") || cmd.equals("removehome") || cmd.equals("deletehome")
                || cmd.equals("rmhome") || cmd.equals("remhome") || cmd.equals("removehomes") || cmd.equals("delhomes")
                || cmd.equals("mdelhome") || cmd.equals("mremovehome") || cmd.equals("moonlight:delhome") || cmd.equals("moonlight:removehome")) {
            event.setCancelled(true);
            if (delHomeCommand != null) {
                String[] args = parts.length > 1 ? Arrays.copyOfRange(parts, 1, parts.length) : new String[0];
                delHomeCommand.onCommand(event.getPlayer(), null, cmd, args);
            }
            return;
        }

        // 14. /logs, /log, /auditlogs, /paperlogs, /mlogs
        if (cmd.equals("logs") || cmd.equals("log") || cmd.equals("auditlogs") || cmd.equals("paperlogs") || cmd.equals("mlogs")
                || cmd.equals("moonlight:logs") || cmd.equals("moonlight:log") || cmd.equals("moonlight:auditlogs")) {
            event.setCancelled(true);
            if (logsCommand != null) {
                String[] args = parts.length > 1 ? Arrays.copyOfRange(parts, 1, parts.length) : new String[0];
                logsCommand.onCommand(event.getPlayer(), null, cmd, args);
            }
            return;
        }

        // 15. /msg, /tell, /w, /whisper, /pm, /m, /t, /mmsg, /mtell, /mw, /mwhisper, /mpm, etc.
        if (cmd.equals("msg") || cmd.equals("tell") || cmd.equals("w") || cmd.equals("whisper") || cmd.equals("pm")
                || cmd.equals("m") || cmd.equals("t")
                || cmd.equals("mmsg") || cmd.equals("mtell") || cmd.equals("mw") || cmd.equals("mwhisper") || cmd.equals("mpm")
                || cmd.equals("minecraft:msg") || cmd.equals("minecraft:tell") || cmd.equals("minecraft:w") || cmd.equals("minecraft:whisper")
                || cmd.equals("paper:msg") || cmd.equals("paper:tell") || cmd.equals("paper:w")
                || cmd.equals("essentials:msg") || cmd.equals("essentials:tell") || cmd.equals("essentials:w") || cmd.equals("essentials:whisper")
                || cmd.equals("essentials:pm") || cmd.equals("essentials:m") || cmd.equals("emsg") || cmd.equals("etell") || cmd.equals("ewhisper")
                || cmd.startsWith("moonlight:msg") || cmd.startsWith("moonlight:tell") || cmd.startsWith("moonlight:w")) {
            event.setCancelled(true);
            try { event.setMessage("/moonlight_handled"); } catch (Exception ignored) {}
            if (messageCommand != null) {
                String[] args = parts.length > 1 ? Arrays.copyOfRange(parts, 1, parts.length) : new String[0];
                messageCommand.onCommand(event.getPlayer(), null, cmd, args);
            }
            return;
        }

        // 16. /r, /reply, /mr, /mreply, /er, /ereply
        if (cmd.equals("r") || cmd.equals("reply") || cmd.equals("mr") || cmd.equals("mreply")
                || cmd.equals("er") || cmd.equals("ereply")
                || cmd.equals("essentials:r") || cmd.equals("essentials:reply")
                || cmd.startsWith("moonlight:r") || cmd.startsWith("moonlight:reply")) {
            event.setCancelled(true);
            try { event.setMessage("/moonlight_handled"); } catch (Exception ignored) {}
            if (replyCommand != null) {
                String[] args = parts.length > 1 ? Arrays.copyOfRange(parts, 1, parts.length) : new String[0];
                replyCommand.onCommand(event.getPlayer(), null, cmd, args);
            }
            return;
        }

        // 17. /pv, /pv1, /pv2, /pv3, /pv4, /pv5, /playervault, /vault, etc.
        if (cmd.equals("pv") || cmd.equals("playervault") || cmd.equals("vault") || cmd.equals("mpv")
                || cmd.matches("^(pv|playervault|vault|mpv)[1-5]$")
                || cmd.startsWith("moonlight:pv") || cmd.startsWith("moonlight:playervault") || cmd.startsWith("moonlight:vault")
                || cmd.startsWith("essentials:playervault") || cmd.startsWith("playervaults:pv")) {
            event.setCancelled(true);
            try { event.setMessage("/moonlight_handled"); } catch (Exception ignored) {}
            if (playerVaultCommand != null) {
                String[] args = parts.length > 1 ? Arrays.copyOfRange(parts, 1, parts.length) : new String[0];
                playerVaultCommand.onCommand(event.getPlayer(), null, cmd, args);
            }
            return;
        }
    }

    /**
     * Paper Native AsyncTabCompleteEvent for instant Brigadier command previews.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onAsyncTabComplete(AsyncTabCompleteEvent event) {
        if (!event.isCommand()) return;
        List<String> suggestions = computeTabCompletions(event.getSender(), event.getBuffer());
        if (suggestions != null) {
            event.setCompletions(suggestions);
            event.setHandled(true);
        }
    }

    /**
     * Bukkit Sync TabCompleteEvent fallback.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onSyncTabComplete(TabCompleteEvent event) {
        List<String> suggestions = computeTabCompletions(event.getSender(), event.getBuffer());
        if (suggestions != null) {
            event.setCompletions(suggestions);
        }
    }

    /**
     * Computes real-time tab completion suggestions for all server commands.
     */
    private List<String> computeTabCompletions(CommandSender sender, String buffer) {
        if (buffer == null) return null;
        String raw = buffer.startsWith("/") ? buffer.substring(1) : buffer;
        boolean endsWithSpace = buffer.endsWith(" ");
        String[] parts = raw.split("\\s+");
        if (parts.length == 0 || (parts.length == 1 && parts[0].isEmpty())) return null;

        String root = parts[0].toLowerCase(Locale.ROOT);
        int argIndex = endsWithSpace ? parts.length - 1 : parts.length - 2;
        String currentToken = endsWithSpace ? "" : parts[parts.length - 1].toLowerCase(Locale.ROOT);

        // 1. /item ...
        if (root.equals("item") || root.equals("moonlight:item")) {
            if (argIndex == 0) {
                List<String> list = new ArrayList<>();
                for (String opt : List.of("warps", "warp", "wraps", "wrap")) {
                    if (opt.startsWith(currentToken)) list.add(opt);
                }
                return list;
            } else if (argIndex == 1 && (parts[1].equalsIgnoreCase("warp") || parts[1].equalsIgnoreCase("warps") || parts[1].equalsIgnoreCase("wrap") || parts[1].equalsIgnoreCase("wraps"))) {
                List<String> list = new ArrayList<>();
                if ("remove".startsWith(currentToken)) list.add("remove");
                if ((sender.hasPermission("moonlight.admin") || sender.isOp()) && "reload".startsWith(currentToken)) {
                    list.add("reload");
                }
                if (plugin.getWrapManager() != null) {
                    if (sender instanceof Player player) {
                        ItemStack held = player.getInventory().getItemInMainHand();
                        if (held != null && !held.getType().isAir()) {
                            for (Wrap w : plugin.getWrapManager().getWrapsFor(held.getType())) {
                                if (w.getKey().toLowerCase(Locale.ROOT).startsWith(currentToken) && !list.contains(w.getKey())) {
                                    list.add(w.getKey());
                                }
                            }
                        }
                    }
                    for (Wrap w : plugin.getWrapManager().getWraps()) {
                        if (w.getKey().toLowerCase(Locale.ROOT).startsWith(currentToken) && !list.contains(w.getKey())) {
                            list.add(w.getKey());
                        }
                    }
                }
                return list;
            }
        }

        // 2. /itemwarps, /itemwarp, /itemwraps, /itemwrap, /wraps, /wrap
        if (root.equals("itemwarps") || root.equals("itemwarp") || root.equals("itemwraps") || root.equals("itemwrap")
                || root.equals("wraps") || root.equals("wrap") || root.endsWith(":itemwarps") || root.endsWith(":itemwarp")
                || root.endsWith(":itemwraps") || root.endsWith(":itemwrap") || root.endsWith(":wraps") || root.endsWith(":wrap")) {
            if (argIndex == 0) {
                List<String> list = new ArrayList<>();
                if ("remove".startsWith(currentToken)) list.add("remove");
                if ((sender.hasPermission("moonlight.admin") || sender.isOp()) && "reload".startsWith(currentToken)) {
                    list.add("reload");
                }
                if (plugin.getWrapManager() != null) {
                    if (sender instanceof Player player) {
                        ItemStack held = player.getInventory().getItemInMainHand();
                        if (held != null && !held.getType().isAir()) {
                            for (Wrap w : plugin.getWrapManager().getWrapsFor(held.getType())) {
                                if (w.getKey().toLowerCase(Locale.ROOT).startsWith(currentToken) && !list.contains(w.getKey())) {
                                    list.add(w.getKey());
                                }
                            }
                        }
                    }
                    for (Wrap w : plugin.getWrapManager().getWraps()) {
                        if (w.getKey().toLowerCase(Locale.ROOT).startsWith(currentToken) && !list.contains(w.getKey())) {
                            list.add(w.getKey());
                        }
                    }
                }
                return list;
            }
        }

        // 3. /changename, /mchangename, /setname, /nickplayer
        if (root.equals("changename") || root.equals("mchangename") || root.equals("setname") || root.equals("nickplayer")
                || root.endsWith(":changename") || root.endsWith(":setname")) {
            if (argIndex == 0) {
                List<String> list = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase(Locale.ROOT).startsWith(currentToken)) {
                        list.add(p.getName());
                    }
                }
                if ("reset".startsWith(currentToken)) list.add("reset");
                if ("list".startsWith(currentToken)) list.add("list");
                return list;
            } else if (argIndex == 1) {
                List<String> list = new ArrayList<>();
                if ("reset".startsWith(currentToken)) list.add("reset");
                if ("clear".startsWith(currentToken)) list.add("clear");
                return list;
            }
        }

        // 4. /scoreboard, /sb, /sbtoggle
        if (root.equals("scoreboard") || root.equals("sb") || root.equals("sbtoggle") || root.endsWith(":sb") || root.endsWith(":scoreboard")) {
            if (argIndex == 0) {
                List<String> list = new ArrayList<>();
                for (String opt : List.of("toggle", "on", "off")) {
                    if (opt.startsWith(currentToken)) list.add(opt);
                }
                return list;
            }
        }

        // 5. /rules, /rulebook
        if (root.equals("rules") || root.equals("rulebook") || root.endsWith(":rules") || root.endsWith(":rulebook")) {
            if (argIndex == 0) {
                List<String> list = new ArrayList<>();
                for (String opt : List.of("1", "2", "3", "book", "gui")) {
                    if (opt.startsWith(currentToken)) list.add(opt);
                }
                return list;
            }
        }

        // 6. /help, /mhelp, /guide
        if (root.equals("help") || root.equals("mhelp") || root.equals("guide") || root.endsWith(":help")) {
            if (argIndex == 0) {
                List<String> list = new ArrayList<>();
                for (String opt : List.of("survival", "cosmetics", "ranks", "clans", "chat", "poses", "rules")) {
                    if (opt.startsWith(currentToken)) list.add(opt);
                }
                return list;
            }
        }

        // 7. /ranks, /store, /buy
        if (root.equals("ranks") || root.equals("store") || root.equals("buy") || root.endsWith(":ranks")) {
            if (argIndex == 0) {
                List<String> list = new ArrayList<>();
                for (String opt : List.of("gui", "info")) {
                    if (opt.startsWith(currentToken)) list.add(opt);
                }
                return list;
            }
        }

        // 8. /rp, /resourcepack
        if (root.equals("rp") || root.equals("resourcepack") || root.endsWith(":rp") || root.endsWith(":resourcepack")) {
            if (argIndex == 0) {
                List<String> list = new ArrayList<>();
                for (String opt : List.of("reload", "load")) {
                    if (opt.startsWith(currentToken)) list.add(opt);
                }
                return list;
            }
        }

        // 9. /home, /mhome
        if (root.equals("home") || root.equals("mhome") || root.endsWith(":home")) {
            if (argIndex == 0 && sender instanceof Player player) {
                List<String> list = new ArrayList<>();
                List<String> homeNames = plugin.getHomeManager().getHomeNamesFast(player.getUniqueId());
                for (String name : homeNames) {
                    if (name.toLowerCase(Locale.ROOT).startsWith(currentToken)) {
                        list.add(name);
                    }
                }
                return list;
            }
        }

        // 10. /delhome, /removehome, /deletehome, /rmhome, /remhome, /removehomes, /delhomes
        if (root.equals("delhome") || root.equals("removehome") || root.equals("deletehome")
                || root.equals("rmhome") || root.equals("remhome") || root.equals("removehomes") || root.equals("delhomes")
                || root.endsWith(":delhome") || root.endsWith(":removehome")) {
            if (argIndex == 0) {
                List<String> list = new ArrayList<>();
                if ("all".startsWith(currentToken)) list.add("all");
                if (sender instanceof Player player) {
                    List<String> homeNames = plugin.getHomeManager().getHomeNamesFast(player.getUniqueId());
                    for (String name : homeNames) {
                        if (name.toLowerCase(Locale.ROOT).startsWith(currentToken)) {
                            list.add(name);
                        }
                    }
                }
                if (!(sender instanceof Player p) || p.isOp() || p.hasPermission("moonlight.admin") || p.hasPermission("moonlight.staff")) {
                    for (Player target : Bukkit.getOnlinePlayers()) {
                        if (target.getName().toLowerCase(Locale.ROOT).startsWith(currentToken) && !list.contains(target.getName())) {
                            list.add(target.getName());
                        }
                    }
                }
                return list;
            } else if (argIndex == 1) {
                if (!(sender instanceof Player p) || p.isOp() || p.hasPermission("moonlight.admin") || p.hasPermission("moonlight.staff")) {
                    List<String> list = new ArrayList<>();
                    if ("all".startsWith(currentToken)) list.add("all");
                    Player target = Bukkit.getPlayer(parts[1]);
                    if (target != null) {
                        List<String> homeNames = plugin.getHomeManager().getHomeNamesFast(target.getUniqueId());
                        for (String name : homeNames) {
                            if (name.toLowerCase(Locale.ROOT).startsWith(currentToken)) {
                                list.add(name);
                            }
                        }
                    }
                    return list;
                }
            }
        }

        // 11. /sethome, /msethome
        if (root.equals("sethome") || root.equals("msethome") || root.endsWith(":sethome")) {
            if (argIndex == 0) {
                List<String> list = new ArrayList<>();
                for (String opt : List.of("home", "base", "farm", "shop", "mine", "nether", "end")) {
                    if (opt.startsWith(currentToken)) list.add(opt);
                }
                return list;
            }
        }

        // 12. /logs, /log, /auditlogs, /paperlogs, /mlogs
        if (root.equals("logs") || root.equals("log") || root.equals("auditlogs") || root.equals("paperlogs") || root.equals("mlogs")
                || root.endsWith(":logs") || root.endsWith(":log") || root.endsWith(":auditlogs")) {
            if (argIndex == 0) {
                List<String> list = new ArrayList<>();
                for (String sub : List.of("gui", "position", "echest", "inventory", "items", "actions", "sessions", "search", "export", "help")) {
                    if (sub.startsWith(currentToken)) list.add(sub);
                }
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase(Locale.ROOT).startsWith(currentToken)) list.add(p.getName());
                }
                return list;
            } else if (argIndex == 1) {
                String first = parts[1].toLowerCase(Locale.ROOT);
                if (first.equals("position") || first.equals("pos") || first.equals("echest") || first.equals("ec")
                        || first.equals("inventory") || first.equals("inv") || first.equals("items") || first.equals("actions")
                        || first.equals("sessions") || first.equals("search") || first.equals("export")) {
                    List<String> list = new ArrayList<>();
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (p.getName().toLowerCase(Locale.ROOT).startsWith(currentToken)) list.add(p.getName());
                    }
                    return list;
                } else {
                    List<String> list = new ArrayList<>();
                    String today = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                    for (String opt : List.of("latest", "now", "10m", "30m", "1h", "1d", today)) {
                        if (opt.startsWith(currentToken)) list.add(opt);
                    }
                    return list;
                }
            } else if (argIndex == 2) {
                String first = parts[1].toLowerCase(Locale.ROOT);
                if (first.equals("position") || first.equals("pos") || first.equals("echest") || first.equals("ec")
                        || first.equals("inventory") || first.equals("inv") || first.equals("items") || first.equals("actions")
                        || first.equals("sessions") || first.equals("export")) {
                    List<String> list = new ArrayList<>();
                    String today = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                    for (String opt : List.of("latest", "now", "10m", "30m", "1h", "1d", today)) {
                        if (opt.startsWith(currentToken)) list.add(opt);
                    }
                    return list;
                } else if (parts[2].contains("/") || parts[2].contains("-")) {
                    List<String> list = new ArrayList<>();
                    for (String t : List.of("00:00:00", "12:00:00", "18:00:00")) {
                        if (t.startsWith(currentToken)) list.add(t);
                    }
                    return list;
                }
            }
        }

        // 13. /inv, /invsee
        if (root.equals("inv") || root.equals("invsee") || root.endsWith(":inv") || root.endsWith(":invsee")) {
            if (argIndex == 0) {
                List<String> list = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase(Locale.ROOT).startsWith(currentToken)) list.add(p.getName());
                }
                return list;
            }
        }

        // 14. /ec, /enderchest
        if (root.equals("ec") || root.equals("enderchest") || root.endsWith(":ec") || root.endsWith(":enderchest")) {
            if (argIndex == 0) {
                List<String> list = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase(Locale.ROOT).startsWith(currentToken)) list.add(p.getName());
                }
                return list;
            }
        }

        // 15. /pv, /pv1, /pv2, /pv3, /pv4, /pv5, /playervault, /vault
        if (root.equals("pv") || root.equals("playervault") || root.equals("vault") || root.equals("mpv")
                || root.endsWith(":pv") || root.endsWith(":playervault") || root.endsWith(":vault")) {
            if (argIndex == 0) {
                List<String> list = new ArrayList<>();
                for (String opt : List.of("1", "2", "3", "4", "5")) {
                    if (opt.startsWith(currentToken)) list.add(opt);
                }
                if (sender.isOp() || sender.hasPermission("moonlight.admin")) {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (p.getName().toLowerCase(Locale.ROOT).startsWith(currentToken)) list.add(p.getName());
                    }
                }
                return list;
            } else if (argIndex == 1 && (sender.isOp() || sender.hasPermission("moonlight.admin"))) {
                List<String> list = new ArrayList<>();
                if (parts[1].matches("^[1-5]$")) {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (p.getName().toLowerCase(Locale.ROOT).startsWith(currentToken)) list.add(p.getName());
                    }
                } else {
                    for (String opt : List.of("1", "2", "3", "4", "5")) {
                        if (opt.startsWith(currentToken)) list.add(opt);
                    }
                }
                return list;
            }
        }

        // 15b. /pv1, /pv2, /pv3, /pv4, /pv5, /playervault1..5
        if (root.matches("^(pv|playervault|vault|mpv)[1-5]$") || root.matches(".*:(pv|playervault|vault|mpv)[1-5]$")) {
            if (argIndex == 0 && (sender.isOp() || sender.hasPermission("moonlight.admin"))) {
                List<String> list = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase(Locale.ROOT).startsWith(currentToken)) list.add(p.getName());
                }
                return list;
            }
        }

        // 16. /shelp, /staffhelp
        if (root.equals("shelp") || root.equals("staffhelp") || root.endsWith(":shelp")) {
            if (argIndex == 0) {
                List<String> list = new ArrayList<>();
                for (String opt : List.of("moderation", "tickets", "security", "bans", "mutes", "rules", "general")) {
                    if (opt.startsWith(currentToken)) list.add(opt);
                }
                return list;
            }
        }

        // 17. /msg, /tell, /w, /whisper, /pm, /m, /t, /mmsg, /mtell
        if (root.equals("msg") || root.equals("tell") || root.equals("w") || root.equals("whisper") || root.equals("pm")
                || root.equals("m") || root.equals("t") || root.equals("mmsg") || root.equals("mtell") || root.equals("mw")
                || root.equals("emsg") || root.equals("etell") || root.equals("ewhisper")
                || root.endsWith(":msg") || root.endsWith(":tell") || root.endsWith(":w") || root.endsWith(":whisper") || root.endsWith(":pm")) {
            if (argIndex == 0) {
                List<String> list = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase(Locale.ROOT).startsWith(currentToken)) list.add(p.getName());
                }
                return list;
            }
        }

        // 18. /bedrock
        if (root.equals("bedrock") || root.endsWith(":bedrock")) {
            if (argIndex == 0) {
                List<String> list = new ArrayList<>();
                for (String opt : List.of("gui", "info")) {
                    if (opt.startsWith(currentToken)) list.add(opt);
                }
                return list;
            }
        }

        return null;
    }
}
