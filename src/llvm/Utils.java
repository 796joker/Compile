package llvm;

import node.*;
import symbol.*;
import token.TokenType;

import java.util.*;

/**
 * @author AS
 */
public class Utils {
    private Utils() {}
    private static final Utils UTILS = new Utils();

    public static Utils getUTILS() {
        return UTILS;
    }

    private static final String BR = "br";
    private static final String TAB = "    ";
    private static final String LABEL = "label";
    private static final String I1 = "i1";
    private static final String I32 = "i32";
    private static final String I32POINT = "i32*";
    private static final String COMMA = ",";

    /**
     * 当前符号表
     */
    private SymbolTable curSymbolTable;

    /**
     * 局部变量表
     */
    private LocalValTable curLocalValTable;

    /**
     * 所有局部变量表
     */
    private List<LocalValTable> localValTables;

    /**
     * 记录SymbolTable的子Table的编号,因为Block可以嵌套,所以需要用栈记录
     */
    private Stack<Integer> childTableIndex;

    /**
     * 符号表建立过程获得的符号表
     */
    private static final List<SymbolTable> SYMBOL_TABLES = SymbolTableBuilder.getSymbolTableBuilder().getSymbolTables();

    /**
     * 这个Map存储标识符对应的的常数值,可能有全局常/变量对应的数值,int类型函数对应地返回值
     */
    private HashMap<Symbol, Integer> constValMap;

    /**
     * 记录常数数组的值
     */
    private HashMap<Symbol, List<Integer>> constArrValMap;

    /**
     * 用于分配的寄存器编号
     */
    private int tmpRegisterNum;

    /**
     * 初始化
     */
    public void init() {
        this.constValMap = new HashMap<>(16);
        this.constArrValMap = new HashMap<>(16);
        this.curSymbolTable = SYMBOL_TABLES.get(0);
        // 初始化符号表序号
        this.childTableIndex = new Stack<>();
        this.childTableIndex.push(0);
        // 添加库函数定义
        addLibFunc();
        this.tmpRegisterNum = 0;
        // 局部变量表初始化
        this.localValTables = new ArrayList<>();
        this.curLocalValTable = null;
    }

    /**
     * 纯粹的寄存器编号++,用于函数定义本身占一个寄存器编号
     */
    public void addRegisterNum() {
        this.tmpRegisterNum++;
    }

    /**
     * 进入新的大块(FuncDef, Main)初始化局部变量相关变量
     */
    public void enterNewLocalArea() {
        // 重置寄存器编号
        this.tmpRegisterNum = -1;
        // 申请新的局部变量表
        this.curLocalValTable = new LocalValTable();
        this.localValTables.add(this.curLocalValTable);
    }

    /**
     * 获取局部变量的类型
     */
    public String getLocalValType(String name) {
        return curLocalValTable.getLocalValType(name);
    }

    /**
     * 添加局部变量
     */
    public void addLocalVal(String name, String type) {
        curLocalValTable.addLocalVal(name, type);
    }

    /**
     * 获得当前符号表的下一个子符号表
     */
    public void enterNextSymbolTable() {
        int curChildIndex = this.childTableIndex.pop();
        SymbolTable childSymbolTable = curSymbolTable.getChilds().get(curChildIndex);
        curChildIndex += 1;
        this.childTableIndex.push(curChildIndex);
        // 切换当前符号表
        this.curSymbolTable = childSymbolTable;
        this.childTableIndex.push(0);
    }

    /**
     * 退出当前层符号表,返回上一层
     */
    public void exitCurSymbolTable() {
        this.curSymbolTable = curSymbolTable.getParent();
        this.childTableIndex.pop();
    }

