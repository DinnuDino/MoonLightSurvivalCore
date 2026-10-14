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
 * YouTubeCommand
 * Implements /yt, /youtube, /youtubers displaying server YouTuber channel links.
 * Also implements OP-only channel management:
 *   /yt add <name> <channel_url> [description...]
 *   /yt remove <name>
 *   /addyoutube <name> <channel_url> [description...]
 * Features clickable [VISIT CHANNEL] buttons redirecting to YouTube creator channels.
 */
public class YouTubeCommand implements CommandExecutor, TabCompleter {

    public static final int ITEMS_PER_PAGE = 8;
    private final MoonlightVanillaSurvivalCore plugin;

    public YouTubeCommand(MoonlightVanillaSurvivalCore plugin) {
        this.plugin = plugin;
    }

    public static class CreatorChannel {
        private final String name;
        private final String channelUrl;
        private final String description;
        private final int priority;
        private final int additionIndex;

        public CreatorChannel(String name, String channelUrl, String description, int priority, int additionIndex) {
            this.name = name != null && !name.isEmpty() ? name : "Moonlight Creator";
            this.channelUrl = channelUrl != null && !channelUrl.isEmpty() ? channelUrl : "https://www.youtube.com/";
            this.description = description;
            this.priority = priority;
            this.additionIndex = additionIndex;
        }

        public CreatorChannel(String name, String channelUrl, String description) {
            this(name, channelUrl, description, 0, 0);
        }

        public String getName() {
            return name;
        }

        public String getChannelUrl() {
            return channelUrl;
        }

        public String getDescription() {
            return description;
        }

        public int getPriority() {
            return priority;
        }

        public int getAdditionIndex() {
            return additionIndex;
        }

        public boolean hasPriority() {
            return priority > 0;
        }
    }

    public static boolean isOp(CommandSender sender) {
        if (sender instanceof org.bukkit.command.ConsoleCommandSender || sender instanceof org.bukkit.command.RemoteConsoleCommandSender) {
            return true;
        }
        return sender.isOp() || sender.hasPermission("moonlight.core.admin") || sender.hasPermission("moonlight.yt.admin") || sender.hasPermission("moonlight.youtube.admin");
    }

    public List<CreatorChannel> getCreators() {
        List<CreatorChannel> creators = new ArrayList<>();

        // 1. Try reading from 'youtube.creators' or 'youtube.channels'
        List<?> rawList = plugin.getConfig().getList("youtube.creators");
        if (rawList == null || rawList.isEmpty()) {
            rawList = plugin.getConfig().getList("youtube.channels");
        }
        if (rawList == null || rawList.isEmpty()) {
            rawList = plugin.getConfig().getList("youtube.links");
        }

        int index = 0;
        if (rawList != null) {
            for (Object obj : rawList) {
                if (obj instanceof Map<?, ?> map) {
                    String name = map.get("name") != null ? map.get("name").toString()
                            : (map.get("title") != null ? map.get("title").toString() : "Moonlight YouTuber");
                    String url = map.get("channel") != null ? map.get("channel").toString()
                            : (map.get("url") != null ? map.get("url").toString() : "https://www.youtube.com/");
                    String desc = map.get("description") != null ? map.get("description").toString() : null;
                    int priority = 0;
                    if (map.get("priority") != null) {
                        try {
                            priority = Integer.parseInt(map.get("priority").toString());
                        } catch (NumberFormatException ignored) {}
                    }
                    creators.add(new CreatorChannel(name, url, desc, priority, index++));
                } else if (obj instanceof String str) {
                    if (str.contains(" - ")) {
                        String[] parts = str.split(" - ", 2);
                        creators.add(new CreatorChannel(parts[0].trim(), parts[1].trim(), null, 0, index++));
                    } else {
                        creators.add(new CreatorChannel(str.trim(), str.trim(), null, 0, index++));
                    }
                }
            }
        }

        // 2. Fallback: ConfigurationSection format
        if (creators.isEmpty()) {
            ConfigurationSection sec = plugin.getConfig().getConfigurationSection("youtube.creators");
            if (sec == null) {
                sec = plugin.getConfig().getConfigurationSection("youtube.channels");
            }
            if (sec != null) {
                for (String key : sec.getKeys(false)) {
                    String name = sec.getString(key + ".name", key);
                    String url = sec.getString(key + ".channel", sec.getString(key + ".url", "https://www.youtube.com/"));
                    String desc = sec.getString(key + ".description", null);
                    int priority = sec.getInt(key + ".priority", 0);
                    creators.add(new CreatorChannel(name, url, desc, priority, index++));
                }
            }
        }

        // Sort creators:
        // Priority 1 is top 1, Priority 2 is top 2, etc.
        // Those with priority > 0 are placed first in ascending order of priority.
        // Remaining creators (priority <= 0) retain their original addition order (additionIndex).
        creators.sort((a, b) -> {
            boolean aHas = a.hasPriority();
            boolean bHas = b.hasPriority();

            if (aHas && bHas) {
                int cmp = Integer.compare(a.getPriority(), b.getPriority());
                if (cmp != 0) return cmp;
                return Integer.compare(a.getAdditionIndex(), b.getAdditionIndex());
            } else if (aHas) {
                return -1;
            } else if (bHas) {
                return 1;
            } else {
                return Integer.compare(a.getAdditionIndex(), b.getAdditionIndex());
            }
        });

        return creators;
    }

