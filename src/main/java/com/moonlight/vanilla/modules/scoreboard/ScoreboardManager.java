package com.moonlight.vanilla.modules.scoreboard;

import com.moonlight.vanilla.MoonlightVanillaSurvivalCore;
import com.moonlight.vanilla.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ScoreboardManager
 * Handles sidebar scoreboards, hides vanilla score numbers (7, 6, 5...) using Paper Modern API,
 * and dynamically manages logo vs text header with smart fallback for all clients/launchers.
 */
public class ScoreboardManager implements Listener {
    private final MoonlightVanillaSurvivalCore plugin;
    private final Set<UUID> hiddenPlayers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> packLoadedPlayers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> noPackPlayers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> preferTextPlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Scoreboard> playerScoreboards = new ConcurrentHashMap<>();
    private BukkitTask updateTask;

    public ScoreboardManager(MoonlightVanillaSurvivalCore plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        this.updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateAll, 20L, 20L);
    }

    public void shutdown() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            removeScoreboard(p);
        }
        playerScoreboards.clear();
        hiddenPlayers.clear();
        packLoadedPlayers.clear();
        noPackPlayers.clear();
        preferTextPlayers.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onResourcePackStatus(PlayerResourcePackStatusEvent event) {
        Player player = event.getPlayer();
        switch (event.getStatus()) {
            case SUCCESSFULLY_LOADED -> {
                packLoadedPlayers.add(player.getUniqueId());
                noPackPlayers.remove(player.getUniqueId());
                updatePlayer(player);
            }
            case DECLINED, FAILED_DOWNLOAD -> {
                noPackPlayers.add(player.getUniqueId());
                packLoadedPlayers.remove(player.getUniqueId());
                updatePlayer(player);
            }
            default -> {}
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        hiddenPlayers.remove(uuid);
        packLoadedPlayers.remove(uuid);
        noPackPlayers.remove(uuid);
        preferTextPlayers.remove(uuid);
        playerScoreboards.remove(uuid);
    }

    public void setupScoreboard(Player player) {
        if (player == null || !player.isOnline()) return;
        if (hiddenPlayers.contains(player.getUniqueId())) return;
        org.bukkit.scoreboard.ScoreboardManager sm = Bukkit.getScoreboardManager();
        Scoreboard board = sm.getNewScoreboard();
        playerScoreboards.put(player.getUniqueId(), board);
        String title = getScoreboardTitle(player);
        Objective obj = board.registerNewObjective("moonlight_sb", Criteria.DUMMY,
                MessageUtil.format(title));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        // Blank out the red score numbers (7, 6, 5...) on Paper 1.20.3+ / 1.21.x
        applyNumberFormatBlank(obj);

        player.setScoreboard(board);
        updatePlayer(player);
    }

    public void removeScoreboard(Player player) {
        if (player == null) return;
        playerScoreboards.remove(player.getUniqueId());
        try {
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        } catch (Exception ignored) {}
    }

    public boolean toggleScoreboard(Player player) {
        if (player == null) return false;
        UUID uuid = player.getUniqueId();
        Scoreboard board = playerScoreboards.get(uuid);
        boolean currentlyActive = !hiddenPlayers.contains(uuid) && board != null && player.getScoreboard() == board;
        if (currentlyActive) {
            hiddenPlayers.add(uuid);
            removeScoreboard(player);
            return false;
        } else {
            hiddenPlayers.remove(uuid);
            setupScoreboard(player);
            return true;
        }
    }

    public boolean toggleTitleMode(Player player) {
        if (player == null) return false;
        UUID uuid = player.getUniqueId();
        if (preferTextPlayers.contains(uuid)) {
            preferTextPlayers.remove(uuid);
            updatePlayer(player);
            return true; // Now in Graphic Logo mode
        } else {
            preferTextPlayers.add(uuid);
            updatePlayer(player);
            return false; // Now in Clean Text mode
        }
    }

    public boolean isUsingTextMode(Player player) {
        return player != null && preferTextPlayers.contains(player.getUniqueId());
    }

    public boolean showScoreboard(Player player) {
        if (player == null) return false;
        UUID uuid = player.getUniqueId();
        hiddenPlayers.remove(uuid);
        setupScoreboard(player);
        return true;
    }

    public void ensureScoreboard(Player player) {
        if (player == null || !player.isOnline()) return;
        if (hiddenPlayers.contains(player.getUniqueId())) return;
        Scoreboard board = playerScoreboards.get(player.getUniqueId());
        if (board == null) {
            setupScoreboard(player);
            return;
        }
        if (player.getScoreboard() != board) {
            player.setScoreboard(board);
        }
        updatePlayer(player);
    }

    public boolean hideScoreboard(Player player) {
        UUID uuid = player.getUniqueId();
        if (hiddenPlayers.add(uuid)) {
            removeScoreboard(player);
            return true;
        }
        return false;
    }

    public boolean isHidden(Player player) {
        return hiddenPlayers.contains(player.getUniqueId());
    }

    public void updateAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!hiddenPlayers.contains(player.getUniqueId())) {
                updatePlayer(player);
            }
        }
    }

    public void updatePlayer(Player player) {
        if (player == null || !player.isOnline()) return;
        Scoreboard board = playerScoreboards.get(player.getUniqueId());
        if (board == null) {
            setupScoreboard(player);
            return;
        }
        if (player.getScoreboard() != board && !hiddenPlayers.contains(player.getUniqueId())) {
            player.setScoreboard(board);
        }
        Objective obj = board.getObjective("moonlight_sb");
        String title = getScoreboardTitle(player);
        if (obj == null) {
            obj = board.registerNewObjective("moonlight_sb", Criteria.DUMMY,
                    MessageUtil.format(title));
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        } else {
            obj.displayName(MessageUtil.format(title));
        }

        applyNumberFormatBlank(obj);

        int kills = player.getStatistic(Statistic.PLAYER_KILLS);
        int deaths = player.getStatistic(Statistic.DEATHS);
        int ping = player.getPing();
        String playtime = formatPlaytime(player);

        List<String> configLines = plugin.getConfig().getStringList("scoreboard.lines");
        if (configLines == null || configLines.isEmpty()) {
            configLines = List.of(
                    "<gray>Player: </gray><white>{player}</white>",
                    "<gray>Playtime: </gray><yellow>{playtime}</yellow>",
                    "<gray>Kills: </gray><green>{kills}</green>",
                    "<gray>Deaths: </gray><red>{deaths}</red>",
                    "<gray>Ping: </gray>{ping}",
                    "<dark_gray>-----------------</dark_gray>",
                    "<gradient:#38bdf8:#1d4ed8>www.moonlightsmp.info</gradient>"
            );
        }

        int total = configLines.size();
        for (int i = 0; i < total; i++) {
            String line = configLines.get(i);
            line = line.replace("{player}", player.getName())
                       .replace("{health}", String.valueOf(Math.round(player.getHealth())))
                       .replace("{playtime}", playtime)
                       .replace("{kills}", String.valueOf(kills))
                       .replace("{deaths}", String.valueOf(deaths))
                       .replace("{ping}", formatPing(ping));

            line = parsePlaceholders(player, line);

            String teamName = "sb_line_" + i;
            Team team = board.getTeam(teamName);
            String entry = getUniqueEntry(i);
            if (team == null) {
                team = board.registerNewTeam(teamName);
                team.addEntry(entry);
            }
            team.prefix(MessageUtil.format(line));
            
            Score score = obj.getScore(entry);
            score.setScore(total - i);
            applyScoreNumberFormatBlank(score);
        }

        // Clean up excess entries if line count was reduced
        for (int i = total; i < 16; i++) {
            String entry = getUniqueEntry(i);
            board.resetScores(entry);
            Team oldTeam = board.getTeam("sb_line_" + i);
            if (oldTeam != null) {
                oldTeam.unregister();
            }
        }
    }

    private void applyNumberFormatBlank(Objective obj) {
        if (!plugin.getConfig().getBoolean("scoreboard.hide-numbers", true)) return;
        try {
            obj.numberFormat(io.papermc.paper.scoreboard.numbers.NumberFormat.blank());
        } catch (Throwable ignored) {}
    }

    private void applyScoreNumberFormatBlank(Score score) {
        if (!plugin.getConfig().getBoolean("scoreboard.hide-numbers", true)) return;
        try {
            score.numberFormat(io.papermc.paper.scoreboard.numbers.NumberFormat.blank());
        } catch (Throwable ignored) {}
    }

    private String parsePlaceholders(Player player, String text) {
        if (text == null || text.isEmpty()) return "";
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                Class<?> papiClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
                java.lang.reflect.Method setPlaceholdersMethod = papiClass.getMethod("setPlaceholders", Player.class, String.class);
                return (String) setPlaceholdersMethod.invoke(null, player, text);
            } catch (Throwable ignored) {}
        }
        return text;
    }

    private String getUniqueEntry(int index) {
        return "\u00a7" + Integer.toHexString(index) + "\u00a7r";
    }

    private String formatPlaytime(Player player) {
        int ticks = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
        long totalSeconds = ticks / 20L;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        return minutes + "m";
    }

    private String formatPing(int ping) {
        if (ping <= 50) return "<green>" + ping + "ms</green>";
        if (ping <= 120) return "<yellow>" + ping + "ms</yellow>";
        return "<red>" + ping + "ms</red>";
    }

    public String getScoreboardTitle(Player player) {
        UUID uuid = player.getUniqueId();
        String logoTitle = plugin.getConfig().getString("scoreboard.title", "\uE001");
        String fallbackTitle = plugin.getConfig().getString("scoreboard.fallback-title", "<gradient:#00d2ff:#3a7bd5><bold>✦ MOONLIGHT SMP ✦</bold></gradient>");

        // 1. If player explicitly selected clean text title mode
        if (preferTextPlayers.contains(uuid)) {
            return fallbackTitle;
        }

        // 2. If smart fallback is enabled and player declined / failed to load pack
        boolean smartFallback = plugin.getConfig().getBoolean("scoreboard.smart-title-fallback", true);
        if (smartFallback && noPackPlayers.contains(uuid)) {
            return fallbackTitle;
        }

        // 3. Default to logo title if available
        if (logoTitle != null && !logoTitle.isEmpty()) {
            return unescapeUnicode(logoTitle);
        }
        return fallbackTitle;
    }

    private String unescapeUnicode(String str) {
        if (str == null || !str.contains("\\u")) return str;
        StringBuilder sb = new StringBuilder();
        int len = str.length();
        for (int i = 0; i < len; i++) {
            if (i <= len - 6 && str.charAt(i) == '\\' && str.charAt(i + 1) == 'u') {
                try {
                    int code = Integer.parseInt(str.substring(i + 2, i + 6), 16);
                    sb.append((char) code);
                    i += 5;
                    continue;
                } catch (NumberFormatException ignored) {}
            }
            sb.append(str.charAt(i));
        }
        return sb.toString();
    }
}
