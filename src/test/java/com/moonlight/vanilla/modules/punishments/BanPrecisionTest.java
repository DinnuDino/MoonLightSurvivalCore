package com.moonlight.vanilla.modules.punishments;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class BanPrecisionTest {

    @Test
    @DisplayName("Verify formatDuration formats precisely down to seconds, e.g. 0h 0m 1s")
    void testFormatDurationPrecision() {
        assertEquals("0h 0m 1s", BanManager.formatDuration(1000L));
        assertEquals("0h 0m 45s", BanManager.formatDuration(45000L));
        assertEquals("0h 10m 0s", BanManager.formatDuration(10 * 60 * 1000L));
        assertEquals("2h 15m 30s", BanManager.formatDuration((2 * 3600 + 15 * 60 + 30) * 1000L));
        assertEquals("1d 0h 0m 1s", BanManager.formatDuration((86400 + 1) * 1000L));
        assertEquals("3d 12h 5m 9s", BanManager.formatDuration((3 * 86400 + 12 * 3600 + 5 * 60 + 9) * 1000L));
    }

    @Test
    @DisplayName("Verify BanRecord getFormattedRemaining outputs precise 0h 0m 1s format")
    void testBanRecordRemainingTime() {
        long now = System.currentTimeMillis();
        // 1 second in the future
        BanRecord ban1s = new BanRecord(1, "#MNL-TEST1", UUID.randomUUID(), "Player1",
                UUID.randomUUID(), "Staff", "Reason", now, now + 1000L, "0h 0m 1s", true,
                null, null, null, "127.0.0.1", "corr-1");
        assertEquals("0h 0m 1s", ban1s.getFormattedRemaining());

        // 3600 seconds (1 hour) in the future
        BanRecord ban1h = new BanRecord(2, "#MNL-TEST2", UUID.randomUUID(), "Player2",
                UUID.randomUUID(), "Staff", "Reason", now, now + 3600000L, "1h 0m 0s", true,
                null, null, null, "127.0.0.1", "corr-2");
        assertEquals("1h 0m 0s", ban1h.getFormattedRemaining());

        // 1 day and 1 second in the future
        BanRecord ban1d1s = new BanRecord(3, "#MNL-TEST3", UUID.randomUUID(), "Player3",
                UUID.randomUUID(), "Staff", "Reason", now, now + 86401000L, "1d 0h 0m 1s", true,
                null, null, null, "127.0.0.1", "corr-3");
        assertEquals("1d 0h 0m 1s", ban1d1s.getFormattedRemaining());

        // Permanent
        BanRecord permBan = new BanRecord(4, "#MNL-TEST4", UUID.randomUUID(), "Player4",
                UUID.randomUUID(), "Staff", "Reason", now, null, "permanent", true,
                null, null, null, "127.0.0.1", "corr-4");
        assertEquals("Permanent", permBan.getFormattedRemaining());

        // Expired
        BanRecord expBan = new BanRecord(5, "#MNL-TEST5", UUID.randomUUID(), "Player5",
                UUID.randomUUID(), "Staff", "Reason", now - 2000L, now - 1000L, "0h 0m 1s", false,
                null, null, null, "127.0.0.1", "corr-5");
        assertEquals("0h 0m 0s (Expired)", expBan.getFormattedRemaining());
    }

    @Test
    @DisplayName("Verify MuteRecord getFormattedRemaining outputs precise format")
    void testMuteRecordRemainingTime() {
        long now = System.currentTimeMillis();
        MuteRecord mute1s = new MuteRecord(UUID.randomUUID(), "Target", "Staff", now + 1000L);
        assertEquals("0h 0m 1s", mute1s.getFormattedRemaining());

        MuteRecord mutePerm = new MuteRecord(UUID.randomUUID(), "Target", "Staff", null);
        assertEquals("Permanent", mutePerm.getFormattedRemaining());
    }
}
