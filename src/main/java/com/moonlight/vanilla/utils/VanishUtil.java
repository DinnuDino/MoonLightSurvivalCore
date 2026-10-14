package com.moonlight.vanilla.utils;

import com.moonlight.vanilla.MoonlightVanillaSurvivalCore;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * VanishUtil
 * Seamless bridge for SuperVanish, PremiumVanish, EssentialsX Vanish, and Bukkit metadata.
 * Provides unified visibility checks, online player filtering, and dynamic event hooks
 * so MoonlightVanillaSurvivalCore and TAB operate in total harmony without leaking vanished staff.
 */
public final class VanishUtil {

    private VanishUtil() {}

    private static Boolean superVanishPresent = null;
    private static Class<?> vanishApiClass = null;
    private static Method isInvisibleMethod = null;
    private static Method canSeeMethod = null;

    private static synchronized void initSuperVanish() {
        if (superVanishPresent != null) return;
        try {
            vanishApiClass = Class.forName("de.myzelyam.api.vanish.VanishAPI");
            isInvisibleMethod = vanishApiClass.getMethod("isInvisible", Player.class);
            canSeeMethod = vanishApiClass.getMethod("canSee", Player.class, Player.class);
            superVanishPresent = true;
        } catch (Throwable t) {
            superVanishPresent = false;
        }
    }

    /**
     * Checks if a player is currently vanished via SuperVanish, PremiumVanish,
     * Bukkit "vanished" metadata, or EssentialsX.
     */
    public static boolean isVanished(Player player) {
        if (player == null || !player.isOnline()) {
            return false;
        }

        // 1. SuperVanish / PremiumVanish API
        initSuperVanish();
        if (Boolean.TRUE.equals(superVanishPresent) && isInvisibleMethod != null) {
            try {
                Object res = isInvisibleMethod.invoke(null, player);
                if (res instanceof Boolean b && b) {
                    return true;
                }
            } catch (Throwable ignored) {}
        }

        // 2. Bukkit Metadata tag "vanished" (standard across SuperVanish, PV, VanishNoPacket, Essentials)
        try {
            if (player.hasMetadata("vanished")) {
                for (MetadataValue meta : player.getMetadata("vanished")) {
                    if (meta.asBoolean()) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {}

        // 3. EssentialsX Vanish API reflection
        try {
            Plugin ess = Bukkit.getPluginManager().getPlugin("Essentials");
            if (ess != null && ess.isEnabled()) {
                Method getUserMethod = ess.getClass().getMethod("getUser", Player.class);
                Object user = getUserMethod.invoke(ess, player);
                if (user != null) {
                    Method isVanishedMethod = user.getClass().getMethod("isVanished");
                    Object res = isVanishedMethod.invoke(user);
                    if (res instanceof Boolean b && b) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {}

        return false;
    }

    /**
     * Checks if a player is vanished or has permission to join vanished on join.
     */
    public static boolean isVanishedOnJoin(Player player) {
        if (player == null) return false;
        if (isVanished(player)) return true;
        return player.hasPermission("sv.joinvanished") ||
               player.hasPermission("pv.joinvanished") ||
               player.hasPermission("vanish.joinvanished");
    }

    /**
     * Checks if a viewer (Player or Console) can see a target player.
     * Respects SuperVanish's canSee logic, sv.see / pv.see permissions, and Bukkit canSee.
     */
    public static boolean canSee(CommandSender viewer, Player target) {
        if (target == null) return false;
        if (viewer == null) return true; // Console or internal caller can see all

        if (!(viewer instanceof Player playerViewer)) {
            return true; // Console can see everyone
        }

        if (playerViewer.getUniqueId().equals(target.getUniqueId())) {
            return true; // Self visibility is always true
        }

        // If target is vanished
        if (isVanished(target)) {
            // Check SuperVanish canSee API
            initSuperVanish();
            if (Boolean.TRUE.equals(superVanishPresent) && canSeeMethod != null) {
                try {
                    Object res = canSeeMethod.invoke(null, playerViewer, target);
                    if (res instanceof Boolean b) {
                        return b;
                    }
                } catch (Throwable ignored) {}
            }

            // Check staff bypass permissions
            if (playerViewer.hasPermission("sv.see") ||
                playerViewer.hasPermission("pv.see") ||
                playerViewer.hasPermission("vanish.see") ||
                playerViewer.hasPermission("moonlight.vanish.see") ||
                playerViewer.isOp()) {
                return true;
            }

            return false;
        }

        // If target is not vanished by plugin, check Bukkit's player visibility
        try {
            return playerViewer.canSee(target);
        } catch (Throwable ignored) {
            return true;
        }
    }

    /**
     * Returns the count of online players visible to the specified viewer.
     */
    public static int getVisibleOnlineCount(Player viewer) {
        int count = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (canSee(viewer, p)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Returns a filtered list of online players that the sender can see (for tab completion).
     */
    public static List<Player> getVisibleOnlinePlayers(CommandSender sender) {
        List<Player> visible = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (canSee(sender, p)) {
                visible.add(p);
            }
        }
        return visible;
    }

    /**
     * Dynamically registers an event hook for SuperVanish / PremiumVanish's PlayerVanishStateChangeEvent.
     * When staff toggle vanish, updates all player scoreboards and lists in real-time.
     */
    public static void registerVanishHook(MoonlightVanillaSurvivalCore plugin) {
        try {
            Class<?> eventClass = Class.forName("de.myzelyam.api.vanish.PlayerVanishStateChangeEvent");
            @SuppressWarnings("unchecked")
            Class<? extends Event> bukkitEventClass = (Class<? extends Event>) eventClass;

            Bukkit.getPluginManager().registerEvent(
                bukkitEventClass,
                new Listener() {},
                EventPriority.MONITOR,
                (listener, event) -> {
                    if (eventClass.isInstance(event)) {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            if (plugin.getScoreboardManager() != null) {
                                plugin.getScoreboardManager().updateAll();
                            }
                        });
                    }
                },
                plugin
            );
            plugin.getLogger().info("SuperVanish/PremiumVanish integration active: Vanish events hooked dynamically for real-time scoreboard & visibility sync.");
        } catch (ClassNotFoundException e) {
            plugin.getLogger().info("SuperVanish plugin API not loaded on classpath (using metadata/Bukkit visibility fallback).");
        } catch (Throwable t) {
            plugin.getLogger().warning("Could not register SuperVanish dynamic listener: " + t.getMessage());
        }
    }
}