    /**
     * 添加库函数定义
     */
    public void addLibFunc() {
        FuncSymbol getint = new FuncSymbol("getint", new ArrayList<>(), FuncSymbol.ReturnType.INT, 0);
        getint.setLlvmName("@" + "getint");

        List<FuncParamSymbol> putintParams = new ArrayList<>() {{
            FuncParamSymbol putintParam = new FuncParamSymbol("a", 0);
            putintParam.setLlvmType(I32);
            add(putintParam);
        }};
        FuncSymbol putint = new FuncSymbol("putint", putintParams, FuncSymbol.ReturnType.VOID, 0);
        putint.setLlvmName("@" + "putint");

        List<FuncParamSymbol> putchParams = new ArrayList<>() {{
            FuncParamSymbol putchParam = new FuncParamSymbol("a", 0);
            putchParam.setLlvmType(I32);
            add(putchParam);
        }};
        FuncSymbol putch = new FuncSymbol("putch", putchParams, FuncSymbol.ReturnType.VOID, 0);
        putch.setLlvmName("@" + "putch");

        // 加入全局符号表
        this.curSymbolTable.addSymbol(getint);
        this.curSymbolTable.addSymbol(putint);
        this.curSymbolTable.addSymbol(putch);
    }


    /**
     * 设置符号表的llvmName和llvmType
     * @param name 符号本身在代码中的名字
     * @param llvmType 符号的llvmType
     * @param llvmName 符号的llvmName
     * @param constValue 如果是常数,可以记录其值
     */
    public void setSymbolLLVMInfo(String name, String llvmType, String llvmName, int constValue, String defName) {
        // 此时才记录进符号表
        Symbol symbol = findSymbol(name, defName);
        symbol.setLlvmName(llvmName);
        symbol.setLlvmType(llvmType);
        constValMap.put(symbol, constValue);
    }


    /**
     * 获取常量的值
     */
    public int getConstValue(Symbol symbol) {
        return constValMap.get(symbol);
    }


    public Symbol findSymbol(String name, String defName) {
        // 正在定义或尚未定义的变量的llvmName是null
        // 已经定义好的变量的llvmName不为null
        SymbolTable tmp = this.curSymbolTable;
        Symbol ret = null;
        while (tmp != null) {
            if ((ret = tmp.getSymbol(name)) != null) {
                // 满足以下条件之一,此符号有效可返回:
                // 1.llvmName不为null,说明已经定义完成
                // 2.llvmName为null,但是现在正将要写入llvmName的定义变量
                if (ret.getLlvmName() != null || Objects.equals(defName, name)) {
                    break;
                }
            }
            tmp = tmp.getParent();
        }
        return ret;
    }




    public static int calVal(TokenType opType, int val1, int val2) {
        return switch (opType) {
            case PLUS -> val1 + val2;
            case MINU -> val1 - val2;
            case MULT -> val1 * val2;
            case DIV -> val1 / val2;
            case MOD -> val1 % val2;
            default -> 0;
        };
    }

    /**
     * 无条件跳转
     */
    public static Instruction unconditionalJump(String dest) {
        // br label <dest>
        StringJoiner stringJoiner = new StringJoiner(" ", TAB, "");
        stringJoiner.add(BR).add(LABEL).add(dest);
        return new Instruction(null, stringJoiner);
    }

    /**
     * 有条件跳转
     * @param cond 条件
     * @param ifTrue 条件为真时跳转的label
     * @param ifFalse 条件为假时跳转的label
     */
    public static Instruction conditionalJump(String cond, String ifTrue, String ifFalse) {
        // br i1 <cond>, label <if-true>, label <if-false>
        StringJoiner stringJoiner = new StringJoiner(" ", TAB, "");
        stringJoiner.add(BR).add(I1).add(cond+COMMA).add(LABEL).add(ifTrue+COMMA).add(LABEL).add(ifFalse);
        return new Instruction(null, stringJoiner);
    }

    /**
     * load变量
     */
    public AllocElement load(String dType, String source) {
        // %7 = load i32, i32* %6
        AllocElement alloc = alloc(dType);
        // 记录局部变量
        curLocalValTable.addLocalVal(alloc.getLlvmName(), dType);
        // 存了申请语句,但不用就行了
        String tmp = alloc.getLlvmName();
        StringJoiner stringJoiner = new StringJoiner(" ", TAB, "");
        stringJoiner.add(tmp).add("=").add("load").add(dType + ",").add(dType+"*").add(source);
        String instruction = stringJoiner.toString();
        return new AllocElement(tmp, instruction);
    }


