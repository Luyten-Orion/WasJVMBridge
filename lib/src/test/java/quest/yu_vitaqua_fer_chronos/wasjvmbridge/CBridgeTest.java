package quest.yu_vitaqua_fer_chronos.wasjvmbridge;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CBridgeTest extends BaseBridgeTest {
    @Override
    protected String getWasmPath() {return "wasm/c_ffi_test.wasm";}

    @Test
    @DisplayName("C: Complete Reflection Flow")
    void testCReflection() {
        bridge.registerClass(TestTarget.class);
        // test_reflection_full_flow -> reflection_get_class, reflection_new_instance, etc.
        long instH = instance.export("test_reflection_full_flow").apply()[0];

        assertTrue(instH > 0);
        TestTarget target = (TestTarget) bridge.getObject(instH);
        assertNull(target.message, "Field should be nulled via reflection_set_field_obj");
    }

    @Test
    @DisplayName("C: String Logic & Memory Ownership")
    void testCStrings() {
        String input = "Peeking into JVM";
        long handle = bridge.registerObject(input);

        // test_string_logic uses strings_get_string_content and strings_create_string
        long resultHandle = instance.export("test_string_logic").apply(handle)[0];
        assertEquals("C-Bridge", bridge.getObject(resultHandle));
    }

    @Test
    @DisplayName("C: Collection Math (Packed i32)")
    void testCCollections() {
        int[] data = {10, 20, 30, 40, 50};
        long handle = bridge.registerObject(data);

        // Uses collections_get_encoded_list_size and collections_pull_list_elements
        long sum = instance.export("test_collection_sum").apply(handle)[0];
        assertEquals(150, (int) sum);
    }

    @Test
    @DisplayName("C: Error Capture & Message Extraction")
    void testCCore() {
        bridge.registerClass(TestTarget.class);
        // test_core_error_trap uses core_flush_error and core_get_last_error_message
        long msgLen = instance.export("test_core_error_trap").apply()[0];
        assertTrue(msgLen > 0);
    }

    @Test
    @DisplayName("C: HostStruct Spec Builder")
    void testCBuilder() {
        bridge.registerClass(TestTarget.class);

        // This would call builder_define_class using the generated WasmClassSpec struct
        // This test requires the C side to populate the struct correctly
        long newClassHandle = instance.export("test_class_definition").apply()[0];
        assertTrue(newClassHandle > 0);
        assertTrue(bridge.classRegistry.containsKey(newClassHandle));
    }
}