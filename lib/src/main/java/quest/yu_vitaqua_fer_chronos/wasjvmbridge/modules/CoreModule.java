package quest.yu_vitaqua_fer_chronos.wasjvmbridge.modules;

import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.ValType;
import quest.yu_vitaqua_fer_chronos.wasjvmbridge.WasJVMBridge;

import java.util.List;

public class CoreModule extends ModuleBase {
    public CoreModule(WasJVMBridge kernel) {
        super(kernel);
    }

    public List<HostFunction> getFunctions() {
        return List.of(
                new HostFunction(
                        WasJVMBridge.NAMESPACE, "flush_error", FunctionType.empty(), (inst, args) -> {
                    kernel.lastError.remove();
                    return null;
                }
                ), new HostFunction(
                        WasJVMBridge.NAMESPACE, "get_last_error_handle", FunctionType.returning(ValType.I64),
                        (inst, args) -> new long[]{kernel.lastError.get().handle()}
                ), new HostFunction(
                        WasJVMBridge.NAMESPACE, "get_last_error_message",
                        FunctionType.of(List.of(ValType.I32, ValType.I32), List.of(ValType.I32)), (inst, args) -> {
                    byte[] msg = kernel.lastError.get().message().getBytes();
                    int length = Math.min(msg.length, (int) args[1]);
                    inst.memory().write((int) args[0], msg, 0, length);
                    return new long[]{length};
                }
                )
        );
    }
}