package com.moonlight.vanilla.modules.wraps;

import com.moonlight.vanilla.MoonlightVanillaSurvivalCore;
import com.moonlight.vanilla.utils.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WrapCommand implements CommandExecutor, TabCompleter {

    private final MoonlightVanillaSurvivalCore plugin;
    private final WrapManager wrapManager;
    private final WrapGui wrapGui;

    public WrapCommand(MoonlightVanillaSurvivalCore plugin, WrapManager wrapManager, WrapGui wrapGui) {
        this.plugin = plugin;
        this.wrapManager = wrapManager;
        this.wrapGui = wrapGui;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.format("&cOnly in-game players can execute /" + label + "."));
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (player.hasPermission("moonlight.admin") || player.isOp()) {
                wrapManager.loadWraps();
                player.sendMessage(MessageUtil.format("&9MoonLight &8❙ &aWeapon wraps reloaded! Total: " + wrapManager.getWraps().size()));
                return true;
            }
        }

        ItemStack held = player.getInventory().getItemInMainHand();
        if (held == null || held.getType().isAir()) {
            player.sendMessage(MessageUtil.format("&9MoonLight &8❙ &cYou must be holding an item in your main hand to open wraps!"));
            return true;
        }

        if (!wrapManager.isSupportedItem(held.getType())) {
            player.sendMessage(MessageUtil.format("&9MoonLight &8❙ &cYou must be holding a supported weapon or tool (Sword, Axe, Pickaxe, Hoe, Mace, Bow, Trident) to open wraps!"));
            return true;
        }

        if (args.length > 0) {
            String sub = args[0].toLowerCase();
            if (sub.equals("remove") || sub.equals("clear") || sub.equals("reset")) {
                boolean hadWrap = wrapManager.removeWrap(player, held);
                player.getInventory().setItemInMainHand(held);
                if (hadWrap) {
                    player.sendMessage(MessageUtil.format("&9MoonLight &8❙ &aWrap removed! Restored default vanilla appearance."));
                } else {
                    player.sendMessage(MessageUtil.format("&9MoonLight &8❙ &eThis weapon is already using its default vanilla appearance."));
                }
                return true;
            }
        }

        wrapGui.openMenu(player, held, 1);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length <= 1) {
            String current = args.length == 0 ? "" : args[0].toLowerCase(java.util.Locale.ROOT);
            List<String> list = new ArrayList<>();
            if ("remove".startsWith(current)) list.add("remove");
            if ((sender.hasPermission("moonlight.admin") || sender.isOp()) && "reload".startsWith(current)) {
                list.add("reload");
            }
            if (wrapManager != null) {
                if (sender instanceof Player player) {
                    ItemStack held = player.getInventory().getItemInMainHand();
                    if (held != null && !held.getType().isAir()) {
                        for (Wrap w : wrapManager.getWrapsFor(held.getType())) {
                            if (w.getKey().toLowerCase(java.util.Locale.ROOT).startsWith(current) && !list.contains(w.getKey())) {
                                list.add(w.getKey());
                            }
                        }
                    }
                }
                for (Wrap w : wrapManager.getWraps()) {
                    if (w.getKey().toLowerCase(java.util.Locale.ROOT).startsWith(current) && !list.contains(w.getKey())) {
                        list.add(w.getKey());
                    }
                }
            }
            return list;
        }
        return Collections.emptyList();
    }
}
