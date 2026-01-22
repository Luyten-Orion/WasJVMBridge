package quest.yu_vitaqua_fer_chronos.wasjvmbridge;

import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Store;
import com.dylibso.chicory.wasm.Parser;
import org.junit.jupiter.api.BeforeEach;

import java.io.InputStream;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

public abstract class BaseBridgeTest {
    protected WasJVMBridge bridge;
    protected Instance instance;
    protected List<HostFunction> hostFunctions;

    // Overridable method to specify which WASM file to load
    protected String getWasmPath() {
        return "wasm/ffi_test.wasm";
    }

    protected HostFunction findFunc(String name) {
        return hostFunctions.stream().filter(f -> f.name().equals(name)).findFirst().orElseThrow(() -> new RuntimeException("Missing: " + name));
    }

    @BeforeEach
    void setup() throws Exception {
        bridge = new WasJVMBridge();
        var store = new Store();
        hostFunctions = bridge.getFunctions(EnumSet.allOf(WasJVMBridge.Permission.class));
        for (var f : hostFunctions) store.addFunction(f);

        InputStream wasmStream = getClass().getClassLoader().getResourceAsStream(getWasmPath());
        instance = store.instantiate("test", Parser.parse(Objects.requireNonNull(wasmStream).readAllBytes()));
    }
}