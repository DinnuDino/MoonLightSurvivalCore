package com.moonlight.vanilla.modules.vault;

import com.moonlight.vanilla.MoonlightVanillaSurvivalCore;
import com.moonlight.vanilla.modules.audit.serialization.InventorySerializer;
import com.moonlight.vanilla.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * PlayerVaultManager
 * Manages 54-slot persistent player storage vaults backed by SQLite / MySQL.
 * Prevents item loss across server restarts and supports /pv, /pv1, /pv2, /pv3, /pv4, /pv5.
 */
public class PlayerVaultManager implements Listener {

    private final MoonlightVanillaSurvivalCore plugin;
    private final InventorySerializer serializer;
    private final Map<String, Inventory> vaultCache = new ConcurrentHashMap<>();

    public PlayerVaultManager(MoonlightVanillaSurvivalCore plugin) {
        this.plugin = plugin;
        this.serializer = new InventorySerializer();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Cache key helper: playerUuid:vaultNumber
     */
    private String getCacheKey(UUID uuid, int vaultNumber) {
        return uuid.toString() + ":" + vaultNumber;
    }

    /**
     * Determines whether a player has permission to open a specific vault number (1-5).
     * Vault 1 is unlocked for ALL players by default in survival.
     */
    public boolean canAccessVault(Player player, int vaultNumber) {
        if (vaultNumber < 1 || vaultNumber > 5) {
            return false;
        }

        // Vault 1 is available to all survival players by default
        if (vaultNumber == 1) {
            return true;
        }

        // Operators & Admins have full access to all vaults
        if (player.isOp() || player.hasPermission("moonlight.admin") || player.hasPermission("moonlight.pv.*")) {
            return true;
        }

        // Direct vault permission: moonlight.pv.2, moonlight.pv.3, etc.
        if (player.hasPermission("moonlight.pv." + vaultNumber)) {
            return true;
        }

        // Max vault permission: moonlight.pv.max.3, etc.
        for (int i = vaultNumber; i <= 5; i++) {
            if (player.hasPermission("moonlight.pv.max." + i)) {
                return true;
            }
        }

        // Rank tier perks:
        // Honored (Rank 2) -> 2 Vaults
        if (vaultNumber <= 2 && (player.hasPermission("moonlight.wraps.tier.honoured")
                || player.hasPermission("moonlight.wraps.tier.honored")
                || player.hasPermission("moonlight.rank.honored"))) {
            return true;
        }

        // Revered (Rank 3) -> 3 Vaults
        if (vaultNumber <= 3 && (player.hasPermission("moonlight.wraps.tier.revered")
                || player.hasPermission("moonlight.rank.revered"))) {
            return true;
        }

        // Exalted (Rank 4) -> 5 Vaults
        if (vaultNumber <= 5 && (player.hasPermission("moonlight.wraps.tier.exalted")
                || player.hasPermission("moonlight.rank.exalted"))) {
            return true;
        }

        return false;
    }

    /**
     * Opens a player vault asynchronously from DB or instantly from memory cache.
     */
    public void openVault(Player viewer, UUID targetUuid, String targetName, int vaultNumber) {
        String cacheKey = getCacheKey(targetUuid, vaultNumber);
        Inventory cached = vaultCache.get(cacheKey);

        if (cached != null) {
            viewer.openInventory(cached);
            viewer.playSound(viewer.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.7f, 1.0f);
            return;
        }

        // Asynchronously load from SQLite / MySQL database
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            ItemStack[] items = null;

            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT inventory_data FROM moonlight_player_vaults WHERE player_uuid = ? AND vault_number = ?")) {
                ps.setString(1, targetUuid.toString());
                ps.setInt(2, vaultNumber);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String data = rs.getString("inventory_data");
                        if (data != null && !data.isEmpty()) {
                            items = serializer.deserializeItemArray(data);
                        }
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load player vault #" + vaultNumber + " for " + targetUuid, e);
            }

            final ItemStack[] finalItems = items;
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!viewer.isOnline()) return;

                // Re-check cache in case loaded while querying
                Inventory inv = vaultCache.computeIfAbsent(cacheKey, k -> {
                    PlayerVaultHolder holder = new PlayerVaultHolder(targetUuid, targetName, vaultNumber);
                    String title = viewer.getUniqueId().equals(targetUuid)
                            ? "<gradient:#6366f1:#a855f7><bold>Player Vault #" + vaultNumber + "</bold></gradient>"
                            : "<gradient:#6366f1:#a855f7><bold>Vault #" + vaultNumber + "</bold></gradient> <gray>(" + targetName + ")</gray>";
                    Inventory created = Bukkit.createInventory(holder, 54, MessageUtil.format(title));
                    holder.setInventory(created);

                    if (finalItems != null && finalItems.length > 0) {
                        for (int i = 0; i < Math.min(54, finalItems.length); i++) {
                            created.setItem(i, finalItems[i]);
                        }
                    }
                    return created;
                });

                viewer.openInventory(inv);
                viewer.playSound(viewer.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.7f, 1.0f);
            });
        });
    }

    /**
     * Automatically saves vault data whenever a player closes the vault inventory.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof PlayerVaultHolder holder) {
            saveVaultAsync(holder);
        }
    }

    /**
     * Serializes and writes a player vault to the database asynchronously.
     */
    public void saveVaultAsync(PlayerVaultHolder holder) {
        if (holder == null || holder.getInventory() == null) return;

        ItemStack[] contents = holder.getInventory().getContents();
        String serialized = serializer.serializeItemArray(contents);
        UUID uuid = holder.getOwnerUuid();
        int vaultNumber = holder.getVaultNumber();
        long now = System.currentTimeMillis();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "INSERT OR REPLACE INTO moonlight_player_vaults (player_uuid, vault_number, inventory_data, updated_at) " +
                         "VALUES (?, ?, ?, ?)")) {
                ps.setString(1, uuid.toString());
                ps.setInt(2, vaultNumber);
                ps.setString(3, serialized);
                ps.setLong(4, now);
                ps.executeUpdate();
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save player vault #" + vaultNumber + " for " + uuid, e);
            }
        });
    }

    /**
     * Synchronously saves all cached vaults on plugin disable / server shutdown.
     */
    public void saveAllOnShutdown() {
        for (Inventory inv : vaultCache.values()) {
            if (inv.getHolder() instanceof PlayerVaultHolder holder) {
                try (Connection conn = plugin.getDatabaseManager().getConnection();
                     PreparedStatement ps = conn.prepareStatement(
                             "INSERT OR REPLACE INTO moonlight_player_vaults (player_uuid, vault_number, inventory_data, updated_at) " +
                             "VALUES (?, ?, ?, ?)")) {
                    ps.setString(1, holder.getOwnerUuid().toString());
                    ps.setInt(2, holder.getVaultNumber());
                    ps.setString(3, serializer.serializeItemArray(inv.getContents()));
                    ps.setLong(4, System.currentTimeMillis());
                    ps.executeUpdate();
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to shutdown-save vault #" + holder.getVaultNumber(), e);
                }
            }
        }
    }
}
