package quest.yu_vitaqua_fer_chronos.wasjvmbridge;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class WasJVMBridgeCTest extends BaseBridgeTest {
    @Override
    protected String getWasmPath() {
        return "wasm/c_ffi_test.wasm";
    }

    @Test
    @DisplayName("C: Test Reflection")
    void testReflectionFullFlow() {
        // Resolve the TestTarget class in the bridge so the C side can find it
        bridge.registerClass(TestTarget.class);

        // Call: test_reflection_full_flow()
        // This tests: get_class, get_constructor_id, new_instance (multivalue),
        // get_field_id, and set_field_obj.
        long instHandle = instance.export("test_reflection_full_flow").apply()[0];

        assertTrue(instHandle > 0, "C should return a valid instance handle");

        Object obj = bridge.getObject(instHandle);
        assertInstanceOf(TestTarget.class, obj);

        TestTarget target = (TestTarget) obj;
        assertNull(target.message, "C logic should have set 'message' field to null");
    }

    @Test
    @DisplayName("C: Test String Logic & Malloc Interop")
    void testStringLogic() {
        String input = "Peeking into JVM";
        long handle = bridge.registerObject(input);

        // Call: test_string_logic(handle)
        // This tests: get_string_content (multivalue [ptr, len]),
        // C-side pointer access, and create_string.
        long resultHandle = instance.export("test_string_logic").apply(handle)[0];

        assertTrue(resultHandle > 0);
        Object output = bridge.getObject(resultHandle);
        assertEquals("C-Bridge", output, "C should have created a new string 'C-Bridge'");
    }

    @Test
    @DisplayName("C: Test Collection Sum (I32 Packed)")
    void testCollectionSum() {
        int[] data = {10, 20, 30, 40, 50};
        long handle = bridge.registerObject(data);

        // Call: test_collection_sum(handle)
        // This tests: get_encoded_list_size, pull_list_elements,
        // and C-side pointer arithmetic on packed I32s.
        long sum = instance.export("test_collection_sum").apply(handle)[0];

        assertEquals(150, (int) sum, "C logic should sum the array correctly");
    }

    @Test
    @DisplayName("C: Test Collection Boolean Check (Packed I8)")
    void testCollectionBool() {
        boolean[] allTrue = {true, true, true};
        boolean[] containsFalse = {true, false, true};

        long h1 = bridge.registerObject(allTrue);
        long h2 = bridge.registerObject(containsFalse);

        long res1 = instance.export("test_collection_bool_check").apply(h1)[0];
        long res2 = instance.export("test_collection_bool_check").apply(h2)[0];

        assertEquals(1, (int) res1, "Should return 1 for all true");
        assertEquals(0, (int) res2, "Should return 0 if a false is present");
    }

    @Test
    @DisplayName("C: Test Core Error Trap")
    void testErrorTrap() {
        // Register the class so C can find it
        bridge.registerClass(TestTarget.class);

        // No arguments needed now, C handles the resolution
        long msgLen = instance.export("test_core_error_trap").apply()[0];

        assertTrue(msgLen > 0, "Should have captured an error message length");
    }
}