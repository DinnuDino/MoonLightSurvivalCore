package com.moonlight.vanilla.commands;

import com.moonlight.vanilla.MoonlightVanillaSurvivalCore;
import com.moonlight.vanilla.utils.MessageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * YouTubeCommand (/yt, /youtube, /youtubers)
 * Manages server content creators, clickable links, priority rankings, and pagination.
 */
public class YouTubeCommand implements CommandExecutor, TabCompleter {

    public static final int ITEMS_PER_PAGE = 8;
    private static final String PREFIX = "&9MoonLight &8❙ ";
    private static final Set<String> PRIORITY_SUBS = Set.of("priority", "setpriority", "order", "rank");
    private static final Set<String> REMOVE_SUBS = Set.of("remove", "delete", "del", "removelink", "dellink");
    private static final List<String> PRIORITY_OPTS = List.of("1", "2", "3", "reset");
    private static final List<String> URL_SUGGESTIONS = List.of("https://www.youtube.com/@", "@");

    private final MoonlightVanillaSurvivalCore plugin;

    public YouTubeCommand(MoonlightVanillaSurvivalCore plugin) {
        this.plugin = plugin;
    }

    public record CreatorChannel(String name, String channelUrl, String description, int priority, int additionIndex) {
        public CreatorChannel {
            name = (name != null && !name.isBlank()) ? name : "Moonlight Creator";
            channelUrl = (channelUrl != null && !channelUrl.isBlank()) ? channelUrl : "https://www.youtube.com/";
        }

        public CreatorChannel(String name, String channelUrl, String description) {
            this(name, channelUrl, description, 0, 0);
        }

        public String getName() { return name; }
        public String getChannelUrl() { return channelUrl; }
        public String getDescription() { return description; }
        public int getPriority() { return priority; }
        public int getAdditionIndex() { return additionIndex; }
        public boolean hasPriority() { return priority > 0; }
    }

    public static boolean isOp(CommandSender sender) {
        return !(sender instanceof Player) || sender.isOp()
                || sender.hasPermission("moonlight.core.admin")
                || sender.hasPermission("moonlight.yt.admin")
                || sender.hasPermission("moonlight.youtube.admin");
    }

    public List<CreatorChannel> getCreators() {
        List<CreatorChannel> creators = new ArrayList<>();
        List<?> rawList = plugin.getConfig().getList("youtube.creators");
        if (rawList == null || rawList.isEmpty()) rawList = plugin.getConfig().getList("youtube.channels");
        if (rawList == null || rawList.isEmpty()) rawList = plugin.getConfig().getList("youtube.links");

        int index = 0;
        if (rawList != null) {
            for (Object obj : rawList) {
                if (obj instanceof Map<?, ?> map) {
                    Object nameVal = map.get("name") != null ? map.get("name") : map.get("title");
                    String name = nameVal != null ? nameVal.toString() : "Moonlight YouTuber";
                    Object urlVal = map.get("channel") != null ? map.get("channel") : map.get("url");
                    String url = urlVal != null ? urlVal.toString() : "https://www.youtube.com/";
                    String desc = map.get("description") != null ? map.get("description").toString() : null;
                    int priority = 0;
                    if (map.get("priority") != null) {
                        try { priority = Integer.parseInt(map.get("priority").toString()); } catch (NumberFormatException ignored) {}
                    }
                    creators.add(new CreatorChannel(name, url, desc, priority, index++));
                } else if (obj instanceof String str) {
                    String[] parts = str.contains(" - ") ? str.split(" - ", 2) : new String[]{str, str};
                    creators.add(new CreatorChannel(parts[0].trim(), parts[1].trim(), null, 0, index++));
                }
            }
        }

        if (creators.isEmpty()) {
            ConfigurationSection sec = plugin.getConfig().getConfigurationSection("youtube.creators");
            if (sec == null) sec = plugin.getConfig().getConfigurationSection("youtube.channels");
            if (sec != null) {
                for (String key : sec.getKeys(false)) {
                    creators.add(new CreatorChannel(
                            sec.getString(key + ".name", key),
                            sec.getString(key + ".channel", sec.getString(key + ".url", "https://www.youtube.com/")),
                            sec.getString(key + ".description", null),
                            sec.getInt(key + ".priority", 0),
                            index++
                    ));
                }
            }
        }

        creators.sort((a, b) -> {
            if (a.hasPriority() && b.hasPriority()) {
                int cmp = Integer.compare(a.priority(), b.priority());
                return cmp != 0 ? cmp : Integer.compare(a.additionIndex(), b.additionIndex());
            }
            if (a.hasPriority()) return -1;
            if (b.hasPriority()) return 1;
            return Integer.compare(a.additionIndex(), b.additionIndex());
        });
        return creators;
    }

