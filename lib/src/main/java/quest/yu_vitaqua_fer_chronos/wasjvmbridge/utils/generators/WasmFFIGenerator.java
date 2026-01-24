package quest.yu_vitaqua_fer_chronos.wasjvmbridge.utils.generators;

import quest.yu_vitaqua_fer_chronos.wasjvmbridge.api.HostApi;
import quest.yu_vitaqua_fer_chronos.wasjvmbridge.api.HostStruct;
import java.util.List;

public interface WasmFFIGenerator {
    String generate(List<HostApi> apis, List<Class<? extends HostStruct>> structs);
}