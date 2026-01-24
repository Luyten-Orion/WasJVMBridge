package quest.yu_vitaqua_fer_chronos.wasjvmbridge.modules.specs;

import quest.yu_vitaqua_fer_chronos.wasjvmbridge.api.HostStruct;

public class WasmClassSpec extends HostStruct {
    public long superHandle;
    public int interfaceCount;
    public int interfacesPtr; // i64*
    public int mappingCount;
    public int mappingsPtr;   // WasmMapping*
}