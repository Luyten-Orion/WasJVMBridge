package quest.yu_vitaqua_fer_chronos.wasjvmbridge;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CollectionModuleTest extends BaseBridgeTest {
    @Test
    @DisplayName("Test Primitive Int Array Serialization")
    void testIntArrayPull() {
        int[] data = {10, 20, 30, 40};
        long handle = bridge.registerObject(data);

        // 1. Get size for allocation
        long encodedSize = findFunc("get_encoded_list_size").handle().apply(instance, handle)[0];
        assertEquals(8 + (4 * 4), (int) encodedSize); // Header(8) + 4*i32(16)

        // 2. Pull elements into WASM
        int bufferPtr = 8000;
        instance.export("malloc").apply(encodedSize); // Ensure heap is advanced
        findFunc("pull_list_elements").handle().apply(instance, handle, (long) bufferPtr);

        // 3. Verify Header and Contiguous Data
        assertEquals(4, instance.memory().readInt(bufferPtr)); // Tag for I32
        assertEquals(4, instance.memory().readInt(bufferPtr + 4)); // Count
        assertEquals(10, instance.memory().readInt(bufferPtr + 8));
        assertEquals(40, instance.memory().readInt(bufferPtr + 20));
    }

    @Test
    @DisplayName("Test Heterogeneous List Fallback (Handles)")
    void testMixedList() {
        List<Object> mixed = List.of("string", 123); // Mixed types trigger HANDLE fallback
        long handle = bridge.registerObject(mixed);

        int bufferPtr = 9000;
        findFunc("pull_list_elements").handle().apply(instance, handle, (long) bufferPtr);

        // Tag 0 is HANDLE (i64)
        assertEquals(0, instance.memory().readInt(bufferPtr));
        assertEquals(2, instance.memory().readInt(bufferPtr + 4));

        long firstHandle = instance.memory().readLong(bufferPtr + 8);
        assertEquals("string", bridge.getObject(firstHandle));
    }
}