    public int getMaxPages() {
        return Math.max(1, (int) Math.ceil((double) getCreators().size() / ITEMS_PER_PAGE));
    }

    public static String formatChannelUrl(String raw) {
        if (raw == null || raw.isBlank()) return "https://www.youtube.com/";
        String clean = raw.trim();
        if (clean.startsWith("http://") || clean.startsWith("https://")) return clean;
        if (clean.startsWith("@")) return "https://www.youtube.com/" + clean;
        if (clean.toLowerCase(Locale.ROOT).startsWith("youtube.com/") || clean.toLowerCase(Locale.ROOT).startsWith("www.youtube.com/")) {
            return "https://" + clean;
        }
        return "https://www.youtube.com/@" + clean;
    }

    private List<Map<String, Object>> loadRawConfigList() {
        List<Map<String, Object>> list = new ArrayList<>();
        List<?> current = plugin.getConfig().getList("youtube.creators");
        if (current == null || current.isEmpty()) current = plugin.getConfig().getList("youtube.channels");
        if (current != null) {
            for (Object o : current) {
                if (o instanceof Map<?, ?> m) {
                    Map<String, Object> copy = new LinkedHashMap<>();
                    m.forEach((k, v) -> copy.put(String.valueOf(k), v));
                    list.add(copy);
                } else if (o instanceof String str) {
                    Map<String, Object> copy = new LinkedHashMap<>();
                    String[] p = str.contains(" - ") ? str.split(" - ", 2) : new String[]{str, str};
                    copy.put("name", p[0].trim());
                    copy.put("channel", p[1].trim());
                    list.add(copy);
                }
            }
        }
        return list;
    }

    private void saveRawConfigList(List<Map<String, Object>> list) {
        plugin.getConfig().set("youtube.creators", list);
        plugin.saveConfig();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String label, @NotNull String[] args) {
        String lower = label.toLowerCase(Locale.ROOT);
        if (lower.equals("ytpriority") || lower.equals("setytpriority")) {
            handlePriorityCommand(sender, args, 0);
            return true;
        }
        if (lower.equals("addyoutube") || lower.equals("addyt")) {
            handleAddCommand(sender, args, 0);
            return true;
        }
        if (Set.of("removeyt", "delyt", "removeyoutube", "delyoutube", "removelink", "dellink").contains(lower)) {
            handleRemoveCommand(sender, args, 0);
            return true;
        }

        if (args.length > 0) {
            String first = args[0].toLowerCase(Locale.ROOT);
            if (PRIORITY_SUBS.contains(first)) { handlePriorityCommand(sender, args, 1); return true; }
            if (first.equals("add")) { handleAddCommand(sender, args, 1); return true; }
            if (REMOVE_SUBS.contains(first)) { handleRemoveCommand(sender, args, 1); return true; }
            if (first.equals("clear") || first.equals("clearall")) { handleClearCommand(sender); return true; }
            if (first.equals("reload")) {
                if (!isOp(sender)) {
                    sender.sendMessage(MessageUtil.format(PREFIX + "&cAccess denied: You must be a server operator (OP) to reload configs."));
                    return true;
                }
                plugin.reloadConfig();
                sender.sendMessage(MessageUtil.format(PREFIX + "&aYouTube creators list reloaded from config.yml!"));
                return true;
            }
            if (first.equals("list")) {
                displayCreatorsList(sender, args.length > 1 ? parsePage(args[1]) : 1);
                return true;
            }
        }

        displayCreatorsList(sender, args.length > 0 ? parsePage(args[0]) : 1);
        return true;
    }

    private int parsePage(String arg) {
        try { return Integer.parseInt(arg); } catch (NumberFormatException ignored) { return 1; }
    }

