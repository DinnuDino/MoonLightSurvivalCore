package com.moonlight.vanilla.modules.wraps;

import com.moonlight.vanilla.MoonlightVanillaSurvivalCore;
import com.moonlight.vanilla.utils.MessageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class WrapGui {

    private final MoonlightVanillaSurvivalCore plugin;
    private final WrapManager wrapManager;

    private final NamespacedKey actionKey;
    private final NamespacedKey wrapSlotKey;
    private final NamespacedKey pageTargetKey;

    private static Method setItemModelMethod = null;
    private static boolean modelChecked = false;

    public WrapGui(MoonlightVanillaSurvivalCore plugin, WrapManager wrapManager) {
        this.plugin = plugin;
        this.wrapManager = wrapManager;
        this.actionKey = new NamespacedKey(plugin, "gui_action");
        this.wrapSlotKey = new NamespacedKey(plugin, "gui_wrap_key");
        this.pageTargetKey = new NamespacedKey(plugin, "gui_page_target");

        if (!modelChecked) {
            modelChecked = true;
            try {
                setItemModelMethod = ItemMeta.class.getMethod("setItemModel", NamespacedKey.class);
            } catch (NoSuchMethodException ignored) {
                setItemModelMethod = null;
            }
        }
    }

    public void openMenu(Player player, ItemStack heldItem, int page) {
        if (heldItem == null || heldItem.getType().isAir()) {
            player.sendMessage(MessageUtil.format("&9MoonLight &8❙ &cYou must be holding an item in your main hand to open wraps!"));
            return;
        }

        if (!wrapManager.isSupportedItem(heldItem.getType())) {
            openNoWrapMenu(player, heldItem);
        } else {
            openWrapListMenu(player, heldItem, page);
        }
    }

    private void openNoWrapMenu(Player player, ItemStack heldItem) {
        WrapInventoryHolder holder = new WrapInventoryHolder(heldItem, 1, 1, true);
        Inventory inv = Bukkit.createInventory(holder, 27, MessageUtil.format("<gradient:#7dd3fc:#38bdf8:#2563eb>✦ MoonLight ❙ Vanilla Item ✦</gradient>"));
        holder.setInventory(inv);

        ItemStack filler = createPane(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, filler);
        }

        // Slot 13: The Vanilla Item
        ItemStack vanillaItem = new ItemStack(heldItem.getType());
        ItemMeta meta = vanillaItem.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtil.format("&e&lDefault Vanilla Item"));
            List<Component> lore = new ArrayList<>();
            lore.add(MessageUtil.format("&7No custom wraps are available for this item."));
            lore.add(MessageUtil.format("&8Hold a sword, axe, pickaxe, hoe, or mace to view wraps!"));
            meta.lore(lore);
            vanillaItem.setItemMeta(meta);
        }
        inv.setItem(13, com.moonlight.vanilla.modules.gui.GuiSecurityListener.markGuiItem(vanillaItem));

        // Slot 22: Close barrier
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        if (closeMeta != null) {
            closeMeta.displayName(MessageUtil.format("&c&lClose Menu"));
            close.setItemMeta(closeMeta);
        }
        inv.setItem(22, com.moonlight.vanilla.modules.gui.GuiSecurityListener.markGuiItem(close));

        player.openInventory(inv);
    }

    private void openWrapListMenu(Player player, ItemStack heldItem, int page) {
        List<Wrap> allWraps = wrapManager.getWraps();
        int total = allWraps.size();
        int perPage = 36;
        int maxPages = Math.max(1, (int) Math.ceil((double) total / perPage));

        if (page < 1) page = 1;
        if (page > maxPages) page = maxPages;

        Wrap currentWrap = wrapManager.getCurrentWrap(heldItem);
        String currentKey = currentWrap != null ? currentWrap.getKey() : null;

        WrapInventoryHolder holder = new WrapInventoryHolder(heldItem, page, maxPages, false);
        Inventory inv = Bukkit.createInventory(holder, 54, MessageUtil.format("<gradient:#7dd3fc:#38bdf8:#2563eb>✦ MoonLight ❙ Wraps (Page " + page + "/" + maxPages + ") ✦</gradient>"));
        holder.setInventory(inv);

        // Fill background borders
        ItemStack filler = createPane(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) inv.setItem(i, filler);
        for (int i = 45; i < 54; i++) inv.setItem(i, filler);

        long remainingCd = wrapManager.getRemainingCooldown(player);
        long cdTotal = wrapManager.getCooldownSeconds(player);

        // Slot 0: Info Icon
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.displayName(MessageUtil.format("&b&lMoonLight Weapon Wraps"));
            List<Component> lore = new ArrayList<>();
            lore.add(MessageUtil.format("&7Held Weapon: &f" + formatMaterialName(heldItem.getType())));
            lore.add(MessageUtil.format("&7Current Wrap: " + (currentWrap != null ? "&a" + currentWrap.getName() : "&eDefault Vanilla")));
            if (cdTotal <= 0) {
                lore.add(MessageUtil.format("&7Cooldown: &aNo Limit (Instant)"));
            } else if (remainingCd > 0) {
                lore.add(MessageUtil.format("&7Cooldown: &c" + wrapManager.formatCooldown(remainingCd) + " &7remaining"));
            } else {
                lore.add(MessageUtil.format("&7Cooldown: &e" + wrapManager.formatCooldown(cdTotal)));
            }
            lore.add(Component.empty());
            lore.add(MessageUtil.format("&8Select any unlocked 3D blade skin"));
            lore.add(MessageUtil.format("&8to customize your weapon appearance!"));
            infoMeta.lore(lore);
            info.setItemMeta(infoMeta);
        }
        inv.setItem(0, info);

        // Slot 4: Vanilla Item at top (Remove Wrap)
        ItemStack vanillaBtn = new ItemStack(heldItem.getType());
        ItemMeta vMeta = vanillaBtn.getItemMeta();
        if (vMeta != null) {
            vMeta.displayName(MessageUtil.format("&e&lDefault Vanilla Appearance"));
            List<Component> lore = new ArrayList<>();
            lore.add(MessageUtil.format("&7Click to remove current wrap and"));
            lore.add(MessageUtil.format("&7restore the default vanilla appearance."));
            lore.add(Component.empty());
            if (currentWrap == null) {
                lore.add(MessageUtil.format("&a✔ Currently using default appearance"));
            } else {
                lore.add(MessageUtil.format("&c✖ Click to strip active wrap &7(&f" + currentWrap.getName() + "&7)"));
            }
            vMeta.lore(lore);
            vMeta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "remove_wrap");
            vanillaBtn.setItemMeta(vMeta);
        }
        inv.setItem(4, vanillaBtn);

        // Populate Wraps (Slots 9 to 44)
        int startIndex = (page - 1) * perPage;
        int endIndex = Math.min(startIndex + perPage, total);

        int slot = 9;
        for (int i = startIndex; i < endIndex; i++) {
            Wrap wrap = allWraps.get(i);
            boolean isEquipped = currentKey != null && currentKey.equalsIgnoreCase(wrap.getKey());
            boolean hasPermission = wrapManager.hasWrapPermission(player, wrap);

            ItemStack wrapItem = new ItemStack(heldItem.getType());
            ItemMeta wMeta = wrapItem.getItemMeta();
            if (wMeta != null) {
                // Match the custom name expected by Blades Of Majestica cases
                wMeta.displayName(Component.text(wrap.getName()).decoration(TextDecoration.ITALIC, false));

                // If Paper 1.21.4 setItemModel is available, set it directly
                if (setItemModelMethod != null) {
                    try {
                        NamespacedKey key = NamespacedKey.minecraft("item/" + wrap.getModelName());
                        setItemModelMethod.invoke(wMeta, key);
                    } catch (Exception ignored) {}
                }

                List<Component> lore = new ArrayList<>();
                lore.add(MessageUtil.format("&83D Model: &7" + wrap.getModelName()));
                lore.add(Component.empty());

                if (isEquipped) {
                    lore.add(MessageUtil.format("&a✔ CURRENTLY EQUIPPED"));
                    lore.add(MessageUtil.format("&7This wrap is active on your held item."));
                } else if (hasPermission) {
                    if (remainingCd > 0) {
                        lore.add(MessageUtil.format("&a✔ Unlocked &8(Permitted)"));
                        lore.add(MessageUtil.format("&c⏳ On Cooldown: &e" + wrapManager.formatCooldown(remainingCd) + " left"));
                    } else {
                        lore.add(MessageUtil.format("&a✔ Unlocked &8(Click to Apply)"));
                        lore.add(MessageUtil.format("&eClick to equip this 3D wrap skin!"));
                    }
                } else {
                    lore.add(MessageUtil.format("&c🔒 Locked &8(Requires Permission)"));
                    lore.add(MessageUtil.format("&7Permission: &c" + wrap.getPermission()));
                    lore.add(MessageUtil.format("&8You do not have access to apply this wrap."));
                    lore.add(MessageUtil.format("&8Grant access via LuckPerms!"));
                }

                wMeta.lore(lore);
                PersistentDataContainer pdc = wMeta.getPersistentDataContainer();
                pdc.set(actionKey, PersistentDataType.STRING, "apply_wrap");
                pdc.set(wrapSlotKey, PersistentDataType.STRING, wrap.getKey());

                wrapItem.setItemMeta(wMeta);
            }
            inv.setItem(slot++, wrapItem);
        }

        // Fill remaining wrap slots if any
        while (slot < 45) {
            inv.setItem(slot++, createPane(Material.GRAY_STAINED_GLASS_PANE, " "));
        }

        // Navigation (Slots 45, 49, 53)
        if (page > 1) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta pMeta = prev.getItemMeta();
            if (pMeta != null) {
                pMeta.displayName(MessageUtil.format("&e&l← Previous Page &7(" + (page - 1) + "/" + maxPages + ")"));
                pMeta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "change_page");
                pMeta.getPersistentDataContainer().set(pageTargetKey, PersistentDataType.INTEGER, page - 1);
                prev.setItemMeta(pMeta);
            }
            inv.setItem(45, prev);
        } else {
            inv.setItem(45, createPane(Material.GRAY_STAINED_GLASS_PANE, " "));
        }

        ItemStack pageInfo = new ItemStack(Material.PAPER);
        ItemMeta piMeta = pageInfo.getItemMeta();
        if (piMeta != null) {
            piMeta.displayName(MessageUtil.format("&b&lPage " + page + " &7of &b&l" + maxPages));
            List<Component> lore = new ArrayList<>();
            lore.add(MessageUtil.format("&7Total Wraps: &f" + total));
            lore.add(MessageUtil.format("&7Showing: &f" + (startIndex + 1) + " - " + endIndex));
            piMeta.lore(lore);
            pageInfo.setItemMeta(piMeta);
        }
        inv.setItem(49, pageInfo);

        if (page < maxPages) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta nMeta = next.getItemMeta();
            if (nMeta != null) {
                nMeta.displayName(MessageUtil.format("&e&lNext Page → &7(" + (page + 1) + "/" + maxPages + ")"));
                nMeta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "change_page");
                nMeta.getPersistentDataContainer().set(pageTargetKey, PersistentDataType.INTEGER, page + 1);
                next.setItemMeta(nMeta);
            }
            inv.setItem(53, next);
        } else {
            inv.setItem(53, createPane(Material.GRAY_STAINED_GLASS_PANE, " "));
        }

        player.openInventory(inv);
    }

    private ItemStack createPane(Material material, String name) {
        ItemStack pane = new ItemStack(material);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtil.format(name));
            pane.setItemMeta(meta);
        }
        return pane;
    }

    private String formatMaterialName(Material mat) {
        String[] parts = mat.name().toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }

    private String formatTierBadge(String tier) {
        if (tier == null) return "&aFriendly";
        return switch (tier.toLowerCase(java.util.Locale.ROOT)) {
            case "friendly" -> "&aFriendly";
            case "honoured" -> "&eHonoured";
            case "revered" -> "&#d946efRevered";
            case "exalted" -> "&#38bdf8Exalted";
            default -> "&aFriendly";
        };
    }

    public NamespacedKey getActionKey() {
        return actionKey;
    }

    public NamespacedKey getWrapSlotKey() {
        return wrapSlotKey;
    }

    public NamespacedKey getPageTargetKey() {
        return pageTargetKey;
    }
}
