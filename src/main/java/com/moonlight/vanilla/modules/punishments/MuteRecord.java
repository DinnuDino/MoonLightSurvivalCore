/*
 * Decompiled with CFR 0.152.
 */
package com.moonlight.vanilla.modules.punishments;

import java.util.UUID;

public class MuteRecord {
    private final UUID targetUuid;
    private final String targetName;
    private final String mutedBy;
    private final Long expiresAt;

    public MuteRecord(UUID targetUuid, String targetName, String mutedBy, Long expiresAt) {
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.mutedBy = mutedBy;
        this.expiresAt = expiresAt;
    }

    public boolean isPermanent() {
        return this.expiresAt == null;
    }

    public boolean isExpired() {
        return this.expiresAt != null && System.currentTimeMillis() > this.expiresAt;
    }

    public long getRemainingMillis() {
        return this.expiresAt != null ? this.expiresAt - System.currentTimeMillis() : -1L;
    }

    public String getFormattedRemaining() {
        if (this.isPermanent()) {
            return "Permanent";
        }
        long diff = this.getRemainingMillis();
        if (diff <= 0L) {
            return "0h 0m 0s (Expired)";
        }
        long totalSecs = (diff + 999L) / 1000L;
        long d = totalSecs / 86400L;
        long h = totalSecs % 86400L / 3600L;
        long m = totalSecs % 3600L / 60L;
        long s = totalSecs % 60L;
        StringBuilder sb = new StringBuilder();
        if (d > 0L) {
            sb.append(d).append("d ");
        }
        sb.append(h).append("h ").append(m).append("m ").append(s).append("s");
        return sb.toString();
    }
}

