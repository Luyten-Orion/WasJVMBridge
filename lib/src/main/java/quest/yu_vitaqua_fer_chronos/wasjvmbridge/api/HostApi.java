package quest.yu_vitaqua_fer_chronos.wasjvmbridge.api;

import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.ValType;
import quest.yu_vitaqua_fer_chronos.wasjvmbridge.ArgType;
import quest.yu_vitaqua_fer_chronos.wasjvmbridge.WasJVMBridge;
import quest.yu_vitaqua_fer_chronos.wasjvmbridge.utils.WasmExport;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class HostApi {
    protected final WasJVMBridge bridge;
    private final List<Method> exportedMethods = new ArrayList<>();

    protected HostApi(WasJVMBridge bridge) {this.bridge = bridge;}

    public List<HostFunction> export() {
        List<HostFunction> functions = new ArrayList<>();
        exportedMethods.clear();

        for (Method method: this.getClass().getDeclaredMethods()) {
            if (!method.isAnnotationPresent(WasmExport.class)) continue;

            exportedMethods.add(method);
            WasmExport ann = method.getAnnotation(WasmExport.class);

            // Priority: 1. Annotation 'module' -> 2. Api 'getNamespace()'
            String moduleName = ann.module().isEmpty() ? this.getNamespace() : ann.module();
            String exportName = ann.name().isEmpty() ? method.getName() : ann.name();

            functions.add(new HostFunction(moduleName, exportName, inferWasmSignature(method), (inst, args) -> {
                try {
                    Object[] javaArgs = bridge.unpackArgsForMethod(method, args, inst);
                    Object result = method.invoke(this, javaArgs);
                    return bridge.wrapResult(method.getReturnType(), result);
                } catch (Exception e) {
                    return bridge.handleError(e);
                }
            }));
        }
        return functions;
    }

    public abstract String getNamespace();

    private FunctionType inferWasmSignature(Method m) {
        List<ValType> params = new ArrayList<>();
        for (Class<?> pType: m.getParameterTypes()) {
            ValType vt = mapJavaToWasm(pType);
            if (vt != null) params.add(vt);
        }
        List<ValType> results = m.getReturnType() == void.class ? List.of() : List.of(mapJavaToWasm(m.getReturnType()));
        return FunctionType.of(params, results);
    }

    public ValType mapJavaToWasm(Class<?> type) {
        if (type == Instance.class) return null;
        return switch (ArgType.determineType(type)) {
            case I32, BOOLEAN, I8, I16, CHAR -> ValType.I32;
            case I64, HANDLE, F64 -> ValType.I64;
            case F32 -> ValType.F32;
        };
    }

    public List<Method> getExportedMethods() {return Collections.unmodifiableList(exportedMethods);}
}