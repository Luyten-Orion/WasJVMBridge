package quest.yu_vitaqua_fer_chronos.wasjvmbridge.api;

/**
 * Base class for Java objects that mirror C structs in WASM memory.
 * Fields should be ordered exactly as they appear in the C 'typedef struct'.
 */
public abstract class HostStruct {
    // Marker class for reflection-based serialization and FFI generation
}