package quest.yu_vitaqua_fer_chronos.wasjvmbridge.utils.generators;

import quest.yu_vitaqua_fer_chronos.wasjvmbridge.ArgType;
import quest.yu_vitaqua_fer_chronos.wasjvmbridge.api.HostApi;
import quest.yu_vitaqua_fer_chronos.wasjvmbridge.api.HostStruct;
import quest.yu_vitaqua_fer_chronos.wasjvmbridge.utils.WasmExport;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

public class CHeaderGenerator {

    public String generate(List<HostApi> apis, List<Class<? extends HostStruct>> structs) {
        StringBuilder sb = new StringBuilder();
        sb.append("#ifndef WASJVM_AUTOGEN_H\n#define WASJVM_AUTOGEN_H\n\n#include <stdint.h>\n\n");
        sb.append("typedef int64_t jvm_handle_t;\n\n");

        sb.append("// --- Data Structures ---\n\n");
        for (Class<? extends HostStruct> struct: structs) {
            String structName = getCStructName(struct);
            sb.append("typedef struct ").append(structName).append(" ").append(structName).append(";\n");
        }
        sb.append("\n");

        for (Class<? extends HostStruct> struct: structs) {
            sb.append("// --- Accessors for: ").append(struct.getSimpleName()).append(" ---\n");
            sb.append(generateStructAccessors(struct));
            sb.append("\n");
        }

        for (HostApi api: apis) {
            sb.append("// --- Module: ").append(api.getNamespace()).append(" ---\n");
            for (Method method: api.getExportedMethods()) {
                sb.append(generateCDecl(api, method));
            }
            sb.append("\n");
        }

        sb.append("#endif\n");
        return sb.toString();
    }

    private String generateStructAccessors(Class<? extends HostStruct> clazz) {
        StringBuilder sb = new StringBuilder();
        // Unity: Use the same name for Module and Prefix
        String moduleName = "wasjvm_" + toSnakeCase(clazz.getSimpleName()) + "_t";
        String structPtr = moduleName + "*";

        for (Field f: clazz.getDeclaredFields()) {
            String cType = mapJavaTypeToC(f.getType());
            String fieldName = toSnakeCase(f.getName());

            // --- Getter ---
            String getterName = "get_" + fieldName;
            sb.append("__attribute__((import_module(\"").append(moduleName).append("\"), ");
            sb.append("import_name(\"").append(getterName).append("\")))\n");
            sb.append("extern ").append(cType).append(" ").append(moduleName).append("_").append(getterName).append("(").append(structPtr).append(" spec);\n\n");

            // --- Setter ---
            String setterName = "set_" + fieldName;
            sb.append("__attribute__((import_module(\"").append(moduleName).append("\"), ");
            sb.append("import_name(\"").append(setterName).append("\")))\n");
            sb.append("extern void ").append(moduleName).append("_").append(setterName).append("(").append(structPtr).append(" spec, ").append(cType).append(" val);\n\n");
        }
        return sb.toString();
    }

    private String generateCDecl(HostApi api, Method m) {
        WasmExport ann = m.getAnnotation(WasmExport.class);
        String moduleName = ann.module().isEmpty() ? api.getNamespace() : ann.module();
        String exportName = ann.name().isEmpty() ? m.getName() : ann.name();
        List<String> paramNames = Arrays.stream(ann.params()).map(this::toSnakeCase).toList();

        StringBuilder sb = new StringBuilder();
        if (!paramNames.isEmpty()) {
            sb.append("/**\n");
            for (String p: paramNames) sb.append(" * @param ").append(p).append("\n");
            sb.append(" */\n");
        }

        sb.append("__attribute__((import_module(\"").append(moduleName).append("\"), ");
        sb.append("import_name(\"").append(exportName).append("\")))\n");

        // FIX: Use the semantic return type (Struct* or Handle) instead of raw ValType
        String retType = mapJavaTypeToC(m.getReturnType());

        sb.append("extern ").append(retType).append(" ").append(api.getNamespace()).append("_").append(exportName).append("(");

        Class<?>[] pTypes = m.getParameterTypes();
        int wasmParamIdx = 0;
        boolean first = true;

        for (Class<?> pType: pTypes) {
            if (pType == com.dylibso.chicory.runtime.Instance.class) continue;

            if (!first) sb.append(", ");
            String pName = (wasmParamIdx < paramNames.size()) ? paramNames.get(wasmParamIdx) : "arg" + wasmParamIdx;

            sb.append(mapJavaTypeToC(pType)).append(" ").append(pName);

            wasmParamIdx++;
            first = false;
        }
        return sb.append(");\n\n").toString();
    }

    private String mapJavaTypeToC(Class<?> type) {
        if (type == void.class || type == Void.class) return "void";

        if (HostStruct.class.isAssignableFrom(type)) {
            return getCStructName(type) + "*";
        }

        ArgType argType = ArgType.determineType(type);
        if (argType == ArgType.HANDLE) return "jvm_handle_t";

        return switch (argType) {
            case I32, BOOLEAN, CHAR, I8, I16 -> "int32_t";
            case I64 -> "int64_t";
            case F32 -> "float";
            case F64 -> "double";
            default -> "int32_t";
        };
    }

    private String getCStructName(Class<?> clazz) {
        return "wasjvm_" + toSnakeCase(clazz.getSimpleName()) + "_t";
    }

    private String getCStructNameNoT(Class<?> clazz) {
        return "wasjvm_" + toSnakeCase(clazz.getSimpleName());
    }


    private String toSnakeCase(String camel) {
        return camel.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
}