    private void handleAddCommand(CommandSender sender, String[] args, int offset) {
        if (!isOp(sender)) {
            sender.sendMessage(MessageUtil.format(PREFIX + "&cAccess denied: You must be a server operator (OP) to add YouTube channel links."));
            return;
        }
        if (args.length < offset + 2) {
            sender.sendMessage(MessageUtil.format(PREFIX + "&cUsage: &e/yt add <name> <channel_url> [description...]"));
            sender.sendMessage(MessageUtil.format("&7Examples:\n  &8• &f/yt add Dinnu https://www.youtube.com/@Dinnu Official Creator\n  &8• &f/yt add Technoblade @Technoblade Server Partner"));
            return;
        }

        String name = args[offset];
        String formattedUrl = formatChannelUrl(args[offset + 1]);
        String desc = args.length > offset + 2 ? String.join(" ", Arrays.copyOfRange(args, offset + 2, args.length)) : null;

        List<Map<String, Object>> creatorList = loadRawConfigList();
        boolean updated = false;
        for (Map<String, Object> map : creatorList) {
            if (name.equalsIgnoreCase(String.valueOf(map.get("name")))) {
                map.put("name", name);
                map.put("channel", formattedUrl);
                if (desc != null && !desc.isBlank()) map.put("description", desc);
                updated = true;
                break;
            }
        }
        if (!updated) {
            Map<String, Object> newCreator = new LinkedHashMap<>();
            newCreator.put("name", name);
            newCreator.put("channel", formattedUrl);
            if (desc != null && !desc.isBlank()) newCreator.put("description", desc);
            creatorList.add(newCreator);
        }
        saveRawConfigList(creatorList);

        if (sender instanceof Player p) p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.4f);
        sender.sendMessage(Component.empty());
        sender.sendMessage(MessageUtil.format("<gradient:#ff0000:#ff4444><bold>▶ YOUTUBE CREATOR " + (updated ? "UPDATED" : "ADDED") + "</bold></gradient>"));
        sender.sendMessage(MessageUtil.format("&7Name: &f&l" + name));
        sender.sendMessage(MessageUtil.format("&7Channel: &b" + formattedUrl));
        if (desc != null) sender.sendMessage(MessageUtil.format("&7Description: &e" + desc));

