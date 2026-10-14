package com.moonlight.vanilla.modules.audit;

import com.moonlight.vanilla.modules.audit.commands.LogsCommand;
import com.moonlight.vanilla.modules.audit.serialization.InventorySerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AuditModuleTest {

    @Test
    @DisplayName("Verify CSV Formula Injection Sanitization neutralizes =, +, -, @")
    void testCsvSanitization() {
        assertEquals("\"'=SUM(A1:A10)\"", LogsCommand.sanitizeCsvField("=SUM(A1:A10)"));
        assertEquals("\"'+cmd.exe\"", LogsCommand.sanitizeCsvField("+cmd.exe"));
        assertEquals("\"'-100\"", LogsCommand.sanitizeCsvField("-100"));
        assertEquals("\"'@attack\"", LogsCommand.sanitizeCsvField("@attack"));
        assertEquals("\"NormalText\"", LogsCommand.sanitizeCsvField("NormalText"));
    }


    @Test
    @DisplayName("Verify Serialization handles empty or null item arrays safely")
    void testSerializerNullSafety() {
        InventorySerializer serializer = new InventorySerializer();
        assertEquals("", serializer.serializeItemArray(null));
        assertEquals("", serializer.serializeItem(null));
        assertEquals(0, serializer.deserializeItemArray("").length);
    }
}
