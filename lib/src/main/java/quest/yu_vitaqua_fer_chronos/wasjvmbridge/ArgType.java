package quest.yu_vitaqua_fer_chronos.wasjvmbridge;

public enum ArgType {
    HANDLE(0), CHAR(1), I8(2), I16(3), I32(4), I64(5), F32(6), F64(7), BOOLEAN(8);

    public final int tag;

    ArgType(int tag) {
        this.tag = tag;
    }

    public static ArgType fromTag(int tag) {
        return values()[tag];
    }

    public static ArgType determineType(Class<?> type) {
        if (type == null) return HANDLE;
        else if (type == int.class || type == Integer.class) return I32;
        else if (type == long.class || type == Long.class) return I64;
        else if (type == float.class || type == Float.class) return F32;
        else if (type == double.class || type == Double.class) return F64;
        else if (type == boolean.class || type == Boolean.class) return BOOLEAN;
        else if (type == byte.class || type == Byte.class) return I8;
        else if (type == short.class || type == Short.class) return I16;
        else if (type == char.class || type == Character.class) return CHAR;
        return HANDLE;
    }
}