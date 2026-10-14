package com.moonlight.vanilla.modules.chat.listeners;

import com.moonlight.vanilla.MoonlightVanillaSurvivalCore;
import com.moonlight.vanilla.modules.chat.commands.MessageCommand;
import com.moonlight.vanilla.utils.MessageUtil;
import com.moonlight.vanilla.utils.VanishUtil;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ChatAndJoinListener
 * Handles custom join/quit announcements ([+] and [-]) and unique player milestone counts.
 * Enforces server mute restrictions without modifying chat prefixes, formats, or tablist names,
 * ensuring 100% compatibility with LuckPerms, LPC, EssentialsChat, TAB, DeluxeChat, and InteractiveChat.
 */
public class ChatAndJoinListener implements Listener {

    private final MoonlightVanillaSurvivalCore plugin;
    private final MessageCommand messageCommand;
    private final AtomicInteger uniquePlayerCount = new AtomicInteger(0);
    private File counterFile;
    private FileConfiguration counterConfig;

    public ChatAndJoinListener(MoonlightVanillaSurvivalCore plugin, MessageCommand messageCommand) {
        this.plugin = plugin;
        this.messageCommand = messageCommand;
        loadPlayerCounter();
    }

    private void loadPlayerCounter() {
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            this.counterFile = new File(plugin.getDataFolder(), "unique_players.yml");
            if (!counterFile.exists()) {
                counterFile.createNewFile();
            }
            this.counterConfig = YamlConfiguration.loadConfiguration(counterFile);
            int count = counterConfig.getInt("unique_player_count", 0);
            this.uniquePlayerCount.set(count);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to initialize unique_players.yml: " + e.getMessage());
        }
    }

    private synchronized int incrementAndSaveCount() {
        int next = uniquePlayerCount.incrementAndGet();
        if (counterConfig != null && counterFile != null) {
            counterConfig.set("unique_player_count", next);
            try {
                counterConfig.save(counterFile);
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to save unique_player_count: " + e.getMessage());
            }
        }
        return next;
    }

    public int getUniquePlayerCount() {
        return uniquePlayerCount.get();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // SuperVanish / PremiumVanish join detection: Suppress join announcements completely if vanished
        boolean vanishedOnJoin = VanishUtil.isVanishedOnJoin(player);
        if (vanishedOnJoin) {
            event.joinMessage(null);
        } else {
            // Check if brand new player joining for the very first time
            if (!player.hasPlayedBefore()) {
                int count = incrementAndSaveCount();
                String formattedNumber = NumberFormat.getNumberInstance(Locale.US).format(count);
                String ordinalSuffix = getOrdinalSuffix(count);

                // Broadcast new player counter: #14,674th player Name!
                Component newPlayerBroadcast = MessageUtil.format(
                        "<yellow>#" + formattedNumber + ordinalSuffix + "</yellow> " +
                        "<green>player</green> " +
                        "<yellow>" + player.getName() + "!</yellow>"
                );
                Bukkit.broadcast(newPlayerBroadcast);
            }

            // Custom join message format: [&8[&a+&8]&r] PlayerName
            event.joinMessage(MessageUtil.format("<dark_gray>[</dark_gray><green>+</green><dark_gray>]</dark_gray> <white>" + player.getName() + "</white>"));
        }

        // Initialize personal scoreboard
        if (plugin.getScoreboardManager() != null) {
            plugin.getScoreboardManager().setupScoreboard(player);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    plugin.getScoreboardManager().ensureScoreboard(player);
                }
            }, 10L);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    plugin.getScoreboardManager().ensureScoreboard(player);
                }
            }, 30L);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                plugin.getScoreboardManager().updateAll();
            }, 5L);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // SuperVanish / PremiumVanish quit detection: Suppress quit announcements completely if vanished
        if (VanishUtil.isVanished(player)) {
            event.quitMessage(null);
        } else {
            // Custom quit message format: [&8[&c-&8]&r] PlayerName
            event.quitMessage(MessageUtil.format("<dark_gray>[</dark_gray><red>-</red><dark_gray>]</dark_gray> <white>" + player.getName() + "</white>"));
        }

        // Remove personal scoreboard and cleanup reply mappings
        if (plugin.getScoreboardManager() != null) {
            plugin.getScoreboardManager().removeScoreboard(player);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                plugin.getScoreboardManager().updateAll();
            }, 2L);
        }
        if (messageCommand != null) {
            messageCommand.clearHistory(player.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        // Real-time update for victim's death counter on scoreboard
        if (plugin.getScoreboardManager() != null) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (victim.isOnline()) {
                    plugin.getScoreboardManager().updatePlayer(victim);
                }
            });
        }

        // Real-time update for killer's kill counter on scoreboard
        if (killer != null && !killer.getUniqueId().equals(victim.getUniqueId())) {
            if (plugin.getScoreboardManager() != null) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (killer.isOnline()) {
                        plugin.getScoreboardManager().updatePlayer(killer);
                    }
                });
            }
        }
    }

    /**
     * Paper Native AsyncChatEvent.
     * Enforces active server mutes without interfering with chat formatting, prefixes,
     * or third-party chat management plugins (LuckPerms, LPC, EssentialsChat, InteractiveChat, DiscordSRV).
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPaperChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        
        // Mute check
        if (plugin.getMuteManager() != null && plugin.getMuteManager().isMuted(player)) {
            com.moonlight.vanilla.modules.punishments.MuteRecord mute = plugin.getMuteManager().getMute(player);
            if (mute != null) {
                if (mute.isPermanent()) {
                    player.sendMessage(MessageUtil.format("<red>You are permanently muted, you cannot chat with others.</red>"));
                } else {
                    player.sendMessage(MessageUtil.format("<red>You are muted, cannot chat for " + mute.getFormattedRemaining() + ".</red>"));
                }
                event.setCancelled(true);
            }
        }
    }

    /**
     * Bukkit/Spigot AsyncPlayerChatEvent.
     * Enforces active server mutes without overriding chat formats or prefixes.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        
        // Mute check
        if (plugin.getMuteManager() != null && plugin.getMuteManager().isMuted(player)) {
            com.moonlight.vanilla.modules.punishments.MuteRecord mute = plugin.getMuteManager().getMute(player);
            if (mute != null) {
                if (mute.isPermanent()) {
                    player.sendMessage(MessageUtil.format("<red>You are permanently muted, you cannot chat with others.</red>"));
                } else {
                    player.sendMessage(MessageUtil.format("<red>You are muted, cannot chat for " + mute.getFormattedRemaining() + ".</red>"));
                }
                event.setCancelled(true);
            }
        }
    }

    /**
     * Hover card for /msg and direct messaging utilities.
     */
    public static Component createPlayerHoverCard(Player player) {
        if (player == null) return Component.empty();
        try {
            String playtime = formatPlaytime(player);
            int ping = player.getPing();

            return MessageUtil.format(
                    "<white>Player: </white><yellow>" + player.getName() + "</yellow><newline>" +
                    "<white>Playtime: </white><#ff7777>" + playtime + "</#ff7777><newline>" +
                    "<white>Ping: </white><yellow>" + ping + "</yellow> <aqua>ms</aqua><newline>" +
                    "<gray><italic>Click to message</italic></gray>"
            );
        } catch (Throwable t) {
            return MessageUtil.format("<white>Player: </white><yellow>" + player.getName() + "</yellow>");
        }
    }

    public static String formatPlaytime(Player player) {
        try {
            int ticks = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
            long totalSeconds = ticks / 20L;
            long hours = totalSeconds / 3600;
            long minutes = (totalSeconds % 3600) / 60;
            if (hours > 0) {
                return hours + "h " + minutes + "m";
            }
            return minutes + "m";
        } catch (Throwable t) {
            return "0m";
        }
    }

    public static String getOrdinalSuffix(int n) {
        int mod100 = n % 100;
        int mod10 = n % 10;
        if (mod100 >= 11 && mod100 <= 13) {
            return "th";
        }
        return switch (mod10) {
            case 1 -> "st";
            case 2 -> "nd";
            case 3 -> "rd";
            default -> "th";
        };
    }
}
