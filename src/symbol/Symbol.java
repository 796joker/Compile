package symbol;

import java.util.Objects;

public class Symbol {
    private String name;

    private String llvmName = null;
    private String llvmType = null;
    private int dimension = -1;
    private String innerDim = null;

    private boolean isConst = false;

    public boolean isConst() {
        return isConst;
    }

    public void setConst() {
        isConst = true;
    }

    /**
     * 是否已经优化忽略掉
     */
    private boolean isIgnored = false;

    public boolean isIgnored() {
        return isIgnored;
    }

    public void ignore() {
        this.isIgnored = true;
    }

    public void setInnerDim(String innerDim) {
        this.innerDim = innerDim;
    }

    public String getInnerDim() {
        return innerDim;
    }

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
