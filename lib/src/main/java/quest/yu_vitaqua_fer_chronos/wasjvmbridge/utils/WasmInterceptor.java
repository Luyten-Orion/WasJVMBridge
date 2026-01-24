package quest.yu_vitaqua_fer_chronos.wasjvmbridge.utils;

import com.dylibso.chicory.runtime.Instance;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;
import quest.yu_vitaqua_fer_chronos.wasjvmbridge.WasJVMBridge;
import quest.yu_vitaqua_fer_chronos.wasjvmbridge.modules.BuilderModule;

import java.lang.reflect.Method;
import java.util.Map;

public class WasmInterceptor {
    private final WasJVMBridge kernel;

    public WasmInterceptor(WasJVMBridge kernel) {
        this.kernel = kernel;
    }

    @RuntimeType
    public Object intercept(@This Object self, @Origin Method method, @AllArguments Object[] args) throws Exception {
        // Find the vtable for this specific generated class
        Map<String, Integer> vtable = BuilderModule.vtables.get(self.getClass());
        if (vtable == null) return null;

        Integer wasmFuncIdx = vtable.get(method.getName());
        if (wasmFuncIdx == null) return null;

        // Borrow a WASM thread from the pool
        Instance inst = kernel.getInstancePool().borrow();
        try {
            // Memory layout: [self_handle] [arg1] [arg2] ...
            int totalArgs = args.length + 1;
            int argsPtr = (int) inst.export("malloc").apply(totalArgs * 16L)[0];

            // Pack the 'this' pointer (self) and all arguments
            kernel.packOneArg(inst, argsPtr, self);
            for (int i = 0; i < args.length; i++) {
                kernel.packOneArg(inst, argsPtr + ((i + 1) * 16), args[i]);
            }

            // Call the C-side dispatcher exported by your WASM binary
            // Signature: wasm_vtable_dispatcher(int func_idx, int args_ptr, int count) -> long
            long result = inst.export("wasm_vtable_dispatcher").apply(wasmFuncIdx, argsPtr, totalArgs)[0];

            return kernel.unpackResult(method.getReturnType(), result);
        } finally {
            kernel.getInstancePool().release(inst);
        }
    }
}