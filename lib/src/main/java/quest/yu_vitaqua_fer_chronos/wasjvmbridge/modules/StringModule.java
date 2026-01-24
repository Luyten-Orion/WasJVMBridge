package quest.yu_vitaqua_fer_chronos.wasjvmbridge.modules;

import com.dylibso.chicory.runtime.Instance;
import quest.yu_vitaqua_fer_chronos.wasjvmbridge.WasJVMBridge;
import quest.yu_vitaqua_fer_chronos.wasjvmbridge.api.HostApi;
import quest.yu_vitaqua_fer_chronos.wasjvmbridge.utils.WasmExport;

import java.nio.charset.StandardCharsets;

public class StringModule extends HostApi {
    public StringModule(WasJVMBridge bridge) {
        super(bridge);
    }

    @Override
    public String getNamespace() {
        return "wasjvmb_strings";
    }

    @WasmExport(params = {"ptr", "len"})
    public long create_string(Instance inst, int ptr, int len) {
        return bridge.registerObject(inst.memory().readString(ptr, len));
    }

    @WasmExport(params = {"handle", "len_out_ptr"})
    public int get_string_content(Instance inst, long handle, int lenOutPtr) {
        byte[] b = ((String) bridge.getObject(handle)).getBytes(StandardCharsets.UTF_8);
        int ptr = (int) inst.export("malloc").apply(b.length)[0];
        inst.memory().write(ptr, b);
        inst.memory().writeI32(lenOutPtr, b.length);
        return ptr;
    }

    @WasmExport(params = {"handle", "ptr", "max_len"})
    public int get_string_into(Instance inst, long handle, int ptr, int maxLen) {
        byte[] b = ((String) bridge.getObject(handle)).getBytes(StandardCharsets.UTF_8);
        int len = Math.min(b.length, maxLen);
        inst.memory().write(ptr, b, 0, len);
        return len;
    }
}