    /**
     * 进行非运算
     */
    public Instruction not(String name) {
        Instruction instruction = new Instruction(null, new StringJoiner("\n"));
        String localValType = getLocalValType(name);
        // 如果是一个常数,非零返回1;否则返回1
        int constInt;
        try {
            constInt = Integer.parseInt(name);
            if (constInt != 0) {
                instruction.setLlvmName("0");
            }
            else {
                instruction.setLlvmName("1");
            }
            return instruction;
        }
        catch (Exception e) {
            constInt = 0;
        }
        if (Objects.equals(localValType, I32)) {
            // 不是条件表达式结果而是一个整数值
            // %12 = load i32, i32* %2, align 4
            //  %13 = icmp ne i32 %12, 0
            //  %14 = xor i1 %13, true
            // 传来的变量已经load了,直接用; 不为零则为真
            AllocElement icmpAlloc = icmp(TokenType.NEQ, name, "0");
            instruction.addInstruction(icmpAlloc.getInstruction());
            // 申请临时变量,无需加入申请指令
            AllocElement allocAlloc = alloc(I1);
            String tmp = allocAlloc.getLlvmName();
            // 进行取反
            StringJoiner stringJoiner = new StringJoiner(" ");
            stringJoiner.add(TAB + tmp).add("=").add("xor").add(I1).add(icmpAlloc.getLlvmName() + ",").add("true");
            // 加入取反指令
            instruction.addInstruction(stringJoiner.toString());
            // 设置返回值
            instruction.setLlvmName(tmp);
        }
        // 是条件表达式结果
        else if (Objects.equals(localValType, I1)){
            // 直接取反
            StringJoiner stringJoiner = new StringJoiner(" ");
            // 申请临时变量
            AllocElement allocAlloc = alloc(I1);
            String tmp = allocAlloc.getLlvmName();
            instruction.addInstruction(allocAlloc.getInstruction());
            stringJoiner.add(tmp).add("=").add("xor").add(I1).add(name + ",").add("true");
            instruction.addInstruction(stringJoiner.toString());
            // 设置返回值
            instruction.setLlvmName(tmp);
        }
        return instruction;
    }

    /**
     * 为局部变量申请空间的语句
     * @return 申请出的局部空间的名字
     */
    public AllocElement alloc(String type) {
        tmpRegisterNum++;
        String llvmName = "%" + tmpRegisterNum;
        // 记录局部变量
        curLocalValTable.addLocalVal(llvmName, type);
        String instruction = TAB + llvmName + " = alloca " + type;
        return new AllocElement(llvmName, instruction);
    }

    /**
     * 申请一个序号给Label
     * @return Label名字
     */
    public String allocLabel() {
        tmpRegisterNum++;
        String labelName = "%" + tmpRegisterNum;
        curLocalValTable.addLocalVal(labelName, "label");
        return labelName;
    }


    public Instruction addLabel(String label) {
        // 标签值没有'%'
        StringJoiner stringJoiner = new StringJoiner("\n");
        // 先换一行
        stringJoiner.add("").add(label.substring(1) + ":");
        return new Instruction(null, stringJoiner);
    }



    /**
     * store变量值
     * @return store指令
     */
    public String store(String sType, String sName, String dType, String dName) {
        StringJoiner stringJoiner = new StringJoiner(" ", TAB, "");
        stringJoiner.add("store").add(sType).add(sName + ",").add(dType).add(dName);
        return stringJoiner.toString();
    }




