package llvm;

import java.util.StringJoiner;

/**
 * @author AS
 */
public class GlobalVal {
    private String name;
    /**
     * 如果是变量,那么可以没有值
     */
    private String value = null;
    private String constOrGlobalLabel;
    private static final String localLabel = "dso_local";

    private String valType;

    public GlobalVal(String name, String value, String constOrGlobalLabel, String valType) {
        this.name = name;
        this.value = value;
        this.constOrGlobalLabel = constOrGlobalLabel;
        this.valType = valType;
    }

    @Override
    public String toString() {
        StringJoiner stringJoiner = new StringJoiner(" ");
        stringJoiner.add(name);
        // 说明有初值
        if (value != null) {
            stringJoiner.add("=");
            stringJoiner.add(localLabel);
            stringJoiner.add(constOrGlobalLabel);
            stringJoiner.add(valType);
            stringJoiner.add(value);
        }
        return stringJoiner.toString();
    }
}