    public int getMaxPages() {
        List<CreatorChannel> creators = getCreators();
        return Math.max(1, (int) Math.ceil((double) creators.size() / ITEMS_PER_PAGE));
    }

    public static String formatChannelUrl(String raw) {
        if (raw == null || raw.isBlank()) return "https://www.youtube.com/";
        String clean = raw.trim();
        if (clean.startsWith("http://") || clean.startsWith("https://")) {
            return clean;
        }
        if (clean.startsWith("@")) {
            return "https://www.youtube.com/" + clean;
        }
        if (clean.toLowerCase().startsWith("youtube.com/") || clean.toLowerCase().startsWith("www.youtube.com/")) {
            return "https://" + clean;
        }
        return "https://www.youtube.com/@" + clean;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String label, @NotNull String[] args) {
        String lowerLabel = label != null ? label.toLowerCase(Locale.ROOT) : "youtube";

        // Dedicated priority commands: /ytpriority <name> <priority> or /setytpriority
        if (lowerLabel.equals("ytpriority") || lowerLabel.equals("setytpriority")) {
            handlePriorityCommand(sender, args, 0);
            return true;
        }

        // Dedicated add commands: /addyoutube <name> <url> [desc] or /addyt ...
        if (lowerLabel.equals("addyoutube") || lowerLabel.equals("addyt")) {
            handleAddCommand(sender, args, 0);
            return true;
        }

        // Dedicated remove commands: /removeyt <name>, /delyt, /removeyoutube, /removelink, /dellink
        if (lowerLabel.equals("removeyt") || lowerLabel.equals("delyt")
                || lowerLabel.equals("removeyoutube") || lowerLabel.equals("delyoutube")
                || lowerLabel.equals("removelink") || lowerLabel.equals("dellink")) {
            handleRemoveCommand(sender, args, 0);
            return true;
        }

        // Subcommands on /yt or /youtube:
        if (args.length > 0) {
            String first = args[0].toLowerCase(Locale.ROOT);
            if (first.equals("priority") || first.equals("setpriority") || first.equals("order") || first.equals("rank")) {
                handlePriorityCommand(sender, args, 1);
                return true;
            }
            if (first.equals("add")) {
                handleAddCommand(sender, args, 1);
                return true;
            }
            if (first.equals("remove") || first.equals("delete") || first.equals("del") || first.equals("removelink") || first.equals("dellink")) {
                handleRemoveCommand(sender, args, 1);
                return true;
            }
            if (first.equals("clear") || first.equals("clearall")) {
                handleClearCommand(sender);
                return true;
            }
            if (first.equals("reload")) {
                if (!isOp(sender)) {
                    sender.sendMessage(MessageUtil.format("&9MoonLight &8❙ &cAccess denied: You must be a server operator (OP) to reload configs."));
                    return true;
                }
                plugin.reloadConfig();
                sender.sendMessage(MessageUtil.format("&9MoonLight &8❙ &aYouTube creators list reloaded from config.yml!"));
                return true;
            }
            if (first.equals("list")) {
                int page = args.length > 1 ? parsePage(args[1]) : 1;
                displayCreatorsList(sender, page);
                return true;
            }
        }

        // Normal player view: /yt [page]
        int page = args.length > 0 ? parsePage(args[0]) : 1;
        displayCreatorsList(sender, page);
        return true;
    }