    /**
     * 进行条件运算
     * @param op opToken的类型
     * @return 条件运算的结果
     */
    public AllocElement icmp(TokenType op, String val1, String val2) {
        String condOp = switch (op) {
            case GEQ -> "sge";
            case GRE -> "sgt";
            case LEQ -> "sle";
            case LSS -> "slt";
            case EQL -> "eq";
            case NEQ -> "ne";
            default -> null;
        };
        if (condOp == null) {
            System.out.println("未知错误");
        }
        // 例子: %9 = icmp slt i32 %8, 2
        StringJoiner stringJoiner = new StringJoiner(" ", TAB, "");
        String tmp = "%" + (++tmpRegisterNum);
        // 记录局部变量
        curLocalValTable.addLocalVal(tmp, I1);
        stringJoiner.add(tmp).add("=").add("icmp").add(condOp).add(I32).add(val1 + ",").add(val2);
        String instruction = stringJoiner.toString();
        return new AllocElement(tmp, instruction);
    }


    public AllocElement zext(String originVal) {
        // %7 = zext i1 %6 to i32
        String tmp = "%" + (++tmpRegisterNum);
        // 记录局部变量
        curLocalValTable.addLocalVal(tmp, I32);
        StringJoiner stringJoiner = new StringJoiner(" ", TAB, "");
        stringJoiner.add(tmp).add("=").add("zext").add(I1).add(originVal).add("to").add(I32);
        String instruction = stringJoiner.toString();
        return new AllocElement(tmp, instruction);
    }

    public AllocElement calPrint(String type, String lName, String rName, TokenType opType) {
        // 查看两操作数是否常量,如果是,直接算出并返回,不用申请空间
        try {
            int val1 = Integer.parseInt(lName);
            int val2 = Integer.parseInt(rName);
            String tmp = String.valueOf(calVal(opType, val1, val2));
            return new AllocElement(tmp, null);
        }
        catch (Exception ignored) {
        }
        // 中间变量
        AllocElement alloc = alloc(I32);
        String tmp = alloc.getLlvmName();
        StringJoiner stringJoiner = new StringJoiner(" ", TAB, "");
        stringJoiner.add(tmp).add("=");
        String instruction = switch (opType) {
            case PLUS -> stringJoiner.add("add").add(type).add(lName + ",").add(rName).toString();
            case MINU -> stringJoiner.add("sub").add(type).add(lName + ",").add(rName).toString();
            case MULT -> stringJoiner.add("mul").add(type).add(lName + ",").add(rName).toString();
            case DIV -> stringJoiner.add("sdiv").add(type).add(lName + ",").add(rName).toString();
            case MOD -> stringJoiner.add("srem").add(type).add(lName + ",").add(rName).toString();
            default -> "";
        };
        return new AllocElement(tmp, instruction);
    }

    /**
     * 函数调用
     */
    public AllocElement callFunc(String funcName, List<String> args) {
        // %7 = call i32 @aaa(i32 %5, i32 %6)
        // call void @putint(i32 %7)
        FuncSymbol funcSymbol = (FuncSymbol) UTILS.findSymbol(funcName, null);
        List<FuncParamSymbol> funcParamSymbols = funcSymbol.getFuncParamSymbols();
        // 遍历参数列表,插入参数
        StringJoiner instruction = new StringJoiner(" ", TAB, "");
        StringJoiner argsJoiner = new StringJoiner(", ", "(", ")");
        if (args != null) {
            for(int i=0; i<args.size(); i++) {
                argsJoiner.add(funcParamSymbols.get(i).getLlvmType() + " " + args.get(i));
            }
        }
        // 记录返回值
        String tmp = null;
        switch (funcSymbol.getReturnType()) {
            case INT -> {
                tmp = "%" + (++tmpRegisterNum);
                // 记录局部变量
                curLocalValTable.addLocalVal(tmp, I32);
                instruction.add(tmp).add("=").add("call").add(I32).add(funcSymbol.getLlvmName() + argsJoiner);
            }
            case VOID -> instruction.add("call").add("void").add(funcSymbol.getLlvmName() + argsJoiner);
            default -> System.out.println("unexpected func returnType");
        }
        return new AllocElement(tmp, instruction.toString());
    }


