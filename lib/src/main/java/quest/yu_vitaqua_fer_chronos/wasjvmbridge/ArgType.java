package quest.yu_vitaqua_fer_chronos.wasjvmbridge;

public enum ArgType {
    HANDLE(0),  // i64 handle
    CHAR(1),    // i16 (UTF-16)
    I8(2),      // byte
    I16(3),     // short
    I32(4),     // int
    I64(5),     // long
    F32(6),     // float
    F64(7),     // double
    BOOLEAN(8); // i32 (0 or 1)

    public final int tag;
    ArgType(int tag) { this.tag = tag; }
    public static ArgType fromTag(int tag) {
        return values()[tag];
    }
}