        Component visitBtn = MessageUtil.format("<gradient:#ff0000:#ff4444><bold>[VISIT CHANNEL]</bold></gradient>")
                .clickEvent(ClickEvent.openUrl(formattedUrl))
                .hoverEvent(HoverEvent.showText(MessageUtil.format("<yellow>Click to test link in web browser</yellow>")));
        Component listBtn = MessageUtil.format("  <aqua><bold>[VIEW /YT LIST]</bold></aqua>")
                .clickEvent(ClickEvent.runCommand("/yt"))
                .hoverEvent(HoverEvent.showText(MessageUtil.format("<green>Click to view all server YouTubers</green>")));
        sender.sendMessage(Component.empty().append(visitBtn).append(listBtn));
        sender.sendMessage(Component.empty());
    }

    private void handleRemoveCommand(CommandSender sender, String[] args, int offset) {
        if (!isOp(sender)) {
            sender.sendMessage(MessageUtil.format(PREFIX + "&cAccess denied: You must be a server operator (OP) to remove YouTube channels."));
            return;
        }
        if (args.length < offset + 1) {
            sender.sendMessage(MessageUtil.format(PREFIX + "&cUsage: &e/yt remove <creator_name|channel_url>"));
            List<CreatorChannel> current = getCreators();
            if (!current.isEmpty()) {
                sender.sendMessage(MessageUtil.format("&7Currently registered channels:"));
                current.forEach(c -> sender.sendMessage(MessageUtil.format("  &8• &e" + c.name() + " &7(" + c.channelUrl() + ")")));
            } else {
                sender.sendMessage(MessageUtil.format("&7No YouTube channels are currently registered."));
            }
            return;
        }

        String target = args[offset].trim();
        List<Map<String, Object>> creatorList = loadRawConfigList();
        String cleanTarget = target.replace("https://www.youtube.com/", "").replace("https://youtube.com/", "")
                .replace("www.youtube.com/", "").replace("youtube.com/", "").replace("@", "").toLowerCase(Locale.ROOT);

        boolean removed = creatorList.removeIf(map -> {
            String n = String.valueOf(map.get("name")).toLowerCase(Locale.ROOT);
            String c = String.valueOf(map.get("channel")).toLowerCase(Locale.ROOT);
            return n.equalsIgnoreCase(target) || c.equalsIgnoreCase(target) || n.contains(cleanTarget) || c.contains(cleanTarget);
        });

        if (removed) {
            saveRawConfigList(creatorList);
            sender.sendMessage(MessageUtil.format(PREFIX + "&aSuccessfully removed YouTube channel &e" + target + "&a from config!"));
            if (sender instanceof Player p) p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 1.2f);
        } else {
            sender.sendMessage(MessageUtil.format(PREFIX + "&cNo YouTube channel found matching &e'" + target + "'&c.\n&7Type &e/yt&7 to view currently registered channels."));
        }
    }

    private void handleClearCommand(CommandSender sender) {
        if (!isOp(sender)) {
            sender.sendMessage(MessageUtil.format(PREFIX + "&cAccess denied: You must be a server operator (OP) to clear YouTube links."));
            return;
        }
        saveRawConfigList(new ArrayList<>());
        sender.sendMessage(MessageUtil.format(PREFIX + "&aAll YouTube creator links have been cleared from config.yml!"));
        if (sender instanceof Player p) p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 1.2f);
    }

    private void handlePriorityCommand(CommandSender sender, String[] args, int offset) {
        if (!isOp(sender)) {
            sender.sendMessage(MessageUtil.format(PREFIX + "&cAccess denied: You must be a server operator (OP) to set YouTuber priority."));
            return;
        }
        if (args.length < offset + 2) {
            sender.sendMessage(MessageUtil.format(PREFIX + "&cUsage: &e/yt priority <name> <1|2|3...|reset>"));
            List<CreatorChannel> current = getCreators();
            if (!current.isEmpty()) {
                sender.sendMessage(MessageUtil.format("&7Current creator order:"));
                for (int i = 0; i < current.size(); i++) {
                    CreatorChannel c = current.get(i);
                    String pStr = c.hasPriority() ? "&b[Top #" + c.priority() + "] " : "&8[Added] ";
                    sender.sendMessage(MessageUtil.format("  &8" + (i + 1) + ". " + pStr + "&e" + c.name() + " &7(" + c.channelUrl() + ")"));
                }
            }
            return;
        }

        String targetName = args[offset].trim();
        String priorityArg = args[offset + 1].trim();
        boolean isReset = Set.of("reset", "clear", "none", "0").contains(priorityArg.toLowerCase(Locale.ROOT));
        int targetPriority = 0;

        if (!isReset) {
            try {
                targetPriority = Integer.parseInt(priorityArg);
                if (targetPriority < 1) {
                    sender.sendMessage(MessageUtil.format(PREFIX + "&cPriority must be 1 or higher (e.g. 1 for Top 1, 2 for Top 2), or 'reset'."));
                    return;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(MessageUtil.format(PREFIX + "&cInvalid priority number: &e" + priorityArg + "&c. Use a number like 1, 2, 3 or 'reset'."));
                return;
            }
        }

        List<Map<String, Object>> creatorList = loadRawConfigList();
        if (creatorList.isEmpty()) {
            sender.sendMessage(MessageUtil.format(PREFIX + "&cNo YouTube creators registered in config yet. Add one first via &e/yt add <name> <url>&c."));
            return;
        }

        String clean = targetName.replace("https://www.youtube.com/", "").replace("https://youtube.com/", "")
                .replace("www.youtube.com/", "").replace("youtube.com/", "").replace("@", "").toLowerCase(Locale.ROOT);
        Map<String, Object> matched = creatorList.stream()
                .filter(m -> String.valueOf(m.get("name")).equalsIgnoreCase(targetName))
                .findFirst()
                .orElseGet(() -> creatorList.stream().filter(m -> {
                    String n = String.valueOf(m.get("name")).toLowerCase(Locale.ROOT);
                    String c = String.valueOf(m.get("channel")).toLowerCase(Locale.ROOT);
                    return n.contains(clean) || c.contains(clean);
                }).findFirst().orElse(null));

        if (matched == null) {
            sender.sendMessage(MessageUtil.format(PREFIX + "&cNo YouTube creator found matching &e'" + targetName + "'&c.\n&7Type &e/yt&7 to view registered creator names."));
            return;
        }

        String matchedName = String.valueOf(matched.get("name"));
        matched.remove("priority");

        if (isReset) {
            recompactPriorities(creatorList);
            saveRawConfigList(creatorList);
            sender.sendMessage(MessageUtil.format(PREFIX + "&aRemoved custom priority from &e" + matchedName + "&a! They now appear in their added order."));
            if (sender instanceof Player p) p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 1.2f);
            return;
        }

        for (Map<String, Object> m : creatorList) {
            if (m != matched && getMapPriority(m) >= targetPriority) {
                m.put("priority", getMapPriority(m) + 1);
            }
        }
        matched.put("priority", targetPriority);
        recompactPriorities(creatorList);
        saveRawConfigList(creatorList);

        if (sender instanceof Player p) p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.4f);
        sender.sendMessage(Component.empty());
        sender.sendMessage(MessageUtil.format("<gradient:#ff0000:#ff4444><bold>▶ YOUTUBER PRIORITY UPDATED</bold></gradient>"));
        sender.sendMessage(MessageUtil.format("&7Creator: &f&l" + matchedName + "\n&7Assigned Rank: &b&lTop #" + targetPriority + "\n&aThis creator will now always appear at position &e#" + targetPriority + "&a in &e/yt&a."));
        sender.sendMessage(Component.empty());
    }

    private int getMapPriority(Map<String, Object> map) {
        if (map == null || !map.containsKey("priority")) return 0;
        try { return Integer.parseInt(String.valueOf(map.get("priority"))); } catch (NumberFormatException ignored) { return 0; }
    }

    private void recompactPriorities(List<Map<String, Object>> list) {
        List<Map<String, Object>> prioritized = list.stream().filter(m -> getMapPriority(m) > 0).sorted(Comparator.comparingInt(this::getMapPriority)).toList();
        int p = 1;
        for (Map<String, Object> m : prioritized) m.put("priority", p++);
    }

    public void displayCreatorsList(CommandSender sender, int requestedPage) {
        List<CreatorChannel> creators = getCreators();
        if (creators.isEmpty()) {
            sender.sendMessage(Component.empty());
            sender.sendMessage(MessageUtil.format("<gradient:#ff0000:#ff4444><bold>▶ MOONLIGHT SMP YOUTUBERS</bold></gradient>"));
            sender.sendMessage(MessageUtil.format("<gray>No server YouTubers registered yet.</gray>"));
            sender.sendMessage(MessageUtil.format(isOp(sender) ? "<yellow>Add channels using: </yellow><white>/yt add <name> <url> [description]</white>" : "<gray>Want to apply for official YouTuber rank? Join </gray><yellow>/discord</yellow>"));
            sender.sendMessage(Component.empty());
            return;
        }

        int maxPages = getMaxPages();
        int page = Math.clamp(requestedPage, 1, maxPages);
        int startIndex = (page - 1) * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, creators.size());

        sender.sendMessage(Component.empty());
        sender.sendMessage(MessageUtil.format("<gradient:#ff0000:#ff4444><bold>▶ MOONLIGHT SMP YOUTUBERS & CREATORS</bold></gradient> <dark_gray>▪</dark_gray> <gray>Page <yellow>" + page + "</yellow>/<yellow>" + maxPages + "</yellow></gray>"));
        sender.sendMessage(MessageUtil.format("<gray>Official content creators representing Moonlight SMP:</gray>\n<dark_gray>──────────────────────────────────────────────────</dark_gray>"));

        for (int i = startIndex; i < endIndex; i++) {
            CreatorChannel creator = creators.get(i);
            Component tooltip = MessageUtil.format("<red><bold>" + creator.name() + "</bold></red><newline>" + (creator.description() != null ? "<gray>" + creator.description() + "</gray><newline>" : "") + "<aqua><underlined>" + creator.channelUrl() + "</underlined></aqua><newline><yellow>Click to visit channel in web browser!</yellow>");
            Component visitButton = MessageUtil.format("<gradient:#ff0000:#ff4444><bold>[VISIT CHANNEL]</bold></gradient>").clickEvent(ClickEvent.openUrl(creator.channelUrl())).hoverEvent(HoverEvent.showText(tooltip));
            Component channelLine = Component.empty()
                    .append(MessageUtil.format("<red><bold>" + (i + 1) + ". </bold></red>"))
                    .append(MessageUtil.format("<white><bold>" + creator.name() + "</bold></white> "))
                    .append(visitButton)
                    .clickEvent(ClickEvent.openUrl(creator.channelUrl()))
                    .hoverEvent(HoverEvent.showText(tooltip));

            sender.sendMessage(channelLine);
            if (creator.description() != null && !creator.description().isBlank()) {
                sender.sendMessage(MessageUtil.format("   <dark_gray>↳ </dark_gray><gray>" + creator.description() + "</gray>"));
            }
        }

        sender.sendMessage(MessageUtil.format("<dark_gray>──────────────────────────────────────────────────</dark_gray>"));
        Component prev = page > 1
                ? MessageUtil.format("<green><bold>[← Previous]</bold></green>").clickEvent(ClickEvent.runCommand("/yt " + (page - 1))).hoverEvent(HoverEvent.showText(MessageUtil.format("<yellow>Click to view page " + (page - 1) + "</yellow>")))
                : MessageUtil.format("<dark_gray>[← Previous]</dark_gray>").hoverEvent(HoverEvent.showText(MessageUtil.format("<red>You are already on the first page.</red>")));
        Component next = page < maxPages
                ? MessageUtil.format("<green><bold>[Next →]</bold></green>").clickEvent(ClickEvent.runCommand("/yt " + (page + 1))).hoverEvent(HoverEvent.showText(MessageUtil.format("<yellow>Click to view page " + (page + 1) + "</yellow>")))
                : MessageUtil.format("<dark_gray>[Next →]</dark_gray>").hoverEvent(HoverEvent.showText(MessageUtil.format("<red>You are on the last page.</red>")));
        Component pageIndicator = MessageUtil.format("<gray> Page <yellow>" + page + "</yellow>/<yellow>" + maxPages + "</yellow> (" + creators.size() + " creators) </gray>");

        sender.sendMessage(Component.empty().append(prev).append(pageIndicator).append(next));
        if (!isOp(sender)) {
            sender.sendMessage(MessageUtil.format("<dark_gray>💡 </dark_gray><gray>Want to apply for official YouTuber rank? Join </gray><yellow>/discord</yellow>"));
        }
        sender.sendMessage(Component.empty());
        if (sender instanceof Player p) p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
    }

    private static List<String> filterPrefix(Collection<String> items, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return items.stream().filter(s -> s.toLowerCase(Locale.ROOT).startsWith(lower)).toList();
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        String lowerAlias = alias.toLowerCase(Locale.ROOT);
        boolean op = isOp(sender);

        if (lowerAlias.equals("ytpriority") || lowerAlias.equals("setytpriority")) {
            if (!op) return List.of();
            if (args.length == 1) return filterPrefix(getCreators().stream().map(CreatorChannel::name).toList(), args[0]);
            if (args.length == 2) return filterPrefix(PRIORITY_OPTS, args[1]);
            return List.of();
        }

        if (lowerAlias.equals("addyoutube") || lowerAlias.equals("addyt")) {
            if (!op) return List.of();
            if (args.length == 1) return filterPrefix(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[0]);
            if (args.length == 2) return filterPrefix(URL_SUGGESTIONS, args[1]);
            return List.of();
        }

        if (Set.of("removeyt", "delyt", "removeyoutube", "delyoutube", "removelink", "dellink").contains(lowerAlias)) {
            if (!op) return List.of();
            if (args.length == 1) return filterPrefix(getCreators().stream().map(CreatorChannel::name).toList(), args[0]);
            return List.of();
        }

        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            for (int i = 1; i <= getMaxPages(); i++) {
                String p = String.valueOf(i);
                if (p.startsWith(args[0])) list.add(p);
            }
            if (op) {
                list.addAll(filterPrefix(List.of("priority", "add", "remove", "delete", "clear", "reload", "list"), args[0]));
            }
            return list;
        }

        if (args.length == 2 && op) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (PRIORITY_SUBS.contains(sub) || REMOVE_SUBS.contains(sub)) {
                return filterPrefix(getCreators().stream().map(CreatorChannel::name).toList(), args[1]);
            }
            if (sub.equals("add")) {
                return filterPrefix(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[1]);
            }
        }

        if (args.length == 3 && op) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (PRIORITY_SUBS.contains(sub)) return filterPrefix(PRIORITY_OPTS, args[2]);
            if (sub.equals("add")) return filterPrefix(URL_SUGGESTIONS, args[2]);
        }

        return List.of();
    }
}
