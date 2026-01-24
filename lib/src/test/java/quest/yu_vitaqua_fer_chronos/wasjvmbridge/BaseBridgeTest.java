package quest.yu_vitaqua_fer_chronos.wasjvmbridge;

import com.dylibso.chicory.runtime.ByteArrayMemory;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Memory;
import com.dylibso.chicory.wasm.types.MemoryLimits;
import org.junit.jupiter.api.BeforeEach;
import quest.yu_vitaqua_fer_chronos.wasjvmbridge.api.HostStruct;
import quest.yu_vitaqua_fer_chronos.wasjvmbridge.modules.*;
import quest.yu_vitaqua_fer_chronos.wasjvmbridge.modules.specs.WasmClassSpec;
import quest.yu_vitaqua_fer_chronos.wasjvmbridge.modules.specs.WasmMapping;

import java.io.InputStream;
import java.util.List;
import java.util.Objects;

public abstract class BaseBridgeTest {
    protected WasJVMBridge bridge;
    protected Instance instance;

    protected abstract String getWasmPath();

    @BeforeEach
    void setup() throws Exception {
        bridge = new WasJVMBridge();

        List<Class<? extends HostStruct>> structs = List.of(
                WasmMapping.class,
                WasmClassSpec.class
        );

        // Register all declarative modules
        bridge.addApi(new CoreModule(bridge));
        bridge.addApi(new StringModule(bridge));
        bridge.addApi(new ReflectionModule(bridge));
        bridge.addApi(new CollectionModule(bridge));
        bridge.addApi(new BuilderModule(bridge));
        bridge.addApi(new StructAccessorModule(bridge, structs));

        // Shared memory for tests
        Memory sharedMem = new ByteArrayMemory(new MemoryLimits(10, 100, true));

        InputStream wasmStream = getClass().getClassLoader().getResourceAsStream(getWasmPath());
        byte[] wasmBytes = Objects.requireNonNull(wasmStream).readAllBytes();

        // Initialize the pool (which instantiates the first instance)
        bridge.initialisePool(sharedMem, wasmBytes);
        instance = bridge.getInstancePool().borrow();
    }
}