package quest.yu_vitaqua_fer_chronos.wasjvmbridge;

public class TestTarget {
    public String message = "initial";
    public String result = "";

    public void multiParam(String prefix, int count, boolean flag) {
        this.result = prefix + " count: " + count + " flag: " + flag;
    }

    public void voidMethod() {}
    public void crash() { throw new RuntimeException("test error"); }
}