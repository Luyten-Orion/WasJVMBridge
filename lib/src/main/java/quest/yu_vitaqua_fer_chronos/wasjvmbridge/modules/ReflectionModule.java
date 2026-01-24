package quest.yu_vitaqua_fer_chronos.wasjvmbridge.modules;

import com.dylibso.chicory.runtime.Instance;
import quest.yu_vitaqua_fer_chronos.wasjvmbridge.WasJVMBridge;
import quest.yu_vitaqua_fer_chronos.wasjvmbridge.api.HostApi;
import quest.yu_vitaqua_fer_chronos.wasjvmbridge.utils.WasmExport;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class ReflectionModule extends HostApi {
    public ReflectionModule(WasJVMBridge bridge) {
        super(bridge);
    }

    @Override
    public String getNamespace() {
        return "wasjvmb_reflection";
    }

    @WasmExport(params = {"name_ptr", "name_len"})
    public long get_class(Instance inst, int ptr, int len) throws Exception {
        String name = inst.memory().readString(ptr, len);
        Class<?> clazz = switch (name) {
            case "int" -> int.class;
            case "boolean" -> boolean.class;
            case "long" -> long.class;
            case "void" -> void.class;
            default -> Class.forName(name);
        };
        return bridge.registerClass(clazz);
    }

    @WasmExport(params = {"class_h", "params_ptr", "count"})
    public long get_constructor_id(Instance inst, long classH, int ptr, int count) throws Exception {
        Class<?> clazz = bridge.classRegistry.get(classH);
        Class<?>[] params = new Class<?>[count];
        for (int i = 0; i < count; i++) params[i] = bridge.classRegistry.get(inst.memory().readLong(ptr + (i * 8)));
        Constructor<?> ctor = clazz.getDeclaredConstructor(params);
        ctor.setAccessible(true);
        long id = bridge.handleCounter.getAndIncrement();
        // Since we removed specific registries, using global registry for IDs is cleaner
        return bridge.registerObject(ctor);
    }

    @WasmExport(params = {"class_h", "name_ptr", "name_len", "params_ptr", "count"})
    public long get_method_id(Instance inst, long classH, int namePtr, int nameLen, int paramsPtr, int count) throws Exception {
        Class<?> clazz = bridge.classRegistry.get(classH);
        String name = inst.memory().readString(namePtr, nameLen);
        Class<?>[] params = new Class<?>[count];
        for (int i = 0; i < count; i++) {
            params[i] = bridge.classRegistry.get(inst.memory().readLong(paramsPtr + (i * 8)));
        }
        return bridge.registerObject(clazz.getMethod(name, params));
    }

    @WasmExport(params = {"obj_h", "method_h", "args_ptr", "count"})
    public long call_method_obj(Instance inst, long objH, long methodH, int argsPtr, int count) throws Exception {
        Method m = (Method) bridge.getObject(methodH);
        Object target = (objH == 0) ? null : bridge.getObject(objH);
        Object[] params = bridge.unpackArgsFromMemory(argsPtr, count, inst);
        return bridge.registerObject(m.invoke(target, params));
    }

    @WasmExport(params = {"obj_h", "method_h", "args_ptr", "count"})
    public void call_method_void(Instance inst, long objH, long methodH, int argsPtr, int count) throws Exception {
        Method m = (Method) bridge.getObject(methodH);
        Object target = (objH == 0) ? null : bridge.getObject(objH);
        m.invoke(target, bridge.unpackArgsFromMemory(argsPtr, count, inst));
    }

    @WasmExport(params = {"ctor_h", "args_ptr", "count"})
    public long new_instance(Instance inst, long ctorH, int argsPtr, int count) throws Exception {
        Object obj = bridge.getObject(ctorH);
        if (!(obj instanceof Constructor<?> ctor)) {
            throw new IllegalArgumentException("Handle is not a constructor");
        }

        Object[] params = bridge.unpackArgsFromMemory(argsPtr, count, inst);
        return bridge.registerObject(ctor.newInstance(params));
    }

    @WasmExport(params = {"class_h", "name_ptr", "name_len"})
    public long get_field_id(Instance inst, long classH, int ptr, int len) throws Exception {
        Class<?> clazz = bridge.classRegistry.get(classH);
        String name = inst.memory().readString(ptr, len);
        java.lang.reflect.Field field = clazz.getDeclaredField(name);
        field.setAccessible(true);
        return bridge.registerObject(field);
    }

    @WasmExport(params = {"obj_h", "field_h", "value_h"})
    public void set_field_obj(Instance inst, long objH, long fieldH, long valueH) throws Exception {
        java.lang.reflect.Field field = (java.lang.reflect.Field) bridge.getObject(fieldH);
        Object target = bridge.getObject(objH);
        Object value = (valueH == 0) ? null : bridge.getObject(valueH);
        field.set(target, value);
    }
}