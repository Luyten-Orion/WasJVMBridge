package quest.yu_vitaqua_fer_chronos.wasjvmbridge.modules;

import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Memory;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.ValType;
import quest.yu_vitaqua_fer_chronos.wasjvmbridge.ArgType;
import quest.yu_vitaqua_fer_chronos.wasjvmbridge.WasJVMBridge;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

public class CollectionModule extends ModuleBase {
    public CollectionModule(WasJVMBridge kernel) {
        super(kernel);
    }

    public List<HostFunction> getFunctions() {
        return List.of(
                /* get_encoded_list_size(list_handle: i64) -> i32
                 * Returns the total bytes required to store the header and contiguous data.
                 * Useful for allocating the destination buffer in WASM via malloc.
                 */
                new HostFunction(WasJVMBridge.NAMESPACE, "get_encoded_list_size", FunctionType.of(List.of(ValType.I64), List.of(ValType.I32)), (inst, args) -> {
                    Object obj = kernel.getObject(args[0]);
                    if (obj == null) return new long[]{0};

                    int count;
                    ArgType type;

                    // Determine count and type based on the same logic as dispatch
                    if (obj instanceof int[]) {
                        count = ((int[]) obj).length;
                        type = ArgType.I32;
                    } else if (obj instanceof long[]) {
                        count = ((long[]) obj).length;
                        type = ArgType.I64;
                    } else if (obj instanceof float[]) {
                        count = ((float[]) obj).length;
                        type = ArgType.F32;
                    } else if (obj instanceof double[]) {
                        count = ((double[]) obj).length;
                        type = ArgType.F64;
                    } else if (obj instanceof boolean[]) {
                        count = ((boolean[]) obj).length;
                        type = ArgType.BOOLEAN;
                    } else if (obj instanceof byte[]) {
                        count = ((byte[]) obj).length;
                        type = ArgType.I8;
                    } else if (obj instanceof short[]) {
                        count = ((short[]) obj).length;
                        type = ArgType.I16;
                    } else if (obj instanceof char[]) {
                        count = ((char[]) obj).length;
                        type = ArgType.CHAR;
                    } else {
                        Object[] elements = (obj instanceof Collection<?> c) ? c.toArray() : (Object[]) obj;
                        count = elements.length;
                        if (count == 0) return new long[]{8}; // Just header space

                        // Use your homogeneity check logic
                        type = determineType(elements[0]);
                        for (Object e : elements) {
                            if (e == null || determineType(e) != type) {
                                type = ArgType.HANDLE;
                                break;
                            }
                        }
                    }

                    int stride = switch (type) {
                        case I8, BOOLEAN -> 1;
                        case I16 -> 2;
                        case I32, F32 -> 4;
                        case I64, F64, CHAR, HANDLE -> 8;
                    };

                    return new long[]{8 + ((long) count * stride)};
                }),

                /* pull_list_elements(list_handle: i64, ptr: i32) -> i32
                 * Serializes the collection into WASM memory at the provided pointer.
                 * Returns the number of elements written.
                 * Layout: [i32 type_tag][i32 count][... contiguous data ...]
                 */
                new HostFunction(WasJVMBridge.NAMESPACE, "pull_list_elements", FunctionType.of(List.of(ValType.I64, ValType.I32), List.of(ValType.I32)), (inst, args) -> {
                    try {
                        Object obj = kernel.getObject(args[0]);
                        int base = (int) args[1];
                        if (obj == null) return new long[]{0};

                        return new long[]{dispatchToInternal(inst, base, obj)};
                    } catch (Exception e) {
                        kernel.handleError(e);
                        return new long[]{-1};
                    }
                }));
    }

    private int dispatchToInternal(Instance inst, int base, Object obj) {
        // Fast Paths: Primitive Arrays
        if (obj instanceof int[] arr) return writeContiguous(inst, base, ArgType.I32, arr);
        if (obj instanceof long[] arr) return writeContiguous(inst, base, ArgType.I64, arr);
        if (obj instanceof float[] arr) return writeContiguous(inst, base, ArgType.F32, arr);
        if (obj instanceof double[] arr) return writeContiguous(inst, base, ArgType.F64, arr);
        if (obj instanceof boolean[] arr) return writeContiguous(inst, base, ArgType.BOOLEAN, arr);
        if (obj instanceof byte[] arr) return writeContiguous(inst, base, ArgType.I8, arr);
        if (obj instanceof short[] arr) return writeContiguous(inst, base, ArgType.I16, arr);
        if (obj instanceof char[] arr) return writeContiguous(inst, base, ArgType.CHAR, arr);

        // Fallback: Boxed Collections/Arrays
        Object[] elements;
        if (obj instanceof Collection<?> c) {
            elements = c.toArray();
        } else if (obj instanceof Object[] a) {
            elements = a;
        } else {
            return 0;
        }

        if (elements.length == 0) return 0;

        // Determine if homogeneous or handle-fallback
        ArgType detectedType = determineType(elements[0]);
        if (detectedType != ArgType.HANDLE) {
            for (Object element : elements) {
                if (element == null || determineType(element) != detectedType) {
                    detectedType = ArgType.HANDLE;
                    break;
                }
            }
        }

        return writeContiguous(inst, base, detectedType, elements);
    }

    // --- Specialized Primitive Overloads ---

