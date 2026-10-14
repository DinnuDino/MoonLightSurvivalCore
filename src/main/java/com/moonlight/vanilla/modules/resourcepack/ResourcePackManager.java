package com.moonlight.vanilla.modules.resourcepack;

import com.moonlight.vanilla.MoonlightVanillaSurvivalCore;
import com.moonlight.vanilla.utils.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * ResourcePackManager
 * Sends the server resource pack with a fixed SHA-1 checksum so that Minecraft clients
 * store it in their temporary cache (downloads/<hash>) and NEVER re-download on subsequent joins.
 */
public class ResourcePackManager implements Listener {

    private final MoonlightVanillaSurvivalCore plugin;
    private final java.util.Set<UUID> loadedPlayers = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public ResourcePackManager(MoonlightVanillaSurvivalCore plugin) {
        this.plugin = plugin;
    }

    public boolean isPackLoaded(UUID uuid) {
        return loadedPlayers.contains(uuid);
    }

    public void initialize() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!plugin.getConfig().getBoolean("resource-pack.enabled", true)) {
            return;
        }

        Player player = event.getPlayer();

        // Bedrock Edition players connect via Geyser/Floodgate and do not download Java resource packs
        if (plugin.getBedrockManager() != null && plugin.getBedrockManager().isBedrockPlayer(player)) {
            return;
        }

        long delayTicks = plugin.getConfig().getLong("resource-pack.send-delay-ticks", 50L);
        if (delayTicks < 30L) delayTicks = 30L;

        // Send pack after delay so mobile launchers (Mojo Launcher, Pojav) finish loading terrain
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                sendPack(player);
            }
        }, delayTicks);

        // Send clickable chat backup at tick (delayTicks + 25) so mobile players who missed the prompt
        // can simply tap in chat to download/apply the pack at any time
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && !loadedPlayers.contains(player.getUniqueId())) {
                Component rpMsg = MessageUtil.format("<gradient:#00d2ff:#3a7bd5>[Moonlight]</gradient> <gray>Server Pack (Logo & 3D Items): </gray>")
                        .append(Component.text("[Click to Load /rp]", net.kyori.adventure.text.format.NamedTextColor.GREEN, net.kyori.adventure.text.format.TextDecoration.BOLD)
                                .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/rp"))
                                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(MessageUtil.format("<green>Click to download & load custom server logo and 3D weapon wraps!</green>"))));
                player.sendMessage(rpMsg);
            }
        }, delayTicks + 25L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPackStatus(PlayerResourcePackStatusEvent event) {
        Player player = event.getPlayer();
        switch (event.getStatus()) {
            case SUCCESSFULLY_LOADED -> {
                player.sendMessage(MessageUtil.format("<gradient:#00d2ff:#3a7bd5>[Moonlight]</gradient> <green>Resource pack loaded! Server logo & 3D wraps active.</green>"));
            }
            case DECLINED -> {
                player.sendMessage(MessageUtil.format("<gradient:#00d2ff:#3a7bd5>[Moonlight]</gradient> <yellow>Resource pack declined. Clean text mode active. Type <gold>/rp</gold> anytime to load logo & wraps.</yellow>"));
            }
            case FAILED_DOWNLOAD -> {
                player.sendMessage(MessageUtil.format("<gradient:#00d2ff:#3a7bd5>[Moonlight]</gradient> <red>Pack download issue on this client. Type <gold>/rp</gold> to retry, or continue with clean text.</red>"));
            }
            default -> {}
        }
    }

    public void sendPack(Player player) {
        if (player == null || !player.isOnline()) return;

        String url = plugin.getConfig().getString("resource-pack.url", "");
        if (url == null || url.isBlank()) return;

        String rawHash = plugin.getConfig().getString("resource-pack.hash", "").trim().toLowerCase();
        boolean required = plugin.getConfig().getBoolean("resource-pack.required", false);
        String promptText = plugin.getConfig().getString("resource-pack.prompt",
                "<gradient:#00d2ff:#3a7bd5><bold>✦ MOONLIGHT SMP ✦</bold></gradient>\n<gray>Load server resource pack</gray>");
        Component prompt = MessageUtil.format(promptText);

        // Attempt 1: Modern Paper API with SHA-1 bytes & pack UUID
        if (rawHash.length() == 40) {
            try {
                byte[] hashBytes = hexToBytes(rawHash);
                UUID packId = UUID.nameUUIDFromBytes(rawHash.getBytes(StandardCharsets.UTF_8));
                player.setResourcePack(packId, url, hashBytes, prompt, required);
                return;
            } catch (Throwable ignored) {}

            // Attempt 2: Paper API with String hash, boolean required, Component prompt
            try {
                player.setResourcePack(url, rawHash, required, prompt);
                return;
            } catch (Throwable ignored) {}

            // Attempt 3: Spigot API with String hash
            try {
                player.setResourcePack(url, rawHash);
                return;
            } catch (Throwable ignored) {}
        }

        // Fallback: Simple setResourcePack(url)
        try {
            player.setResourcePack(url);
        } catch (Throwable t) {
            plugin.getLogger().warning("Failed to send resource pack to " + player.getName() + ": " + t.getMessage());
        }
    }

    private byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
