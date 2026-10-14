package com.moonlight.vanilla.modules.ranks;

import com.moonlight.vanilla.MoonlightVanillaSurvivalCore;
import com.moonlight.vanilla.utils.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * RanksCommand
 * Opens the interactive paid ranks GUI or lists rank details via /ranks [rank].
 */
public class RanksCommand implements CommandExecutor, TabCompleter {

    private final MoonlightVanillaSurvivalCore plugin;
    private final RanksManager manager;
    private final RanksGui gui;

    public RanksCommand(MoonlightVanillaSurvivalCore plugin, RanksManager manager, RanksGui gui) {
        this.plugin = plugin;
        this.manager = manager;
        this.gui = gui;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("moonlight.ranks")) {
            sender.sendMessage(MessageUtil.format("&9MoonLight &8❙ &cYou are forbidden to use this."));
            return true;
        }

        if (sender instanceof Player player) {
            if (args.length == 0) {
                try {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
                } catch (Exception ignored) {}
                gui.openGui(player);
                return true;
            }

            // If player asked for specific rank details via chat
            String rankArg = args[0].toLowerCase(Locale.ROOT);
            RankTier tier = manager.getRank(rankArg);
            if (tier != null) {
                sendRankChatOverview(player, tier);
            } else {
                gui.openGui(player);
            }
            return true;
        }

        // Console fallback
        sender.sendMessage(MessageUtil.format("<gradient:#00d2ff:#3a7bd5><bold>Moonlight Paid Ranks:</bold></gradient>"));
        for (RankTier t : manager.getAllRanks()) {
            sender.sendMessage(MessageUtil.format(t.getDisplayName() + " <gold>(" + t.getPrice() + "):</gold> <gray>" + t.getDescription() + "</gray>"));
        }
        sender.sendMessage(MessageUtil.format("<gray>Store URL: " + manager.getStoreUrl() + "</gray>"));
        return true;
    }

    private void sendRankChatOverview(Player player, RankTier tier) {
        player.sendMessage(Component.empty());
        player.sendMessage(MessageUtil.format("<gradient:#00d2ff:#3a7bd5><bold>═════════ </bold></gradient>")
                .append(MessageUtil.format(tier.getDisplayName()))
                .append(MessageUtil.format("<gradient:#00d2ff:#3a7bd5><bold> ═════════</bold></gradient>")));

        player.sendMessage(MessageUtil.format("<gold><bold>Price:</bold> <yellow>" + tier.getPrice() + "</yellow></gold>"));
        player.sendMessage(MessageUtil.format("<gray>" + tier.getDescription() + "</gray>"));
        player.sendMessage(Component.empty());
        player.sendMessage(MessageUtil.format("<yellow><bold>Rank Perks:</bold></yellow>"));

        for (String perk : tier.getPerks()) {
            player.sendMessage(MessageUtil.format(" <gray>✔ </gray>" + perk));
        }

        player.sendMessage(MessageUtil.format("<gray>Store: </gray><aqua>" + manager.getStoreUrl() + "</aqua>"));
        player.sendMessage(MessageUtil.format("<gradient:#00d2ff:#3a7bd5><bold>═════════════════════════════════════════</bold></gradient>"));
        player.sendMessage(Component.empty());
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length <= 1) {
            String query = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
            List<String> matches = new ArrayList<>();
            for (RankTier t : manager.getAllRanks()) {
                if (t.getId().startsWith(query)) {
                    matches.add(t.getId());
                }
            }
            return matches;
        }
        return Collections.emptyList();
    }
}
