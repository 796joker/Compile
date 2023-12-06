package symbol;

import java.util.List;

public class FuncSymbol extends Symbol{
    // 返回类型内部枚举类
    public enum ReturnType {
        INT, VOID
    }
    /**
     * 参数列表
     */
    private List<FuncParamSymbol> funcParamSymbols;
    /**
     * 返回值类型
     */
    private ReturnType returnType;

    // 参数个数
    private int paramNum;

    public FuncSymbol(String name, List<FuncParamSymbol> funcParamSymbols, ReturnType returnType, int paramNum) {
        super(name);
        this.funcParamSymbols = funcParamSymbols;
        this.returnType = returnType;
        this.paramNum = paramNum;
    }

    public List<FuncParamSymbol> getFuncParamSymbols() {
        return funcParamSymbols;
    }

    public ReturnType getReturnType() {
        return returnType;
    }

    public int getParamNum() {
        return paramNum;
    }
}
