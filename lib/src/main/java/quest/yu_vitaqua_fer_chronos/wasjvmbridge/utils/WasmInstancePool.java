package quest.yu_vitaqua_fer_chronos.wasjvmbridge.utils;

import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Store;
import com.dylibso.chicory.wasm.Parser;

import java.util.concurrent.ConcurrentLinkedQueue;

public class WasmInstancePool {
    private final ConcurrentLinkedQueue<Instance> pool = new ConcurrentLinkedQueue<>();
    private final Store store;
    private final byte[] binary;

    public WasmInstancePool(Store store, byte[] binary) {
        this.store = store;
        this.binary = binary;
    }

    public Instance borrow() {
        Instance inst = pool.poll();
        return (inst != null) ? inst : store.instantiate("test", Parser.parse(binary));
    }

    public void release(Instance inst) {
        pool.offer(inst);
    }
}