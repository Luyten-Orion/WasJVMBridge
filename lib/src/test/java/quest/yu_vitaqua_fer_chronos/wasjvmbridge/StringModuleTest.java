package quest.yu_vitaqua_fer_chronos.wasjvmbridge;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StringModuleTest extends BaseBridgeTest {
    @Test
    @DisplayName("Test WASM String Creation and Retrieval")
    void testStringInterchange() {
        // 1. WASM creates String and returns handle
        long strH = findFunc("create_string").handle().apply(instance, 140L, 14L)[0];
        Object jvmStr = bridge.getObject(strH);
        assertEquals("Result Prefix:", jvmStr);

        // 2. Retrieve into existing WASM buffer
        int bufPtr = 7000;
        long copied = instance.export("test_get_string_into").apply(strH, (long) bufPtr, 50L)[0];
        assertEquals(14, (int) copied);
        assertEquals("Result Prefix:", instance.memory().readString(bufPtr, 14));
    }
}