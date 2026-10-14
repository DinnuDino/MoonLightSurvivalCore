/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 */
package com.moonlight.vanilla.modules.punishments;

import com.moonlight.vanilla.MoonlightVanillaSurvivalCore;
import com.moonlight.vanilla.modules.punishments.MuteRecord;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class MuteManager {
    private final MoonlightVanillaSurvivalCore plugin;
    private final Map<UUID, MuteRecord> activeMutes = new ConcurrentHashMap<UUID, MuteRecord>();

    public MuteManager(MoonlightVanillaSurvivalCore plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        this.plugin.getServer().getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> {
            try (Connection conn = this.plugin.getDatabaseManager().getConnection();
                 Statement stmt = conn.createStatement();){
                stmt.execute("CREATE TABLE IF NOT EXISTS player_mutes (id INTEGER PRIMARY KEY AUTOINCREMENT, target_uuid TEXT, target_name TEXT, muted_by TEXT, expires_at BIGINT)");
                try (ResultSet rs = stmt.executeQuery("SELECT * FROM player_mutes");){
                    while (rs.next()) {
                        long expires;
                        Long expiresAt;
                        String mutedBy;
                        String targetName;
                        UUID uuid = UUID.fromString(rs.getString("target_uuid"));
                        MuteRecord rec = new MuteRecord(uuid, targetName = rs.getString("target_name"), mutedBy = rs.getString("muted_by"), expiresAt = (expires = rs.getLong("expires_at")) == -1L ? null : Long.valueOf(expires));
                        if (rec.isExpired()) continue;
                        this.activeMutes.put(uuid, rec);
                    }
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public MuteRecord getMute(Player player) {
        MuteRecord rec = this.activeMutes.get(player.getUniqueId());
        if (rec != null && rec.isExpired()) {
            this.unmute(player.getUniqueId());
            return null;
        }
        return rec;
    }

    public boolean isMuted(Player player) {
        return this.getMute(player) != null;
    }

    public void mute(Player target, String mutedBy, Long expiresAt) {
        MuteRecord rec = new MuteRecord(target.getUniqueId(), target.getName(), mutedBy, expiresAt);
        this.activeMutes.put(target.getUniqueId(), rec);
        this.plugin.getServer().getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> {
            try (Connection conn = this.plugin.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement("INSERT INTO player_mutes (target_uuid, target_name, muted_by, expires_at) VALUES (?, ?, ?, ?)");){
                ps.setString(1, target.getUniqueId().toString());
                ps.setString(2, target.getName());
                ps.setString(3, mutedBy);
                ps.setLong(4, expiresAt == null ? -1L : expiresAt);
                ps.executeUpdate();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void unmute(UUID targetUuid) {
        this.activeMutes.remove(targetUuid);
        this.plugin.getServer().getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> {
            try (Connection conn = this.plugin.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM player_mutes WHERE target_uuid = ?");){
                ps.setString(1, targetUuid.toString());
                ps.executeUpdate();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}