    private int parsePage(String arg) {
        try {
            return Integer.parseInt(arg);
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    private void handleAddCommand(CommandSender sender, String[] args, int offset) {
        if (!isOp(sender)) {
            sender.sendMessage(MessageUtil.format("&9MoonLight &8❙ &cAccess denied: You must be a server operator (OP) to add YouTube channel links."));
            return;
        }

        if (args.length < offset + 2) {
            sender.sendMessage(MessageUtil.format("&9MoonLight &8❙ &cUsage: &e/yt add <name> <channel_url> [description...]"));
            sender.sendMessage(MessageUtil.format("&7Examples:"));
            sender.sendMessage(MessageUtil.format("  &8• &f/yt add Dinnu https://www.youtube.com/@Dinnu Official Creator"));
            sender.sendMessage(MessageUtil.format("  &8• &f/yt add Technoblade @Technoblade Server Partner"));
            return;
        }

        String name = args[offset];
        String rawUrl = args[offset + 1];
        String description = args.length > offset + 2 ? String.join(" ", Arrays.copyOfRange(args, offset + 2, args.length)) : null;

        String formattedUrl = formatChannelUrl(rawUrl);

        List<Map<String, Object>> creatorList = new ArrayList<>();
        List<?> current = plugin.getConfig().getList("youtube.creators");
        if (current == null || current.isEmpty()) {
            current = plugin.getConfig().getList("youtube.channels");
        }
        if (current != null) {
            for (Object o : current) {
                if (o instanceof Map<?, ?> m) {
                    Map<String, Object> copy = new LinkedHashMap<>();
                    for (Map.Entry<?, ?> e : m.entrySet()) {
                        copy.put(String.valueOf(e.getKey()), e.getValue());
                    }
                    creatorList.add(copy);
                } else if (o instanceof String str) {
                    Map<String, Object> copy = new LinkedHashMap<>();
                    if (str.contains(" - ")) {
                        String[] p = str.split(" - ", 2);
                        copy.put("name", p[0].trim());
                        copy.put("channel", p[1].trim());
                    } else {
                        copy.put("name", str.trim());
                        copy.put("channel", str.trim());
                    }
                    creatorList.add(copy);
                }
            }
        }

        boolean updated = false;
        for (Map<String, Object> map : creatorList) {
            if (name.equalsIgnoreCase(String.valueOf(map.get("name")))) {
                map.put("name", name);
                map.put("channel", formattedUrl);
                if (description != null && !description.isBlank()) {
                    map.put("description", description);
                }
                updated = true;
                break;
            }
        }

        if (!updated) {
            Map<String, Object> newCreator = new LinkedHashMap<>();
            newCreator.put("name", name);
            newCreator.put("channel", formattedUrl);
            if (description != null && !description.isBlank()) {
                newCreator.put("description", description);
            }
            creatorList.add(newCreator);
        }

        plugin.getConfig().set("youtube.creators", creatorList);
        plugin.saveConfig();

        if (sender instanceof Player player) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.4f);
        }

        sender.sendMessage(Component.empty());
        sender.sendMessage(MessageUtil.format(
                "<gradient:#ff0000:#ff4444><bold>▶ YOUTUBE CREATOR " + (updated ? "UPDATED" : "ADDED") + "</bold></gradient>"));
        sender.sendMessage(MessageUtil.format("&7Name: &f&l" + name));
        sender.sendMessage(MessageUtil.format("&7Channel: &b" + formattedUrl));
        if (description != null) {
            sender.sendMessage(MessageUtil.format("&7Description: &e" + description));
        }

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
            sender.sendMessage(MessageUtil.format("&9MoonLight &8❙ &cAccess denied: You must be a server operator (OP) to remove YouTube channels."));
            return;
        }

