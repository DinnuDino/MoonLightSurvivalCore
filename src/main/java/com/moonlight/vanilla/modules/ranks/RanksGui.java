package com.moonlight.vanilla.modules.ranks;

import com.moonlight.vanilla.MoonlightVanillaSurvivalCore;
import com.moonlight.vanilla.utils.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * RanksGui
 * Builds the interactive 45-slot Store / Ranks GUI matching the official server screenshots.
 * Layout:
 *   Row 0 (0-8): Orange Stained Glass Panes
 *   Row 1 (9-17): Empty slots
 *   Row 2: Slot 19 (Friendly), Slot 21 (Honored), Slot 23 (Revered), Slot 25 (Exalted)
 *   Row 3 (27-35): Empty slots
 *   Row 4 (36-44): Orange Stained Glass Panes with Slot 40 Campfire Store button
 */
public class RanksGui {

    private final MoonlightVanillaSurvivalCore plugin;
    private final RanksManager manager;

    public RanksGui(MoonlightVanillaSurvivalCore plugin, RanksManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    public void openGui(Player player) {
        RanksInventoryHolder holder = new RanksInventoryHolder(player);
        Component titleComp = MessageUtil.format(manager.getGuiTitle());
        Inventory inv = Bukkit.createInventory(holder, 45, titleComp);
        holder.setInventory(inv);

        // 1. Fill top row (0..8) and bottom row (36..44) with Orange Stained Glass Panes
        ItemStack orangePane = createPane(Material.ORANGE_STAINED_GLASS_PANE);
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, orangePane);
        }
        for (int i = 36; i < 45; i++) {
            inv.setItem(i, orangePane);
        }

        // 2. Middle Campfire button on bottom row (Slot 40)
        ItemStack campfireBtn = new ItemStack(Material.CAMPFIRE);
        ItemMeta campfireMeta = campfireBtn.getItemMeta();
        if (campfireMeta != null) {
            campfireMeta.displayName(MessageUtil.format("<gradient:#ff8800:#ffaa00><bold>Moonlight Web Store</bold></gradient>"));
            List<Component> campfireLore = new ArrayList<>();
            campfireLore.add(MessageUtil.format("<gray>Visit our official web store to purchase ranks:</gray>"));
            campfireLore.add(MessageUtil.format("<aqua><underlined>" + manager.getStoreUrl() + "</underlined></aqua>"));
            campfireLore.add(Component.empty());
            campfireLore.add(MessageUtil.format("<gradient:#ff8800:#ffaa00><bold>▶ CLICK TO RECEIVE STORE LINK</bold></gradient>"));
            campfireMeta.lore(campfireLore);
            campfireBtn.setItemMeta(campfireMeta);
        }
        inv.setItem(40, com.moonlight.vanilla.modules.gui.GuiSecurityListener.markGuiItem(campfireBtn));

        // 3. Populate Rank Tier Cards at row 2 (Slots 19, 21, 23, 25)
        Set<String> processed = new HashSet<>();
        for (RankTier tier : manager.getAllRanks()) {
            if (tier == null || !processed.add(tier.getName().toLowerCase())) {
                continue;
            }
            ItemStack card = createRankCard(player, tier);
            int slot = tier.getSlot();
            if (slot >= 0 && slot < 45) {
                inv.setItem(slot, com.moonlight.vanilla.modules.gui.GuiSecurityListener.markGuiItem(card));
            }
        }

        player.openInventory(inv);
    }

    public ItemStack createRankCard(Player player, RankTier tier) {
        boolean isOwned = manager.hasRank(player, tier);

        // Display Knowledge Book (green book) if owned/selected, otherwise Enchanted Book
        Material bookMat = isOwned ? Material.KNOWLEDGE_BOOK : Material.ENCHANTED_BOOK;
        ItemStack item = new ItemStack(bookMat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        // Display Name (e.g. FRIENDLY RANK, HONORED RANK, REVERED RANK, EXALTED RANK)
        meta.displayName(MessageUtil.format(tier.getDisplayName()));

        // Tooltip lore matching screenshots exactly:
        // ❖ PERKS (underlined pink)
        // ▪ Perk (commands in blue /HAT, /SKIN, /WORKBENCH, /REPAIR, /CONDENSE)
        // (empty line)
        // ▶ CLICK TO PURCHASE! ◀
        List<Component> lore = new ArrayList<>();
        lore.add(MessageUtil.format("&f❖ &d&l&nPERKS&r"));

        for (String perk : tier.getPerks()) {
            String cleanPerk = perk.replace("<blue>", "&9")
                                   .replace("</blue>", "&f")
                                   .replace("<white>", "&f")
                                   .replace("</white>", "&f");

            // Highlight commands like /HAT, /SKIN, /WORKBENCH, /REPAIR, /CONDENSE in blue (&9)
            if (!cleanPerk.contains("&9")) {
                cleanPerk = cleanPerk.replace("/HAT", "&9/HAT&f")
                                     .replace("/SKIN", "&9/SKIN&f")
                                     .replace("/WORKBENCH", "&9/WORKBENCH&f")
                                     .replace("/REPAIR", "&9/REPAIR&f")
                                     .replace("/CONDENSE", "&9/CONDENSE&f");
            }

            lore.add(MessageUtil.format("&8▪ &f" + cleanPerk));
        }

        lore.add(Component.empty());
        lore.add(MessageUtil.format("&6▶ &6&l&nCLICK TO PURCHASE!&r &6◀"));

        meta.lore(lore);

        // Clean tooltips with no vanilla enchantment/attribute baggage
        if (!isOwned) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        }
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        try {
            meta.addItemFlags(ItemFlag.valueOf("HIDE_ADDITIONAL_TOOLTIP"));
        } catch (Throwable ignored) {}

        item.setItemMeta(meta);
        return com.moonlight.vanilla.modules.gui.GuiSecurityListener.markGuiItem(item);
    }

    private ItemStack createPane(Material mat) {
        ItemStack pane = new ItemStack(mat);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtil.format("<gray> </gray>"));
            pane.setItemMeta(meta);
        }
        return com.moonlight.vanilla.modules.gui.GuiSecurityListener.markGuiItem(pane);
    }
}
