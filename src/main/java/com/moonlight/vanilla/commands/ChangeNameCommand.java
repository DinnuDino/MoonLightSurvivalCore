package com.moonlight.vanilla.commands;

import com.moonlight.vanilla.MoonlightVanillaSurvivalCore;
import com.moonlight.vanilla.utils.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ChangeNameCommand (/changename, /mchangename, /setname)
 * Allows administrators to change or reset any player's display name and list name with persistence.
 * Sends polite change and reset requests to LuckPerms (metadata), Chat (display name), and TAB (custom tab/tag name)
 * without force-changing, clobbering chat prefixes, or breaking scoreboard teams.
 */
public class ChangeNameCommand implements CommandExecutor, TabCompleter, Listener {

    private final MoonlightVanillaSurvivalCore plugin;
    private final File storageFile;
    private final YamlConfiguration storageConfig;
    private final Map<UUID, String> customNames = new ConcurrentHashMap<>();

    public ChangeNameCommand(MoonlightVanillaSurvivalCore plugin) {
        this.plugin = plugin;
        this.storageFile = new File(plugin.getDataFolder(), "custom_names.yml");
        this.storageConfig = new YamlConfiguration();
        loadData();
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Apply custom names to all currently online players (e.g. after reload or startup)
        for (Player p : Bukkit.getOnlinePlayers()) {
            String custom = customNames.get(p.getUniqueId());
            if (custom != null && !custom.isBlank()) {
                applyCustomName(p, custom);
                requestTabChange(p.getUniqueId(), p.getName(), custom, false);
            }
        }
    }

