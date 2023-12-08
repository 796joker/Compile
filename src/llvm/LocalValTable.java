package llvm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author AS
 */
public class LocalValTable {
    private HashMap<String, String> localVal = new HashMap<>();
    public void addLocalVal(String name, String type) {
        localVal.put(name, type);
    }
    public String getLocalValType(String name) {
        return localVal.get(name);
    }
}
