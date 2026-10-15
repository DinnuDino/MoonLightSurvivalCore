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

import java.util.Collections;
import java.util.List;

/**
 * HelpCommand
 * Interactive, category-based Moonlight SMP Player Guide.
 * Displays a clean, compact category selector menu so it never floods the player's screen,
 * with clickable interactive buttons to view specific categories and return to the main menu.
 */
public class HelpCommand implements CommandExecutor, TabCompleter {

    private record HelpEntry(String name, String desc, String action, boolean runDirectly) {}

    private static final List<String> CATEGORIES = List.of(
            "survival", "cosmetics", "ranks", "clans", "chat", "poses", "rules"
    );

    private final MoonlightVanillaSurvivalCore plugin;

    public HelpCommand(MoonlightVanillaSurvivalCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        executeHelp(sender, args);
        return true;
    }

    public void executeHelp(CommandSender sender, String[] args) {
        if (sender instanceof Player player) {
            try {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
            } catch (Exception ignored) {}
        }

        String category = args.length > 0 ? args[0].toLowerCase() : "menu";

        switch (category) {
            case "1", "survival", "tp", "teleport", "nav", "navigation" -> sendSurvivalHelp(sender);
            case "2", "cosmetics", "wraps", "skins", "weapons" -> sendCosmeticsHelp(sender);
            case "3", "ranks", "rank", "store", "donor", "donate" -> sendRanksHelp(sender);
            case "4", "clans", "clan", "teams" -> sendClansHelp(sender);
            case "5", "chat", "msg", "social", "voice" -> sendChatHelp(sender);
            case "6", "poses", "fun", "sit", "lay", "emotes" -> sendPosesHelp(sender);
            case "7", "rules", "info", "links" -> sendRulesHelp(sender);
            default -> sendMainHelp(sender);
        }
    }

    private void sendMainHelp(CommandSender sender) {
        String header = plugin.getConfig().getString("help.header", "<gradient:#00d2ff:#3a7bd5><bold>═══════════ MOONLIGHT SMP HELP GUIDE ═══════════</bold></gradient>");
        String welcome = plugin.getConfig().getString("help.welcome-message", "<gray>Please select a category below to view commands:</gray>");
        String footer = plugin.getConfig().getString("help.footer", "<gradient:#00d2ff:#3a7bd5><bold>═════════════════════════════════════════════════</bold></gradient>");

        sender.sendMessage(Component.empty());
        sender.sendMessage(MessageUtil.format(header));
        sender.sendMessage(MessageUtil.format(welcome));
        sender.sendMessage(Component.empty());

        sender.sendMessage(makeCategoryRow("✦", "Survival & Teleport", "#38bdf8", "#6366f1", "Spawn, RTP, Homes, Waypoints", "/help survival"));
        sender.sendMessage(makeCategoryRow("✦", "Weapon Wraps & Skins", "#ec4899", "#a855f7", "3D Wraps, apply & remove skins", "/help cosmetics"));
        sender.sendMessage(makeCategoryRow("✦", "Donor Ranks & Store", "#f59e0b", "#ef4444", "Perks, prices, /ranks GUI", "/ranks"));
        sender.sendMessage(makeCategoryRow("✦", "Clan Warfare & Teams", "#10b981", "#059669", "Clans, base, clan chat", "/help clans"));
        sender.sendMessage(makeCategoryRow("✦", "Chat & Proximity Voice", "#06b6d4", "#3b82f6", "Private /msg, [item], Voice Chat", "/help chat"));
        sender.sendMessage(makeCategoryRow("✦", "Poses & Emotes", "#8b5cf6", "#6366f1", "/sit, /lay, /crawl, /spin", "/help poses"));
        sender.sendMessage(makeCategoryRow("✦", "Server Rules & Links", "#eab308", "#f97316", "Rules, Discord, Website, YouTube", "/help rules"));

        sender.sendMessage(Component.empty());
        sender.sendMessage(MessageUtil.format("<dark_gray>💡 </dark_gray><gray>Click any category above or type </gray><yellow>/help <category></yellow>"));
        sender.sendMessage(MessageUtil.format(footer));
        sender.sendMessage(Component.empty());
    }

