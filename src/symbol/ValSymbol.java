package symbol;

public class ValSymbol extends Symbol{
    /**
     * 是否常量
     */
    private boolean isConst;
    /**
     * 维度
     * 0, 1, 2维
     */
    private int dimension;

    public ValSymbol(String name, boolean isConst, int dimension) {
        super(name);
        this.isConst = isConst;
        this.dimension = dimension;
    }

    public boolean isConst() {
        return isConst;
    }

    public int getDimension() {
        return dimension;
    }
}
