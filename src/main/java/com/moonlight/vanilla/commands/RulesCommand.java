package com.moonlight.vanilla.commands;

import com.moonlight.vanilla.MoonlightVanillaSurvivalCore;
import com.moonlight.vanilla.utils.MessageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * RulesCommand
 * Interactive, category-based Moonlight SMP Server Rulebook.
 * Displays a clean, compact category selector in chat so players can easily read rules
 * on both Java and Bedrock editions without GUI failures or chat flooding, with an
 * optional [Open as Book] feature for physical in-game book reading.
 */
public class RulesCommand implements CommandExecutor, TabCompleter, Listener {

    private final MoonlightVanillaSurvivalCore plugin;

    public RulesCommand(MoonlightVanillaSurvivalCore plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = event.getItem();
            if (item != null && item.getType() == Material.WRITTEN_BOOK && item.hasItemMeta() && item.getItemMeta() instanceof BookMeta bm) {
                if ("Moonlight Rules".equalsIgnoreCase(bm.getTitle()) || "Server Rules".equalsIgnoreCase(bm.getTitle())
                        || (bm.hasDisplayName() && MessageUtil.toLegacy(bm.displayName()).contains("Rule"))) {
                    tryOpenBook(event.getPlayer(), item);
                }
            }
        }
    }

    public void tryOpenBook(Player player, ItemStack ruleBook) {
        if (player == null || !player.isOnline()) {
            return;
        }
        try {
            if (ruleBook != null && ruleBook.hasItemMeta() && ruleBook.getItemMeta() instanceof BookMeta bm && !bm.pages().isEmpty()) {
                Component titleComp = bm.title() != null ? bm.title() : Component.text("Moonlight Rules");
                Component authorComp = bm.author() != null ? bm.author() : Component.text("Moonlight SMP Staff");
                net.kyori.adventure.inventory.Book advBook = net.kyori.adventure.inventory.Book.book(titleComp, authorComp, bm.pages());
                player.openBook(advBook);
                return;
            }
        } catch (Throwable ignored) {}
        try {
            if (ruleBook != null) {
                player.openBook(ruleBook);
            }
        } catch (Throwable t2) {
            executeRules(player);
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        executeRules(sender, args);
        return true;
    }

    public void executeRules(CommandSender sender) {
        executeRules(sender, new String[0]);
    }

    public void executeRules(CommandSender sender, String[] args) {
        // Rules are universally readable by all players
        if (sender instanceof Player player) {
            try {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
            } catch (Exception ignored) {}
        }

        String sub = args.length > 0 ? args[0].toLowerCase() : "menu";

        switch (sub) {
            case "1", "conduct", "general", "respect" -> sendConductRules(sender);
            case "2", "fairplay", "cheats", "hacks", "xray" -> sendFairPlayRules(sender);
            case "3", "griefing", "grief", "claims", "stealing" -> sendGriefingRules(sender);
            case "4", "chatrules", "economy", "scam", "trade" -> sendChatRules(sender);
            case "5", "building", "lag", "redstone", "farms" -> sendBuildingRules(sender);
            case "6", "staff", "appeals", "discord", "punishments" -> sendStaffRules(sender);
            case "book", "openbook" -> openRulesBook(sender);
            default -> sendMainRulesMenu(sender);
        }
    }

    private void sendMainRulesMenu(CommandSender sender) {
        sender.sendMessage(Component.empty());
        sender.sendMessage(MessageUtil.format("<gradient:#00d2ff:#3a7bd5><bold>═══════════ MOONLIGHT SERVER RULES ═══════════</bold></gradient>"));
        sender.sendMessage(MessageUtil.format("<gray>Please select a rule category below to review our guidelines:</gray>"));
        sender.sendMessage(Component.empty());

        sender.sendMessage(makeRuleRow("§1.", "General Conduct & Respect", "#38bdf8", "#6366f1", "Respect all players, zero toxicity/threats", "/rules 1"));
        sender.sendMessage(makeRuleRow("§2.", "Fair Play & Cheat/Hacks Policy", "#ef4444", "#dc2626", "No hacked clients, auto-clickers, or X-Ray", "/rules 2"));
        sender.sendMessage(makeRuleRow("§3.", "Griefing, Claims & Stealing", "#f59e0b", "#d97706", "Protect builds, no griefing or trap-killing", "/rules 3"));
        sender.sendMessage(makeRuleRow("§4.", "Chat Etiquette & Economy", "#10b981", "#059669", "No spam, advertising, scamming, or RMT", "/rules 4"));
        sender.sendMessage(makeRuleRow("§5.", "Building & Anti-Lag Standards", "#8b5cf6", "#7c3aed", "No lag machines, clock loops, or NSFW builds", "/rules 5"));
        sender.sendMessage(makeRuleRow("§6.", "Staff Authority & Ban Appeals", "#06b6d4", "#0284c7", "Staff decisions, warnings, appeal on Discord", "/rules 6"));

        sender.sendMessage(Component.empty());

        // Book open button row for Java players
        Component bookBtn = MessageUtil.format("<gray>Prefer a physical book? </gray>")
                .append(MessageUtil.format("<gradient:#00d2ff:#3a7bd5><bold>[📖 Read as Book]</bold></gradient>")
                        .clickEvent(ClickEvent.runCommand("/rules book"))
                        .hoverEvent(HoverEvent.showText(MessageUtil.format("<yellow>Click to open the Written Book directly on your screen!</yellow>"))));
        sender.sendMessage(bookBtn);

        sender.sendMessage(Component.empty());
        sender.sendMessage(MessageUtil.format("<dark_gray>💡 </dark_gray><gray>Click any category above or type </gray><yellow>/rules <1-6></yellow>"));
        sender.sendMessage(MessageUtil.format("<gradient:#00d2ff:#3a7bd5><bold>═══════════════════════════════════════════════</bold></gradient>"));
        sender.sendMessage(Component.empty());
    }

    private void sendConductRules(CommandSender sender) {
        sender.sendMessage(Component.empty());
        sender.sendMessage(MessageUtil.format("<gradient:#38bdf8:#6366f1><bold>═════ §1. GENERAL CONDUCT & RESPECT ═════</bold></gradient>"));
        sender.sendMessage(MessageUtil.format(" <gray>•</gray> <white>Treat all players and staff members with courtesy and respect.</white>"));
        sender.sendMessage(MessageUtil.format(" <gray>•</gray> <red><bold>Zero Tolerance For:</bold></red>"));
        sender.sendMessage(MessageUtil.format("   <dark_gray>▪</dark_gray> <gray>Toxicity, racism, homophobia, sexism, or discrimination.</gray>"));
        sender.sendMessage(MessageUtil.format("   <dark_gray>▪</dark_gray> <gray>Harassment, personal attacks, hate speech, or bullying.</gray>"));
        sender.sendMessage(MessageUtil.format("   <dark_gray>▪</dark_gray> <gray>Doxxing, sharing private info, or real-life threats.</gray>"));
        sender.sendMessage(MessageUtil.format(" <gray>•</gray> <white>Do not attempt to evade mutes or bans using alternate accounts.</white>"));
        sender.sendMessage(MessageUtil.format(" <gray>•</gray> <white>Always listen to instructions provided by server staff.</white>"));
        sender.sendMessage(Component.empty());
        sender.sendMessage(makeNavigationRow(1));
        sender.sendMessage(MessageUtil.format("<gradient:#38bdf8:#6366f1><bold>══════════════════════════════════════════</bold></gradient>"));
        sender.sendMessage(Component.empty());
    }

    private void sendFairPlayRules(CommandSender sender) {
        sender.sendMessage(Component.empty());
        sender.sendMessage(MessageUtil.format("<gradient:#ef4444:#dc2626><bold>═════ §2. FAIR PLAY, CHEATS & HACKS ═════</bold></gradient>"));
        sender.sendMessage(MessageUtil.format(" <gray>•</gray> <red><bold>Strictly Prohibited Clients & Mods:</bold></red>"));
        sender.sendMessage(MessageUtil.format("   <dark_gray>▪</dark_gray> <gray>Hacked clients (Meteor, Aristois, Wurst, etc.).</gray>"));
        sender.sendMessage(MessageUtil.format("   <dark_gray>▪</dark_gray> <gray>Combat & movement cheats (Fly, Speed, Killaura, Baritone).</gray>"));
        sender.sendMessage(MessageUtil.format("   <dark_gray>▪</dark_gray> <gray>Hardware auto-clickers exceeding fair clicking limits.</gray>"));
        sender.sendMessage(MessageUtil.format(" <gray>•</gray> <white>X-Ray resource packs, mods, and coordinate exploits are banned.</white>"));
        sender.sendMessage(MessageUtil.format(" <gray>•</gray> <white>Duplication glitches and game exploit abuse are forbidden.</white>"));
        sender.sendMessage(MessageUtil.format(" <gray>•</gray> <yellow>Found a bug? Report it on Discord for bug hunter rewards: /discord</yellow>"));
        sender.sendMessage(Component.empty());
        sender.sendMessage(makeNavigationRow(2));
        sender.sendMessage(MessageUtil.format("<gradient:#ef4444:#dc2626><bold>══════════════════════════════════════════</bold></gradient>"));
        sender.sendMessage(Component.empty());
    }

    private void sendGriefingRules(CommandSender sender) {
        sender.sendMessage(Component.empty());
        sender.sendMessage(MessageUtil.format("<gradient:#f59e0b:#d97706><bold>═════ §3. GRIEFING, CLAIMS & STEALING ═════</bold></gradient>"));
        sender.sendMessage(MessageUtil.format(" <gray>•</gray> <white>Do not destroy, modify, or vandalize other players' structures.</white>"));
        sender.sendMessage(MessageUtil.format(" <gray>•</gray> <white>Respect claimed bases and community builds.</white>"));
        sender.sendMessage(MessageUtil.format(" <gray>•</gray> <white>Stealing from chests, furnaces, or farms without consent is forbidden.</white>"));
        sender.sendMessage(MessageUtil.format(" <gray>•</gray> <white>No trap-killing, spawn camping, or tricking players into lethal traps.</white>"));
        sender.sendMessage(MessageUtil.format(" <gray>•</gray> <white>All actions are recorded by audit logs (/logs) and will be rolled back.</white>"));
        sender.sendMessage(Component.empty());
        sender.sendMessage(makeNavigationRow(3));
        sender.sendMessage(MessageUtil.format("<gradient:#f59e0b:#d97706><bold>═══════════════════════════════════════════</bold></gradient>"));
        sender.sendMessage(Component.empty());
    }

    private void sendChatRules(CommandSender sender) {
        sender.sendMessage(Component.empty());
        sender.sendMessage(MessageUtil.format("<gradient:#10b981:#059669><bold>═════ §4. CHAT ETIQUETTE & ECONOMY ═════</bold></gradient>"));
        sender.sendMessage(MessageUtil.format(" <gray>•</gray> <white>Keep public chat clean: No spamming, flood typing, or excessive caps.</white>"));
        sender.sendMessage(MessageUtil.format(" <gray>•</gray> <red>No advertising external Minecraft servers, Discords, or services.</red>"));
        sender.sendMessage(MessageUtil.format(" <gray>•</gray> <white>Scamming players during trades or agreements is strictly punished.</white>"));
        sender.sendMessage(MessageUtil.format(" <gray>•</gray> <red>Real Money Trading (RMT) outside the official store is forbidden.</red>"));
        sender.sendMessage(MessageUtil.format(" <gray>•</gray> <white>Use </white><yellow>/msg <player></yellow><white> for private conversations.</white>"));
        sender.sendMessage(Component.empty());
        sender.sendMessage(makeNavigationRow(4));
        sender.sendMessage(MessageUtil.format("<gradient:#10b981:#059669><bold>═════════════════════════════════════════</bold></gradient>"));
        sender.sendMessage(Component.empty());
    }

    private void sendBuildingRules(CommandSender sender) {
        sender.sendMessage(Component.empty());
        sender.sendMessage(MessageUtil.format("<gradient:#8b5cf6:#7c3aed><bold>═════ §5. BUILDING & ANTI-LAG STANDARDS ═════</bold></gradient>"));
        sender.sendMessage(MessageUtil.format(" <gray>•</gray> <white>No intentional lag machines or clock circuits designed to drop server TPS.</white>"));
        sender.sendMessage(MessageUtil.format(" <gray>•</gray> <white>Turn off heavy redstone clocks when not in active use.</white>"));
        sender.sendMessage(MessageUtil.format(" <gray>•</gray> <white>Keep farm entity counts within reasonable limits to preserve performance.</white>"));
        sender.sendMessage(MessageUtil.format(" <gray>•</gray> <red>Inappropriate, NSFW, or offensive pixel art will be removed immediately.</red>"));
        sender.sendMessage(Component.empty());
        sender.sendMessage(makeNavigationRow(5));
        sender.sendMessage(MessageUtil.format("<gradient:#8b5cf6:#7c3aed><bold>═════════════════════════════════════════════</bold></gradient>"));
        sender.sendMessage(Component.empty());
    }

    private void sendStaffRules(CommandSender sender) {
        sender.sendMessage(Component.empty());
        sender.sendMessage(MessageUtil.format("<gradient:#06b6d4:#0284c7><bold>═════ §6. STAFF & PUNISHMENT APPEALS ═════</bold></gradient>"));
        sender.sendMessage(MessageUtil.format(" <gray>•</gray> <white>Staff enforce rules to keep Moonlight safe, fair, and fun for all.</white>"));
        sender.sendMessage(MessageUtil.format(" <gray>•</gray> <white>Punishment tiers: Warning ➔ Mute ➔ Kick ➔ Temp-Ban ➔ Perm-Ban.</white>"));
        sender.sendMessage(MessageUtil.format(" <gray>•</gray> <white>Have a dispute or wish to submit an appeal? Join our Discord:</white>"));
        sender.sendMessage(MessageUtil.format("   <aqua><underlined>https://discord.gg/BVEa5UbCHg</underlined></aqua> ")
                .append(MessageUtil.format("<gradient:#5865f2:#818cf8><bold>[OPEN DISCORD]</bold></gradient>")
                        .clickEvent(ClickEvent.openUrl("https://discord.gg/BVEa5UbCHg"))));
        sender.sendMessage(MessageUtil.format(" <gray>•</gray> <white>Official Website: </white><aqua><underlined>https://www.moonlightsmp.info/</underlined></aqua>"));
        sender.sendMessage(Component.empty());
        sender.sendMessage(makeNavigationRow(6));
        sender.sendMessage(MessageUtil.format("<gradient:#06b6d4:#0284c7><bold>═══════════════════════════════════════════</bold></gradient>"));
        sender.sendMessage(Component.empty());
    }

    private void openRulesBook(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sendMainRulesMenu(sender);
            return;
        }

        ItemStack ruleBook = createRulesBook();
        try {
            player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
        } catch (Exception ignored) {}

        try {
            player.openBook(ruleBook);
        } catch (Exception e) {
            // If openBook fails on client (e.g. Bedrock/Geyser), fall back to chat menu
            sendMainRulesMenu(sender);
            return;
        }

        player.sendMessage(MessageUtil.format("<gradient:#00d2ff:#3a7bd5><bold>MOONLIGHT SMP</bold></gradient> <gray>» The official server rulebook has been opened on your screen!</gray>"));
    }

    private Component makeRuleRow(String index, String title, String color1, String color2, String description, String clickCmd) {
        Component indexComp = MessageUtil.format("<yellow><bold>" + index + " </bold></yellow>");
        Component titleComp = MessageUtil.format("<gradient:" + color1 + ":" + color2 + "><bold>[" + title + "]</bold></gradient>")
                .clickEvent(ClickEvent.runCommand(clickCmd))
                .hoverEvent(HoverEvent.showText(MessageUtil.format("<yellow>Click to read: </yellow><white>" + title + "</white><newline><gray>" + description + "</gray>")));

        Component descComp = MessageUtil.format(" <dark_gray>»</dark_gray> <gray>" + description + "</gray>");

        return Component.empty()
                .append(indexComp)
                .append(titleComp)
                .append(descComp);
    }

    private Component makeNavigationRow(int currentCat) {
        Component backBtn = MessageUtil.format("<gradient:#38bdf8:#6366f1><bold>« [Back to Rules]</bold></gradient>")
                .clickEvent(ClickEvent.runCommand("/rules"))
                .hoverEvent(HoverEvent.showText(MessageUtil.format("<yellow>Click to return to the rules category menu</yellow>")));

        Component nextBtn = Component.empty();
        if (currentCat < 6) {
            int nextCat = currentCat + 1;
            nextBtn = MessageUtil.format("  <gradient:#10b981:#059669><bold>[Next Category »]</bold></gradient>")
                    .clickEvent(ClickEvent.runCommand("/rules " + nextCat))
                    .hoverEvent(HoverEvent.showText(MessageUtil.format("<green>Click to read category §" + nextCat + "</green>")));
        }

        return Component.empty().append(backBtn).append(nextBtn);
    }

    public ItemStack createRulesBook() {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        if (meta == null) return book;

        FileConfiguration config = plugin.getConfig();
        String title = config.getString("rules.title", "Moonlight Rules");
        String author = config.getString("rules.author", "Moonlight SMP Staff");
        meta.setTitle(title);
        meta.setAuthor(author);
        meta.setGeneration(BookMeta.Generation.ORIGINAL);

        List<Component> pageComponents = new ArrayList<>();
        List<String> configPages = config.getStringList("rules.pages");
        if (configPages != null && !configPages.isEmpty()) {
            for (String p : configPages) {
                if (p != null && !p.isBlank()) {
                    pageComponents.add(MessageUtil.format(p.trim()));
                }
            }
        }

        if (pageComponents.isEmpty()) {
            pageComponents.add(MessageUtil.format(
                    "<dark_blue><bold>MOONLIGHT SMP</bold></dark_blue>\n" +
                    "<blue><bold>Official Rulebook</bold></blue>\n" +
                    "<dark_gray>-----------------</dark_gray>\n" +
                    "<dark_gray>Welcome to Moonlight!</dark_gray>\n" +
                    "<dark_gray>Please read and follow all server rules.</dark_gray>\n\n" +
                    "<black><bold>Categories:</bold></black>\n" +
                    "<dark_blue>§1.</dark_blue> <dark_gray>General Conduct</dark_gray>\n" +
                    "<dark_blue>§2.</dark_blue> <dark_gray>Fair Play & Hacks</dark_gray>\n" +
                    "<dark_blue>§3.</dark_blue> <dark_gray>Griefing & Claims</dark_gray>\n" +
                    "<dark_blue>§4.</dark_blue> <dark_gray>Chat & Economy</dark_gray>\n" +
                    "<dark_blue>§5.</dark_blue> <dark_gray>Building & Lag</dark_gray>\n" +
                    "<dark_blue>§6.</dark_blue> <dark_gray>Staff & Appeals</dark_gray>"
            ));

        pageComponents.add(MessageUtil.format(
                "<dark_blue><bold>§1. General Conduct</bold></dark_blue>\n" +
                "<dark_gray>-----------------</dark_gray>\n" +
                "<black>• Treat all players with respect.</black>\n\n" +
                "<dark_red>• Zero tolerance for:</dark_red>\n" +
                "<dark_gray>- Toxicity, racism, sexism</dark_gray>\n" +
                "<dark_gray>- Harassment, hate speech</dark_gray>\n" +
                "<dark_gray>- Doxxing or threats</dark_gray>\n\n" +
                "<black>• Do not evade mutes or bans using alts.</black>\n\n" +
                "<black>• Follow staff instructions.</black>"
        ));

        pageComponents.add(MessageUtil.format(
                "<dark_blue><bold>§2. Fair Play & Hacks</bold></dark_blue>\n" +
                "<dark_gray>-----------------</dark_gray>\n" +
                "<dark_red><bold>Strictly Prohibited:</bold></dark_red>\n" +
                "<black>• Hacked clients (Fly, Speed, Killaura, Baritone).</black>\n\n" +
                "<black>• X-Ray packs, mods, or exploits.</black>\n\n" +
                "<black>• Duplication glitches.</black>\n\n" +
                "<dark_blue>Report bugs on /discord!</dark_blue>"
        ));

        pageComponents.add(MessageUtil.format(
                "<dark_blue><bold>§3. Griefing & Claims</bold></dark_blue>\n" +
                "<dark_gray>-----------------</dark_gray>\n" +
                "<black>• Do not grief, destroy, or vandalize builds.</black>\n\n" +
                "<black>• Respect claimed lands.</black>\n\n" +
                "<black>• Stealing from chests without permission is forbidden.</black>\n\n" +
                "<black>• No trap-killing or spawn camping.</black>"
        ));

        pageComponents.add(MessageUtil.format(
                "<dark_blue><bold>§4. Chat & Economy</bold></dark_blue>\n" +
                "<dark_gray>-----------------</dark_gray>\n" +
                "<black>• No spamming or excessive caps.</black>\n\n" +
                "<black>• No advertising other servers or discords.</black>\n\n" +
                "<black>• No scamming in trades.</black>\n\n" +
                "<dark_red>• Real Money Trading (RMT) is prohibited.</dark_red>"
        ));

        pageComponents.add(MessageUtil.format(
                "<dark_blue><bold>§5. Building & Lag</bold></dark_blue>\n" +
                "<dark_gray>-----------------</dark_gray>\n" +
                "<black>• No intentional lag machines or clock circuits.</black>\n\n" +
                "<black>• Limit farm entity counts.</black>\n\n" +
                "<dark_red>• Inappropriate pixel art or builds will be removed.</dark_red>"
        ));

        pageComponents.add(MessageUtil.format(
                "<dark_blue><bold>§6. Staff & Appeals</bold></dark_blue>\n" +
                "<dark_gray>-----------------</dark_gray>\n" +
                "<black>Violations result in warnings, mutes, kicks, or bans.</black>\n\n" +
                "<black><bold>Need Support or Appeal?</bold></black>\n" +
                "<dark_blue>Discord:</dark_blue> <blue>discord.gg/BVEa5UbCHg</blue>\n" +
                "<dark_blue>Website:</dark_blue> <blue>www.moonlightsmp.info</blue>"
        ));
        }

        meta.pages(pageComponents);
        book.setItemMeta(meta);
        return book;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> options = List.of("1", "2", "3", "4", "5", "6", "conduct", "fairplay", "griefing", "chat", "building", "appeals", "book");
            List<String> matches = new ArrayList<>();
            for (String opt : options) {
                if (opt.startsWith(args[0].toLowerCase())) {
                    matches.add(opt);
                }
            }
            return matches;
        }
        return Collections.emptyList();
    }
}
