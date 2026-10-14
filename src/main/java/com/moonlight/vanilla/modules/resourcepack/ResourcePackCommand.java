package com.moonlight.vanilla.modules.resourcepack;

import com.moonlight.vanilla.MoonlightVanillaSurvivalCore;
import com.moonlight.vanilla.utils.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ResourcePackCommand implements CommandExecutor, TabCompleter {

    private final MoonlightVanillaSurvivalCore plugin;
    private final ResourcePackManager resourcePackManager;

    public ResourcePackCommand(MoonlightVanillaSurvivalCore plugin, ResourcePackManager resourcePackManager) {
        this.plugin = plugin;
        this.resourcePackManager = resourcePackManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("moonlight.admin") && !sender.isOp()) {
                sender.sendMessage(MessageUtil.format("<red>You don't have permission to reload the resource pack config.</red>"));
                return true;
            }
            plugin.reloadConfig();
            sender.sendMessage(MessageUtil.format("<gradient:#00d2ff:#3a7bd5>[Moonlight]</gradient> <green>Resource pack settings reloaded from config.yml!</green>"));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }

        if (plugin.getBedrockManager() != null && plugin.getBedrockManager().isBedrockPlayer(player)) {
            player.sendMessage(MessageUtil.format("<gradient:#00d2ff:#3a7bd5>[Moonlight]</gradient> <aqua>You are on Bedrock Edition! Textures & UI are automatically optimized for Bedrock without extra downloads.</aqua>"));
            return true;
        }

        player.sendMessage(MessageUtil.format("<gradient:#00d2ff:#3a7bd5>[Moonlight]</gradient> <yellow>Requesting resource pack from server cache...</yellow>"));
        resourcePackManager.sendPack(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            if (sender.hasPermission("moonlight.admin") || sender.isOp()) {
                completions.add("reload");
            }
        }
        return completions;
    }
}
