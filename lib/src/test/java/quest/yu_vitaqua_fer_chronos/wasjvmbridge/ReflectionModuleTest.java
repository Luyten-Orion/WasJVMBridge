package quest.yu_vitaqua_fer_chronos.wasjvmbridge;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ReflectionModuleTest extends BaseBridgeTest {
    @Test
    @DisplayName("Test Multi-Parameter Tagged Union Call")
    void testMultiParam() {
        long classH = instance.export("test_get_class").apply()[0];
        long instH = instance.export("test_instantiate").apply(classH)[0];

        // Setup method resolution for: multiParam(String, int, boolean)
        long strClass = findFunc("get_class").handle().apply(instance, 160L, 16L)[0];
        instance.memory().writeString(3000, "int");
        long intClass = findFunc("get_class").handle().apply(instance, 3000L, 3L)[0];
        instance.memory().writeString(3010, "boolean");
        long boolClass = findFunc("get_class").handle().apply(instance, 3010L, 7L)[0];

        instance.memory().writeLong(4000, strClass);
        instance.memory().writeLong(4008, intClass);
        instance.memory().writeLong(4016, boolClass);

        long methodId = findFunc("get_method_id").handle().apply(instance, classH, 120L, 10L, 4000L, 3L)[0];

        // Execute tagged union call from WASM
        instance.export("test_multi_call").apply(instH, methodId);

        TestTarget target = (TestTarget) bridge.getObject(instH);
        assertEquals("Result Prefix: count: 777 flag: true", target.result.trim());
    }

    @Test
    @DisplayName("Test Field Getter and Setter")
    void testFields() {
        long classH = instance.export("test_get_class").apply()[0];
        long instH = instance.export("test_instantiate").apply(classH)[0];
        long fieldId = findFunc("get_field_id").handle().apply(instance, classH, 80L, 7L)[0];

        // Set field to null from WASM
        instance.export("test_set_field").apply(instH, fieldId, 0L);
        assertNull(((TestTarget) bridge.getObject(instH)).message);
    }
}