package quest.yu_vitaqua_fer_chronos.wasjvmbridge;

import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.runtime.Instance;
import quest.yu_vitaqua_fer_chronos.wasjvmbridge.modules.CollectionModule;
import quest.yu_vitaqua_fer_chronos.wasjvmbridge.modules.CoreModule;
import quest.yu_vitaqua_fer_chronos.wasjvmbridge.modules.ReflectionModule;
import quest.yu_vitaqua_fer_chronos.wasjvmbridge.modules.StringModule;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class WasJVMBridge {
    public static final long INVALID_HANDLE = -1L;
    public static final String NAMESPACE = "wasjvmbridge";

    // Public registries for module access
    public final Map<Long, Class<?>> classRegistry = new ConcurrentHashMap<>();
    public final Map<Long, Method> methodRegistry = new ConcurrentHashMap<>();
    public final Map<Long, Constructor<?>> constructorRegistry = new ConcurrentHashMap<>();
    public final Map<Long, Field> fieldRegistry = new ConcurrentHashMap<>();
    public final Map<Long, Object> globalInstanceRegistry = new ConcurrentHashMap<>();

    public final AtomicLong handleCounter = new AtomicLong(1);
    public final ThreadLocal<ErrorContext> lastError = ThreadLocal.withInitial(() -> new ErrorContext("", INVALID_HANDLE));

    public List<HostFunction> getFunctions(EnumSet<Permission> permissions) {
        List<HostFunction> functions = new ArrayList<>();

        // Logic: If we haven't added these to a Store yet, add them now
        if (permissions.contains(Permission.CORE)) {
            functions.addAll(new CoreModule(this).getFunctions());
        }
        if (permissions.contains(Permission.REFLECTION)) {
            functions.addAll(new ReflectionModule(this).getFunctions());
        }
        if (permissions.contains(Permission.STRINGS)) {
            functions.addAll(new StringModule(this).getFunctions());
        }
        if (permissions.contains(Permission.COLLECTION)) {
            functions.addAll(new CollectionModule(this).getFunctions());
        }

        return functions;
    }

    // Unified argument packer for Methods and Constructors
    public Object[] packArgs(int argsPtr, int argsCount, Instance inst) {
        Object[] params = new Object[argsCount];
        for (int i = 0; i < argsCount; i++) {
            int base = argsPtr + (i * 16);
            ArgType type = ArgType.fromTag(inst.memory().readInt(base));
            long data = inst.memory().readLong(base + 8);

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

    public Object invokeConstructorInternal(long ctorId, int argsPtr, int argsCount, Instance inst) throws Exception {
        Constructor<?> ctor = constructorRegistry.get(ctorId);
        if (ctor == null) throw new IllegalArgumentException("Invalid constructor handle");
        Object[] params = packArgs(argsPtr, argsCount, inst);
        return ctor.newInstance(params);
    }

    public Object invokeInternal(long objId, long mid, int argsPtr, int argsCount, com.dylibso.chicory.runtime.Instance inst) throws Exception {
        java.lang.reflect.Method method = methodRegistry.get(mid);
        if (method == null) {
            throw new IllegalArgumentException("Invalid method handle: " + mid);
        }

        Object target = (objId == 0) ? null : globalInstanceRegistry.get(objId);

        validateMember(method.getDeclaringClass(), target, method.getName());

        Object[] params = packArgs(argsPtr, argsCount, inst);

        return method.invoke(target, params);
    }

    public void validateMember(Class<?> declClass, Object target, String name) {
        if (target != null && !declClass.isInstance(target)) {
            throw new IllegalArgumentException("Compatibility error: Member " + name + " belongs to " + declClass.getName() + " but target is " + target.getClass().getName());
        }
    }

    public long registerClass(Class<?> clazz) {
        return classRegistry.entrySet().stream().filter(entry -> entry.getValue().equals(clazz)).map(Map.Entry::getKey).findFirst().orElseGet(() -> {
            long id = handleCounter.getAndIncrement();
            classRegistry.put(id, clazz);
            return id;
        });
    }

    public long registerObject(Object obj) {
        long id = handleCounter.getAndIncrement();
        globalInstanceRegistry.put(id, obj);
        return id;
    }

    public long findRegisteredClass(Class<?> c) {
        return classRegistry.entrySet().stream().filter(e -> e.getValue().equals(c)).map(Map.Entry::getKey).findFirst().orElse(INVALID_HANDLE);
    }

    public long[] handleError(Exception e) {
        Throwable actual = (e instanceof java.lang.reflect.InvocationTargetException) ? e.getCause() : e;
        long exClassHandle = registerClass(actual.getClass());
        lastError.set(new ErrorContext(actual.getMessage() != null ? actual.getMessage() : actual.toString(), exClassHandle));
        return new long[]{INVALID_HANDLE};
    }

    public Object getObject(long handle) {
        return globalInstanceRegistry.get(handle);
    }

    public enum Permission {CORE, REFLECTION, STRINGS, COLLECTION}
}