    private int writeContiguous(Instance inst, int base, ArgType type, int[] arr) {
        writeHeader(inst, base, type, arr.length);
        for (int i = 0; i < arr.length; i++) inst.memory().writeI32(base + 8 + (i * 4), arr[i]);
        return arr.length;
    }

    private int writeContiguous(Instance inst, int base, ArgType type, long[] arr) {
        writeHeader(inst, base, type, arr.length);
        for (int i = 0; i < arr.length; i++) inst.memory().writeLong(base + 8 + (i * 8), arr[i]);
        return arr.length;
    }

    private int writeContiguous(Instance inst, int base, ArgType type, float[] arr) {
        writeHeader(inst, base, type, arr.length);
        for (int i = 0; i < arr.length; i++) inst.memory().writeF32(base + 8 + (i * 4), arr[i]);
        return arr.length;
    }

    private int writeContiguous(Instance inst, int base, ArgType type, double[] arr) {
        writeHeader(inst, base, type, arr.length);
        for (int i = 0; i < arr.length; i++) inst.memory().writeF64(base + 8 + (i * 8), arr[i]);
        return arr.length;
    }

    private int writeContiguous(Instance inst, int base, ArgType type, boolean[] arr) {
        writeHeader(inst, base, type, arr.length);
        for (int i = 0; i < arr.length; i++) {
            inst.memory().writeByte(base + 8 + i, (byte) (arr[i] ? 1 : 0));
        }
        return arr.length;
    }

    private int writeContiguous(Instance inst, int base, ArgType type, byte[] arr) {
        writeHeader(inst, base, type, arr.length);
        inst.memory().write(base + 8, arr);
        return arr.length;
    }

    private int writeContiguous(Instance inst, int base, ArgType type, short[] arr) {
        writeHeader(inst, base, type, arr.length);
        for (int i = 0; i < arr.length; i++) {
            inst.memory().writeShort(base + 8 + (i * 2), arr[i]);
        }
        return arr.length;
    }

    private int writeContiguous(Instance inst, int base, ArgType type, char[] arr) {
        writeHeader(inst, base, type, arr.length);
        for (int i = 0; i < arr.length; i++) {
            byte[] bytes = String.valueOf(arr[i]).getBytes(StandardCharsets.UTF_8);
            int ptr = (int) inst.export("malloc").apply(bytes.length)[0];
            inst.memory().write(ptr, bytes);
            // char uses 8-byte slots for [ptr, len]
            inst.memory().writeI32(base + 8 + (i * 8), ptr);
            inst.memory().writeI32(base + 8 + (i * 8) + 4, bytes.length);
        }
        return arr.length;
    }

    // --- Boxed/Heterogeneous Overload ---

    private int writeContiguous(Instance inst, int base, ArgType type, Object[] elements) {
        writeHeader(inst, base, type, elements.length);
        Memory mem = inst.memory();
        int dataStart = base + 8;

        for (int i = 0; i < elements.length; i++) {
            Object val = elements[i];
            if (val == null) {
                writeDefaultValue(mem, type, dataStart, i);
                continue;
            }

            switch (type) {
                case HANDLE -> {
                    long handle = kernel.registerObject(val);
                    mem.writeLong(dataStart + (i * 8), handle);
                }
                case I32 -> mem.writeI32(dataStart + (i * 4), (Integer) val);
                case I64 -> mem.writeLong(dataStart + (i * 8), (Long) val);
                case F32 -> mem.writeF32(dataStart + (i * 4), (Float) val);
                case F64 -> mem.writeF64(dataStart + (i * 8), (Double) val);
                case BOOLEAN -> mem.writeByte(dataStart + i, (byte) ((Boolean) val ? 1 : 0));
                case I8 -> mem.writeByte(dataStart + i, (Byte) val);
                case I16 -> mem.writeShort(dataStart + (i * 2), (Short) val);
                case CHAR -> {
                    byte[] bytes = String.valueOf((Character) val).getBytes(StandardCharsets.UTF_8);
                    int ptr = (int) inst.export("malloc").apply(bytes.length)[0];
                    inst.memory().write(ptr, bytes);
                    mem.writeI32(dataStart + (i * 8), ptr);
                    mem.writeI32(dataStart + (i * 8) + 4, bytes.length);
                }
            }
        }
        return elements.length;
    }

    private void writeDefaultValue(Memory mem, ArgType type, int start, int index) {
        switch (type) {
            case HANDLE -> mem.writeLong(start + (index * 8), WasJVMBridge.INVALID_HANDLE);
            case I8, BOOLEAN -> mem.writeByte(start + index, (byte) 0);
            case I16 -> mem.writeShort(start + (index * 2), (short) 0);
            case I32, F32 -> mem.writeI32(start + (index * 4), 0);
            case I64, F64, CHAR -> mem.writeLong(start + (index * 8), 0L);
        }
    }

    private void writeHeader(Instance inst, int base, ArgType type, int count) {
        inst.memory().writeI32(base, type.tag);
        inst.memory().writeI32(base + 4, count);
    }

    private ArgType determineType(Object first) {
        if (first instanceof Integer) return ArgType.I32;
        if (first instanceof Long) return ArgType.I64;
        if (first instanceof Float) return ArgType.F32;
        if (first instanceof Double) return ArgType.F64;
        if (first instanceof Boolean) return ArgType.BOOLEAN;
        if (first instanceof Byte) return ArgType.I8;
        if (first instanceof Short) return ArgType.I16;
        if (first instanceof Character) return ArgType.CHAR;
        return ArgType.HANDLE;
    }
}