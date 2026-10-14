package com.moonlight.vanilla.modules.ranks;

import com.moonlight.vanilla.MoonlightVanillaSurvivalCore;
import com.moonlight.vanilla.utils.MessageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

/**
 * RanksListener
 * Handles interactive clicks inside the /ranks GUI with anti-theft protection.
 */
public class RanksListener implements Listener {

    private final MoonlightVanillaSurvivalCore plugin;
    private final RanksManager manager;

    public RanksListener(MoonlightVanillaSurvivalCore plugin, RanksManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof RanksInventoryHolder)) {
            return;
        }

        // Strict anti-theft: prevent taking or moving items
        event.setCancelled(true);
        event.setResult(org.bukkit.event.Event.Result.DENY);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        // Instantly wipe cursor and force inventory sync
        player.setItemOnCursor(null);
        event.getView().setCursor(null);
        Bukkit.getScheduler().runTask(plugin, player::updateInventory);

        if (event.getClick().isShiftClick() || event.getClick().isKeyboardClick() || event.getClick() == org.bukkit.event.inventory.ClickType.DOUBLE_CLICK) {
            return;
        }

        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getInventory())) {
            return;
        }

        int slot = event.getSlot();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) {
            return;
        }

        // 1. Close Button (Barrier)
        if (clicked.getType() == Material.BARRIER) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.0f);
            player.closeInventory();
            return;
        }

        // 2. Help Guide Button (Compass)
        if (clicked.getType() == Material.COMPASS) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
            player.closeInventory();
            plugin.getHelpCommand().executeHelp(player, new String[0]);
            return;
        }

        // 3. Web Store Button (Campfire / Nether Star / Slot 40)
        if (slot == 40 || clicked.getType() == Material.CAMPFIRE || clicked.getType() == Material.NETHER_STAR) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.5f);
            player.closeInventory();
            sendStoreLinkMessage(player, null);
            return;
        }

        // 4. Clicked a Rank Card (Slots 19, 21, 23, 25 or any defined slot)
        RankTier tier = manager.getRankBySlot(slot);
        if (tier != null) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.5f);
            player.closeInventory();
            sendRankDetailsMessage(player, tier);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof RanksInventoryHolder) {
            event.setCancelled(true);
            event.setResult(org.bukkit.event.Event.Result.DENY);
            if (event.getWhoClicked() instanceof Player player) {
                player.setItemOnCursor(null);
                event.getView().setCursor(null);
                Bukkit.getScheduler().runTask(plugin, player::updateInventory);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof RanksInventoryHolder) {
            if (event.getPlayer() instanceof Player player) {
                player.setItemOnCursor(null);
                event.getView().setCursor(null);
                com.moonlight.vanilla.modules.gui.GuiSecurityListener.cleanGuiItemsFromPlayer(player);
                Bukkit.getScheduler().runTask(plugin, player::updateInventory);
            }
        }
    }

    public void sendStoreLinkMessage(Player player, String targetRank) {
        String url = manager.getStoreUrl();
        player.sendMessage(Component.empty());
        player.sendMessage(MessageUtil.format("<gradient:#00d2ff:#3a7bd5><bold>══════════════ MOONLIGHT WEB STORE ══════════════</bold></gradient>"));
        player.sendMessage(MessageUtil.format("<gray>Visit our official store to purchase supporter ranks, perks, and keys:</gray>"));
        player.sendMessage(Component.empty());

        Component linkComp = MessageUtil.format("<aqua><underlined>" + url + "</underlined></aqua>")
                .clickEvent(ClickEvent.openUrl(url))
                .hoverEvent(HoverEvent.showText(MessageUtil.format("<yellow>Click to open: </yellow><aqua>" + url + "</aqua>")));

        Component button = MessageUtil.format(" <gradient:#00d2ff:#3a7bd5><bold>[CLICK TO OPEN STORE]</bold></gradient>")
                .clickEvent(ClickEvent.openUrl(url))
                .hoverEvent(HoverEvent.showText(MessageUtil.format("<green>Click to open Moonlight store in your browser!</green>")));

        player.sendMessage(linkComp.append(button));
        player.sendMessage(MessageUtil.format("<gradient:#00d2ff:#3a7bd5><bold>═════════════════════════════════════════════════</bold></gradient>"));
        player.sendMessage(Component.empty());
    }

    private void sendRankDetailsMessage(Player player, RankTier tier) {
        String url = manager.getStoreUrl();
        player.sendMessage(Component.empty());
        player.sendMessage(MessageUtil.format("<gradient:#00d2ff:#3a7bd5><bold>═════════ </bold></gradient>")
                .append(MessageUtil.format(tier.getDisplayName()))
                .append(MessageUtil.format("<gradient:#00d2ff:#3a7bd5><bold> ═════════</bold></gradient>")));

        player.sendMessage(MessageUtil.format("<gold><bold>Price:</bold> <yellow>" + tier.getPrice() + "</yellow></gold>"));
        player.sendMessage(MessageUtil.format("<gray>" + tier.getDescription() + "</gray>"));
        player.sendMessage(Component.empty());
        player.sendMessage(MessageUtil.format("<yellow><bold>Included Perks & Features:</bold></yellow>"));

        for (String perk : tier.getPerks()) {
            player.sendMessage(MessageUtil.format(" <gray>✔ </gray>" + perk));
        }

        player.sendMessage(Component.empty());

        Component linkComp = MessageUtil.format("<aqua><underlined>" + url + "</underlined></aqua>")
                .clickEvent(ClickEvent.openUrl(url))
                .hoverEvent(HoverEvent.showText(MessageUtil.format("<yellow>Click to purchase: </yellow><aqua>" + url + "</aqua>")));

        Component button = MessageUtil.format(" <gradient:#00d2ff:#3a7bd5><bold>[PURCHASE " + tier.getName().toUpperCase(java.util.Locale.ROOT) + " RANK]</bold></gradient>")
                .clickEvent(ClickEvent.openUrl(url))
                .hoverEvent(HoverEvent.showText(MessageUtil.format("<green>Click to open store checkout in your browser!</green>")));

        player.sendMessage(linkComp.append(button));
        player.sendMessage(MessageUtil.format("<gradient:#00d2ff:#3a7bd5><bold>═════════════════════════════════════════════════</bold></gradient>"));
        player.sendMessage(Component.empty());
    }
}