        if (args.length < offset + 1) {
            sender.sendMessage(MessageUtil.format("&9MoonLight &8❙ &cUsage: &e/yt remove <creator_name|channel_url>"));
            List<CreatorChannel> currentCreators = getCreators();
            if (!currentCreators.isEmpty()) {
                sender.sendMessage(MessageUtil.format("&7Currently registered channels:"));
                for (CreatorChannel c : currentCreators) {
                    sender.sendMessage(MessageUtil.format("  &8• &e" + c.getName() + " &7(" + c.getChannelUrl() + ")"));
                }
            } else {
                sender.sendMessage(MessageUtil.format("&7No YouTube channels are currently registered."));
            }
            return;
        }

        String target = args[offset].trim();
        List<Map<String, Object>> creatorList = new ArrayList<>();
        List<?> current = plugin.getConfig().getList("youtube.creators");
        if (current == null || current.isEmpty()) {
            current = plugin.getConfig().getList("youtube.channels");
        }
        if (current != null) {
            for (Object o : current) {
                if (o instanceof Map<?, ?> m) {
                    Map<String, Object> copy = new LinkedHashMap<>();
                    for (Map.Entry<?, ?> e : m.entrySet()) {
                        copy.put(String.valueOf(e.getKey()), e.getValue());
                    }
                    creatorList.add(copy);
                } else if (o instanceof String str) {
                    Map<String, Object> copy = new LinkedHashMap<>();
                    if (str.contains(" - ")) {
                        String[] p = str.split(" - ", 2);
                        copy.put("name", p[0].trim());
                        copy.put("channel", p[1].trim());
                    } else {
                        copy.put("name", str.trim());
                        copy.put("channel", str.trim());
                    }
                    creatorList.add(copy);
                }
            }
        }

        String targetClean = target.replace("https://www.youtube.com/", "")
                                   .replace("https://youtube.com/", "")
                                   .replace("www.youtube.com/", "")
                                   .replace("youtube.com/", "")
                                   .replace("@", "")
                                   .toLowerCase(Locale.ROOT);

        boolean removed = creatorList.removeIf(map -> {
            String name = String.valueOf(map.get("name")).toLowerCase(Locale.ROOT);
            String channel = String.valueOf(map.get("channel")).toLowerCase(Locale.ROOT);
            return name.equalsIgnoreCase(target)
                    || channel.equalsIgnoreCase(target)
                    || name.contains(targetClean)
                    || channel.contains(targetClean);
        });