    private void loadData() {
        try {
            if (!storageFile.exists()) {
                if (storageFile.getParentFile() != null) {
                    storageFile.getParentFile().mkdirs();
                }
                storageFile.createNewFile();
            }
            storageConfig.load(storageFile);
            ConfigurationSection sec = storageConfig.getConfigurationSection("names");
            if (sec != null) {
                for (String key : sec.getKeys(false)) {
                    try {
                        UUID uuid = UUID.fromString(key);
                        String raw = sec.getString(key + ".display");
                        if (raw != null && !raw.isBlank()) {
                            customNames.put(uuid, raw);
                        }
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Could not load custom_names.yml: " + e.getMessage());
        }
    }

    private synchronized void saveData() {
        try {
            storageConfig.set("names", null);
            for (Map.Entry<UUID, String> entry : customNames.entrySet()) {
                String key = "names." + entry.getKey();
                storageConfig.set(key + ".display", entry.getValue());
                OfflinePlayer off = Bukkit.getOfflinePlayer(entry.getKey());
                if (off.getName() != null) {
                    storageConfig.set(key + ".username", off.getName());
                }
            }
            storageConfig.save(storageFile);
        } catch (Exception e) {
            plugin.getLogger().warning("Could not save custom_names.yml: " + e.getMessage());
        }
    }

    public void applyCustomName(Player player, String rawName) {
        if (player == null || !player.isOnline()) return;
        Component comp = MessageUtil.format(rawName);
        String legacy = MessageUtil.toLegacy(comp);

        // Native Adventure Display Name & Tablist Name (Paper chat & tab)
        player.displayName(comp);
        player.playerListName(comp);
        player.customName(comp);
        player.setCustomNameVisible(true);

        // Bukkit / Spigot Legacy Display Name support for third-party chat plugins (LPC, EssentialsChat, Vault)
        try {
            player.setDisplayName(legacy);
            player.setPlayerListName(legacy);
            player.setCustomName(legacy);
        } catch (Throwable ignored) {}
    }

    public void resetCustomName(Player player) {
        if (player == null || !player.isOnline()) return;
        Component realNameComp = Component.text(player.getName());
        String realName = player.getName();

        // Native Adventure Reset (Paper)
        player.displayName(realNameComp);
        player.playerListName(null);
        player.customName(null);
        player.setCustomNameVisible(false);

        // Bukkit / Spigot Legacy Reset
        try {
            player.setDisplayName(realName);
            player.setPlayerListName(null);
            player.setCustomName(null);
        } catch (Throwable ignored) {}
    }

    /**
     * Converts a raw display name (containing MiniMessage tags, section symbols, or ampersand color codes)
     * into a standard ampersand-formatted color string (&b, &e, &l, etc.) consumable by LuckPerms and TAB.
     */
    public static String toColorAmpersand(String raw) {
        if (raw == null || raw.isBlank()) return "";
        if (raw.contains("<") && raw.contains(">")) {
            Component comp = MessageUtil.format(raw);
            String legacy = MessageUtil.toLegacy(comp);
            return legacy.replace('§', '&');
        }
        return raw.replace('§', '&');
    }

    /**
     * Sends a polite change or reset request to LuckPerms for displayname and nickname metadata.
     * No force change: Does not tamper with internal node engines or force clobbering.
     * If LuckPerms is absent, cleanly skips without error.
     */
    public void requestLuckPermsChange(UUID uuid, String username, String rawName, boolean isReset) {
        if (!Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
            return;
        }

        String targetIdentifier = (username != null && !username.isBlank())
                ? username
                : (uuid != null ? uuid.toString() : null);

        if (targetIdentifier == null) return;

        try {
            if (isReset) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + targetIdentifier + " meta unset displayname");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + targetIdentifier + " meta unset nickname");
            } else {
                String colorName = toColorAmpersand(rawName);
                String safe = colorName.replace("\"", "");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + targetIdentifier + " meta set displayname \"" + safe + "\"");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + targetIdentifier + " meta set nickname \"" + safe + "\"");
            }
        } catch (Throwable e) {
            plugin.getLogger().fine("LuckPerms request notice: " + e.getMessage());
        }

        // Supplementary: Check if LuckPerms Java API is loaded and sync cached user in memory
        try {
            Class<?> providerClass = Class.forName("net.luckperms.api.LuckPermsProvider");
            Object lp = providerClass.getMethod("get").invoke(null);
            if (lp != null && uuid != null) {
                Object userManager = lp.getClass().getMethod("getUserManager").invoke(lp);
                if (userManager != null) {
                    Method getUserMethod = userManager.getClass().getMethod("getUser", UUID.class);
                    Object user = getUserMethod.invoke(userManager, uuid);
                    if (user != null) {
                        Method saveMethod = userManager.getClass().getMethod("saveUser", user.getClass().getInterfaces()[0]);
                        saveMethod.invoke(userManager, user);
                    }
                }
            }
        } catch (Throwable ignored) {
            // Optional API reflection; console dispatch handles standard workflow
        }
    }

    /**
     * Sends a polite change or reset request to TAB (NEZNAMY) for custom tab name and tag name.
     * No force change: Does not create conflicting scoreboard teams, does not hijack packets.
     * If TAB is absent, cleanly skips without error.
     */
    public void requestTabChange(UUID uuid, String username, String rawName, boolean isReset) {
        if (!Bukkit.getPluginManager().isPluginEnabled("TAB")) {
            return;
        }

        String targetIdentifier = (username != null && !username.isBlank())
                ? username
                : (uuid != null ? uuid.toString() : null);

        if (targetIdentifier == null) return;

        // 1. Try TabAPI via reflection if present
        try {
            Class<?> tabApiClass = Class.forName("me.neznamy.tab.api.TabAPI");
            Object tabApi = tabApiClass.getMethod("getInstance").invoke(null);
            if (tabApi != null && uuid != null) {
                Method getPlayerMethod = tabApi.getClass().getMethod("getPlayer", UUID.class);
                Object tabPlayer = getPlayerMethod.invoke(tabApi, uuid);
                if (tabPlayer != null) {
                    try {
                        Method getTabListMgr = tabApi.getClass().getMethod("getTabListFormatManager");
                        Object tabListMgr = getTabListMgr.invoke(tabApi);
                        if (tabListMgr != null) {
                            for (Method m : tabListMgr.getClass().getMethods()) {
                                if (m.getName().equals("setName") && m.getParameterCount() == 2) {
                                    String colorName = isReset ? null : toColorAmpersand(rawName);
                                    m.invoke(tabListMgr, tabPlayer, colorName);
                                    break;
                                }
                            }
                        }
                    } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable ignored) {}

        // 2. Safe TAB Console Dispatch (Official TAB commands across all TAB versions)
        try {
            if (isReset) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tab player " + targetIdentifier + " customtabname remove");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tab player " + targetIdentifier + " customtagname remove");
            } else {
                String colorName = toColorAmpersand(rawName);
                String safe = colorName.replace("\"", "");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tab player " + targetIdentifier + " customtabname " + safe);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tab player " + targetIdentifier + " customtagname " + safe);
            }
        } catch (Throwable e) {
            plugin.getLogger().fine("TAB request notice: " + e.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String custom = customNames.get(player.getUniqueId());
        if (custom != null && !custom.isBlank()) {
            applyCustomName(player, custom);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline() && customNames.containsKey(player.getUniqueId())) {
                    String latestCustom = customNames.get(player.getUniqueId());
                    applyCustomName(player, latestCustom);
                    requestTabChange(player.getUniqueId(), player.getName(), latestCustom, false);
                }
            }, 5L);
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        boolean isOpOrPerm = !(sender instanceof Player)
                || sender.isOp()
                || sender.hasPermission("moonlight.core.admin")
                || sender.hasPermission("moonlight.admin");

        if (!isOpOrPerm) {
            sender.sendMessage(MessageUtil.format("&9MoonLight &8❙ &cAccess denied: You must be an administrator (OP or moonlight.core.admin) to use this command."));
            return true;
        }

        if (args.length < 2) {
            if (args.length == 1 && args[0].equalsIgnoreCase("list")) {
                if (customNames.isEmpty()) {
                    sender.sendMessage(MessageUtil.format("&9MoonLight &8❙ &7No players currently have custom display names set."));
                } else {
                    sender.sendMessage(MessageUtil.format("&9MoonLight &8❙ &6Active Changed Display Names (&e" + customNames.size() + "&6):"));
                    for (Map.Entry<UUID, String> entry : customNames.entrySet()) {
                        OfflinePlayer off = Bukkit.getOfflinePlayer(entry.getKey());
                        String name = off.getName() != null ? off.getName() : entry.getKey().toString();
                        sender.sendMessage(MessageUtil.format("  &8• &e" + name + " &8↳ " + entry.getValue()));
                    }
                }
                return true;
            }

            sender.sendMessage(MessageUtil.format("&9MoonLight &8❙ &cUsage: &e/changename <player> <new_name>"));
            sender.sendMessage(MessageUtil.format("&9MoonLight &8❙ &cReset: &e/changename <player> reset"));
            sender.sendMessage(MessageUtil.format("&7Examples:"));
            sender.sendMessage(MessageUtil.format("  &8• &f/changename Dinnu &b[VIP] Dinnu &7(Changes display nickname)"));
            sender.sendMessage(MessageUtil.format("  &8• &f/changename Dinnu reset &7(Restores original Minecraft name)"));
            sender.sendMessage(MessageUtil.format("  &8• &f/changename list &7(Lists all changed names)"));
            return true;
        }

        boolean isReset = false;
        String targetArg;
        String rawNameArg = null;

        if (args[0].equalsIgnoreCase("reset") || args[0].equalsIgnoreCase("clear")) {
            isReset = true;
            targetArg = args[1];
        } else if (args[1].equalsIgnoreCase("reset") || args[1].equalsIgnoreCase("clear")
                || args[1].equalsIgnoreCase("default") || args[1].equalsIgnoreCase("off")) {
            isReset = true;
            targetArg = args[0];
        } else {
            targetArg = args[0];
            rawNameArg = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        }

        Player target = Bukkit.getPlayer(targetArg);
        if (target != null && target.isOnline()) {
            UUID uuid = target.getUniqueId();
            String name = target.getName();
            if (isReset) {
                customNames.remove(uuid);
                saveData();
                resetCustomName(target);
                requestLuckPermsChange(uuid, name, null, true);
                requestTabChange(uuid, name, null, true);
                sender.sendMessage(MessageUtil.format("&9MoonLight &8❙ &aSuccessfully reset display name of &e" + name + "&a back to original."));
                target.sendMessage(MessageUtil.format("&9MoonLight &8❙ &aYour display name was reset back to original by an administrator."));
            } else {
                customNames.put(uuid, rawNameArg);
                saveData();
                applyCustomName(target, rawNameArg);
                requestLuckPermsChange(uuid, name, rawNameArg, false);
                requestTabChange(uuid, name, rawNameArg, false);
                sender.sendMessage(MessageUtil.format("&9MoonLight &8❙ &aChanged display name of &e" + name + "&a to: " + rawNameArg));
                target.sendMessage(MessageUtil.format("&9MoonLight &8❙ &aYour display name was updated to: " + rawNameArg));
            }
            return true;
        }

        // Support for offline players
        OfflinePlayer offline = Bukkit.getOfflinePlayer(targetArg);
        if (offline.hasPlayedBefore() || offline.getName() != null) {
            UUID uuid = offline.getUniqueId();
            String name = offline.getName() != null ? offline.getName() : targetArg;
            if (isReset) {
                customNames.remove(uuid);
                saveData();
                requestLuckPermsChange(uuid, name, null, true);
                requestTabChange(uuid, name, null, true);
                sender.sendMessage(MessageUtil.format("&9MoonLight &8❙ &aSuccessfully reset display name of offline player &e" + name + "&a back to original."));
            } else {
                customNames.put(uuid, rawNameArg);
                saveData();
                requestLuckPermsChange(uuid, name, rawNameArg, false);
                requestTabChange(uuid, name, rawNameArg, false);
                sender.sendMessage(MessageUtil.format("&9MoonLight &8❙ &aChanged display name of offline player &e" + name + "&a to: " + rawNameArg + " &7(will apply on login)."));
            }
            return true;
        }

        sender.sendMessage(MessageUtil.format("&9MoonLight &8❙ &cPlayer &e'" + targetArg + "'&c not found."));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            String q = args[0].toLowerCase(Locale.ROOT);
            List<String> list = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(q)) {
                    list.add(p.getName());
                }
            }
            if ("reset".startsWith(q)) list.add("reset");
            if ("list".startsWith(q)) list.add("list");
            return list;
        }

        if (args.length == 2) {
            String q = args[1].toLowerCase(Locale.ROOT);
            if (args[0].equalsIgnoreCase("reset")) {
                List<String> list = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase(Locale.ROOT).startsWith(q)) {
                        list.add(p.getName());
                    }
                }
                return list;
            } else {
                List<String> list = new ArrayList<>();
                if ("reset".startsWith(q)) list.add("reset");
                Player target = Bukkit.getPlayer(args[0]);
                if (target != null && customNames.containsKey(target.getUniqueId())) {
                    list.add(customNames.get(target.getUniqueId()));
                }
                return list;
            }
        }

        return Collections.emptyList();
    }
}
