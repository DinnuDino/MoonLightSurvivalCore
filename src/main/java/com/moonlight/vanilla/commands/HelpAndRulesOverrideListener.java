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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * HelpAndRulesOverrideListener
 * Intercepts core Moonlight commands at lowest event priority to eliminate third-party plugin collisions
 * and powers instant Brigadier tab autocompletions for all server commands.
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

    private static final Set<String> HELP_CMDS = Set.of("help", "bukkit:help", "minecraft:help", "paper:help", "deluxehub:help", "mhelp", "guide", "playerguide");
    private static final Set<String> DISCORD_CMDS = Set.of("discord", "dc", "dclink", "discordlink", "discordsrv:discord", "mdiscord");
    private static final Set<String> DISCORD_EXCLUDE_SUBS = Set.of("link", "unlink", "bcast", "broadcast", "toggle");
    private static final Set<String> WEB_CMDS = Set.of("website", "link", "weblink", "site", "web", "mwebsite", "mweblink");
    private static final Set<String> YT_CMDS = Set.of("youtube", "yt", "videos", "youtubelinks", "youtubers", "myoutube",
            "addyoutube", "addyt", "removeyoutube", "removeyt", "delyoutube", "delyt", "removelink", "dellink",
            "ytpriority", "setytpriority", "moonlight:yt", "moonlight:youtube", "moonlight:ytpriority");
    private static final Set<String> RANKS_CMDS = Set.of("ranks", "rank", "store", "buy", "paidranks", "donateranks", "donorranks", "webstore",
            "moonlight:ranks", "moonlight:store", "moonlight:buy");
    private static final Set<String> INV_CMDS = Set.of("inv", "invsee", "openinv", "moonlight:inv", "moonlight:invsee", "essentials:invsee", "openinv:openinv");
    private static final Set<String> WRAP_CMDS = Set.of("itemwarps", "itemwraps", "itemwarp", "itemwrap", "wraps", "wrap",
            "mitemwarps", "mitemwarp", "mwraps", "mwrap", "moonlight:itemwarps", "moonlight:itemwarp", "moonlight:wraps", "moonlight:wrap");
    private static final Set<String> CHANGENAME_CMDS = Set.of("changename", "mchangename", "setname", "nickplayer",
            "moonlight:changename", "moonlight:mchangename", "moonlight:setname");
    private static final Set<String> SB_CMDS = Set.of("sb", "sbtoggle", "scoreboard", "msb", "msbtoggle", "moonlight:sb", "moonlight:scoreboard");
    private static final Set<String> HOME_CMDS = Set.of("home", "homes", "mhome", "mhomes", "moonlight:home", "moonlight:homes");
    private static final Set<String> SETHOME_CMDS = Set.of("sethome", "msethome", "moonlight:sethome");
    private static final Set<String> DELHOME_CMDS = Set.of("delhome", "removehome", "deletehome", "rmhome", "remhome", "removehomes", "delhomes",
            "mdelhome", "mremovehome", "moonlight:delhome", "moonlight:removehome");
    private static final Set<String> LOGS_CMDS = Set.of("logs", "log", "auditlogs", "paperlogs", "mlogs", "moonlight:logs", "moonlight:log", "moonlight:auditlogs");
    private static final Set<String> MSG_CMDS = Set.of("msg", "tell", "w", "whisper", "pm", "m", "t", "mmsg", "mtell", "mw", "mwhisper", "mpm",
            "minecraft:msg", "minecraft:tell", "minecraft:w", "minecraft:whisper", "paper:msg", "paper:tell", "paper:w",
            "essentials:msg", "essentials:tell", "essentials:w", "essentials:whisper", "essentials:pm", "essentials:m", "emsg", "etell", "ewhisper");
    private static final Set<String> REPLY_CMDS = Set.of("r", "reply", "mr", "mreply", "er", "ereply", "essentials:r", "essentials:reply");

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

        String[] parts = raw.substring(1).trim().split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) return;

        String cmd = parts[0].toLowerCase(Locale.ROOT);
        String[] args = parts.length > 1 ? Arrays.copyOfRange(parts, 1, parts.length) : new String[0];

        if (HELP_CMDS.contains(cmd)) {
            event.setCancelled(true);
            helpCommand.executeHelp(event.getPlayer(), args);
            return;
        }

        if (cmd.equals("rules") || cmd.equals("rulebook") || cmd.equals("mrules") || cmd.equals("mrulebook")
                || cmd.endsWith(":rules") || cmd.endsWith(":rulebook")) {
            event.setCancelled(true);
            rulesCommand.executeRules(event.getPlayer(), args);
            return;
        }

        if (DISCORD_CMDS.contains(cmd)) {
            if (args.length > 0 && DISCORD_EXCLUDE_SUBS.contains(args[0].toLowerCase(Locale.ROOT))) return;
            event.setCancelled(true);
            if (linkCommand != null) linkCommand.sendDiscordLink(event.getPlayer());
            return;
        }

        if (WEB_CMDS.contains(cmd)) {
            if (cmd.equals("link") && args.length > 0) return;
            event.setCancelled(true);
            if (linkCommand != null) linkCommand.sendWebsiteLink(event.getPlayer());
            return;
        }

        if (YT_CMDS.contains(cmd)) {
            event.setCancelled(true);
            if (youTubeCommand != null) youTubeCommand.onCommand(event.getPlayer(), null, cmd, args);
            return;
        }

        if (RANKS_CMDS.contains(cmd)) {
            event.setCancelled(true);
            if (ranksCommand != null) ranksCommand.onCommand(event.getPlayer(), null, cmd, args);
            return;
        }

        if (INV_CMDS.contains(cmd)) {
            event.setCancelled(true);
            if (invseeCommand != null) invseeCommand.onCommand(event.getPlayer(), null, cmd, args);
            return;
        }

        if (cmd.equals("item") && args.length > 0 && Set.of("warps", "wraps", "warp", "wrap").contains(args[0].toLowerCase(Locale.ROOT))) {
            event.setCancelled(true);
            if (wrapCommand != null) wrapCommand.onCommand(event.getPlayer(), null, "itemwarps", Arrays.copyOfRange(args, 1, args.length));
            return;
        }

        if (WRAP_CMDS.contains(cmd)) {
            event.setCancelled(true);
            if (wrapCommand != null) wrapCommand.onCommand(event.getPlayer(), null, cmd, args);
            return;
        }

        if (CHANGENAME_CMDS.contains(cmd)) {
            event.setCancelled(true);
            if (changeNameCommand != null) changeNameCommand.onCommand(event.getPlayer(), null, cmd, args);
            return;
        }

        if (SB_CMDS.contains(cmd)) {
            event.setCancelled(true);
            if (scoreboardCommand != null) scoreboardCommand.onCommand(event.getPlayer(), null, cmd, args);
            return;
        }

        if (HOME_CMDS.contains(cmd)) {
            event.setCancelled(true);
            if (homeCommand != null) homeCommand.onCommand(event.getPlayer(), null, cmd, args);
            return;
        }

        if (SETHOME_CMDS.contains(cmd)) {
            event.setCancelled(true);
            if (setHomeCommand != null) setHomeCommand.onCommand(event.getPlayer(), null, cmd, args);
            return;
        }

        if (DELHOME_CMDS.contains(cmd)) {
            event.setCancelled(true);
            if (delHomeCommand != null) delHomeCommand.onCommand(event.getPlayer(), null, cmd, args);
            return;
        }

        if (LOGS_CMDS.contains(cmd)) {
            event.setCancelled(true);
            if (logsCommand != null) logsCommand.onCommand(event.getPlayer(), null, cmd, args);
            return;
        }

        if (MSG_CMDS.contains(cmd) || cmd.startsWith("moonlight:msg") || cmd.startsWith("moonlight:tell") || cmd.startsWith("moonlight:w")) {
            event.setCancelled(true);
            try { event.setMessage("/moonlight_handled"); } catch (Exception ignored) {}
            if (messageCommand != null) messageCommand.onCommand(event.getPlayer(), null, cmd, args);
            return;
        }

        if (REPLY_CMDS.contains(cmd) || cmd.startsWith("moonlight:r") || cmd.startsWith("moonlight:reply")) {
            event.setCancelled(true);
            try { event.setMessage("/moonlight_handled"); } catch (Exception ignored) {}
            if (replyCommand != null) replyCommand.onCommand(event.getPlayer(), null, cmd, args);
            return;
        }

        if (cmd.equals("pv") || cmd.equals("playervault") || cmd.equals("vault") || cmd.equals("mpv")
                || cmd.matches("^(pv|playervault|vault|mpv)[1-5]$")
                || cmd.startsWith("moonlight:pv") || cmd.startsWith("moonlight:playervault") || cmd.startsWith("moonlight:vault")
                || cmd.startsWith("essentials:playervault") || cmd.startsWith("playervaults:pv")) {
            event.setCancelled(true);
            try { event.setMessage("/moonlight_handled"); } catch (Exception ignored) {}
            if (playerVaultCommand != null) playerVaultCommand.onCommand(event.getPlayer(), null, cmd, args);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onAsyncTabComplete(AsyncTabCompleteEvent event) {
        if (!event.isCommand()) return;
        List<String> suggestions = computeTabCompletions(event.getSender(), event.getBuffer());
        if (suggestions != null) {
            event.setCompletions(suggestions);
            event.setHandled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onSyncTabComplete(TabCompleteEvent event) {
        List<String> suggestions = computeTabCompletions(event.getSender(), event.getBuffer());
        if (suggestions != null) {
            event.setCompletions(suggestions);
        }
    }

    private static List<String> match(Collection<String> pool, String token) {
        String lower = token.toLowerCase(Locale.ROOT);
        return pool.stream().filter(s -> s.toLowerCase(Locale.ROOT).startsWith(lower)).distinct().toList();
    }

    private static List<String> matchPlayers(String token) {
        return match(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), token);
    }

    private List<String> computeTabCompletions(CommandSender sender, String buffer) {
        if (buffer == null) return null;
        String raw = buffer.startsWith("/") ? buffer.substring(1) : buffer;
        boolean endsWithSpace = buffer.endsWith(" ");
        String[] parts = raw.split("\\s+");
        if (parts.length == 0 || (parts.length == 1 && parts[0].isEmpty())) return null;

        String root = parts[0].toLowerCase(Locale.ROOT);
        String cleanRoot = root.contains(":") ? root.substring(root.indexOf(':') + 1) : root;
        int argIndex = endsWithSpace ? parts.length - 1 : parts.length - 2;
        String token = endsWithSpace ? "" : parts[parts.length - 1];

        // 1. /item
        if (cleanRoot.equals("item")) {
            if (argIndex == 0) return match(List.of("warps", "warp", "wraps", "wrap"), token);
            if (argIndex == 1 && Set.of("warp", "warps", "wrap", "wraps").contains(parts[1].toLowerCase(Locale.ROOT))) {
                return getWrapSuggestions(sender, token);
            }
        }

        // 2. /itemwarps, /itemwraps, /wraps, /wrap
        if (Set.of("itemwarps", "itemwarp", "itemwraps", "itemwrap", "wraps", "wrap").contains(cleanRoot)) {
            if (argIndex == 0) return getWrapSuggestions(sender, token);
        }

        // 3. /changename
        if (Set.of("changename", "mchangename", "setname", "nickplayer").contains(cleanRoot)) {
            if (argIndex == 0) {
                List<String> list = new ArrayList<>(matchPlayers(token));
                if ("reset".startsWith(token.toLowerCase(Locale.ROOT))) list.add("reset");
                if ("list".startsWith(token.toLowerCase(Locale.ROOT))) list.add("list");
                return list;
            }
            if (argIndex == 1) return match(List.of("reset", "clear"), token);
        }

        // 4. /scoreboard, /sb, /sbtoggle
        if (Set.of("scoreboard", "sb", "sbtoggle", "msb", "msbtoggle").contains(cleanRoot)) {
            if (argIndex == 0) return match(List.of("toggle", "on", "off"), token);
        }

        // 5. /rules
        if (cleanRoot.equals("rules") || cleanRoot.equals("rulebook")) {
            if (argIndex == 0) return match(List.of("1", "2", "3", "book", "gui"), token);
        }

        // 6. /help, /guide
        if (Set.of("help", "mhelp", "guide").contains(cleanRoot)) {
            if (argIndex == 0) return match(List.of("survival", "cosmetics", "ranks", "clans", "chat", "poses", "rules"), token);
        }

        // 7. /ranks, /store, /buy
        if (Set.of("ranks", "store", "buy").contains(cleanRoot)) {
            if (argIndex == 0) return match(List.of("gui", "info"), token);
        }

        // 8. /rp, /resourcepack
        if (Set.of("rp", "resourcepack").contains(cleanRoot)) {
            if (argIndex == 0) return match(List.of("reload", "load"), token);
        }

        // 9. /home
        if (Set.of("home", "mhome").contains(cleanRoot)) {
            if (argIndex == 0 && sender instanceof Player player) {
                return match(plugin.getHomeManager().getHomeNamesFast(player.getUniqueId()), token);
            }
        }

        // 10. /delhome
        if (Set.of("delhome", "removehome", "deletehome", "rmhome", "remhome", "removehomes", "delhomes").contains(cleanRoot)) {
            boolean isAdmin = !(sender instanceof Player p) || p.isOp() || p.hasPermission("moonlight.admin") || p.hasPermission("moonlight.staff");
            if (argIndex == 0) {
                List<String> list = new ArrayList<>();
                if ("all".startsWith(token.toLowerCase(Locale.ROOT))) list.add("all");
                if (sender instanceof Player p) list.addAll(match(plugin.getHomeManager().getHomeNamesFast(p.getUniqueId()), token));
                if (isAdmin) list.addAll(matchPlayers(token));
                return list.stream().distinct().toList();
            }
            if (argIndex == 1 && isAdmin) {
                List<String> list = new ArrayList<>();
                if ("all".startsWith(token.toLowerCase(Locale.ROOT))) list.add("all");
                Player target = Bukkit.getPlayer(parts[1]);
                if (target != null) list.addAll(match(plugin.getHomeManager().getHomeNamesFast(target.getUniqueId()), token));
                return list;
            }
        }

        // 11. /sethome
        if (Set.of("sethome", "msethome").contains(cleanRoot)) {
            if (argIndex == 0) return match(List.of("home", "base", "farm", "shop", "mine", "nether", "end"), token);
        }

        // 12. /logs
        if (Set.of("logs", "log", "auditlogs", "paperlogs", "mlogs").contains(cleanRoot)) {
            String today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            List<String> timeOpts = List.of("latest", "now", "10m", "30m", "1h", "1d", today);
            if (argIndex == 0) {
                List<String> list = new ArrayList<>(match(List.of("gui", "position", "echest", "inventory", "items", "actions", "sessions", "search", "export", "help"), token));
                list.addAll(matchPlayers(token));
                return list;
            }
            if (argIndex == 1) {
                String sub = parts[1].toLowerCase(Locale.ROOT);
                if (Set.of("position", "pos", "echest", "ec", "inventory", "inv", "items", "actions", "sessions", "search", "export").contains(sub)) {
                    return matchPlayers(token);
                }
                return match(timeOpts, token);
            }
            if (argIndex == 2) {
                String sub = parts[1].toLowerCase(Locale.ROOT);
                if (Set.of("position", "pos", "echest", "ec", "inventory", "inv", "items", "actions", "sessions", "export").contains(sub)) {
                    return match(timeOpts, token);
                }
                if (parts[2].contains("/") || parts[2].contains("-")) {
                    return match(List.of("00:00:00", "12:00:00", "18:00:00"), token);
                }
            }
        }

        // 13. /inv, /invsee
        if (Set.of("inv", "invsee").contains(cleanRoot)) {
            if (argIndex == 0) return matchPlayers(token);
        }

        // 14. /ec, /enderchest
        if (Set.of("ec", "enderchest").contains(cleanRoot)) {
            if (argIndex == 0) return matchPlayers(token);
        }

        // 15. /pv, /playervault
        if (Set.of("pv", "playervault", "vault", "mpv").contains(cleanRoot)) {
            boolean admin = sender.isOp() || sender.hasPermission("moonlight.admin");
            if (argIndex == 0) {
                List<String> list = new ArrayList<>(match(List.of("1", "2", "3", "4", "5"), token));
                if (admin) list.addAll(matchPlayers(token));
                return list;
            }
            if (argIndex == 1 && admin) {
                return parts[1].matches("^[1-5]$") ? matchPlayers(token) : match(List.of("1", "2", "3", "4", "5"), token);
            }
        }

        if (cleanRoot.matches("^(pv|playervault|vault|mpv)[1-5]$")) {
            if (argIndex == 0 && (sender.isOp() || sender.hasPermission("moonlight.admin"))) return matchPlayers(token);
        }

        // 16. /shelp
        if (cleanRoot.equals("shelp") || cleanRoot.equals("staffhelp")) {
            if (argIndex == 0) return match(List.of("moderation", "tickets", "security", "bans", "mutes", "rules", "general"), token);
        }

        // 17. /msg
        if (Set.of("msg", "tell", "w", "whisper", "pm", "m", "t", "mmsg", "mtell", "mw", "emsg", "etell", "ewhisper").contains(cleanRoot)) {
            if (argIndex == 0) return matchPlayers(token);
        }

        // 18. /bedrock
        if (cleanRoot.equals("bedrock")) {
            if (argIndex == 0) return match(List.of("gui", "info"), token);
        }

        return null;
    }

    private List<String> getWrapSuggestions(CommandSender sender, String token) {
        List<String> list = new ArrayList<>();
        if ("remove".startsWith(token.toLowerCase(Locale.ROOT))) list.add("remove");
        if ((sender.hasPermission("moonlight.admin") || sender.isOp()) && "reload".startsWith(token.toLowerCase(Locale.ROOT))) {
            list.add("reload");
        }
        if (plugin.getWrapManager() != null) {
            if (sender instanceof Player player) {
                ItemStack held = player.getInventory().getItemInMainHand();
                if (!held.getType().isAir()) {
                    for (Wrap w : plugin.getWrapManager().getWrapsFor(held.getType())) {
                        if (w.getKey().toLowerCase(Locale.ROOT).startsWith(token.toLowerCase(Locale.ROOT)) && !list.contains(w.getKey())) {
                            list.add(w.getKey());
                        }
                    }
                }
            }
            for (Wrap w : plugin.getWrapManager().getWraps()) {
                if (w.getKey().toLowerCase(Locale.ROOT).startsWith(token.toLowerCase(Locale.ROOT)) && !list.contains(w.getKey())) {
                    list.add(w.getKey());
                }
            }
        }
        return list;
    }
}
