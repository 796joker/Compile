package symbol;

public class FuncParamSymbol extends Symbol{
    /**
     * 维度, 0, 1, 2
     */
    private int dimension;

    public FuncParamSymbol(String name, int demision) {
        super(name);
        this.dimension = demision;
    }

    public int getDimension() {
        return dimension;
    }
}