    private void sendPage(CommandSender sender, String gradient, String title, List<HelpEntry> cmds, List<String> notes) {
        sender.sendMessage(Component.empty());
        sender.sendMessage(MessageUtil.format("<gradient:" + gradient + "><bold>═════════ " + title + " ═════════</bold></gradient>"));
        for (var cmd : cmds) {
            sender.sendMessage(makeCmd(cmd.name(), cmd.desc(), cmd.action(), cmd.runDirectly()));
        }
        if (notes != null) {
            for (String note : notes) {
                sender.sendMessage(note.isEmpty() ? Component.empty() : MessageUtil.format(note));
            }
        }
        sender.sendMessage(Component.empty());
        sender.sendMessage(makeBackToMenuBtn());
        sender.sendMessage(MessageUtil.format("<gradient:" + gradient + "><bold>══════════════════════════════════════════════</bold></gradient>"));
        sender.sendMessage(Component.empty());
    }

    private void sendSurvivalHelp(CommandSender sender) {
        sendPage(sender, "#38bdf8:#6366f1", "SURVIVAL & TELEPORT GUIDE", List.of(
                new HelpEntry("/spawn", "Teleport safely to server spawn", "/spawn", true),
                new HelpEntry("/rtp", "Random teleport far into the wilderness", "/rtp", true),
                new HelpEntry("/sethome <name>", "Create a home waypoint (e.g. /sethome base)", "/sethome ", false),
                new HelpEntry("/home [name]", "Teleport directly to your home", "/home ", false),
                new HelpEntry("/homes", "Open your personal homes GUI / list", "/homes", true),
                new HelpEntry("/delhome <name>", "Delete a saved home location", "/delhome ", false)
        ), null);
    }

    private void sendCosmeticsHelp(CommandSender sender) {
        sendPage(sender, "#ec4899:#a855f7", "WEAPON WRAPS & COSMETICS", List.of(
                new HelpEntry("/wraps", "Open the 3D weapon wraps gallery GUI", "/wraps", true),
                new HelpEntry("/wrap <name>", "Apply a cosmetic skin to your held item", "/wrap ", false),
                new HelpEntry("/wrap remove", "Remove wrap and restore vanilla appearance", "/wrap remove", true),
                new HelpEntry("/ranks", "Open Paid Ranks GUI to view skin tier perks", "/ranks", true)
        ), List.of(
                "",
                " <gray>• Supports Swords, Axes, Pickaxes, Shovels, Hoes, Mace, Bows, Tridents, & Spears!</gray>",
                " <gray>• All 132 wraps are permanent once applied and will never disappear from gear.</gray>"
        ));
    }

    private void sendRanksHelp(CommandSender sender) {
        if (sender instanceof Player player) {
            plugin.getRanksGui().openGui(player);
            return;
        }
        sendPage(sender, "#f59e0b:#ef4444", "DONOR RANKS & STORE (/ranks)", List.of(), List.of(
                " <gray>• Official Web Store: </gray><aqua><underlined>https://www.moonlightsmp.info/</underlined></aqua>",
                " <gray>• Available Ranks: </gray><green>Friendly</green><gray>, </gray><gold>Honoured</gold><gray>, </gray><light_purple>Revered</light_purple><gray>, </gray><red>Exalted</red>"
        ));
    }

    private void sendClansHelp(CommandSender sender) {
        sendPage(sender, "#10b981:#059669", "CLAN WARFARE & TEAMS", List.of(
                new HelpEntry("/clan create <name>", "Create a new clan for you and friends", "/clan create ", false),
                new HelpEntry("/clan invite <player>", "Invite a player to join your clan", "/clan invite ", false),
                new HelpEntry("/clan join <clan>", "Accept a clan invitation and join", "/clan join ", false),
                new HelpEntry("/clan sethome", "Set your clan base location", "/clan sethome", true),
                new HelpEntry("/clan home", "Teleport directly to your clan base", "/clan home", true),
                new HelpEntry("/clan chat", "Toggle public chat vs clan-only chat", "/clan chat", true),
                new HelpEntry("/clan info", "View your clan's stats, level, and member list", "/clan info", true),
                new HelpEntry("/clan list", "Browse all active clans on the server", "/clan list", true)
        ), null);
    }

