package quest.yu_vitaqua_fer_chronos.wasjvmbridge.modules;

import com.dylibso.chicory.runtime.Instance;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;
import quest.yu_vitaqua_fer_chronos.wasjvmbridge.WasJVMBridge;
import quest.yu_vitaqua_fer_chronos.wasjvmbridge.api.HostApi;
import quest.yu_vitaqua_fer_chronos.wasjvmbridge.modules.specs.WasmClassSpec;
import quest.yu_vitaqua_fer_chronos.wasjvmbridge.modules.specs.WasmMapping;
import quest.yu_vitaqua_fer_chronos.wasjvmbridge.utils.WasmExport;
import quest.yu_vitaqua_fer_chronos.wasjvmbridge.utils.WasmInterceptor;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BuilderModule extends HostApi {
    public static final Map<Class<?>, Map<String, Integer>> vtables = new ConcurrentHashMap<>();

    public BuilderModule(WasJVMBridge bridge) {super(bridge);}

    @Override
    public String getNamespace() {return "wasjvmb_builder";}

    @WasmExport(params = {"spec_ptr"}, name = "define_class")
    public long define_class(Instance inst, int ptr) {
        WasmClassSpec spec = bridge.readStruct(inst, ptr, WasmClassSpec.class);

        // 1. Resolve Super/Interfaces (unchanged)
        Class<?> superClass = bridge.classRegistry.getOrDefault(spec.superHandle, Object.class);
        Class<?>[] interfaces = new Class<?>[spec.interfaceCount];
        for (int i = 0; i < spec.interfaceCount; i++) {
            interfaces[i] = bridge.classRegistry.get(inst.memory().readLong(spec.interfacesPtr + (i * 8)));
        }

        // 2. Resolve VTable Mappings (Fixed pointer math)
        Map<String, Integer> vtable = new HashMap<>();
        for (int i = 0; i < spec.mappingCount; i++) {
            // sizeof(WasmMapping) = 12 (ptr:4, len:4, idx:4)
            int entryPtr = spec.mappingsPtr + (i * 12);
            WasmMapping mapping = bridge.readStruct(inst, entryPtr, WasmMapping.class);

            String methodName = inst.memory().readString(mapping.namePtr, mapping.nameLen);
            vtable.put(methodName, mapping.wasmFuncIdx);
        }

        // 3. ByteBuddy (unchanged)
        Class<?> dynamicClass = new ByteBuddy().subclass(superClass).implement(interfaces).method(ElementMatchers.any()).intercept(MethodDelegation.to(new WasmInterceptor(bridge))).make().load(getClass().getClassLoader()).getLoaded();

        vtables.put(dynamicClass, vtable);
        return bridge.registerClass(dynamicClass);
    }
}