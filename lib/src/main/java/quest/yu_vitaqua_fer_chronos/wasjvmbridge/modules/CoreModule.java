package quest.yu_vitaqua_fer_chronos.wasjvmbridge.modules;

import com.dylibso.chicory.runtime.Instance;
import quest.yu_vitaqua_fer_chronos.wasjvmbridge.WasJVMBridge;
import quest.yu_vitaqua_fer_chronos.wasjvmbridge.api.HostApi;
import quest.yu_vitaqua_fer_chronos.wasjvmbridge.utils.WasmExport;

public class CoreModule extends HostApi {
    public CoreModule(WasJVMBridge bridge) {
        super(bridge);
    }

    @Override
    public String getNamespace() {
        return "wasjvmb_core";
    }

    @WasmExport
    public void flush_error() {
        bridge.lastError.remove();
    }

    @WasmExport
    public long get_last_error_handle() {
        return bridge.lastError.get().handle();
    }

    @WasmExport(params = {"buf_ptr", "max_len"})
    public int get_last_error_message(Instance inst, int ptr, int maxLen) {
        byte[] msg = bridge.lastError.get().message().getBytes();
        int len = Math.min(msg.length, maxLen);
        inst.memory().write(ptr, msg, 0, len);
        return len;
    }

    @WasmExport(params = {"handle"})
    public void release_instance(long handle) {
        bridge.globalInstanceRegistry.remove(handle);
    }
}