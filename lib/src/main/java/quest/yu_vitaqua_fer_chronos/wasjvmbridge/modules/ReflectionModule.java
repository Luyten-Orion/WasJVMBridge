package quest.yu_vitaqua_fer_chronos.wasjvmbridge.modules;

import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.ValType;
import quest.yu_vitaqua_fer_chronos.wasjvmbridge.WasJVMBridge;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public class ReflectionModule extends ModuleBase {
    public ReflectionModule(WasJVMBridge kernel) {
        super(kernel);
    }

    public List<HostFunction> getFunctions() {
        return List.of(
                /* get_class(name_ptr: i32, name_len: i32) -> i64 */
                new HostFunction(WasJVMBridge.NAMESPACE, "get_class", FunctionType.of(List.of(ValType.I32, ValType.I32), List.of(ValType.I64)), (inst, args) -> {
                    try {
                        String name = inst.memory().readString((int) args[0], (int) args[1]);
                        Class<?> clazz = switch (name) {
                            case "int" -> int.class;
                            case "boolean" -> boolean.class;
                            case "long" -> long.class;
                            case "void" -> void.class;
                            default -> Class.forName(name);
                        };
                        return new long[]{kernel.registerClass(clazz)};
                    } catch (Exception e) {
                        return kernel.handleError(e);
                    }
                }),

                /* get_constructor_id(class_handle: i64, params_ptr: i32, count: i32) -> i64
                 */
                new HostFunction(WasJVMBridge.NAMESPACE, "get_constructor_id", FunctionType.of(List.of(ValType.I64, ValType.I32, ValType.I32), List.of(ValType.I64)), (inst, args) -> {
                    try {
                        Class<?> clazz = kernel.classRegistry.get(args[0]);
                        int count = (int) args[2];
                        Class<?>[] params = new Class<?>[count];
                        for (int i = 0; i < count; i++) {
                            params[i] = kernel.classRegistry.get(inst.memory().readLong((int) args[1] + (i * 8)));
                        }
                        var ctor = clazz.getDeclaredConstructor(params);
                        ctor.setAccessible(true);
                        long id = kernel.handleCounter.getAndIncrement();
                        kernel.constructorRegistry.put(id, ctor);
                        return new long[]{id};
                    } catch (Exception e) {
                        return kernel.handleError(e);
                    }
                }),

                /* new_instance(ctor_handle: i64, args_ptr: i32, count: i32) -> (i64)
                 * Returns [ClassID, InstanceID]
                 */
                new HostFunction(WasJVMBridge.NAMESPACE, "new_instance", FunctionType.of(List.of(ValType.I64, ValType.I32, ValType.I32), List.of(ValType.I64)), (inst, args) -> {
                    try {
                        Object res = kernel.invokeConstructorInternal(args[0], (int) args[1], (int) args[2], inst);
                        long id = kernel.handleCounter.getAndIncrement();
                        kernel.globalInstanceRegistry.put(id, res);
                        return new long[]{id};
                    } catch (Exception e) {
                        return kernel.handleError(e);
                    }
                }),

                /* get_method_id(class_handle, name_ptr, name_len, params_ptr, params_count) -> i64 */
                new HostFunction(WasJVMBridge.NAMESPACE, "get_method_id", FunctionType.of(List.of(ValType.I64, ValType.I32, ValType.I32, ValType.I32, ValType.I32), List.of(ValType.I64)), (inst, args) -> {
                    try {
                        Class<?> clazz = kernel.classRegistry.get(args[0]);
                        String name = inst.memory().readString((int) args[1], (int) args[2]);
                        int count = (int) args[4];
                        Class<?>[] params = new Class<?>[count];
                        for (int i = 0; i < count; i++) {
                            params[i] = kernel.classRegistry.get(inst.memory().readLong((int) args[3] + (i * 8)));
                        }
                        Method m = clazz.getMethod(name, params);
                        long id = kernel.handleCounter.getAndIncrement();
                        kernel.methodRegistry.put(id, m);
                        return new long[]{id};
                    } catch (Exception e) {
                        return kernel.handleError(e);
                    }
                }),

                /* call_method_void(obj_handle, method_id, args_ptr, args_count) -> void */
                new HostFunction(WasJVMBridge.NAMESPACE, "call_method_void", FunctionType.of(List.of(ValType.I64, ValType.I64, ValType.I32, ValType.I32), List.of()), (inst, args) -> {
                    try {
                        kernel.invokeInternal(args[0], args[1], (int) args[2], (int) args[3], inst);
                        return null;
                    } catch (Exception e) {
                        kernel.handleError(e);
                        return null;
                    }
                }),

                /* call_method_obj(obj_handle, method_id, args_ptr, args_count) -> (i64) */
                new HostFunction(WasJVMBridge.NAMESPACE, "call_method_obj", FunctionType.of(List.of(ValType.I64, ValType.I64, ValType.I32, ValType.I32), List.of(ValType.I64)), (inst, args) -> {
                    try {
                        Object res = kernel.invokeInternal(args[0], args[1], (int) args[2], (int) args[3], inst);
                        if (res == null) return new long[]{0L};
                        long id = kernel.handleCounter.getAndIncrement();
                        kernel.globalInstanceRegistry.put(id, res);
                        return new long[]{id};
                    } catch (Exception e) {
                        return kernel.handleError(e);
                    }
                }),

                /* get_field_id(class_handle, name_ptr, name_len) -> i64 */
                new HostFunction(WasJVMBridge.NAMESPACE, "get_field_id", FunctionType.of(List.of(ValType.I64, ValType.I32, ValType.I32), List.of(ValType.I64)), (inst, args) -> {
                    try {
                        Class<?> clazz = kernel.classRegistry.get(args[0]);
                        String name = inst.memory().readString((int) args[1], (int) args[2]);
                        Field f = clazz.getField(name);
                        long id = kernel.handleCounter.getAndIncrement();
                        kernel.fieldRegistry.put(id, f);
                        return new long[]{id};
                    } catch (Exception e) {
                        return kernel.handleError(e);
                    }
                }),

                /* get_field_obj(obj_handle, field_id) -> i64 */
                new HostFunction(WasJVMBridge.NAMESPACE, "get_field_obj", FunctionType.of(List.of(ValType.I64, ValType.I64), List.of(ValType.I64)), (inst, args) -> {
                    try {
                        Object target = kernel.globalInstanceRegistry.get(args[0]);
                        Field f = kernel.fieldRegistry.get(args[1]);
                        kernel.validateMember(f.getDeclaringClass(), target, f.getName());
                        Object val = f.get(target);
                        long id = kernel.handleCounter.getAndIncrement();
                        kernel.globalInstanceRegistry.put(id, val);
                        return new long[]{id};
                    } catch (Exception e) {
                        return kernel.handleError(e);
                    }
                }),

                /* set_field_obj(obj_handle, field_id, value_handle) -> void */
                new HostFunction(WasJVMBridge.NAMESPACE, "set_field_obj", FunctionType.of(List.of(ValType.I64, ValType.I64, ValType.I64), List.of()), (inst, args) -> {
                    try {
                        Object target = kernel.globalInstanceRegistry.get(args[0]);
                        Field f = kernel.fieldRegistry.get(args[1]);
                        kernel.validateMember(f.getDeclaringClass(), target, f.getName());
                        Object value = (args[2] == 0) ? null : kernel.globalInstanceRegistry.get(args[2]);
                        f.set(target, value);
                        return null;
                    } catch (Exception e) {
                        kernel.handleError(e);
                        return null;
                    }
                }),

                /* release_instance(obj_handle: i64) -> void */
                new HostFunction(WasJVMBridge.NAMESPACE, "release_instance", FunctionType.of(List.of(ValType.I64), List.of()), (inst, args) -> {
                    kernel.globalInstanceRegistry.remove(args[0]);
                    return null;
                }));
    }
}