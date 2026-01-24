package quest.yu_vitaqua_fer_chronos.wasjvmbridge;

import com.dylibso.chicory.runtime.*;
import quest.yu_vitaqua_fer_chronos.wasjvmbridge.api.HostApi;
import quest.yu_vitaqua_fer_chronos.wasjvmbridge.api.HostStruct;
import quest.yu_vitaqua_fer_chronos.wasjvmbridge.utils.WasmInstancePool;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class WasJVMBridge {
    public static final long INVALID_HANDLE = -1L;
    public static final String NAMESPACE = "wasjvmbridge";

    public final Map<Long, Class<?>> classRegistry = new ConcurrentHashMap<>();
    public final Map<Long, Object> globalInstanceRegistry = new ConcurrentHashMap<>();
    public final AtomicLong handleCounter = new AtomicLong(1);
    public final ThreadLocal<ErrorContext> lastError = ThreadLocal.withInitial(() -> new ErrorContext("", INVALID_HANDLE));

    private final List<HostApi> apis = new ArrayList<>();
    private final List<HostFunction> hostFunctions = new ArrayList<>();
    private WasmInstancePool instancePool;

    public void addApi(HostApi api) {
        this.apis.add(api);
        this.hostFunctions.addAll(api.export());
    }

    public void initialisePool(Memory sharedMemory, byte[] wasmBinary) {
        Store store = new Store();
        store.addMemory(new ImportMemory("env", "memory", sharedMemory));
        for (var f: hostFunctions) store.addFunction(f);
        this.instancePool = new WasmInstancePool(store, wasmBinary);
    }

    /**
     * Writes an entire Java HostStruct back into WASM memory.
     */
    public void writeStruct(Instance inst, int ptr, HostStruct struct) {
        int offset = 0;
        for (Field f: struct.getClass().getDeclaredFields()) {
            f.setAccessible(true);
            try {
                offset = writeField(inst, ptr, offset, f, f.get(struct));
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to write struct field: " + f.getName(), e);
            }
        }
    }

    /**
     * Writes a single value to WASM memory based on Java type, handling alignment.
     * Returns the new offset after writing.
     */
    public int writeField(Instance inst, int ptr, int offset, Field field, Object value) {
        Memory mem = inst.memory();
        Class<?> type = field.getType();
        ArgType argType = ArgType.determineType(type);

        // Handle 8-byte alignment for 64-bit types
        if (argType == ArgType.I64 || argType == ArgType.F64 || argType == ArgType.HANDLE) {
            if (offset % 8 != 0) offset += (8 - (offset % 8));
        }

        int absolutePtr = ptr + offset;

        switch (argType) {
            case I32, CHAR, I8, I16 -> {
                mem.writeI32(absolutePtr, ((Number) value).intValue());
                offset += 4;
            }
            case BOOLEAN -> {
                mem.writeByte(absolutePtr, (byte) ((Boolean) value ? 1 : 0));
                offset += 1; // Structs usually pack booleans as bytes
            }
            case I64 -> {
                mem.writeLong(absolutePtr, (Long) value);
                offset += 8;
            }
            case HANDLE -> {
                mem.writeLong(absolutePtr, registerObject(value));
                offset += 8;
            }
            case F32 -> {
                mem.writeF32(absolutePtr, (Float) value);
                offset += 4;
            }
            case F64 -> {
                mem.writeF64(absolutePtr, (Double) value);
                offset += 8;
            }
        }
        return offset;
    }

    public long registerObject(Object obj) {
        if (obj == null) return 0;
        long id = handleCounter.getAndIncrement();
        globalInstanceRegistry.put(id, obj);
        return id;
    }

    /**
     * Reads a HostStruct directly from WASM memory.
     * Handles padding and alignment for 64-bit types automatically.
     */
    public <T extends HostStruct> T readStruct(Instance inst, int ptr, Class<T> clazz) {
        try {
            T struct = clazz.getDeclaredConstructor().newInstance();
            Memory mem = inst.memory();
            int offset = 0;

            for (java.lang.reflect.Field f: clazz.getDeclaredFields()) {
                f.setAccessible(true);
                Class<?> type = f.getType();

                if (type == int.class) {
                    f.setInt(struct, mem.readInt(ptr + offset));
                    offset += 4;
                } else if (type == long.class) {
                    // WASM/C usually align i64 to 8-byte boundaries
                    if (offset % 8 != 0) offset += (8 - (offset % 8));
                    f.setLong(struct, mem.readLong(ptr + offset));
                    offset += 8;
                } else if (type == float.class) {
                    f.setFloat(struct, mem.readFloat(ptr + offset));
                    offset += 4;
                } else if (type == double.class) {
                    if (offset % 8 != 0) offset += (8 - (offset % 8));
                    f.setDouble(struct, mem.readDouble(ptr + offset));
                    offset += 8;
                }
            }
            return struct;
        } catch (Exception e) {
            throw new RuntimeException("Memory mapping error for struct: " + clazz.getName(), e);
        }
    }

    public Object unpackResult(Class<?> returnType, long wasmResult) {
        if (returnType == void.class || returnType == Void.class) return null;

        return switch (ArgType.determineType(returnType)) {
            case BOOLEAN -> wasmResult != 0;
            case HANDLE -> (wasmResult == 0) ? null : globalInstanceRegistry.get(wasmResult);
            case F32 -> Float.intBitsToFloat((int) wasmResult);
            case F64 -> Double.longBitsToDouble(wasmResult);
            case I32 -> (int) wasmResult;
            case I8 -> (byte) wasmResult;
            case I16 -> (short) wasmResult;
            case CHAR -> (char) wasmResult;
            case I64 -> wasmResult;
        };
    }

    public Object unpackOneValue(Class<?> type, long rawVal, Instance inst) {
        return switch (ArgType.determineType(type)) {
            case I32 -> (int) rawVal;
            case I64 -> rawVal;
            case F32 -> Float.intBitsToFloat((int) rawVal);
            case F64 -> Double.longBitsToDouble(rawVal);
            case BOOLEAN -> rawVal != 0;
            case I8 -> (byte) rawVal;
            case I16 -> (short) rawVal;
            case CHAR -> (char) rawVal;
            case HANDLE -> (rawVal == 0) ? null : globalInstanceRegistry.get(rawVal);
        };
    }

    public void packOneArg(Instance inst, int ptr, Object val) {
        ArgType type = ArgType.determineType(val != null ? val.getClass() : Object.class);
        inst.memory().writeI32(ptr, type.tag);

        long data = val != null ? switch (type) {
            case HANDLE -> registerObject(val);
            case I32 -> ((Integer) val).longValue();
            case I64 -> (Long) val;
            case F32 -> (long) Float.floatToRawIntBits((Float) val);
            case F64 -> Double.doubleToRawLongBits((Double) val);
            case BOOLEAN -> (Boolean) val ? 1L : 0L;
            case I8 -> ((Byte) val).longValue();
            case I16 -> ((Short) val).longValue();
            case CHAR -> (long) ((Character) val);
        } : 0L;
        inst.memory().writeLong(ptr + 8, data);
    }

    public Object[] unpackArgsForMethod(Method method, long[] args, Instance inst) {
        Class<?>[] paramTypes = method.getParameterTypes();
        Object[] javaArgs = new Object[paramTypes.length];
        int wasmIdx = 0;

        for (int i = 0; i < paramTypes.length; i++) {
            Class<?> pType = paramTypes[i];
            if (pType == Instance.class) {
                javaArgs[i] = inst;
                continue;
            }

            long val = args[wasmIdx++];
            javaArgs[i] = switch (ArgType.determineType(pType)) {
                case I32 -> (int) val;
                case I64 -> val;
                case F32 -> Float.intBitsToFloat((int) val);
                case F64 -> Double.longBitsToDouble(val);
                case BOOLEAN -> val != 0;
                case I8 -> (byte) val;
                case I16 -> (short) val;
                case CHAR -> (char) val;
                case HANDLE -> (val == 0) ? null : globalInstanceRegistry.get(val);
            };
        }
        return javaArgs;
    }

    public Object[] unpackArgsFromMemory(int ptr, int count, Instance inst) {
        Object[] params = new Object[count];
        Memory mem = inst.memory();
        for (int i = 0; i < count; i++) {
            int base = ptr + (i * 16);
            ArgType type = ArgType.fromTag(mem.readInt(base));
            long data = mem.readLong(base + 8);

            params[i] = switch (type) {
                case HANDLE -> (data == 0) ? null : globalInstanceRegistry.get(data);
                case CHAR -> (char) data;
                case I8 -> (byte) data;
                case I16 -> (short) data;
                case I32 -> (int) data;
                case I64 -> data;
                case F32 -> Float.intBitsToFloat((int) data);
                case F64 -> Double.longBitsToDouble(data);
                case BOOLEAN -> data != 0;
            };
        }
        return params;
    }

    public long[] wrapResult(Class<?> returnType, Object result) {
        if (returnType == void.class) return new long[0];
        ArgType type = ArgType.determineType(returnType);
        return new long[]{switch (type) {
            case BOOLEAN -> (Boolean) result ? 1L : 0L;
            case HANDLE -> registerObject(result);
            case F32 -> (long) Float.floatToRawIntBits((Float) result);
            case F64 -> Double.doubleToRawLongBits((Double) result);
            case CHAR -> (long) (Character) result;
            default -> ((Number) result).longValue();
        }};
    }

    public long[] handleError(Exception e) {
        Throwable actual = (e instanceof java.lang.reflect.InvocationTargetException) ? e.getCause() : e;
        long exClassHandle = registerClass(actual.getClass());
        lastError.set(new ErrorContext(actual.getMessage() != null ? actual.getMessage() : actual.toString(), exClassHandle));
        return new long[]{INVALID_HANDLE};
    }

    public long registerClass(Class<?> clazz) {
        return classRegistry.entrySet().stream().filter(e -> e.getValue().equals(clazz)).map(Map.Entry::getKey).findFirst().orElseGet(() -> {
            long id = handleCounter.getAndIncrement();
            classRegistry.put(id, clazz);
            return id;
        });
    }

    public Object getObject(long handle) {
        return globalInstanceRegistry.get(handle);
    }

    public List<HostApi> getApis() {
        return Collections.unmodifiableList(apis);
    }

    public WasmInstancePool getInstancePool() {
        return instancePool;
    }
}