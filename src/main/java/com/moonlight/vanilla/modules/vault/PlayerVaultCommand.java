package com.moonlight.vanilla.modules.vault;

import com.moonlight.vanilla.MoonlightVanillaSurvivalCore;
import com.moonlight.vanilla.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * PlayerVaultCommand
 * Handles /pv, /pv1, /pv2, /pv3, /pv4, /pv5, /playervault, /vault.
 * Supports direct number labels (e.g. /pv1, /pv2) and admin player inspection.
 */
public class PlayerVaultCommand implements CommandExecutor, TabCompleter {

    private final MoonlightVanillaSurvivalCore plugin;
    private final PlayerVaultManager manager;

    public PlayerVaultCommand(MoonlightVanillaSurvivalCore plugin, PlayerVaultManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Player vaults can only be opened by in-game players.");
            return true;
        }

        int vaultNum = 1;
        String cleanLabel = label.toLowerCase(Locale.ROOT).replace("moonlight:", "");

        // 1. Check if the label itself ends with a vault digit, e.g. /pv1, /pv2, /playervault3
        if (cleanLabel.matches(".*[1-5]$")) {
            char lastChar = cleanLabel.charAt(cleanLabel.length() - 1);
            vaultNum = Character.getNumericValue(lastChar);
        }

        UUID targetUuid = player.getUniqueId();
        String targetName = player.getName();

        boolean isAdmin = player.isOp() || player.hasPermission("moonlight.admin");

        if (args.length > 0) {
            String firstArg = args[0];

            // Case A: /pv <1-5> [player]
            if (firstArg.matches("^[1-5]$")) {
                vaultNum = Integer.parseInt(firstArg);

                if (args.length > 1 && isAdmin) {
                    OfflinePlayer target = resolveTarget(args[1]);
                    if (target == null) {
                        player.sendMessage(MessageUtil.format("&9MoonLight &8❙ &cPlayer '&e" + args[1] + "&c' not found."));
                        return true;
                    }
                    targetUuid = target.getUniqueId();
                    targetName = target.getName() != null ? target.getName() : args[1];
                }
            }
            // Case B: /pv <player> [1-5] (Admin syntax)
            else if (isAdmin) {
                OfflinePlayer target = resolveTarget(firstArg);
                if (target != null) {
                    targetUuid = target.getUniqueId();
                    targetName = target.getName() != null ? target.getName() : firstArg;

                    if (args.length > 1 && args[1].matches("^[1-5]$")) {
                        vaultNum = Integer.parseInt(args[1]);
                    }
                } else {
                    player.sendMessage(MessageUtil.format("&9MoonLight &8❙ &cInvalid vault number or player not found: '&e" + firstArg + "&c'. (Use 1-5)"));
                    return true;
                }
            } else {
                player.sendMessage(MessageUtil.format("<red>Usage: /" + label + " [1-5]</red>"));
                return true;
            }
        }

        // Validate bounds
        if (vaultNum < 1 || vaultNum > 5) {
            player.sendMessage(MessageUtil.format("&9MoonLight &8❙ &cAvailable vault numbers are 1 through 5."));
            return true;
        }

        // Self-permission check (only applies if opening own vault; admins bypass when viewing others)
        boolean isSelf = player.getUniqueId().equals(targetUuid);
        if (isSelf && !manager.canAccessVault(player, vaultNum)) {
            player.sendMessage(MessageUtil.format("&9MoonLight &8❙ &cYou do not have access to Player Vault #" + vaultNum + "."));
            player.sendMessage(MessageUtil.format("&7Upgrade your rank at &e/ranks&7 or web store &e/store&7 to unlock up to 5 Player Vaults!"));
            return true;
        }

        manager.openVault(player, targetUuid, targetName, vaultNum);
        return true;
    }

    private OfflinePlayer resolveTarget(String name) {
        Player online = Bukkit.getPlayer(name);
        if (online != null) return online;

        OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        if (offline.hasPlayedBefore() || offline.getName() != null) {
            return offline;
        }
        return null;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        String current = args.length > 0 ? args[args.length - 1].toLowerCase(Locale.ROOT) : "";
        String cleanLabel = alias.toLowerCase(Locale.ROOT).replace("moonlight:", "");

        boolean isAdmin = sender.isOp() || sender.hasPermission("moonlight.admin");

        // If the command itself specifies a vault number (e.g. /pv1, /pv2, /playervault3)
        if (cleanLabel.matches(".*[1-5]$")) {
            if (args.length <= 1 && isAdmin) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase(Locale.ROOT).startsWith(current)) {
                        completions.add(p.getName());
                    }
                }
            }
            return completions;
        }

        if (args.length <= 1) {
            // Suggest vault numbers 1-5
            for (int i = 1; i <= 5; i++) {
                String s = String.valueOf(i);
                if (s.startsWith(current)) {
                    completions.add(s);
                }
            }

            // If admin, also suggest online player names
            if (isAdmin) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase(Locale.ROOT).startsWith(current)) {
                        completions.add(p.getName());
                    }
                }
            }
        } else if (args.length == 2 && isAdmin) {
            if (args[0].matches("^[1-5]$")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase(Locale.ROOT).startsWith(current)) {
                        completions.add(p.getName());
                    }
                }
            } else {
                for (int i = 1; i <= 5; i++) {
                    String s = String.valueOf(i);
                    if (s.startsWith(current)) {
                        completions.add(s);
                    }
                }
            }
        }

        return completions;
    }
}
