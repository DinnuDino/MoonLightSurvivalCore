package com.moonlight.vanilla.modules.wraps;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.moonlight.vanilla.MoonlightVanillaSurvivalCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;

public class WrapManager {

    private final MoonlightVanillaSurvivalCore plugin;
    private final List<Wrap> wraps = new ArrayList<>();
    private final Map<String, Wrap> wrapMap = new HashMap<>();

    private final NamespacedKey wrapKey;
    private final NamespacedKey originalNameKey;
    private final NamespacedKey originalLoreKey;

    public WrapManager(MoonlightVanillaSurvivalCore plugin) {
        this.plugin = plugin;
        this.wrapKey = new NamespacedKey(plugin, "wrap_id");
        this.originalNameKey = new NamespacedKey(plugin, "original_custom_name");
        this.originalLoreKey = new NamespacedKey(plugin, "original_custom_lore");
    }

    public void initialize() {
        loadWraps();
    }

    public void loadWraps() {
        wraps.clear();
        wrapMap.clear();

        // 1. Load from wraps.json in jar if available
        try (InputStream in = plugin.getResource("wraps.json")) {
            if (in != null) {
                List<Map<String, Object>> rawList = new Gson().fromJson(
                        new InputStreamReader(in, StandardCharsets.UTF_8),
                        new TypeToken<List<Map<String, Object>>>() {}.getType()
                );
                for (Map<String, Object> m : rawList) {
                    String key = (String) m.get("key");
                    String name = (String) m.get("name");
                    String category = (String) m.get("category");
                    String colorFormat = (String) m.get("color_format");
                    String tier = (String) m.getOrDefault("tier", "default");
                    String model = (String) m.get("model");
                    String modelName = (String) m.get("model_name");
                    int customModelData = 0;
                    if (m.containsKey("custom_model_data")) {
                        Object cmdVal = m.get("custom_model_data");
                        if (cmdVal instanceof Number) {
                            customModelData = ((Number) cmdVal).intValue();
                        } else if (cmdVal != null) {
                            try {
                                customModelData = Integer.parseInt(cmdVal.toString());
                            } catch (Exception ignored) {}
                        }
                    }
                    Wrap wrap = new Wrap(key, name, category, colorFormat, tier, model, modelName, customModelData, "moonlight.wraps.use." + key);
                    wraps.add(wrap);
                    wrapMap.put(key.toLowerCase(Locale.ROOT), wrap);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load wraps from wraps.json: " + e.getMessage(), e);
        }

        plugin.getLogger().info("[Wraps] Loaded " + wraps.size() + " weapon wraps successfully.");
    }

    public List<Wrap> getWraps() {
        return Collections.unmodifiableList(wraps);
    }

    public Wrap getWrap(String key) {
        if (key == null) return null;
        return wrapMap.get(key.toLowerCase(Locale.ROOT));
    }

    public String getItemCategory(Material material) {
        if (material == null) return null;
        String name = material.name();
        if (name.endsWith("_SWORD")) return "SWORD";
        if (name.endsWith("_AXE")) return "AXE";
        if (name.endsWith("_PICKAXE")) return "PICKAXE";
        if (name.endsWith("_SHOVEL")) return "SHOVEL";
        if (name.endsWith("_HOE")) return "HOE";
        if (name.equals("BOW") || name.equals("CROSSBOW")) return "BOW";
        if (name.equals("TRIDENT")) return "TRIDENT";
        if (name.equals("MACE")) return "MACE";
        if (name.endsWith("_SPEAR") || name.equals("SPEAR")) return "SPEAR";
        return null;
    }

    public List<Wrap> getWrapsFor(Material material) {
        String cat = getItemCategory(material);
        if (cat == null) return Collections.emptyList();
        List<Wrap> matching = new ArrayList<>();
        for (Wrap w : wraps) {
            if (w.getCategory().equalsIgnoreCase(cat)) {
                matching.add(w);
            } else if (cat.equals("HOE") && w.getKey().contains("scythe")) {
                matching.add(w);
            } else if (cat.equals("SPEAR") && (w.getCategory().equalsIgnoreCase("TRIDENT") || w.getCategory().equalsIgnoreCase("SPEAR"))) {
                matching.add(w);
            }
        }
        return matching;
    }

    public boolean isSupportedItem(Material material) {
        if (material == null || material.isAir()) return false;
        return !getWrapsFor(material).isEmpty();
    }

    public static String formatMaterialName(Material mat) {
        if (mat == null) return "";
        String[] parts = mat.name().toLowerCase(Locale.ROOT).split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }

    public boolean hasWrapPermission(Player player, Wrap wrap) {
        if (player == null) return false;
        if (player.isOp()) return true;
        if (player.hasPermission("moonlight.admin") || player.hasPermission("*")) return true;
        if (player.hasPermission("moonlight.wraps.all") || player.hasPermission("moonlight.wraps.use.*")) return true;
        if (player.hasPermission("moonlight.wraps.use")) return true;
        return player.hasPermission(wrap.getPermission()) || player.hasPermission("moonlight.wraps.use." + wrap.getKey());
    }

    public String getCurrentWrapKey(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (pdc.has(wrapKey, PersistentDataType.STRING)) {
            return pdc.get(wrapKey, PersistentDataType.STRING);
        }

        // Fallback: check if display name matches any wrap name exactly
        if (meta.hasDisplayName()) {
            String name = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(meta.displayName());
            for (Wrap w : wraps) {
                if (w.getName().equalsIgnoreCase(name.trim())) {
                    return w.getKey();
                }
            }
        }
        return null;
    }

    public Wrap getCurrentWrap(ItemStack item) {
        String key = getCurrentWrapKey(item);
        return key != null ? getWrap(key) : null;
    }

    public boolean applyWrap(Player player, ItemStack item, Wrap wrap) {
        if (item == null || wrap == null) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        boolean alreadyHasWrap = pdc.has(wrapKey, PersistentDataType.STRING);

        // Store original name and lore if not wrapped already
        if (!alreadyHasWrap) {
            if (meta.hasDisplayName()) {
                String orig = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(meta.displayName());
                pdc.set(originalNameKey, PersistentDataType.STRING, orig);
            }
            if (meta.hasLore()) {
                List<String> origLore = new ArrayList<>();
                for (Component c : meta.lore()) {
                    origLore.add(net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson().serialize(c));
                }
                pdc.set(originalLoreKey, PersistentDataType.STRING, new Gson().toJson(origLore));
            }
        }

        pdc.set(wrapKey, PersistentDataType.STRING, wrap.getKey());

        // Set 3D item model directly (Paper 1.21.4 native)
        if (wrap.getModelName() != null && !wrap.getModelName().isEmpty()) {
            meta.setItemModel(NamespacedKey.minecraft(wrap.getModelName()));
        }

        // Set CustomModelData for Geyser crossplay and legacy Java clients (1.16 - 1.20.x)
        if (wrap.getCustomModelData() > 0) {
            meta.setCustomModelData(wrap.getCustomModelData());
        }

        meta.setEnchantmentGlintOverride(null);

        // Set cool colored display name
        meta.displayName(com.moonlight.vanilla.utils.MessageUtil.format(wrap.getColorFormat()));

        // Add Base Item in lore below the name
        List<Component> currentLore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        currentLore.removeIf(c -> {
            String text = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(c);
            return text.toLowerCase(Locale.ROOT).contains("base item:");
        });
        currentLore.add(0, com.moonlight.vanilla.utils.MessageUtil.format("<italic:false><gray>Base Item: <white>" + formatMaterialName(item.getType())));
        meta.lore(currentLore);

        item.setItemMeta(meta);
        return true;
    }

    public boolean removeWrap(Player player, ItemStack item) {
        if (item == null) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        boolean hadWrap = pdc.has(wrapKey, PersistentDataType.STRING);

        // Remove 3D item model, CustomModelData & glint override
        meta.setItemModel(null);
        meta.setCustomModelData(null);
        meta.setEnchantmentGlintOverride(null);

        // Restore original name
        if (pdc.has(originalNameKey, PersistentDataType.STRING)) {
            String orig = pdc.get(originalNameKey, PersistentDataType.STRING);
            if (orig != null && !orig.isEmpty()) {
                meta.displayName(Component.text(orig));
            } else {
                meta.displayName(null);
            }
            pdc.remove(originalNameKey);
        } else {
            meta.displayName(null);
        }

        // Restore original lore
        if (pdc.has(originalLoreKey, PersistentDataType.STRING)) {
            String json = pdc.get(originalLoreKey, PersistentDataType.STRING);
            try {
                List<String> list = new Gson().fromJson(json, new TypeToken<List<String>>(){}.getType());
                List<Component> restoredLore = new ArrayList<>();
                for (String s : list) {
                    restoredLore.add(net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson().deserialize(s));
                }
                meta.lore(restoredLore);
            } catch (Exception e) {
                meta.lore(null);
            }
            pdc.remove(originalLoreKey);
        } else {
            if (meta.hasLore()) {
                List<Component> lore = new ArrayList<>(meta.lore());
                lore.removeIf(c -> {
                    String text = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(c);
                    return text.toLowerCase(Locale.ROOT).contains("base item:");
                });
                meta.lore(lore.isEmpty() ? null : lore);
            }
        }

        pdc.remove(wrapKey);

        item.setItemMeta(meta);
        return hadWrap;
    }

    private final Map<UUID, Long> lastWrapTimes = new java.util.concurrent.ConcurrentHashMap<>();

    public long getCooldownSeconds(Player player) {
        if (player == null) return 0;
        if (player.isOp() || player.hasPermission("moonlight.wraps.bypass.cooldown") || player.hasPermission("moonlight.admin")) {
            return 0; // No limit for operators and admins
        }
        if (!plugin.getConfig().getBoolean("wraps.cooldown.enabled", true)) {
            return 0;
        }

        long defaultSec = plugin.getConfig().getLong("wraps.cooldown.default-seconds", 300L);
        long lowestCooldown = defaultSec;

        // Check configured rank groups (e.g., moonlight.wraps.cooldown.vip)
        org.bukkit.configuration.ConfigurationSection ranks = plugin.getConfig().getConfigurationSection("wraps.cooldown.ranks");
        if (ranks != null) {
            for (String rank : ranks.getKeys(false)) {
                if (player.hasPermission("moonlight.wraps.cooldown." + rank.toLowerCase(Locale.ROOT))) {
                    long rankSec = ranks.getLong(rank, defaultSec);
                    if (rankSec < lowestCooldown) {
                        lowestCooldown = rankSec;
                    }
                }
            }
        }

        return lowestCooldown;
    }

    public long getRemainingCooldown(Player player) {
        if (player == null) return 0;
        long cd = getCooldownSeconds(player);
        if (cd <= 0) return 0;

        long last = lastWrapTimes.getOrDefault(player.getUniqueId(), 0L);
        long elapsedSec = (System.currentTimeMillis() - last) / 1000L;
        if (elapsedSec < cd) {
            return cd - elapsedSec;
        }
        return 0;
    }

    public void recordWrapApplication(Player player) {
        if (player != null) {
            lastWrapTimes.put(player.getUniqueId(), System.currentTimeMillis());
        }
    }

    public String formatCooldown(long seconds) {
        if (seconds <= 0) return "0s";
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        if (h > 0) {
            return h + "h " + m + "m " + s + "s";
        } else if (m > 0) {
            return m + "m " + s + "s";
        } else {
            return s + "s";
        }
    }

    public NamespacedKey getWrapKey() {
        return wrapKey;
    }
}
