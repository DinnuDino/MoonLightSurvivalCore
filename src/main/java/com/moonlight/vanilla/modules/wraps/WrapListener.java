package com.moonlight.vanilla.modules.wraps;

import com.moonlight.vanilla.MoonlightVanillaSurvivalCore;
import com.moonlight.vanilla.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class WrapListener implements Listener {

    private final MoonlightVanillaSurvivalCore plugin;
    private final WrapManager wrapManager;
    private final WrapGui wrapGui;

    public WrapListener(MoonlightVanillaSurvivalCore plugin, WrapManager wrapManager, WrapGui wrapGui) {
        this.plugin = plugin;
        this.wrapManager = wrapManager;
        this.wrapGui = wrapGui;
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof WrapInventoryHolder holder)) {
            return;
        }

        // Strict Anti-Theft: Deny all item movement, number keys, shift-clicks, offhand swap, and dropping
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

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) {
            return;
        }

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        String action = pdc.get(wrapGui.getActionKey(), PersistentDataType.STRING);
        if (action == null) return;

        ItemStack inHand = player.getInventory().getItemInMainHand();
        if (inHand.getType().isAir()) {
            player.sendMessage(MessageUtil.format("&9MoonLight &8❙ &cYou must be holding your weapon to manage wraps!"));
            player.closeInventory();
            return;
        }

        switch (action) {
            case "remove_wrap" -> {
                boolean hadWrap = wrapManager.removeWrap(player, inHand);
                player.getInventory().setItemInMainHand(inHand);
                if (hadWrap) {
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);
                    player.sendMessage(MessageUtil.format("&9MoonLight &8❙ &aWrap removed! Restored default vanilla appearance."));
                } else {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.0f);
                    player.sendMessage(MessageUtil.format("&9MoonLight &8❙ &eThis weapon is already using its default vanilla appearance."));
                }
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (player.isOnline()) {
                        wrapGui.openMenu(player, inHand, holder.getPage());
                    }
                });
            }

            case "apply_wrap" -> {
                String wrapKey = pdc.get(wrapGui.getWrapSlotKey(), PersistentDataType.STRING);
                if (wrapKey == null) return;

                Wrap wrap = wrapManager.getWrap(wrapKey);
                if (wrap == null) return;

                // LuckPerms / Permission Check
                if (!wrapManager.hasWrapPermission(player, wrap)) {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    player.sendMessage(MessageUtil.format("&9MoonLight &8❙ &cYou do not have access to use &r" + wrap.getColorFormat() + "&c!"));
                    player.sendMessage(MessageUtil.format("&9MoonLight &8❙ &7Access can be granted via LuckPerms: &e/lp user " + player.getName() + " permission set " + wrap.getPermission() + " true"));
                    return;
                }

                // Rank Cooldown Check (OPs and Admins bypass)
                long remainingSec = wrapManager.getRemainingCooldown(player);
                if (remainingSec > 0) {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    player.sendMessage(MessageUtil.format("&9MoonLight &8❙ &cYou must wait &e" + wrapManager.formatCooldown(remainingSec) + " &cbefore applying another wrap!"));
                    return;
                }

                wrapManager.applyWrap(player, inHand, wrap);
                wrapManager.recordWrapApplication(player);
                player.getInventory().setItemInMainHand(inHand);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
                boolean isBedrock = plugin.getBedrockManager() != null && plugin.getBedrockManager().isBedrockPlayer(player);
                if (isBedrock) {
                    player.sendMessage(MessageUtil.format("&9MoonLight &8❙ &aEquipped &r" + wrap.getColorFormat() + " &aweapon wrap!"));
                } else {
                    player.sendMessage(MessageUtil.format("&9MoonLight &8❙ &aEquipped &r" + wrap.getColorFormat() + " &a3D wrap on your weapon!"));
                }
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (player.isOnline()) {
                        wrapGui.openMenu(player, inHand, holder.getPage());
                    }
                });
            }

            case "change_page" -> {
                Integer targetPage = pdc.get(wrapGui.getPageTargetKey(), PersistentDataType.INTEGER);
                if (targetPage != null) {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (player.isOnline()) {
                            wrapGui.openMenu(player, inHand, targetPage);
                        }
                    });
                }
            }
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerItemHeld(org.bukkit.event.player.PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItem(event.getNewSlot());
        syncWrapModelForPlayer(player, item);
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickupItem(org.bukkit.event.entity.EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            syncWrapModelForPlayer(player, event.getItem().getItemStack());
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR, ignoreCancelled = true)
    public void onSwapHandItems(org.bukkit.event.player.PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        syncWrapModelForPlayer(player, event.getMainHandItem());
        syncWrapModelForPlayer(player, event.getOffHandItem());
    }

    public void syncWrapModelForPlayer(Player player, ItemStack item) {
        if (player == null || item == null || item.getType().isAir() || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(wrapManager.getWrapKey(), PersistentDataType.STRING)) return;

        String wrapKey = pdc.get(wrapManager.getWrapKey(), PersistentDataType.STRING);
        Wrap wrap = wrapManager.getWrap(wrapKey);
        if (wrap == null) return;

        boolean isBedrock = plugin.getBedrockManager() != null && plugin.getBedrockManager().isBedrockPlayer(player);
        if (isBedrock) {
            // Bedrock player: If item currently has a 3D model attached, strip it so Geyser doesn't render glitched purple cubes!
            if (meta.getItemModel() != null) {
                meta.setItemModel(null);
                meta.setEnchantmentGlintOverride(true);
                item.setItemMeta(meta);
            }
        } else {
            // Java player: If item is missing its 3D model, restore it so Java players see the full 3D custom skin!
            if (meta.getItemModel() == null) {
                meta.setItemModel(org.bukkit.NamespacedKey.minecraft(wrap.getModelName()));
                meta.setEnchantmentGlintOverride(null);
                item.setItemMeta(meta);
            }
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof WrapInventoryHolder) {
            event.setCancelled(true);
            event.setResult(org.bukkit.event.Event.Result.DENY);
            if (event.getWhoClicked() instanceof Player player) {
                player.setItemOnCursor(null);
                event.getView().setCursor(null);
                Bukkit.getScheduler().runTask(plugin, player::updateInventory);
            }
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void onInventoryClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof WrapInventoryHolder) {
            if (event.getPlayer() instanceof Player player) {
                player.setItemOnCursor(null);
                event.getView().setCursor(null);
                com.moonlight.vanilla.modules.gui.GuiSecurityListener.cleanGuiItemsFromPlayer(player);
                Bukkit.getScheduler().runTask(plugin, player::updateInventory);
            }
        }
    }
}