        if (removed) {
            plugin.getConfig().set("youtube.creators", creatorList);
            plugin.saveConfig();
            sender.sendMessage(MessageUtil.format("&9MoonLight &8❙ &aSuccessfully removed YouTube channel &e" + target + "&a from config!"));
            if (sender instanceof Player player) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 1.2f);
            }
        } else {
            sender.sendMessage(MessageUtil.format("&9MoonLight &8❙ &cNo YouTube channel found matching &e'" + target + "'&c."));
            sender.sendMessage(MessageUtil.format("&7Type &e/yt&7 to view currently registered channels."));
        }
    }

    private void handleClearCommand(CommandSender sender) {
        if (!isOp(sender)) {
            sender.sendMessage(MessageUtil.format("&9MoonLight &8❙ &cAccess denied: You must be a server operator (OP) to clear YouTube links."));
            return;
        }

        plugin.getConfig().set("youtube.creators", new ArrayList<>());
        plugin.saveConfig();

        sender.sendMessage(MessageUtil.format("&9MoonLight &8❙ &aAll YouTube creator links have been cleared from config.yml!"));
        if (sender instanceof Player player) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 1.2f);
        }
    }

    private void handlePriorityCommand(CommandSender sender, String[] args, int offset) {
        if (!isOp(sender)) {
            sender.sendMessage(MessageUtil.format("&9MoonLight &8❙ &cAccess denied: You must be a server operator (OP) to set YouTuber priority."));
            return;
        }

        if (args.length < offset + 2) {
            sender.sendMessage(MessageUtil.format("&9MoonLight &8❙ &cUsage: &e/yt priority <name> <1|2|3...|reset>"));
            sender.sendMessage(MessageUtil.format("&7Examples:"));
            sender.sendMessage(MessageUtil.format("  &8• &f/yt priority Dinnu 1 &7(Sets Dinnu to Top #1)"));
            sender.sendMessage(MessageUtil.format("  &8• &f/yt priority Technoblade 2 &7(Sets Technoblade to Top #2)"));
            sender.sendMessage(MessageUtil.format("  &8• &f/yt priority Dinnu reset &7(Clears custom priority, restores added order)"));

            List<CreatorChannel> current = getCreators();
            if (!current.isEmpty()) {
                sender.sendMessage(MessageUtil.format("&7Current creator order:"));
                for (int i = 0; i < current.size(); i++) {
                    CreatorChannel c = current.get(i);
                    String pStr = c.hasPriority() ? "&b[Top #" + c.getPriority() + "] " : "&8[Added] ";
                    sender.sendMessage(MessageUtil.format("  &8" + (i + 1) + ". " + pStr + "&e" + c.getName() + " &7(" + c.getChannelUrl() + ")"));
                }
            }
            return;
        }

        String targetName = args[offset].trim();
        String priorityArg = args[offset + 1].trim();

        boolean isReset = priorityArg.equalsIgnoreCase("reset")
                || priorityArg.equalsIgnoreCase("clear")
                || priorityArg.equalsIgnoreCase("none")
                || priorityArg.equals("0");
        int targetPriority = 0;

        if (!isReset) {
            try {
                targetPriority = Integer.parseInt(priorityArg);
                if (targetPriority < 1) {
                    sender.sendMessage(MessageUtil.format("&9MoonLight &8❙ &cPriority must be 1 or higher (e.g. 1 for Top 1, 2 for Top 2), or 'reset'."));
                    return;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(MessageUtil.format("&9MoonLight &8❙ &cInvalid priority number: &e" + priorityArg + "&c. Use a number like 1, 2, 3 or 'reset'."));
                return;
            }
        }

        // Read all creators from config as raw map list
        List<Map<String, Object>> creatorList = new ArrayList<>();
        List<?> current = plugin.getConfig().getList("youtube.creators");
        if (current == null || current.isEmpty()) {
            current = plugin.getConfig().getList("youtube.channels");
        }
        if (current != null) {
            for (Object o : current) {
                if (o instanceof Map<?, ?> m) {
                    Map<String, Object> copy = new LinkedHashMap<>();
                    for (Map.Entry<?, ?> e : m.entrySet()) {
                        copy.put(String.valueOf(e.getKey()), e.getValue());
                    }
                    creatorList.add(copy);
                } else if (o instanceof String str) {
                    Map<String, Object> copy = new LinkedHashMap<>();
                    if (str.contains(" - ")) {
                        String[] p = str.split(" - ", 2);
                        copy.put("name", p[0].trim());
                        copy.put("channel", p[1].trim());
                    } else {
                        copy.put("name", str.trim());
                        copy.put("channel", str.trim());
                    }
                    creatorList.add(copy);
                }
            }
        }

        if (creatorList.isEmpty()) {
            sender.sendMessage(MessageUtil.format("&9MoonLight &8❙ &cNo YouTube creators registered in config yet. Add one first via &e/yt add <name> <url>&c."));
            return;
        }

        // Find the matched creator map
        Map<String, Object> matchedMap = null;
        for (Map<String, Object> m : creatorList) {
            String name = String.valueOf(m.get("name"));
            if (name.equalsIgnoreCase(targetName)) {
                matchedMap = m;
                break;
            }
        }

        if (matchedMap == null) {
            // Partial / URL match
            String targetClean = targetName.replace("https://www.youtube.com/", "")
                    .replace("https://youtube.com/", "")
                    .replace("www.youtube.com/", "")
                    .replace("youtube.com/", "")
                    .replace("@", "")
                    .toLowerCase(Locale.ROOT);
            for (Map<String, Object> m : creatorList) {
                String name = String.valueOf(m.get("name")).toLowerCase(Locale.ROOT);
                String channel = String.valueOf(m.get("channel")).toLowerCase(Locale.ROOT);
                if (name.contains(targetClean) || channel.contains(targetClean)) {
                    matchedMap = m;
                    break;
                }
            }
        }

        if (matchedMap == null) {
            sender.sendMessage(MessageUtil.format("&9MoonLight &8❙ &cNo YouTube creator found matching &e'" + targetName + "'&c."));
            sender.sendMessage(MessageUtil.format("&7Type &e/yt&7 to view registered creator names."));
            return;
        }

        String matchedName = String.valueOf(matchedMap.get("name"));

        if (isReset) {
            matchedMap.remove("priority");
            recompactPriorities(creatorList);
            plugin.getConfig().set("youtube.creators", creatorList);
            plugin.saveConfig();

            sender.sendMessage(MessageUtil.format("&9MoonLight &8❙ &aRemoved custom priority from &e" + matchedName + "&a! They now appear in their added order."));
            if (sender instanceof Player player) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 1.2f);
            }
            return;
        }

        // Setting a priority:
        // First remove this creator's existing priority so we shift others cleanly
        matchedMap.remove("priority");

        // Shift any creator that has priority >= targetPriority by +1
        for (Map<String, Object> m : creatorList) {
            if (m == matchedMap) continue;
            int p = getMapPriority(m);
            if (p >= targetPriority) {
                m.put("priority", p + 1);
            }
        }

        // Assign the target priority
        matchedMap.put("priority", targetPriority);

        // Re-compact priorities sequentially (1, 2, 3...)
        recompactPriorities(creatorList);

        plugin.getConfig().set("youtube.creators", creatorList);
        plugin.saveConfig();

        if (sender instanceof Player player) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.4f);
        }

        sender.sendMessage(Component.empty());
        sender.sendMessage(MessageUtil.format(
                "<gradient:#ff0000:#ff4444><bold>▶ YOUTUBER PRIORITY UPDATED</bold></gradient>"));
        sender.sendMessage(MessageUtil.format("&7Creator: &f&l" + matchedName));
        sender.sendMessage(MessageUtil.format("&7Assigned Rank: &b&lTop #" + targetPriority));
        sender.sendMessage(MessageUtil.format("&aThis creator will now always appear at position &e#" + targetPriority + "&a in &e/yt&a."));
        sender.sendMessage(Component.empty());
    }

    private int getMapPriority(Map<String, Object> map) {
        if (map == null || !map.containsKey("priority")) return 0;
        try {
            return Integer.parseInt(String.valueOf(map.get("priority")));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private void recompactPriorities(List<Map<String, Object>> list) {
        List<Map<String, Object>> prioritized = new ArrayList<>();
        for (Map<String, Object> m : list) {
            if (getMapPriority(m) > 0) {
                prioritized.add(m);
            }
        }

        // Sort by their current priority number ascending
        prioritized.sort(Comparator.comparingInt(this::getMapPriority));

        // Re-number 1, 2, 3...
        int p = 1;
        for (Map<String, Object> m : prioritized) {
            m.put("priority", p++);
        }
    }

    public void displayCreatorsList(CommandSender sender, int requestedPage) {
        List<CreatorChannel> creators = getCreators();
        if (creators.isEmpty()) {
            sender.sendMessage(Component.empty());
            sender.sendMessage(MessageUtil.format("<gradient:#ff0000:#ff4444><bold>▶ MOONLIGHT SMP YOUTUBERS</bold></gradient>"));
            sender.sendMessage(MessageUtil.format("<gray>No server YouTubers registered yet.</gray>"));
            if (isOp(sender)) {
                sender.sendMessage(MessageUtil.format("<yellow>Add channels using: </yellow><white>/yt add <name> <url> [description]</white>"));
            } else {
                sender.sendMessage(MessageUtil.format("<gray>Want to apply for official YouTuber rank? Join </gray><yellow>/discord</yellow>"));
            }
            sender.sendMessage(Component.empty());
            return;
        }

        int maxPages = Math.max(1, (int) Math.ceil((double) creators.size() / ITEMS_PER_PAGE));
        int page = requestedPage;
        if (page < 1) page = 1;
        if (page > maxPages) page = maxPages;

        int startIndex = (page - 1) * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, creators.size());

        // Header
        sender.sendMessage(Component.empty());
        sender.sendMessage(MessageUtil.format(
                "<gradient:#ff0000:#ff4444><bold>▶ MOONLIGHT SMP YOUTUBERS & CREATORS</bold></gradient> <dark_gray>▪</dark_gray> <gray>Page <yellow>" + page + "</yellow>/<yellow>" + maxPages + "</yellow></gray>"
        ));
        sender.sendMessage(MessageUtil.format("<gray>Official content creators representing Moonlight SMP:</gray>"));
        sender.sendMessage(MessageUtil.format("<dark_gray>──────────────────────────────────────────────────</dark_gray>"));

        // Creators list
        for (int i = startIndex; i < endIndex; i++) {
            CreatorChannel creator = creators.get(i);
            int number = i + 1;

            Component numComp = MessageUtil.format("<red><bold>" + number + ". </bold></red>");
            Component nameComp = MessageUtil.format("<white><bold>" + creator.getName() + "</bold></white> ");

            Component hoverTooltip = MessageUtil.format(
                    "<red><bold>" + creator.getName() + "</bold></red><newline>" +
                    (creator.getDescription() != null ? "<gray>" + creator.getDescription() + "</gray><newline>" : "") +
                    "<aqua><underlined>" + creator.getChannelUrl() + "</underlined></aqua><newline>" +
                    "<yellow>Click to visit channel in web browser!</yellow>"
            );

            Component visitButton = MessageUtil.format("<gradient:#ff0000:#ff4444><bold>[VISIT CHANNEL]</bold></gradient>")
                    .clickEvent(ClickEvent.openUrl(creator.getChannelUrl()))
                    .hoverEvent(HoverEvent.showText(hoverTooltip));

            Component channelLine = Component.empty()
                    .append(numComp)
                    .append(nameComp)
                    .append(visitButton)
                    .clickEvent(ClickEvent.openUrl(creator.getChannelUrl()))
                    .hoverEvent(HoverEvent.showText(hoverTooltip));

            sender.sendMessage(channelLine);

            if (creator.getDescription() != null && !creator.getDescription().isEmpty()) {
                sender.sendMessage(MessageUtil.format("   <dark_gray>↳ </dark_gray><gray>" + creator.getDescription() + "</gray>"));
            }
        }

        // Footer with clickable pagination buttons
        sender.sendMessage(MessageUtil.format("<dark_gray>──────────────────────────────────────────────────</dark_gray>"));

        Component prevButton;
        if (page > 1) {
            prevButton = MessageUtil.format("<green><bold>[← Previous]</bold></green>")
                    .clickEvent(ClickEvent.runCommand("/yt " + (page - 1)))
                    .hoverEvent(HoverEvent.showText(MessageUtil.format("<yellow>Click to view page " + (page - 1) + "</yellow>")));
        } else {
            prevButton = MessageUtil.format("<dark_gray>[← Previous]</dark_gray>")
                    .hoverEvent(HoverEvent.showText(MessageUtil.format("<red>You are already on the first page.</red>")));
        }

        Component nextButton;
        if (page < maxPages) {
            nextButton = MessageUtil.format("<green><bold>[Next →]</bold></green>")
                    .clickEvent(ClickEvent.runCommand("/yt " + (page + 1)))
                    .hoverEvent(HoverEvent.showText(MessageUtil.format("<yellow>Click to view page " + (page + 1) + "</yellow>")));
        } else {
            nextButton = MessageUtil.format("<dark_gray>[Next →]</dark_gray>")
                    .hoverEvent(HoverEvent.showText(MessageUtil.format("<red>You are on the last page.</red>")));
        }

        Component pageIndicator = MessageUtil.format("<gray> Page <yellow>" + page + "</yellow>/<yellow>" + maxPages + "</yellow> (" + creators.size() + " creators) </gray>");

        Component footer = Component.empty()
                .append(prevButton)
                .append(pageIndicator)
                .append(nextButton);

        sender.sendMessage(footer);

        if (!isOp(sender)) {
            sender.sendMessage(MessageUtil.format("<dark_gray>💡 </dark_gray><gray>Want to apply for official YouTuber rank? Join </gray><yellow>/discord</yellow>"));
        }
        sender.sendMessage(Component.empty());

        if (sender instanceof Player player) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        String lowerAlias = alias != null ? alias.toLowerCase(Locale.ROOT) : "";
        boolean isOp = isOp(sender);

        if (lowerAlias.equals("ytpriority") || lowerAlias.equals("setytpriority")) {
            if (!isOp) return Collections.emptyList();
            if (args.length == 1) {
                List<String> names = new ArrayList<>();
                for (CreatorChannel c : getCreators()) {
                    if (c.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                        names.add(c.getName());
                    }
                }
                return names;
            }
            if (args.length == 2) {
                List<String> options = List.of("1", "2", "3", "reset");
                List<String> matches = new ArrayList<>();
                for (String opt : options) {
                    if (opt.startsWith(args[1].toLowerCase())) matches.add(opt);
                }
                return matches;
            }
            return Collections.emptyList();
        }

        if (lowerAlias.equals("addyoutube") || lowerAlias.equals("addyt")) {
            if (!isOp) return Collections.emptyList();
            if (args.length == 1) {
                List<String> list = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                        list.add(p.getName());
                    }
                }
                return list;
            }
            if (args.length == 2) {
                return List.of("https://www.youtube.com/@", "@");
            }
            return Collections.emptyList();
        }

        if (lowerAlias.equals("removeyt") || lowerAlias.equals("delyt")
                || lowerAlias.equals("removeyoutube") || lowerAlias.equals("delyoutube")
                || lowerAlias.equals("removelink") || lowerAlias.equals("dellink")) {
            if (!isOp) return Collections.emptyList();
            if (args.length == 1) {
                List<String> names = new ArrayList<>();
                for (CreatorChannel c : getCreators()) {
                    if (c.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                        names.add(c.getName());
                    }
                }
                return names;
            }
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            int maxPages = getMaxPages();
            for (int i = 1; i <= maxPages; i++) {
                String p = String.valueOf(i);
                if (p.startsWith(args[0])) suggestions.add(p);
            }
            if (isOp) {
                for (String sub : List.of("priority", "add", "remove", "delete", "clear", "reload", "list")) {
                    if (sub.startsWith(args[0].toLowerCase())) suggestions.add(sub);
                }
            }
            return suggestions;
        }

        if (args.length == 2 && isOp) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("priority") || sub.equals("setpriority") || sub.equals("order") || sub.equals("rank")) {
                List<String> names = new ArrayList<>();
                for (CreatorChannel c : getCreators()) {
                    if (c.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        names.add(c.getName());
                    }
                }
                return names;
            }
            if (sub.equals("remove") || sub.equals("delete") || sub.equals("del")) {
                List<String> names = new ArrayList<>();
                for (CreatorChannel c : getCreators()) {
                    if (c.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        names.add(c.getName());
                    }
                }
                return names;
            }
            if (sub.equals("add")) {
                List<String> list = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        list.add(p.getName());
                    }
                }
                return list;
            }
        }

        if (args.length == 3 && isOp) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("priority") || sub.equals("setpriority") || sub.equals("order") || sub.equals("rank")) {
                List<String> options = List.of("1", "2", "3", "reset");
                List<String> matches = new ArrayList<>();
                for (String opt : options) {
                    if (opt.startsWith(args[2].toLowerCase())) matches.add(opt);
                }
                return matches;
            }
            if (sub.equals("add")) {
                return List.of("https://www.youtube.com/@", "@");
            }
        }

        return Collections.emptyList();
    }
}
