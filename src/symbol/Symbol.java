package symbol;

import java.util.Objects;

public class Symbol {
    private String name;

    private String llvmName = null;
    private String llvmType = null;
    private int dimension = -1;
    public void setLlvmType(String llvmType) {
        this.llvmType = llvmType;
    }

    public void setLlvmName(String llvmName) {
        this.llvmName = llvmName;
    }

    public String getLlvmName() {
        return llvmName;
    }

    public int getDimension() {
        return dimension;
    }

    public String getLlvmType() {
        return llvmType;
    }

    public Symbol(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Symbol symbol = (Symbol) o;
        // 两个Symbol对象相等当且仅当它们的name相等
        return Objects.equals(name, symbol.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, llvmName, llvmType);
    }
}
