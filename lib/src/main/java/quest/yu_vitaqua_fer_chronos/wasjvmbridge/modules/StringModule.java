package quest.yu_vitaqua_fer_chronos.wasjvmbridge.modules;

import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.ValType;
import quest.yu_vitaqua_fer_chronos.wasjvmbridge.WasJVMBridge;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class StringModule extends ModuleBase {
    public StringModule(WasJVMBridge kernel) {
        super(kernel);
    }

    public List<HostFunction> getFunctions() {
        return List.of(
                /* create_string(ptr: i32, len: i32) -> i64
                 * Reads a UTF-8 string from WASM memory and registers it as a Java String.
                 * Returns the new object handle.
                 */
                new HostFunction(WasJVMBridge.NAMESPACE, "create_string", FunctionType.of(List.of(ValType.I32, ValType.I32), List.of(ValType.I64)), (inst, args) -> {
                    String s = inst.memory().readString((int) args[0], (int) args[1]);
                    long id = kernel.handleCounter.getAndIncrement();
                    kernel.globalInstanceRegistry.put(id, s);
                    return new long[]{id};
                }),

                new HostFunction(WasJVMBridge.NAMESPACE, "get_string_content", FunctionType.of(List.of(ValType.I64, ValType.I32), List.of(ValType.I32)), (inst, args) -> {
                    String s = (String) kernel.getObject(args[0]);
                    byte[] b = s.getBytes(StandardCharsets.UTF_8);
                    int ptr = (int) inst.export("malloc").apply(b.length)[0];
                    inst.memory().write(ptr, b);
                    // Write the length into the provided pointer
                    inst.memory().writeI32((int) args[1], b.length);
                    return new long[]{ptr};
                }),

                /* get_string_into(handle: i64, ptr: i32, max_len: i32) -> i32
                 * Copies a Java String's UTF-8 bytes into a pre-allocated WASM buffer.
                 * Returns the number of bytes actually written.
                 */
                new HostFunction(WasJVMBridge.NAMESPACE, "get_string_into", FunctionType.of(List.of(ValType.I64, ValType.I32, ValType.I32), List.of(ValType.I32)), (inst, args) -> {
                    String s = (String) kernel.globalInstanceRegistry.get(args[0]);
                    byte[] b = s.getBytes(StandardCharsets.UTF_8);
                    int len = Math.min(b.length, (int) args[2]);
                    inst.memory().write((int) args[1], b, 0, len);
                    return new long[]{len};
                }));
    }
}