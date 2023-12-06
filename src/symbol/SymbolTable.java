package symbol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SymbolTable {
    /**
     * 当前所在层次
     */
    private int level;
    /**
     * 当前层次所包含的符号
     */
    private HashMap<String, Symbol> symbols;

    /**
     * 当前符号表的父符号表
     */
    private SymbolTable parent;

    /**
     * 当前符号表的子符号表
     */
    private List<SymbolTable> childs;

    public SymbolTable(int level, SymbolTable parent) {
        this.level = level;
        this.symbols = new HashMap<>();
        this.parent = parent;
        this.childs = new ArrayList<>();
    }

    public int getLevel() {
        return level;
    }

    public HashMap<String, Symbol> getSymbols() {
        return symbols;
    }

    public List<SymbolTable> getChilds() {
        return childs;
    }

    /**
     * 添加符号到当前层次的符号表中
     */
    public void addSymbol(Symbol symbol) {
        this.symbols.put(symbol.getName(), symbol);
    }

    public SymbolTable getParent() {
        return parent;
    }

    public boolean contains(String name) {
        return symbols.containsKey(name);
    }

    public Symbol getSymbol(String name) {
        return symbols.get(name);
    }

    public void addChild(SymbolTable symbolTable) {
        this.childs.add(symbolTable);
    }
}