    /**
     * 获取函数参数的类型
     */
    public String getParamType(FuncFParamNode funcFParamNode) {
        int dim = funcFParamNode.getLeftBracketTokens().size();
        String paramType;
        if (dim == 0) {
            paramType = I32;
        }
        else if (dim == 1) {
            paramType = I32POINT;
        }
        else {
            // 计算第二维的维度
            int lastDim = constAddExpHandler(funcFParamNode.getConstExpNode().getAddExpNode());
            paramType = "[" + lastDim + " x i32]*";
        }
        return paramType;
    }

    /**
     * 返回计算得到的常量数值
     */
    public int constAddExpHandler(AddExpNode addExpNode) {
        // AddExp → MulExp | AddExp ('+' | '−') MulExp
        int val1 = 0;
        boolean needCal = false;
        if (addExpNode.getAddExpNode() != null) {
            val1 = constAddExpHandler(addExpNode.getAddExpNode());
            needCal = true;
        }
        int val2 = constMulExpHandler(addExpNode.getMulExpNode());
        if (needCal) {
            return calVal(addExpNode.getOpToken().getType(), val1, val2);
        }
        else {
            return val2;
        }
    }

    public int constMulExpHandler(MulExpNode mulExpNode) {
        // MulExp → UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
        int val1 = 0;
        boolean needCal = false;
        if (mulExpNode.getMulExpNode() != null) {
            val1 = constMulExpHandler(mulExpNode.getMulExpNode());
            needCal = true;
        }
        int val2 = constUnaryExpHandler(mulExpNode.getUnaryExpNode());
        if (needCal) {
            return calVal(mulExpNode.getOpToken().getType(), val1, val2);
        }
        else {
            return val2;
        }
    }

    public int constUnaryExpHandler(UnaryExpNode unaryExpNode) {
        // UnaryExp → PrimaryExp | UnaryOp UnaryExp | Ident '(' [FuncRParams] ')'
        // 注意,常量表达式中的UnaryOp不会出现 '!'
        if (unaryExpNode.getUnaryOpNode() != null) {
            int val = constUnaryExpHandler(unaryExpNode.getUnaryExpNode());
            val = calVal(unaryExpNode.getUnaryOpNode().getOpToken().getType(), 0, val);
            return val;
        }
        else if (unaryExpNode.getPrimaryExpNode() != null) {
            return constPrimaryExpHandler(unaryExpNode.getPrimaryExpNode());
        }
        else {
            // 函数调用, 全局变量中不会出现函数调用
            return 0;
        }
    }

    /**
     * 常数表达式constPrimaryExp
     */
    public int constPrimaryExpHandler(PrimaryExpNode primaryExpNode) {
        // PrimaryExp → '(' Exp ')' | Number | LVal
        if (primaryExpNode.getExpNode() != null) {
            return constAddExpHandler(primaryExpNode.getExpNode().getAddExpNode());
        }
        else if (primaryExpNode.getNumberNode() != null) {
            // 常数
            return Integer.parseInt(primaryExpNode.getNumberNode().getIntConstToken().getValue());
        }
        else {
            // LVal 标识符
            LValNode lValNode = primaryExpNode.getlValNode();
            if (lValNode.getExpNodes().size() == 0) {
                // 不是数组, 查表找到其对应值
                Symbol symbol = findSymbol(lValNode.getIdentToken().getValue(), null);
                return constValMap.get(symbol);
            }
            else {
                // 常量表达式不包括数组元素
                return 0;
            }
        }
    }

    /**
     * 记录常数数组的值
     */
    public void addConstArrVal(Symbol symbol, List<Integer> constValues) {
        this.constArrValMap.put(symbol, constValues);
    }

    /**
     * 获取常量数组元素的值
     */
    public String getConstArrEle(Symbol symbol, int i, int j) {
        List<Integer> constValues = constArrValMap.get(symbol);
        int index;
        if (symbol.getDimension() == 1) {
            index = i;
        }
        else {
            index = i * ((ValSymbol) symbol).getInnerSize() + j;
        }
        return String.valueOf(constValues.get(index));
    }

}
