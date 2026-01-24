package quest.yu_vitaqua_fer_chronos.wasjvmbridge.modules;

import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.ValType;
import quest.yu_vitaqua_fer_chronos.wasjvmbridge.ArgType;
import quest.yu_vitaqua_fer_chronos.wasjvmbridge.WasJVMBridge;
import quest.yu_vitaqua_fer_chronos.wasjvmbridge.api.HostApi;
import quest.yu_vitaqua_fer_chronos.wasjvmbridge.api.HostStruct;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class StructAccessorModule extends HostApi {
    private final List<Class<? extends HostStruct>> registeredStructs;

    public StructAccessorModule(WasJVMBridge bridge, List<Class<? extends HostStruct>> structs) {
        super(bridge);
        this.registeredStructs = structs;
    }

    @Override
    public List<HostFunction> export() {
        List<HostFunction> functions = new ArrayList<>();
        for (Class<? extends HostStruct> clazz: registeredStructs) {
            // EXACT MATCH: wasjvm_struct_name_t
            String moduleName = "wasjvm_" + toSnakeCase(clazz.getSimpleName()) + "_t";

            for (Field field: clazz.getDeclaredFields()) {
                field.setAccessible(true);
                String fieldName = toSnakeCase(field.getName());
                ValType vType = mapJavaToWasm(field.getType());

                // Registration
                functions.add(new HostFunction(moduleName, "get_" + fieldName, FunctionType.of(List.of(ValType.I32), List.of(vType)), (inst, args) -> {
                    try {
                        Object struct = bridge.readStruct(inst, (int) args[0], clazz);
                        return bridge.wrapResult(field.getType(), field.get(struct));
                    } catch (Exception e) {return bridge.handleError(e);}
                }));

                functions.add(new HostFunction(moduleName, "set_" + fieldName, FunctionType.of(List.of(ValType.I32, vType), List.of()), (inst, args) -> {
                    try {
                        int ptr = (int) args[0];
                        int offset = calculateFieldOffset(clazz, field);
                        Object val = bridge.unpackArgsForMethod(null, new long[]{args[1]}, inst)[0];
                        bridge.writeField(inst, ptr, offset, field, val);
                    } catch (Exception e) {bridge.handleError(e);}
                    return new long[0];
                }));
            }
        }
        return functions;
    }

    @Override
    public String getNamespace() {return "wasjvm_internal_accessors";}

    private String toSnakeCase(String camel) {
        return camel.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    private int calculateFieldOffset(Class<?> clazz, Field targetField) {
        int offset = 0;
        for (Field f: clazz.getDeclaredFields()) {
            ArgType type = ArgType.determineType(f.getType());
            if (type == ArgType.I64 || type == ArgType.F64 || type == ArgType.HANDLE) {
                if (offset % 8 != 0) offset += (8 - (offset % 8));
            }
            if (f.equals(targetField)) return offset;

            offset += switch (type) {
                case BOOLEAN -> 1;
                case I8 -> 1;
                case I16 -> 2;
                case I32, F32 -> 4;
                default -> 8;
            };
        }
        return offset;
    }
}