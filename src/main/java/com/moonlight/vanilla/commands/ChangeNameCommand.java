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
 * Integrates politely with LuckPerms and TAB.
 */
public class ChangeNameCommand implements CommandExecutor, TabCompleter, Listener {

    private static final String PREFIX = "&9MoonLight &8❙ ";
    private static final Set<String> RESET_KEYWORDS = Set.of("reset", "clear", "default", "off");

    private final MoonlightVanillaSurvivalCore plugin;
    private final File storageFile;
    private final YamlConfiguration storageConfig = new YamlConfiguration();
    private final Map<UUID, String> customNames = new ConcurrentHashMap<>();

    public ChangeNameCommand(MoonlightVanillaSurvivalCore plugin) {
        this.plugin = plugin;
        this.storageFile = new File(plugin.getDataFolder(), "custom_names.yml");
        loadData();
        Bukkit.getPluginManager().registerEvents(this, plugin);

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
                if (storageFile.getParentFile() != null) storageFile.getParentFile().mkdirs();
                storageFile.createNewFile();
            }
            storageConfig.load(storageFile);
            ConfigurationSection sec = storageConfig.getConfigurationSection("names");
            if (sec != null) {
                for (String key : sec.getKeys(false)) {
                    try {
                        String raw = sec.getString(key + ".display");
                        if (raw != null && !raw.isBlank()) customNames.put(UUID.fromString(key), raw);
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
            customNames.forEach((uuid, display) -> {
                String key = "names." + uuid;
                storageConfig.set(key + ".display", display);
                OfflinePlayer off = Bukkit.getOfflinePlayer(uuid);
                if (off.getName() != null) storageConfig.set(key + ".username", off.getName());
            });
            storageConfig.save(storageFile);
        } catch (Exception e) {
            plugin.getLogger().warning("Could not save custom_names.yml: " + e.getMessage());
        }
    }

    public void applyCustomName(Player player, String rawName) {
        if (player == null || !player.isOnline()) return;
        Component comp = MessageUtil.format(rawName);
        String legacy = MessageUtil.toLegacy(comp);

        player.displayName(comp);
        player.playerListName(comp);
        player.customName(comp);
        player.setCustomNameVisible(true);

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

        player.displayName(realNameComp);
        player.playerListName(null);
        player.customName(null);
        player.setCustomNameVisible(false);

        try {
            player.setDisplayName(realName);
            player.setPlayerListName(null);
            player.setCustomName(null);
        } catch (Throwable ignored) {}
    }

    public static String toColorAmpersand(String raw) {
        if (raw == null || raw.isBlank()) return "";
        if (raw.contains("<") && raw.contains(">")) {
            return MessageUtil.toLegacy(MessageUtil.format(raw)).replace('§', '&');
        }
        return raw.replace('§', '&');
    }

    public void requestLuckPermsChange(UUID uuid, String username, String rawName, boolean isReset) {
        if (!Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) return;
        String id = (username != null && !username.isBlank()) ? username : Objects.toString(uuid, null);
        if (id == null) return;

        try {
            if (isReset) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + id + " meta unset displayname");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + id + " meta unset nickname");
            } else {
                String safe = toColorAmpersand(rawName).replace("\"", "");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + id + " meta set displayname \"" + safe + "\"");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + id + " meta set nickname \"" + safe + "\"");
            }
        } catch (Throwable e) {
            plugin.getLogger().fine("LuckPerms request notice: " + e.getMessage());
        }

        try {
            Class<?> provider = Class.forName("net.luckperms.api.LuckPermsProvider");
            Object lp = provider.getMethod("get").invoke(null);
            if (lp != null && uuid != null) {
                Object userMgr = lp.getClass().getMethod("getUserManager").invoke(lp);
                if (userMgr != null) {
                    Object user = userMgr.getClass().getMethod("getUser", UUID.class).invoke(userMgr, uuid);
                    if (user != null) {
                        userMgr.getClass().getMethod("saveUser", user.getClass().getInterfaces()[0]).invoke(userMgr, user);
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    public void requestTabChange(UUID uuid, String username, String rawName, boolean isReset) {
        if (!Bukkit.getPluginManager().isPluginEnabled("TAB")) return;
        String id = (username != null && !username.isBlank()) ? username : Objects.toString(uuid, null);
        if (id == null) return;

        try {
            Class<?> tabApiClass = Class.forName("me.neznamy.tab.api.TabAPI");
            Object tabApi = tabApiClass.getMethod("getInstance").invoke(null);
            if (tabApi != null && uuid != null) {
                Object tabPlayer = tabApi.getClass().getMethod("getPlayer", UUID.class).invoke(tabApi, uuid);
                if (tabPlayer != null) {
                    Object tabListMgr = tabApi.getClass().getMethod("getTabListFormatManager").invoke(tabApi);
                    if (tabListMgr != null) {
                        for (Method m : tabListMgr.getClass().getMethods()) {
                            if (m.getName().equals("setName") && m.getParameterCount() == 2) {
                                m.invoke(tabListMgr, tabPlayer, isReset ? null : toColorAmpersand(rawName));
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}

        try {
            if (isReset) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tab player " + id + " customtabname remove");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tab player " + id + " customtagname remove");
            } else {
                String safe = toColorAmpersand(rawName).replace("\"", "");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tab player " + id + " customtabname " + safe);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tab player " + id + " customtagname " + safe);
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
                    String latest = customNames.get(player.getUniqueId());
                    applyCustomName(player, latest);
                    requestTabChange(player.getUniqueId(), player.getName(), latest, false);
                }
            }, 5L);
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player p && !p.isOp() && !p.hasPermission("moonlight.core.admin") && !p.hasPermission("moonlight.admin")) {
            sender.sendMessage(MessageUtil.format(PREFIX + "&cAccess denied: You must be an administrator (OP or moonlight.core.admin) to use this command."));
            return true;
        }

        if (args.length < 2) {
            if (args.length == 1 && args[0].equalsIgnoreCase("list")) {
                if (customNames.isEmpty()) {
                    sender.sendMessage(MessageUtil.format(PREFIX + "&7No players currently have custom display names set."));
                } else {
                    sender.sendMessage(MessageUtil.format(PREFIX + "&6Active Changed Display Names (&e" + customNames.size() + "&6):"));
                    customNames.forEach((uuid, display) -> {
                        OfflinePlayer off = Bukkit.getOfflinePlayer(uuid);
                        String name = off.getName() != null ? off.getName() : uuid.toString();
                        sender.sendMessage(MessageUtil.format("  &8• &e" + name + " &8↳ " + display));
                    });
                }
                return true;
            }

            sender.sendMessage(MessageUtil.format(PREFIX + "&cUsage: &e/changename <player> <new_name>"));
            sender.sendMessage(MessageUtil.format(PREFIX + "&cReset: &e/changename <player> reset"));
            sender.sendMessage(MessageUtil.format("&7Examples:\n  &8• &f/changename Dinnu &b[VIP] Dinnu\n  &8• &f/changename Dinnu reset\n  &8• &f/changename list"));
            return true;
        }

        boolean isReset = false;
        String targetArg;
        String rawNameArg = null;

        if (RESET_KEYWORDS.contains(args[0].toLowerCase(Locale.ROOT))) {
            isReset = true;
            targetArg = args[1];
        } else if (RESET_KEYWORDS.contains(args[1].toLowerCase(Locale.ROOT))) {
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
                sender.sendMessage(MessageUtil.format(PREFIX + "&aSuccessfully reset display name of &e" + name + "&a back to original."));
                target.sendMessage(MessageUtil.format(PREFIX + "&aYour display name was reset back to original by an administrator."));
            } else {
                customNames.put(uuid, rawNameArg);
                saveData();
                applyCustomName(target, rawNameArg);
                requestLuckPermsChange(uuid, name, rawNameArg, false);
                requestTabChange(uuid, name, rawNameArg, false);
                sender.sendMessage(MessageUtil.format(PREFIX + "&aChanged display name of &e" + name + "&a to: " + rawNameArg));
                target.sendMessage(MessageUtil.format(PREFIX + "&aYour display name was updated to: " + rawNameArg));
            }
            return true;
        }

        OfflinePlayer offline = Bukkit.getOfflinePlayer(targetArg);
        if (offline.hasPlayedBefore() || offline.getName() != null) {
            UUID uuid = offline.getUniqueId();
            String name = offline.getName() != null ? offline.getName() : targetArg;
            if (isReset) {
                customNames.remove(uuid);
                saveData();
                requestLuckPermsChange(uuid, name, null, true);
                requestTabChange(uuid, name, null, true);
                sender.sendMessage(MessageUtil.format(PREFIX + "&aSuccessfully reset display name of offline player &e" + name + "&a back to original."));
            } else {
                customNames.put(uuid, rawNameArg);
                saveData();
                requestLuckPermsChange(uuid, name, rawNameArg, false);
                requestTabChange(uuid, name, rawNameArg, false);
                sender.sendMessage(MessageUtil.format(PREFIX + "&aChanged display name of offline player &e" + name + "&a to: " + rawNameArg + " &7(will apply on login)."));
            }
            return true;
        }

        sender.sendMessage(MessageUtil.format(PREFIX + "&cPlayer &e'" + targetArg + "'&c not found."));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            String q = args[0].toLowerCase(Locale.ROOT);
            List<String> list = new ArrayList<>(Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n.toLowerCase(Locale.ROOT).startsWith(q)).toList());
            if ("reset".startsWith(q)) list.add("reset");
            if ("list".startsWith(q)) list.add("list");
            return list;
        }

        if (args.length == 2) {
            String q = args[1].toLowerCase(Locale.ROOT);
            if (args[0].equalsIgnoreCase("reset")) {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n.toLowerCase(Locale.ROOT).startsWith(q)).toList();
            }
            List<String> list = new ArrayList<>();
            if ("reset".startsWith(q)) list.add("reset");
            Player target = Bukkit.getPlayer(args[0]);
            if (target != null && customNames.containsKey(target.getUniqueId())) {
                list.add(customNames.get(target.getUniqueId()));
            }
            return list;
        }
        return List.of();
    }
}
