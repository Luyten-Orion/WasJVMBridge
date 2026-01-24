package quest.yu_vitaqua_fer_chronos.wasjvmbridge.tasks.generators;

import quest.yu_vitaqua_fer_chronos.wasjvmbridge.WasJVMBridge;
import quest.yu_vitaqua_fer_chronos.wasjvmbridge.api.HostStruct;
import quest.yu_vitaqua_fer_chronos.wasjvmbridge.modules.*;
import quest.yu_vitaqua_fer_chronos.wasjvmbridge.modules.specs.WasmClassSpec;
import quest.yu_vitaqua_fer_chronos.wasjvmbridge.modules.specs.WasmMapping;
import quest.yu_vitaqua_fer_chronos.wasjvmbridge.utils.generators.CHeaderGenerator;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Runner {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) throw new IllegalArgumentException("Output path required");

        WasJVMBridge bridge = new WasJVMBridge();
        bridge.addApi(new CoreModule(bridge));
        bridge.addApi(new StringModule(bridge));
        bridge.addApi(new ReflectionModule(bridge));
        bridge.addApi(new CollectionModule(bridge));
        bridge.addApi(new BuilderModule(bridge));
        // Define which structs we want to expose to the FFI
        List<Class<? extends HostStruct>> structs = List.of(WasmMapping.class, WasmClassSpec.class);
        bridge.addApi(new StructAccessorModule(bridge, structs));

        CHeaderGenerator gen = new CHeaderGenerator();
        String header = gen.generate(bridge.getApis(), structs);
        Files.writeString(Paths.get(args[0]), header);

        System.out.println("ABI Header generated with " + structs.size() + " structs.");
    }
}