    private void sendChatHelp(CommandSender sender) {
        sendPage(sender, "#06b6d4:#3b82f6", "CHAT & PROXIMITY VOICE", List.of(
                new HelpEntry("/msg <player> <msg>", "Send a private direct message to a player", "/msg ", false),
                new HelpEntry("/r <msg>", "Quickly reply to the last player who messaged you", "/r ", false)
        ), List.of(
                "",
                "<yellow><bold>▶ Interactive Chat Showcases:</bold></yellow>",
                " <aqua>[item]</aqua> <gray>- Type in chat to showcase held weapon/item with tooltips</gray>",
                " <aqua>[inv]</aqua> <gray>- Type in chat to showcase your full inventory</gray>",
                " <aqua>[ender]</aqua> <gray>- Type in chat to showcase your Ender Chest</gray>",
                " <aqua>@Player</aqua> <gray>- Mention a player in chat to notify them with a chime</gray>",
                "",
                "<yellow><bold>▶ Simple Voice Chat (Proximity Voice):</bold></yellow>",
                " <gray>• Press </gray><yellow><bold>V</bold></yellow><gray> key to open Voice Chat settings, volume, and groups.</gray>",
                " <gray>• Press </gray><yellow><bold>M</bold></yellow><gray> key to mute or unmute your microphone.</gray>"
        ));
    }

    private void sendPosesHelp(CommandSender sender) {
        sendPage(sender, "#8b5cf6:#6366f1", "POSES & EMOTES GUIDE", List.of(
                new HelpEntry("/sit", "Sit on the ground or right-click any stair/slab block", "/sit", true),
                new HelpEntry("/lay", "Lay down comfortably anywhere", "/lay", true),
                new HelpEntry("/crawl", "Crawl into 1-block spaces for mining or exploring", "/crawl", true),
                new HelpEntry("/spin", "Spin around in circles playfully", "/spin", true),
                new HelpEntry("/bellyflop", "Perform a diving bellyflop onto the ground", "/bellyflop", true)
        ), null);
    }

    private void sendRulesHelp(CommandSender sender) {
        sendPage(sender, "#eab308:#f97316", "SERVER RULES & COMMUNITY LINKS", List.of(
                new HelpEntry("/rules", "Open the official interactive server rulebook", "/rules", true),
                new HelpEntry("/discord", "Join our official Discord community", "/discord", true),
                new HelpEntry("/website", "Visit the official Moonlight SMP website", "/website", true),
                new HelpEntry("/youtube", "Watch community YouTube highlights & tutorials", "/youtube", true)
        ), null);
    }

    private Component makeCategoryRow(String badge, String name, String gradientStart, String gradientEnd, String description, String cmd) {
        Component btn = MessageUtil.format("<gradient:" + gradientStart + ":" + gradientEnd + "><bold>▶ " + badge + " " + name + "</bold></gradient>")
                .clickEvent(ClickEvent.runCommand(cmd))
                .hoverEvent(HoverEvent.showText(MessageUtil.format("<yellow>Click to view </yellow><white>" + name + "</white><gray> commands</gray>")));

        return MessageUtil.format("  ").append(btn).append(MessageUtil.format(" <dark_gray>»</dark_gray> <gray>" + description + "</gray>"));
    }

    private Component makeCmd(String cmdName, String description, String action, boolean runDirectly) {
        Component cmdComp = MessageUtil.format("<aqua><bold>" + cmdName + "</bold></aqua>")
                .hoverEvent(HoverEvent.showText(MessageUtil.format("<gray>Click to " + (runDirectly ? "run" : "type") + ": </gray><yellow>" + action + "</yellow>")));

        cmdComp = runDirectly ? cmdComp.clickEvent(ClickEvent.runCommand(action)) : cmdComp.clickEvent(ClickEvent.suggestCommand(action));
        return MessageUtil.format(" ").append(cmdComp).append(MessageUtil.format(" <gray>» " + description + "</gray>"));
    }

    private Component makeBackToMenuBtn() {
        return MessageUtil.format("<gray>« </gray>")
                .append(MessageUtil.format("<gradient:#00d2ff:#3a7bd5><bold>[Back to Categories]</bold></gradient>")
                        .clickEvent(ClickEvent.runCommand("/help"))
                        .hoverEvent(HoverEvent.showText(MessageUtil.format("<yellow>Click to return to category menu</yellow>"))));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length <= 1) {
            String query = args.length == 0 ? "" : args[0].toLowerCase();
            return CATEGORIES.stream().filter(c -> c.startsWith(query)).toList();
        }
        return Collections.emptyList();
    }
}
