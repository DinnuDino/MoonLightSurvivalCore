package com.moonlight.vanilla.modules.ranks;

import com.moonlight.vanilla.MoonlightVanillaSurvivalCore;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * RanksManager
 * Handles configuration loading, tier definitions, and permission checks for paid ranks.
 */
public class RanksManager {

    private final MoonlightVanillaSurvivalCore plugin;
    private final Map<String, RankTier> ranks = new LinkedHashMap<>();
    private final Map<Integer, RankTier> ranksBySlot = new HashMap<>();
    private String storeUrl;
    private String guiTitle;

    public RanksManager(MoonlightVanillaSurvivalCore plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        reload();
    }

    public void reload() {
        ranks.clear();
        ranksBySlot.clear();

        FileConfiguration config = plugin.getConfig();
        this.storeUrl = config.getString("ranks.store-url", config.getString("links.website", "https://www.moonlightsmp.info/"));
        this.guiTitle = config.getString("ranks.gui-title", "▶ STORE ◀");

        ConfigurationSection section = config.getConfigurationSection("ranks.tiers");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection tierSec = section.getConfigurationSection(key);
                if (tierSec == null) continue;

                String id = key.toLowerCase(Locale.ROOT);
                String name = tierSec.getString("name", capitalize(id));
                String display = tierSec.getString("display", "<gold><bold>" + name.toUpperCase(Locale.ROOT) + " RANK</bold></gold>");
                String price = tierSec.getString("price", "$4.99");
                String matStr = tierSec.getString("material", "ENCHANTED_BOOK");
                Material mat = Material.matchMaterial(matStr);
                if (mat == null) mat = Material.ENCHANTED_BOOK;
                int slot = tierSec.getInt("slot", 19);
                String desc = tierSec.getString("description", "");
                String perm = tierSec.getString("permission", "moonlight.wraps.tier." + id);
                List<String> perks = tierSec.getStringList("perks");

                RankTier tier = new RankTier(id, name, display, price, mat, slot, desc, perm, perks);
                ranks.put(id, tier);
                if (id.equals("honored")) {
                    ranks.put("honoured", tier);
                } else if (id.equals("honoured")) {
                    ranks.put("honored", tier);
                }
                ranksBySlot.put(slot, tier);
            }
        }

        // Ensure defaults if no ranks defined
        if (ranks.isEmpty()) {
            loadDefaultRanks();
        }

        plugin.getLogger().info("[RanksManager] Loaded " + ranks.size() + " paid ranks for /ranks GUI.");
    }

    private void loadDefaultRanks() {
        // 1. Friendly (Slot 19)
        RankTier friendly = new RankTier(
                "friendly", "Friendly", "<green><bold>FRIENDLY RANK</bold></green>",
                "$4.99 / ₹299", Material.ENCHANTED_BOOK, 19,
                "Entry supporter rank with essential quality-of-life perks.",
                "moonlight.wraps.tier.friendly",
                List.of(
                        "Friendly Prefix & Nametag",
                        "2 Particle Cosmetic Slots",
                        "Medieval Skinline Wraps",
                        "10% Discount on Ingame Store Purchases",
                        "Double Voting Rewards",
                        "Access to <blue>/HAT</blue>",
                        "Access to <blue>/SKIN</blue>",
                        "Access to <blue>/WORKBENCH</blue> (Survival/OB)",
                        "Access to <blue>/REPAIR</blue> (Survival/OB)",
                        "1 PlayerVaults(54 Slots) (Survival/OB)",
                        "1 Extra Home (Survival/OB)"
                )
        );

        // 2. Honored (Slot 21)
        RankTier honored = new RankTier(
                "honored", "Honored", "<yellow><bold>HONORED RANK</bold></yellow>",
                "$9.99 / ₹599", Material.ENCHANTED_BOOK, 21,
                "Elevate your survival experience with advanced tools.",
                "moonlight.wraps.tier.honoured",
                List.of(
                        "Honored Prefix & Nametag",
                        "4 Particle Cosmetic Slots",
                        "Jungle Skinline Wraps",
                        "20% Discount on Ingame Store Purchases",
                        "Double Voting Rewards",
                        "Access to <blue>/HAT</blue>",
                        "Access to <blue>/SKIN</blue>",
                        "Access to <blue>/WORKBENCH</blue> (Survival/OB)",
                        "Access to <blue>/REPAIR</blue> (Survival/OB)",
                        "2 PlayerVaults(54 Slots) (Survival/OB)",
                        "2 Extra Home (Survival/OB)"
                )
        );

        // 3. Revered (Slot 23)
        RankTier revered = new RankTier(
                "revered", "Revered", "<light_purple><bold>REVERED RANK</bold></light_purple>",
                "$19.99 / ₹1,199", Material.ENCHANTED_BOOK, 23,
                "Elite survival rank with powerful convenience perks.",
                "moonlight.wraps.tier.revered",
                List.of(
                        "Revered Prefix & Nametag",
                        "6 Particle Cosmetic Slots",
                        "Vinland Skinline Wraps",
                        "30% Discount on Ingame Store Purchases",
                        "Double Voting Rewards",
                        "Access to <blue>/HAT</blue>",
                        "Access to <blue>/SKIN</blue>",
                        "Access to <blue>/WORKBENCH</blue> (Survival/OB)",
                        "Access to <blue>/REPAIR</blue> (Survival/OB)",
                        "Access to <blue>/CONDENSE</blue> (Survival/OB)",
                        "3 PlayerVaults(54 Slots) (Survival/OB)",
                        "3 Extra Home (Survival/OB)",
                        "3 Keep XP on Death (Survival/OB)"
                )
        );

        // 4. Exalted (Slot 25)
        RankTier exalted = new RankTier(
                "exalted", "Exalted", "<aqua><bold>EXALTED RANK</bold></aqua>",
                "$34.99 / ₹1,999", Material.ENCHANTED_BOOK, 25,
                "The ultimate prestige rank with complete access to all perks.",
                "moonlight.wraps.tier.exalted",
                List.of(
                        "Exalted Prefix & Nametag",
                        "10 Particle Cosmetic Slots",
                        "Cyberpunk Skinline Wraps",
                        "40% Discount on Ingame Store Purchases",
                        "Double Voting Rewards",
                        "Access to <blue>/HAT</blue>",
                        "Access to <blue>/SKIN</blue>",
                        "Access to <blue>/WORKBENCH</blue> (Survival/OB)",
                        "Access to <blue>/REPAIR</blue> (Survival/OB)",
                        "Access to <blue>/CONDENSE</blue> (Survival/OB)",
                        "5 PlayerVaults(54 Slots) (Survival/OB)",
                        "5 Extra Home (Survival/OB)",
                        "Keep XP on Death (Survival/OB)",
                        "No AFK Kick"
                )
        );

        ranks.put("friendly", friendly);
        ranks.put("honored", honored);
        ranks.put("honoured", honored);
        ranks.put("revered", revered);
        ranks.put("exalted", exalted);

        ranksBySlot.put(19, friendly);
        ranksBySlot.put(21, honored);
        ranksBySlot.put(23, revered);
        ranksBySlot.put(25, exalted);
    }

    public boolean hasRank(Player player, RankTier tier) {
        if (player == null || tier == null) return false;
        if (player.isOp() || player.hasPermission("*") || player.hasPermission("moonlight.admin")) {
            return true;
        }

        String id = tier.getId().toLowerCase(Locale.ROOT);

        // Direct permission check
        if (player.hasPermission(tier.getPermission()) ||
            player.hasPermission("group." + id) ||
            player.hasPermission("moonlight.ranks." + id)) {
            return true;
        }

        // Hierarchy inheritance check
        if (player.hasPermission("moonlight.wraps.tier.exalted") || player.hasPermission("group.exalted")) {
            return true;
        }
        if (player.hasPermission("moonlight.wraps.tier.revered") || player.hasPermission("group.revered")) {
            return !id.equals("exalted");
        }
        if (player.hasPermission("moonlight.wraps.tier.honoured") || player.hasPermission("group.honoured")) {
            return id.equals("honoured") || id.equals("friendly");
        }
        if (player.hasPermission("moonlight.wraps.tier.friendly") || player.hasPermission("group.friendly")) {
            return id.equals("friendly");
        }

        return false;
    }

    public Collection<RankTier> getAllRanks() {
        return ranks.values();
    }

    public RankTier getRank(String id) {
        if (id == null) return null;
        return ranks.get(id.toLowerCase(Locale.ROOT));
    }

    public RankTier getRankBySlot(int slot) {
        return ranksBySlot.get(slot);
    }

    public String getStoreUrl() {
        return storeUrl;
    }

    public String getGuiTitle() {
        return guiTitle;
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return "";
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
}
