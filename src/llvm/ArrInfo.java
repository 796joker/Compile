package llvm;

public class ArrInfo {
    private String[] arrSymbolInfo;
    private int[] dimensionInfo;

    public ArrInfo(String[] arrSymbolInfo, int[] dimensionInfo) {
        this.arrSymbolInfo = arrSymbolInfo;
        this.dimensionInfo = dimensionInfo;
    }

    public int[] getDimensionInfo() {
        return dimensionInfo;
    }

    public String[] getArrSymbolInfo() {
        return arrSymbolInfo;
    }
}
