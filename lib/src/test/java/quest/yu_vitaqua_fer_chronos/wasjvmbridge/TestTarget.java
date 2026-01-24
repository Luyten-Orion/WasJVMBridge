package quest.yu_vitaqua_fer_chronos.wasjvmbridge;

import java.util.List;

public class TestTarget {
    public String message = "initial";
    public String result = "";
    public int value = 0;

    public void multiParam(String prefix, int count, boolean flag) {
        this.result = prefix + " count: " + count + " flag: " + flag;
    }

    public List<String> getStrings() {
        return List.of("A", "B", "C");
    }

    public void crash() {
        throw new RuntimeException("test error");
    }
}