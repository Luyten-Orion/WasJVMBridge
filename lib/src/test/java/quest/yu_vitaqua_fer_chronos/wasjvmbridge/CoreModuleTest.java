package quest.yu_vitaqua_fer_chronos.wasjvmbridge;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CoreModuleTest extends BaseBridgeTest {
    @Test
    @DisplayName("Test Error Lifecycle: Flush, Trigger, and Retrieve")
    void testErrorFlow() {
        long classH = instance.export("test_get_class").apply()[0];
        long instH = instance.export("test_instantiate").apply(classH)[0];
        // Resolve 'crash' method
        long mid = findFunc("get_method_id").handle().apply(instance, classH, 100L, 5L, 0L, 0L)[0];

        // test_error_flow flushes, calls crash(), then returns error handle
        long errH = instance.export("test_error_flow").apply(instH, mid)[0];

        assertNotEquals(-1L, errH);
        assertEquals(RuntimeException.class, bridge.classRegistry.get(errH));

        // Test getting error message into WASM memory
        int msgPtr = 6000;
        long written = findFunc("get_last_error_message").handle().apply(instance, (long) msgPtr, 100L)[0];
        assertTrue(written > 0);
        String msg = instance.memory().readString(msgPtr, (int) written);
        assertEquals("test error", msg);
    }

    @Test
    @DisplayName("Test Handle Release")
    void testRelease() {
        long classH = instance.export("test_get_class").apply()[0];
        long instH = instance.export("test_instantiate").apply(classH)[0];
        assertNotNull(bridge.getObject(instH));

        instance.export("test_release").apply(instH);
        assertNull(bridge.getObject(instH));
    }
}