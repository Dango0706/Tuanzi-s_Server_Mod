package me.tuanzi.cdk;

import java.util.HashMap;
import java.util.Map;

public class CDKData {
    private final Map<String, CDKEntry> cdks = new HashMap<>();

    public Map<String, CDKEntry> getCdks() {
        return cdks;
    }

    public void addCDK(CDKEntry entry) {
        cdks.put(entry.getCode(), entry);
    }

    public void removeCDK(String code) {
        cdks.remove(code);
    }

    public CDKEntry getCDK(String code) {
        return cdks.get(code);
    }
}
