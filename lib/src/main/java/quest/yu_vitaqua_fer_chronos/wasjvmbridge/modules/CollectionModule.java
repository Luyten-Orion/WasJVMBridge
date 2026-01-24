package quest.yu_vitaqua_fer_chronos.wasjvmbridge.modules;

import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Memory;
import quest.yu_vitaqua_fer_chronos.wasjvmbridge.ArgType;
import quest.yu_vitaqua_fer_chronos.wasjvmbridge.WasJVMBridge;
import quest.yu_vitaqua_fer_chronos.wasjvmbridge.api.HostApi;
import quest.yu_vitaqua_fer_chronos.wasjvmbridge.utils.WasmExport;

import java.nio.charset.StandardCharsets;
import java.util.Collection;

public class CollectionModule extends HostApi {
    public CollectionModule(WasJVMBridge bridge) {
        super(bridge);
    }

    @Override
    public String getNamespace() {
        return "wasjvmb_collections";
    }

    @WasmExport(params = {"handle"})
    public int get_encoded_list_size(long handle) {
        Object obj = bridge.getObject(handle);
        if (obj == null) return 0;

        int count = getLength(obj);
        ArgType type = determineHomogeneousType(obj);
        int stride = getStride(type);

        // Header (8 bytes: 4 for tag, 4 for count) + (elements * size)
        return 8 + (count * stride);
    }

    private int getLength(Object obj) {
        if (obj instanceof Collection<?> c) return c.size();
        return java.lang.reflect.Array.getLength(obj);
    }

    // --- Internal Marshalling Logic ---

    private ArgType determineHomogeneousType(Object obj) {
        if (obj instanceof int[]) return ArgType.I32;
        if (obj instanceof long[]) return ArgType.I64;
        if (obj instanceof boolean[]) return ArgType.BOOLEAN;
        if (obj instanceof byte[]) return ArgType.I8;
        if (obj instanceof float[]) return ArgType.F32;
        if (obj instanceof double[]) return ArgType.F64;
        if (obj instanceof short[]) return ArgType.I16;
        if (obj instanceof char[]) return ArgType.CHAR;

        Object[] elements = toArray(obj);
        if (elements.length == 0) return ArgType.HANDLE;
        ArgType first = ArgType.determineType(elements[0].getClass());
        for (Object e: elements) {
            if (e == null || ArgType.determineType(e.getClass()) != first) return ArgType.HANDLE;
        }
        return first;
    }

    private int getStride(ArgType type) {
        return switch (type) {
            case I8, BOOLEAN -> 1;
            case I16 -> 2;
            case I32, F32 -> 4;
            default -> 8; // I64, F64, HANDLE, CHAR
        };
    }

    private Object[] toArray(Object obj) {
        if (obj instanceof Object[] arr) return arr;
        if (obj instanceof Collection<?> c) return c.toArray();
        int len = java.lang.reflect.Array.getLength(obj);
        Object[] res = new Object[len];
        for (int i = 0; i < len; i++) res[i] = java.lang.reflect.Array.get(obj, i);
        return res;
    }

    @WasmExport(params = {"handle", "ptr"})
    public int pull_list_elements(Instance inst, long handle, int ptr) {
        Object obj = bridge.getObject(handle);
        if (obj == null) return 0;

        // 1. Determine Type and Length
        int count = getLength(obj);
        ArgType type = determineHomogeneousType(obj);

        // 2. Write Header: [Tag(i32)][Count(i32)]
        Memory mem = inst.memory();
        mem.writeI32(ptr, type.tag);
        mem.writeI32(ptr + 4, count);

        // 3. Dispatch to specific writer based on Type
        int dataStart = ptr + 8;
        return switch (type) {
            case I8, BOOLEAN -> writeI8(mem, dataStart, obj, count);
            case I16 -> writeI16(mem, dataStart, obj, count);
            case I32, F32 -> writeI32(mem, dataStart, obj, count);
            case I64, F64, HANDLE -> writeI64(mem, dataStart, obj, count);
            case CHAR -> writeChars(inst, dataStart, obj, count);
        };
    }

    private int writeI8(Memory mem, int start, Object obj, int count) {
        if (obj instanceof byte[] arr) {
            mem.write(start, arr);
        } else if (obj instanceof boolean[] arr) {
            for (int i = 0; i < count; i++) mem.writeByte(start + i, (byte) (arr[i] ? 1 : 0));
        } else {
            Object[] elements = toArray(obj);
            for (int i = 0; i < count; i++) {
                byte b = (elements[i] instanceof Boolean bool) ? (byte) (bool ? 1 : 0) : (Byte) elements[i];
                mem.writeByte(start + i, b);
            }
        }
        return count;
    }

    // --- Helper Utilities ---

    private int writeI16(Memory mem, int start, Object obj, int count) {
        if (obj instanceof short[] arr) {
            for (int i = 0; i < count; i++) mem.writeShort(start + (i * 2), arr[i]);
        } else {
            Object[] elements = toArray(obj);
            for (int i = 0; i < count; i++) mem.writeShort(start + (i * 2), (Short) elements[i]);
        }
        return count;
    }

    private int writeI32(Memory mem, int start, Object obj, int count) {
        if (obj instanceof int[] arr) {
            for (int i = 0; i < count; i++) mem.writeI32(start + (i * 4), arr[i]);
        } else if (obj instanceof float[] arr) {
            for (int i = 0; i < count; i++) mem.writeF32(start + (i * 4), arr[i]);
        } else {
            Object[] elements = toArray(obj);
            for (int i = 0; i < count; i++) {
                if (elements[i] instanceof Integer val) mem.writeI32(start + (i * 4), val);
                else mem.writeF32(start + (i * 4), (Float) elements[i]);
            }
        }
        return count;
    }

    private int writeI64(Memory mem, int start, Object obj, int count) {
        if (obj instanceof long[] arr) {
            for (int i = 0; i < count; i++) mem.writeLong(start + (i * 8), arr[i]);
        } else if (obj instanceof double[] arr) {
            for (int i = 0; i < count; i++) mem.writeF64(start + (i * 8), arr[i]);
        } else {
            Object[] elements = toArray(obj);
            for (int i = 0; i < count; i++) {
                if (elements[i] instanceof Long val) mem.writeLong(start + (i * 8), val);
                else if (elements[i] instanceof Double val) mem.writeF64(start + (i * 8), val);
                else mem.writeLong(start + (i * 8), bridge.registerObject(elements[i]));
            }
        }
        return count;
    }

    private int writeChars(Instance inst, int start, Object obj, int count) {
        Object[] elements = toArray(obj);
        for (int i = 0; i < count; i++) {
            byte[] bytes = String.valueOf(elements[i]).getBytes(StandardCharsets.UTF_8);
            int ptr = (int) inst.export("malloc").apply(bytes.length)[0];
            inst.memory().write(ptr, bytes);
            inst.memory().writeI32(start + (i * 8), ptr);
            inst.memory().writeI32(start + (i * 8) + 4, bytes.length);
        }
        return count;
    }
}