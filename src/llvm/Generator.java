package llvm;

import frontend.Parser;
import node.*;
import symbol.*;
import token.TokenType;

import java.util.*;

/**
 * @author AS
 */
public class Generator {
    public Generator() {
    }

    private enum CondType {
        IF,
        FOR,
        IF_ELSE
    }
    private Stack<CondType> condTypes;
    private static final String I32 = "i32";
    private static final String I8 = "i8";
    private static final String I1 = "i1";
    private static final String I32POINT = "i32*";
    private static final String I8POINT = "i8*";
    private static final String I1POINT = "i1*";

    private static final String ZEROINITIALIZER = "zeroinitializer";

    private static final String LBRACE = "{";
    private static final String RBRACE = "}";
    private static final String BR = "br";
    private static final String LABEL = "label";
    private static final String COMMA = ",";
    private static final String TAB = "    ";

    /**
     * 记录当前的for,if,else是否有直接跳转
     */
    Stack<Boolean> forJump;
    Stack<Boolean> ifJump;
    Stack<Boolean> elseJump;

    /**
     * 记录SymbolTable的子Table的编号,因为Block可以嵌套,所以需要用栈记录
     */
    private Stack<Integer> childTableIndex;

    /**
     * 记录是否需要回填continue和break语句
     */
    private Stack<Integer> needContinues;
    private Stack<Integer> needBreaks;

    /**
     * 记录需要回填的continue和break语句的Instruction
     */
    private Stack<Instruction> continueInstructions;
    private Stack<Instruction> breakInstructions;

    private static final List<SymbolTable> SYMBOL_TABLES = SymbolTableBuilder.getSymbolTableBuilder().getSymbolTables();

    private SymbolTable curSymbolTable;

    private int tmpRegisterNum;

    private StringJoiner ret;

    /**
     * 标记是否正在定义初始化
     */
    private String defName;

    private static final Generator GENERATOR = new Generator();

    public static Generator getGenerator() {
        return GENERATOR;
    }

    public String getRet() {
        return ret.toString();
    }

    /**
     * 这个Map存储标识符对应的的常数值,可能有全局常/变量对应的数值,int类型函数对应地返回值
     * 不存储局部变量的值,这样不会有重名的key
     */
    private final HashMap<Symbol, Integer> symbolIntegerHashMap = new HashMap<>();
    private List<LocalValTable> localValTables;

    private LocalValTable curLocalValTable;

    /**
     * 获取函数参数的类型
     */
    private String getParamType(FuncFParamNode funcFParamNode) {
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
     *
     * 获取当前Block所属的SymbolTable在当前SymbolTable中子符号表的编号
     */
    public int getChildTableIndex() {
        // 从栈顶取出
        Integer ret = childTableIndex.pop();
        // 下一次取出就是下一个子符号表的编号
        childTableIndex.push(ret + 1);
        return ret;
    }

    public Symbol findSymbol(String name) {
        // 正在定义或尚未定义的变量的llvmName是null
        // 已经定义好的变量的llvmName不为null
        SymbolTable tmp = curSymbolTable;
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

    public String getLocalValType(String name) {
        return curLocalValTable.getLocalValType(name);
    }

    /**
     * 初始化
     */
    public void init() {
        this.curSymbolTable = SYMBOL_TABLES.get(0);
        this.tmpRegisterNum = 0;
        this.ret = new StringJoiner("\n");
        this.localValTables = new ArrayList<>();
        this.curLocalValTable = null;
        this.condTypes = new Stack<>();
        this.continueInstructions = new Stack<>();
        this.breakInstructions = new Stack<>();
        this.needContinues = new Stack<>();
        this.needBreaks = new Stack<>();
        // 初始化符号表序号
        this.childTableIndex = new Stack<>();
        this.childTableIndex.push(0);
        this.defName = null;
        // 初始化Jump标记
        forJump = new Stack<>();
        ifJump = new Stack<>();
        elseJump = new Stack<>();
    }


    public int calVal(TokenType opType, int val1, int val2) {
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
     * 进行非运算
     */
    public Instruction not(String name) {
        String localValType = getLocalValType(name);
        // 不是条件表达式结果而是一个整数值
        // %12 = load i32, i32* %2, align 4
        //  %13 = icmp ne i32 %12, 0
        //  %14 = xor i1 %13, true
        Instruction instruction = new Instruction(null, new StringJoiner("\n"));
        if (Objects.equals(localValType, I32)) {
            // 加载此变量
            AllocElement loadAlloc = load(I32, name);
            instruction.addInstruction(loadAlloc.getInstruction());
            // 不为零则为真
            AllocElement icmpAlloc = icmp(TokenType.NEQ, loadAlloc.getLlvmName(), "0");
            // 申请临时变量
            AllocElement allocAlloc = alloc(I1);
            String tmp = allocAlloc.getLlvmName();
            instruction.addInstruction(allocAlloc.getInstruction());
            // 进行取反
            StringJoiner stringJoiner = new StringJoiner(" ");
            stringJoiner.add(tmp).add("=").add("xor").add(I1).add(icmpAlloc.getLlvmName() + ",").add("true");
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
     * store变量值
     * @return store指令
     */
    public String store(String sType, String sName, String dType, String dName) {
        StringJoiner stringJoiner = new StringJoiner(" ", TAB, "");
        stringJoiner.add("store").add(sType).add(sName + ",").add(dType).add(dName);
        return stringJoiner.toString();
    }

    /**
     * 改变控制流
     */
    public Instruction unconditionalJump(String dest) {
        // br label <dest>
        StringJoiner stringJoiner = new StringJoiner(" ", TAB, "");
        stringJoiner.add(BR).add(LABEL).add(dest);
        return new Instruction(null, stringJoiner);
    }

    public Instruction conditionalJump(String cond, String ifTrue, String ifFalse) {
        // br i1 <cond>, label <if-true>, label <if-false>
        StringJoiner stringJoiner = new StringJoiner(" ", TAB, "");
        stringJoiner.add(BR).add(I1).add(cond+COMMA).add(LABEL).add(ifTrue+COMMA).add(LABEL).add(ifFalse);
        return new Instruction(null, stringJoiner);
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
        FuncSymbol funcSymbol = (FuncSymbol) findSymbol(funcName);
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
        }
        return new AllocElement(tmp, instruction.toString());
    }

    // 添加库函数定义
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
        curSymbolTable.addSymbol(getint);
        curSymbolTable.addSymbol(putint);
        curSymbolTable.addSymbol(putch);
        // 加入库函数定义
        // declare i32 @getint()
        // declare void @putint(i32)
        // declare void @putch(i32)
        ret.add("declare i32 @getint()");
        ret.add("declare void @putint(i32)");
        ret.add("declare void @putch(i32)");
    }

    /**
     * 添加数组初始化值
     * @param stringJoiner 指令拼装者
     */
    public boolean initArr(ConstInitValNode constInitValNode, InitValNode initValNode, StringJoiner stringJoiner, int size, boolean isConst, String innerDim) {
        boolean allZeroFlag = true;
        ArrayList<Integer> initValList = new ArrayList<>();
        int initVal;
        if (isConst) {
            for (ConstInitValNode ele : constInitValNode.getConstInitValNodes()) {
                initVal = constAddExpHandler(ele.getConstExpNode().getAddExpNode());
                if (initVal != 0) {
                    allZeroFlag = false;
                }
                initValList.add(initVal);
            }
        }
        else {
            for (InitValNode ele : initValNode.getInitValNodes()) {
                initVal = constAddExpHandler(ele.getExpNode().getAddExpNode());
                if (initVal != 0) {
                    allZeroFlag = false;
                }
                initValList.add(initVal);
            }
        }
        // 如果全零,则用zeroinitializer
        if (allZeroFlag) {
            stringJoiner.add(ZEROINITIALIZER);
        }
        // 不是则一个个放入
        // @a = dso_local global [10 x i32] [i32 1, i32 2, i32 3, i32 0, i32 0, i32 0, i32 0, i32 0, i32 0, i32 0]
        else {
            StringJoiner initValSj = new StringJoiner(", ", "[", "]");
            for (int i=0; i<size; i++) {
                initValSj.add(I32 + " " + initValList.get(i).toString());
            }
            // 是二维的,要加上二维类型
            if (innerDim != null) {
                stringJoiner.add(innerDim + " " + initValSj);
            }
            else {
                stringJoiner.add(initValSj.toString());
            }
        }
        return allZeroFlag;
    }


    // 获取语法分析时生成的AST,遍历各个成分进行分析
    public void generate() {
        // 加入库函数
        addLibFunc();
        CompUnitNode compUnitNode = Parser.getPARSER().getCompUnitNode();
        List<DeclNode> declNodes = compUnitNode.getDeclNodes();
        for (DeclNode declNode : declNodes) {
            ret.add(globalDeclHandler(declNode).toString());
        }
        List<FuncDefNode> funcDefNodes = compUnitNode.getFuncDefNodes();
        for (FuncDefNode funcDefNode : funcDefNodes) {
            ret.add(funcDefHandler(funcDefNode).toString());
        }
        // 主函数中指令已经加入到ret中了
        MainFuncDefNode mainFuncDefNode = compUnitNode.getMainFuncDefNode();
        mainFuncHandler(mainFuncDefNode);
    }

    /**
     * 数组处理函数
     */
    public String arrHandler(StringJoiner stringJoiner, List<ConstExpNode> constExpNodes, int size, InitValNode initValNode, ConstInitValNode constInitValNode, boolean isConst) {
        // VarDef → Ident { '[' ConstExp ']' } | Ident { '[' ConstExp ']' } '=' InitVal
        // 计算各个维度维数

        int[] dim = {0, 0};
        String llvmType;
        for (int i=0; i<size; i++) {
            ConstExpNode constExpNode = constExpNodes.get(i);
            dim[i] = constAddExpHandler(constExpNode.getAddExpNode());
        }
        // 记录维度
        // 内层维度,一维即本身,二维是第二维的维度
        String innerDim = null;
        if (size == 1) {
            llvmType = "[" + dim[0] + " x " + I32 + "]";
        }
        else {
            innerDim = "[" + dim[1] + " x " + I32 + "]";
            llvmType = "[" + dim[0] + " x " + innerDim + "]";
        }
        // 指令拼接类型
        stringJoiner.add(llvmType);
        // 非全局常量,且无初值,默认全零
        if (!isConst && initValNode == null) {
            stringJoiner.add(ZEROINITIALIZER);
        }
        // 有初值
        else {
            // 一维
            if (size == 1) {
                initArr(constInitValNode, initValNode, stringJoiner, size, isConst, null);
            }
            // 二维
            else {
                boolean allZeroFlag = true;
                StringJoiner initValSj = new StringJoiner(", ", "[", "]");
                // 常量的初值表达式
                if (isConst) {
                    for (ConstInitValNode ele : constInitValNode.getConstInitValNodes()) {
                        boolean innerFlag = initArr(ele, null, initValSj, dim[1], true, innerDim);
                        if (!innerFlag) {
                            allZeroFlag = false;
                        }
                    }
                }
                // 变量的初值表达式
                else {
                    for (InitValNode ele : initValNode.getInitValNodes()) {
                        boolean innerFlag = initArr(null, ele, initValSj, dim[1], false, innerDim);
                        if (!innerFlag) {
                            allZeroFlag = false;
                        }
                    }
                }
                // 二维都全零
                // @b = dso_local global [10 x [20 x i32]] zeroinitializer
                if (allZeroFlag) {
                    stringJoiner.add(ZEROINITIALIZER);
                }
                // 非全零
                else {
                    stringJoiner.add(initValSj.toString());
                }
            }
        }
        // TODO 优化设置每一维的维度
        return llvmType;
    }



    /**
     * 处理全局变量的定义
     */
    public Instruction globalDeclHandler(DeclNode declNode) {
        VarDeclNode varDeclNode = declNode.getVarDeclNode();
        ConstDeclNode constDeclNode = declNode.getConstDeclNode();
        Instruction instruction = new Instruction(null, new StringJoiner("\n"));
        int constValue = 0;
        String llvmType;
        String name;
        int size;
        // 全局变量
        if (varDeclNode != null) {
            // @b = dso_local global i32 5
            List<VarDefNode> varDefNodes = varDeclNode.getVarDefNodes();
            for (VarDefNode varDefNode : varDefNodes) {
                // 遍历变量定义
                name = varDefNode.getIdentToken().getValue();
                size = varDefNode.getConstExpNodes().size();
                InitValNode initValNode = varDefNode.getInitValNode();
                // 用于拼接指令
                StringJoiner stringJoiner = new StringJoiner(" ", "", "");
                stringJoiner.add("@" + name).add("=").add("dso_local").add("global");
                // 非数组
                if (size == 0) {
                    // 查看是否有初值, 没有初始值的默认为0
                    constValue = 0;
                    llvmType = I32;
                    if (initValNode != null) {
                        AddExpNode addExpNode = varDefNode.getInitValNode().getExpNode().getAddExpNode();
                        constValue = constAddExpHandler(addExpNode);
                    }
                    // 记录初值
                    stringJoiner.add(I32).add(String.valueOf(constValue));
                }
                // 数组
                else {
                    llvmType = arrHandler(stringJoiner, varDefNode.getConstExpNodes(), size, initValNode, null, false);
                }
                // 定义完毕,设置定义变量llvmName
                defName = name;
                // 此时才记录进符号表
                Symbol symbol = findSymbol(name);
                defName = null;
                symbol.setLlvmName("@" + name);
                symbol.setLlvmType(llvmType);
                symbolIntegerHashMap.put(symbol, constValue);
                // 记录指令
                instruction.addInstruction(stringJoiner.toString());
            }
        }
        // 全局常量, 一定有初值
        else if (constDeclNode != null) {
            // ConstDef → Ident { '[' ConstExp ']' } '=' ConstInitVal
            // // @a = dso_local constant i32 5
            for (ConstDefNode constDefNode : constDeclNode.getConstDefNodes()) {
                // 遍历常量定义
                name = constDefNode.getIdentToken().getValue();
                size = constDefNode.getConstExpNodes().size();
                ConstInitValNode constInitValNode = constDefNode.getConstInitValNode();
                // 用于拼接指令
                StringJoiner stringJoiner = new StringJoiner(" ", "", "");
                stringJoiner.add("@" + name).add("=").add("dso_local").add("constant");
                // 非数组
                if (size == 0) {
                    constValue = constAddExpHandler(constInitValNode.getConstExpNode().getAddExpNode());
                    llvmType = I32;
                    // 记录初值
                    stringJoiner.add(I32).add(String.valueOf(constValue));
                }
                // 数组
                else {
                    llvmType = arrHandler(stringJoiner, constDefNode.getConstExpNodes(), size, null, constInitValNode, true);
                }
                // 定义完毕,设置定义变量llvmName
                defName = name;
                // 此时才记录进符号表
                Symbol symbol = findSymbol(name);
                defName = null;
                symbol.setLlvmName("@" + name);
                symbol.setLlvmType(llvmType);
                symbolIntegerHashMap.put(symbol, constValue);
                // 记录指令
                instruction.addInstruction(stringJoiner.toString());
            }
        }
        return instruction;
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
                Symbol symbol = findSymbol(lValNode.getIdentToken().getValue());
                return symbolIntegerHashMap.get(symbol);
            }
            else {
                // 是数组, 算其对应值
                return 0;
            }
        }
    }

    public Instruction funcDefHandler(FuncDefNode funcDefNode) {
        //  FuncDef → FuncType Ident '(' [FuncFParams] ')' Block
        // define dso_local i32 @aaa(i32 %0, i32 %1) {block} 有返回值
        // define dso_local void @ab() {block} 无返回值
        Instruction instruction = new Instruction(null, new StringJoiner("\n"));
        StringJoiner funcDef = new StringJoiner(" ", "", "");
        funcDef.add("define").add("dso_local");
        // 函数返回值类型
        FuncTypeNode funcTypeNode = funcDefNode.getFuncTypeNode();
        if (funcTypeNode.getToken().getType() == TokenType.VOIDTK) {
            funcDef.add("void");
        }
        else {
            funcDef.add(I32);
        }
        // 函数名
        String funcName = funcDefNode.getIdentToken().getValue();
        defName = funcName;
        FuncSymbol funcSymbol = (FuncSymbol) findSymbol(funcName);
        defName = null;
        List<FuncParamSymbol> funcParamSymbols = funcSymbol.getFuncParamSymbols();

        // 进入参数分析tmpRegisterNum需要重置,每次申请寄存器先++,从0开始,所以初始值置为-1
        tmpRegisterNum = -1;
        curLocalValTable = new LocalValTable();
        localValTables.add(curLocalValTable);
        // 进入参数表后,SymbolTable层数加1
        curSymbolTable = curSymbolTable.getChilds().get(getChildTableIndex());
        childTableIndex.push(0);

        // 函数参数
        FuncFParamsNode funcFParamsNode = funcDefNode.getFuncFParamsNode();
        // 记录参数定义指令
        StringJoiner funcFParams = new StringJoiner(", ", "(", ")");
        // 如果有参数
        if (funcFParamsNode != null) {
            //  FuncFParams → FuncFParam { ',' FuncFParam }
            List<FuncFParamNode> funcFParamNodes = funcFParamsNode.getFuncFParamNodes();
            // FuncFParam → BType Ident ['[' ']' { '[' ConstExp ']' }]
            // 记录定义参数的名字
            List<String> paramNames = new ArrayList<>();
            for (int i=0; i<funcFParamNodes.size(); i++) {
                FuncFParamNode funcFParamNode = funcFParamNodes.get(i);
                String paramType = getParamType(funcFParamNode);
                // 申请寄存器
                tmpRegisterNum++;
                String llvmName = "%" + tmpRegisterNum;
                paramNames.add(llvmName);
                // 记录局部变量
                curLocalValTable.addLocalVal(llvmName, paramType);
                funcFParams.add(paramType + " " + llvmName);
                // 此处不用记录参数,因为参数的值存储的寄存器还要在之后申请
                // 但是需要记录函数参数符号定义的类型,以便后续使用
                funcParamSymbols.get(i).setLlvmType(paramType);
            }
            // 函数本身占一个寄存器编号
            tmpRegisterNum++;
            funcDef.add("@" + funcName + funcFParams);
            funcDef.add(LBRACE);
            instruction.addInstruction(funcDef.toString());
            // 在分析函数体之前,需要先申请临时寄存器将函数的参数存储下来
            for (int i=0; i<funcFParamNodes.size(); i++) {
                FuncFParamNode funcFParamNode = funcFParamNodes.get(i);
                String sName = paramNames.get(i);
                //  %4 = alloca [3 x i32]*
                // store [3 x i32]* %2, [3 x i32]* * %4
                String paramType = getParamType(funcFParamNode);
                // 申请寄存器
                AllocElement allocAlloc = alloc(paramType);
                String dName = allocAlloc.getLlvmName();
                instruction.addInstruction(allocAlloc.getInstruction());
                String storeAlloc = store(paramType, sName, paramType + "*", dName);
                instruction.addInstruction(storeAlloc);
                // 记录到符号表中
                String funcFParamName = funcFParamNode.getIdentToken().getValue();
                defName = funcFParamName;
                findSymbol(funcFParamName).setLlvmName(dName);
                defName = null;
                // 记录到局部变量表中
                curLocalValTable.addLocalVal(dName, paramType);
            }
        }
        // 无参数,寄存器直接++
        else {
            tmpRegisterNum++;
            funcDef.add("@" + funcName + funcFParams);
            funcDef.add(LBRACE);
            instruction.addInstruction(funcDef.toString());
        }

        // 参数定义完,函数名已经可以使用
        funcSymbol.setLlvmName("@" + funcName);

        // 分析函数体
        List<BlockItemNode> blockItemNodes = funcDefNode.getBlockNode().getBlockItemNodes();
        for (BlockItemNode blockItemNode : blockItemNodes) {
            Instruction block = blockItemHandler(blockItemNode, false, false, false);
            // blockItem里的Stmt里的Block类型需要注意整合指令
            if (block.isBlock()) {
                block.unionNeedBack();
            }
            instruction.addInstruction(block.toString());
        }
        instruction.addInstruction(RBRACE);
        // 结束函数分析,SymbolTable回退,出栈当前符号表的孩子符号
        curSymbolTable = curSymbolTable.getParent();
        childTableIndex.pop();
        return instruction;
    }

    public void mainFuncHandler(MainFuncDefNode mainFuncDefNode) {
        // MainFuncDef → 'int' 'main' '(' ')' Block
        String mainFuncHeader = "define dso_local i32 @main()";
        ret.add(mainFuncHeader + LBRACE);
        List<BlockItemNode> blockItemNodes = mainFuncDefNode.getBlockNode().getBlockItemNodes();
        // 重置临时变量数字, main函数先占一个0,临时局部变量从1开始
        tmpRegisterNum = 0;
        curLocalValTable = new LocalValTable();
        localValTables.add(curLocalValTable);
        // 进入block层数++,main函数的SymbolTable是全局函数表中的子符号表的最后一个
        curSymbolTable = curSymbolTable.getChilds().get(getChildTableIndex());
        childTableIndex.push(0);
        Instruction blockItemInstruction;
        for (BlockItemNode blockItemNode : blockItemNodes) {
            // 主函数分析里得到的blockItem返回的指令集直接加入结果就好,没有回填的说法
            // 但是仍然可能存在无for的Block, 如 if-Block else-Block 和 纯 Block,所以需要聚合
            blockItemInstruction = blockItemHandler(blockItemNode, false, false, false);
            blockItemInstruction.unionNeedBack();
            ret.add(blockItemInstruction.toString());
        }
        // 退出block层数--,回退到全局符号表
        curSymbolTable = curSymbolTable.getParent();
        childTableIndex.pop();
        ret.add(RBRACE);
    }

    public Instruction blockItemHandler(BlockItemNode blockItemNode, boolean ifFlag, boolean elseFlag, boolean forFlag) {
        // 指令集合
        StringJoiner instructions = new StringJoiner("\n");
        Instruction instruction = new Instruction(null, instructions);
        //  BlockItem → Decl | Stmt
        DeclNode declNode = blockItemNode.getDeclNode();
        // Decl语句
        if (declNode != null) {
            VarDeclNode varDeclNode = declNode.getVarDeclNode();
            // 局部变量
            if (varDeclNode != null) {
                for (VarDefNode varDefNode : varDeclNode.getVarDefNodes()) {
                    //  VarDef → Ident { '[' ConstExp ']' } | Ident { '[' ConstExp ']' } '=' InitVal
                    // 非数组
                    if (varDefNode.getConstExpNodes().size() == 0) {
                        String name = varDefNode.getIdentToken().getValue();

                        // 申请空间,记录alloc指令
                        AllocElement alloc = alloc(I32);
                        instruction.addInstruction(alloc.getInstruction());
                        String dName = alloc.getLlvmName();

                        // 是否有初值
                        if (varDefNode.getInitValNode() != null) {
                            Instruction addExpInstruction = addExpHandler(varDefNode.getInitValNode().getExpNode().getAddExpNode(), true);
                            // 记录addExp分析的指令
                            instruction.addInstruction(addExpInstruction.toString());
                            String sName = addExpInstruction.getLlvmName();
                            // 记录store指令
                            instruction.addInstruction(store(I32, sName, I32POINT, dName));
                        }
                        // 标记defName
                        defName = name;
                        // 这时候才记录llvmName
                        Symbol symbol = findSymbol(name);
                        defName = null;
                        symbol.setLlvmName(dName);
                        symbol.setLlvmType(I32);
                    }
                    else {
                        // TODO 数组
                    }
                }
            }
            // 局部常量
            else {
                ConstDeclNode constDeclNode = declNode.getConstDeclNode();
                List<ConstDefNode> constDefNodes = constDeclNode.getConstDefNodes();
                // 遍历局部常量定义
                for (ConstDefNode constDefNode : constDefNodes) {
                    // ConstDef → Ident { '[' ConstExp ']' } '=' ConstInitVal
                    // 非数组
                    if (constDefNode.getConstExpNodes().size() == 0) {
                        String name = constDefNode.getIdentToken().getValue();

                        // 申请空间
                        AllocElement alloc = alloc(I32);
                        instruction.addInstruction(alloc.getInstruction());
                        String dName = alloc.getLlvmName();

                        // 记录addExp分析的指令
                        Instruction addExpInstruction = addExpHandler(constDefNode.getConstInitValNode().getConstExpNode().getAddExpNode(), true);
                        instruction.addInstruction(addExpInstruction.toString());
                        String sName = addExpInstruction.getLlvmName();

                        // 记录store指令
                        instruction.addInstruction(store(I32, sName, I32POINT, dName));

                        // 标记defName
                        defName = name;
                        Symbol symbol = findSymbol(name);
                        defName = null;
                        symbol.setLlvmName(dName);
                        symbol.setLlvmType(I32);
                    }
                    else {
                        // 数组
                    }
                }
            }
        }
        // Stmt语句
        else {
            StmtNode stmtNode = blockItemNode.getStmtNode();
            // 此处不能聚合为Block类型的,要预留嵌套回填,直接返回即可
            return stmtHandler(stmtNode, ifFlag, elseFlag, forFlag);
        }
        return instruction;
    }

    public Instruction stmtHandler(StmtNode stmtNode, boolean ifFlag, boolean elseFlag, boolean forFlag) {
        Instruction instruction = new Instruction(null, new StringJoiner("\n"));
        Instruction addExpInstruction;
        Integer popNum;
        String nextLabel;
        switch (stmtNode.getStmtType()) {
            case EXP:
                if (stmtNode.getExpNode() != null) {
                    // 有Exp则分析,没有直接过,不用管
                    return addExpHandler(stmtNode.getExpNode().getAddExpNode(), false);
                }
                break;
            case BLOCK:
                // 进入Block层数++,并入栈初始化子符号表的孩子序号
                curSymbolTable = curSymbolTable.getChilds().get(getChildTableIndex());
                childTableIndex.push(0);
                List<BlockItemNode> blockItemNodes = stmtNode.getBlockNode().getBlockItemNodes();
                Instruction blockInstruction = new Instruction(null, new StringJoiner("\n"));
                List<Instruction> blockInstructions = new ArrayList<>();
                StmtNode tmpStmt;
                for (BlockItemNode blockItemNode : blockItemNodes) {
                    // 遍历加入每一个BlockItem获得的指令集
                    blockInstructions.add(blockItemHandler(blockItemNode, ifFlag, elseFlag, forFlag));
                    // 如果是一个Block里的continue或break语句,那么后面的都不用再分析了
                    if ((tmpStmt = blockItemNode.getStmtNode()) != null) {
                        if (tmpStmt.getStmtType() == StmtNode.StmtType.CONTINUE || tmpStmt.getStmtType() == StmtNode.StmtType.BREAK) {
                            break;
                        }
                    }
                }
                // 退出Block层数--,出栈当前符号表的孩子序号
                curSymbolTable = curSymbolTable.getParent();
                childTableIndex.pop();
                blockInstruction.setNeedBackInstructions(blockInstructions);
                // 标记为Block,以便递归回填
                blockInstruction.setBlock(true);
                return blockInstruction;
            case ASSIGN:
                // LVal '=' Exp ';'

                // 分析Exp
               addExpInstruction = addExpHandler(stmtNode.getExpNode().getAddExpNode(), true);
               String sName = addExpInstruction.getLlvmName();
               instruction.addInstruction(addExpInstruction.toString());

                // 通过标识符名称找到其在中间代码中的名称(全局变量为@*,局部变量为%*)
                String name = stmtNode.getlValNode().getIdentToken().getValue();
                ValSymbol valSymbol = (ValSymbol) findSymbol(name);
                String dName = valSymbol.getLlvmName();
                int dimension = valSymbol.getDimension();
                if (dimension == 0) {
                    // 非数组
                    instruction.addInstruction(store(I32, sName, I32POINT, dName));
                }
                else {
                    // TODO 数组
                }
                break;
            case PRINT:
                //  'printf''('FormatString{','Exp}')'';'

                String str = stmtNode.getFormatStringToken().getValue();
                List<ExpNode> expNodes = stmtNode.getExpNodes();
                int expIndex = 0;
                // 跳过开头和结尾的'"'
                for (int i=1; i<str.length()-1; i++) {
                    char curChar = str.charAt(i);
                    if (curChar == '%') {
                        // 占位符输出,输出Exp对应的值或临时变量的符号
                        addExpInstruction = addExpHandler(expNodes.get(expIndex++).getAddExpNode(), false);
                        instruction.addInstruction(addExpInstruction.toString());
                        String argVal = addExpInstruction.getLlvmName();
                        List<String> args = new ArrayList<>(){{
                           add(argVal);
                        }};
                        instruction.addInstruction(callFunc("putint", args).getInstruction());
                        i++;
                    }
                    else if (curChar == '\\') {
                        // 换行符输出
                        int ch = '\n';
                        String argVal = String.valueOf(ch);
                        List<String> args = new ArrayList<>(){{
                            add(argVal);
                        }};
                        instruction.addInstruction(callFunc("putch", args).getInstruction());
                        i++;
                    }
                    else {
                        // 普通字符输出
                        String argVal = String.valueOf((int) curChar);
                        List<String> args = new ArrayList<>(){{
                            add(argVal);
                        }};
                        instruction.addInstruction(callFunc("putch", args).getInstruction());
                    }
                }
                break;
            case CONTINUE:
                // 查看在哪种Block里,打上标记
                if (ifFlag) {
                    ifJump.pop();
                    ifJump.push(true);
                }
                if (elseFlag) {
                    elseJump.pop();
                    elseJump.push(true);
                }
                if (forFlag) {
                    forJump.pop();
                    forJump.push(true);
                }
                // 需要回填,返回的是对象,可以之后回填,打上标记即可
                continueInstructions.push(instruction);
                popNum = needContinues.pop();
                popNum += 1;
                needContinues.push(popNum);
                return instruction;
            case BREAK:
                if (ifFlag) {
                    ifJump.pop();
                    ifJump.push(true);
                }
                if (elseFlag) {
                    elseJump.pop();
                    elseJump.push(true);
                }
                if (forFlag) {
                    forJump.pop();
                    forJump.push(true);
                }
                // 需要回填,打上标记
                breakInstructions.push(instruction);
                popNum = needBreaks.pop();
                popNum += 1;
                needBreaks.push(popNum);
                return instruction;
            case IF:
                //  'if' '(' Cond ')' Stmt [ 'else' Stmt ]

                // 记录条件类型,以便条件表达式处理子程序进行跳转
                condTypes.push(CondType.IF);

                // Cond, ifCond是不用直接跳转的,按顺序分析即可
                List<Instruction> ifCondInstructions = lOrExpHandler(stmtNode.getCondNode().getlOrExpNode());

                // ifBody
                ifJump.push(false);
                String ifBodyLabel = allocLabel();
                Instruction ifBodyInstructions = stmtHandler(stmtNode.getStmtNodes().get(0), true, false, false);
                // 若是Block类型,标记;此时ifBodyInstructions本身已经有标记
                boolean ifBodyB = ifBodyInstructions.isBlock();
                // 此处若是Block类型,其所有指令存储在ifBodyInstructions.needBackInstructions中

                // elseBody
                Instruction elseBodyInstructions = null;
                String elseBodyLabel = null;
                boolean elseBodyB = false;
                if (stmtNode.getElesToken() != null) {
                    elseJump.push(false);
                    elseBodyLabel= allocLabel();
                    elseBodyInstructions = stmtHandler(stmtNode.getStmtNodes().get(1), false, true, false);
                    // 若是Block类型,标记,之后聚合
                    if (elseBodyInstructions.isBlock()) {
                        elseBodyB = true;
                    }
                }

                // nextLabel, 加上Label即可,下一条语句stmt会按它自己的处理执行
                nextLabel = allocLabel();

                // 进行回填
                // ifCond语句里的跳转语句回填,根据标记好的语句类型进行回填
                for (Instruction ifCondInstruction : ifCondInstructions) {
                    String nextLAndLabel = ifCondInstruction.getNextLAndLabel();
                    String nextLOrLabel = ifCondInstruction.getNextLOrLabel();
                    String cond = ifCondInstruction.getLlvmName();
                    switch (ifCondInstruction.getConditionJumpType()) {
                        case IAN -> ifCondInstruction.addInstruction(conditionalJump(cond, nextLAndLabel, ifBodyLabel).toString());
                        case IAO, IEAO -> ifCondInstruction.addInstruction(conditionalJump(cond, nextLAndLabel, nextLOrLabel).toString());
                        case IBN -> ifCondInstruction.addInstruction(conditionalJump(cond, ifBodyLabel, nextLabel).toString());
                        case IBO, IEBO -> ifCondInstruction.addInstruction(conditionalJump(cond, ifBodyLabel, nextLOrLabel).toString());
                        case IEAE -> ifCondInstruction.addInstruction(conditionalJump(cond, nextLAndLabel, elseBodyLabel).toString());
                    }
                }
                // 如果当前if语句已经有跳转语句,不用回填
                if (!ifJump.pop()) {
                    // ifBody最后一句是跳转语句, 需要回填
                    // 如果是Block类型,那么需要等待之后回填
                    if (ifBodyB) {
                        ifBodyInstructions.addNeedBackInstruction(unconditionalJump(nextLabel));
                    }
                    // 否则直接加入指令集即可
                    else {
                        ifBodyInstructions.addInstruction(unconditionalJump(nextLabel).toString());
                    }
                }
                // elseBody最后一条语句也是跳转语句,如果有else,需要回填
                if (elseBodyLabel != null) {
                    // 同理,else有直接跳转也不用回填
                    if (!elseJump.pop()) {
                        // 如果是Block类型,需要等待之后回填
                        if (elseBodyB) {
                            elseBodyInstructions.addNeedBackInstruction(unconditionalJump(nextLabel));
                        }
                        // 否则直接加入指令集即可
                        else {
                            elseBodyInstructions.addInstruction(unconditionalJump(nextLabel).toString());
                        }
                    }
                }

                // 统合if(-else)结构的所有指令
                // 如果ifBody与elseBody有Block类型,那么此指令也应设置为Block类型以便回填
                if (ifBodyB || elseBodyB) {
                    instruction.setBlock(true);
                    instruction.setNeedBackInstructions(new ArrayList<>());
                }
                // ifCond
                for (Instruction ifCondInstruction : ifCondInstructions) {
                    // 添加基本块标签
                    instruction.addInstruction(ifCondInstruction.toString());
                    // 如果有下一个条件判断的基本块,那么加入指令
                    if (ifCondInstruction.getNextLabel() != null) {
                        instruction.addInstruction(addLabel(ifCondInstruction.getNextLabel()).toString());
                    }
                }

                // ifBody
                instruction.addInstruction(addLabel(ifBodyLabel).toString());
                // 如果是Block类型(ifBody和elseBody任何一个),需要留着回填
                if (instruction.isBlock()) {
                    instruction.addNeedBackInstruction(ifBodyInstructions);
                }
                else {
                    instruction.addInstruction(ifBodyInstructions.toString());
                }

                // elseBody
                if (elseBodyLabel != null) {
                    // 如果是Block类型,需要留着回填
                    if (instruction.isBlock()) {
                        instruction.addNeedBackInstruction(addLabel(elseBodyLabel));
                        instruction.addNeedBackInstruction(elseBodyInstructions);
                    }
                    else {
                        instruction.addInstruction(addLabel(elseBodyLabel).toString());
                        instruction.addInstruction(elseBodyInstructions.toString());
                    }
                }

                // 肯定有下一条语句,就算是void函数,也有ret void
                // 注意,因为顺序是 ifCond -> ifBody -> {elseBody} -> nextLabel
                // 所以如果ifBody和elseBody语句需要回填,那么nextLabel也应该回填
                if (instruction.isBlock()) {
                    instruction.addNeedBackInstruction(addLabel(nextLabel));
                }
                else {
                    instruction.addInstruction(addLabel(nextLabel).toString());
                }

                // 分析完成,弹出条件类型
                condTypes.pop();

                break;

            case FOR:
                // 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt

                // 记录条件类型,以便条件表达式处理子程序进行跳转
                condTypes.push(CondType.FOR);
                // 假设不用continue和break回填
                needContinues.push(0);
                needBreaks.push(0);
                // 假设需要回填的数量为0

                // ForStmtOne,初始化语句
                if (stmtNode.getForStmtOne() != null) {
                    // 初始化语句直接添加即可
                    instruction.addInstruction(forStmtHandler(stmtNode.getForStmtOne()).getInstructions().toString());
                }
                // forCond,条件判断语句,不一定有,如果没有,应该直接跳转到forBody
                List<Instruction> forCondInstructions = null;
                String forCondLabel = null;
                if (stmtNode.getCondNode() != null) {
                    forCondLabel = allocLabel();
                    forCondInstructions = lOrExpHandler(stmtNode.getCondNode().getlOrExpNode());
                }

                // forBody, 循环体语句
                String forBodyLabel = allocLabel();
                StmtNode forBodyStmt = stmtNode.getStmtNode();
                Instruction forBodyInstruction;
                forJump.push(false);
                // 直接为跳转语句非Block
                if (forBodyStmt.getStmtType() == StmtNode.StmtType.CONTINUE || forBodyStmt.getStmtType() == StmtNode.StmtType.BREAK) {
                    forBodyInstruction = stmtHandler(forBodyStmt, false, false, true);
                }
                // Block
                else if (forBodyStmt.getStmtType() == StmtNode.StmtType.BLOCK) {
                    // 进行Block分析时, Block → '{' { BlockItem } '}', BlockItem → Decl | Stmt
                    // Decl语句不必管 主要是Stmt里的Continue和Break语句会导致回填;
                    // 另外,当Stmt语句再导出Block时,若还是在同一层for循环里,里面的Continue和Break语句也会导致回填
                    // 因此Block进行分析时,里面的每一个BlockItem的指令集合都要进行保留,留待回到for循环回填完毕再统合
                    forBodyInstruction = stmtHandler(forBodyStmt, false, false, true);
                }
                // 除了跳转语句和Block以外的语句
                else {
                    forBodyInstruction = stmtHandler(forBodyStmt, false, false, true);
                }

                // forStep, 步长语句
                Instruction forStepInstruction = null;
                String forStepLabel = null;
                if (stmtNode.getForStmtTwo() != null) {
                    forStepLabel = allocLabel();
                    forStepInstruction = forStmtHandler(stmtNode.getForStmtTwo());
                }

                // nextLabel
                nextLabel = allocLabel();

                // 进行回填操作
                // forCond如果有的话, 对每一个跳转语句进行回填
                if (forCondLabel != null) {
                    // 遍历forCondInstruction的所有跳转语句,进行回填
                    for (Instruction forCondInstruction : forCondInstructions) {
                       // 根据eqExp的类型,进行跳转语句的回填
                        String nextLAndLabel = forCondInstruction.getNextLAndLabel();
                        String nextLOrLabel = forCondInstruction.getNextLOrLabel();
                        String cond = forCondInstruction.getLlvmName();
                        switch (forCondInstruction.getConditionJumpType()) {
                            case FAN -> forCondInstruction.addInstruction(conditionalJump(cond, nextLAndLabel, nextLabel).toString());
                            case FAO -> forCondInstruction.addInstruction(conditionalJump(cond, nextLAndLabel, nextLOrLabel).toString());
                            case FBN -> forCondInstruction.addInstruction(conditionalJump(cond, forBodyLabel, nextLabel).toString());
                            case FBO -> forCondInstruction.addInstruction(conditionalJump(cond, forBodyLabel, nextLOrLabel).toString());
                        }
                    }
                }

                // forStep, 最后一条语句是跳转语句,跳转到forCond(如果有),否则跳转到循环体,需要回填
                if (forStepLabel != null) {
                    forStepInstruction.addInstruction(unconditionalJump(Objects.requireNonNullElse(forCondLabel, forBodyLabel)).toString());
                }

                // 进行可能有的Continue和Break的回填
                // continue回填
                Integer pop;
                pop = needContinues.pop();
                while (pop != 0) {
                    // 需要回填,回填到forStep(如果有),否则回填到forCond(如果有),否则回填到forBody
                    Instruction needFillContinue = continueInstructions.pop();
                    if (forStepLabel != null) {
                        needFillContinue.addInstruction(unconditionalJump(forStepLabel).toString());
                    }
                    else {
                        needFillContinue.addInstruction(unconditionalJump(Objects.requireNonNullElse(forCondLabel, forBodyLabel)).toString());
                    }
                    // 更新pop
                    pop--;
                }
                // break回填
                pop = needBreaks.pop();
                while (pop != 0) {
                    // 需要回填,回填到nextLabel
                    Instruction needFillBreak = breakInstructions.pop();
                    // 回填到nextLabel
                    needFillBreak.addInstruction(unconditionalJump(nextLabel).toString());
                    // 更新pop
                    pop--;
                }

                // 除了forBody外语句回填完毕,现在统合可能为Block类型的循环体
                if (forBodyInstruction.isBlock()) {
                    // 其内嵌套所有可能的回填,并在此一次性解决for循环块内的回填
                    forBodyInstruction.unionNeedBack();
                }

                // forBody, 最后一条语句是跳转语句,需要回填
                // 因为可能是Block类型的Stmt,所以需要在其他回填结束后聚合再回填
                // 如果为直接跳转语句,那么不需要填额外的跳转语句; 否则需要填跳转语句
                if (!forJump.pop()) {
                    // 跳转到步长语句(如果有), 否则跳转到forCond(如果有),否则跳转到循环体
                    if (forStepLabel != null) {
                        forBodyInstruction.addInstruction(unconditionalJump(forStepLabel).toString());
                    }
                    else {
                        forBodyInstruction.addInstruction(unconditionalJump(Objects.requireNonNullElse(forCondLabel, forBodyLabel)).toString());
                    }
                }


                // 整合for循环的所有指令
                // 有条件判断的跳转条件判断,没有条件判断的直接跳转循环体
                instruction.addInstruction(unconditionalJump(Objects.requireNonNullElse(forCondLabel, forBodyLabel)).toString());
                // forCond
                if (forCondLabel != null) {
                    instruction.addInstruction(addLabel(forCondLabel).toString());
                    for (Instruction forCondInstruction : forCondInstructions) {
                        instruction.addInstruction(forCondInstruction.toString());
                        // 如果有下一个条件判断的基本块,那么加入指令
                        if (forCondInstruction.getNextLabel() != null) {
                            instruction.addInstruction(addLabel(forCondInstruction.getNextLabel()).toString());
                        }
                    }
                }
                // forBody
                instruction.addInstruction(addLabel(forBodyLabel).toString());
                instruction.addInstruction(forBodyInstruction.toString());
                // forStep
                if (forStepLabel != null) {
                    instruction.addInstruction(addLabel(forStepLabel).toString());
                    instruction.addInstruction(forStepInstruction.toString());
                }

                // nextLabel, 直接加入指令集合即可,不用管下一条语句的详细分析过程
                instruction.addInstruction(addLabel(nextLabel).toString());

                // 分析完成,弹出条件类型
                condTypes.pop();

                break;

            case GET:
                // LVal '=' 'getint''('')'
                AllocElement allocElement = callFunc("getint", null);
                String rVal = allocElement.getLlvmName();
                // 记录函数调用指令
                instruction.addInstruction(allocElement.getInstruction());
                // 通过标识符名称找到其在中间代码中的名称(全局变量为@*,局部变量为%*)
                name = stmtNode.getlValNode().getIdentToken().getValue();
                valSymbol = (ValSymbol) findSymbol(name);
                dName = valSymbol.getLlvmName();
                dimension = valSymbol.getDimension();
                // 非数组
                if (dimension == 0) {
                    // 记录store指令
                    instruction.addInstruction(store(I32, rVal, I32POINT, dName));
                }
                // TODO 数组
                else {
                }
                break;
            case RETURN:
                // 'return' [Exp] ';'
                if (stmtNode.getExpNode() != null) {
                    // 有返回值
                    addExpInstruction = addExpHandler(stmtNode.getExpNode().getAddExpNode(), true);
                    // 记录Exp指令
                    instruction.addInstruction(addExpInstruction.toString());
                    String retName = addExpInstruction.getLlvmName();
                    // 记录返回指令
                    instruction.addInstruction(TAB + "ret " + I32 + " " + retName);
                }
                else {
                    // 无返回值
                    instruction.addInstruction(TAB + "ret void");
                }
                break;
            default:
                break;
        }

        return instruction;
    }

    private Instruction forStmtHandler(ForStmtNode forStmtOne) {
        // ForStmt → LVal '=' Exp
        Instruction addExpInstruction = addExpHandler(forStmtOne.getExpNode().getAddExpNode(), true);
        String rVal = addExpInstruction.getLlvmName();
        // 记录Exp的指令
        Instruction instruction = new Instruction(null, new StringJoiner("\n"));
        instruction.addInstruction(addExpInstruction.toString());

        // LVal类型
        String name = forStmtOne.getlValNode().getIdentToken().getValue();
        ValSymbol valSymbol = (ValSymbol) findSymbol(name);
        int dimension = valSymbol.getDimension();
        // 非数组
        if (dimension == 0) {
            // 记录store指令
            instruction.addInstruction(store(I32, rVal, I32POINT, valSymbol.getLlvmName()));
        }
        // 数组
        else {
            // 取出具体元素再赋值
        }
        return instruction;
    }

    public List<Instruction> lOrExpHandler(LOrExpNode lOrExpNode) {
        //  LOrExp → LAndExp | LOrExp '||' LAndExp
        // 递归取出所有的LAndExp,并进行条件运算
        Stack<LAndExpNode> lAndExpNodes = new Stack<>();
        LOrExpNode tmp = lOrExpNode;
        while (tmp != null) {
            // 从右到左放入LAndExp,最后一个放入的LAndExp实际上是从左到右第一个
            lAndExpNodes.push(tmp.getlAndExpNode());
            tmp = tmp.getlOrExpNode();
        }

        // 存放所有eqExp的指令集合
        List<Instruction> instructions = new ArrayList<>();

        // 顺序取出LAndExp调用LAndExp解析子程序进行解析
        while (!lAndExpNodes.isEmpty()) {
            LAndExpNode popLAnd = lAndExpNodes.pop();
            boolean hasNextLOr = !lAndExpNodes.isEmpty();
            // 获取LAnd分析得到的eqExp列表
            // (eqExp11, eqExp12, eqExp13...) (eqExp21, eqExp22, eqExp23...) ........
            //  -------LAndExp1------------   ----------LAndExp2----------   ........
            // 传入hasNextLOr标记,以便打上跳转语句类型
            List<Instruction> eqExpInstructions = lAndExpHandler(popLAnd, hasNextLOr);
            if (hasNextLOr) {
                // 申请Label
                String nextLOrLabel = allocLabel();
                // 遍历得到的指令集合,为其加上nextLOrLabel
                for (Instruction eqExpInstruction : eqExpInstructions) {
                    eqExpInstruction.setNextLOrLabel(nextLOrLabel);
                }
                // 为最后一个eqExp指令集设置其下一个基本块Label为nextLOrLabel
                eqExpInstructions.get(eqExpInstructions.size()-1).setNextLabel(nextLOrLabel);
            }
            // 加入所有eqExp的指令集合中
            instructions.addAll(eqExpInstructions);
        }
        return instructions;
    }

    public List<Instruction> lAndExpHandler(LAndExpNode lAndExpNode, boolean hasNextLOr) {
        //  LAndExp → EqExp | LAndExp '&&' EqExp

        // 从右到左取出所有EqExp
        Stack<EqExpNode> eqExpNodes = new Stack<>();
        LAndExpNode tmp = lAndExpNode;
        while (tmp != null) {
            eqExpNodes.push(tmp.getEqExpNode());
            tmp = tmp.getlAndExpNode();
        }

        // 存放一个LAndExp里所有的EqExp的指令集合
        List<Instruction> instructions = new ArrayList<>();

        // 从左到右按序取出EqExp,进行条件运算
        while (!eqExpNodes.isEmpty()) {
            Instruction eqExpInstruction = eqExpHandler(eqExpNodes.pop(), false);
            // 若有下一条LAndExp,需要进行Label申请,视情况进行跳转
            boolean hasNextLAnd = !eqExpNodes.isEmpty();
            if (hasNextLAnd) {
                // 申请Label
                String nextLAnd = allocLabel();
                eqExpInstruction.setNextLAndLabel(nextLAnd);
                // 为当前指令集记录下一个基本块的Label
                eqExpInstruction.setNextLabel(nextLAnd);
            }
            switch (condTypes.peek()) {
                case IF:
                    // 有下一条 LAnd
                    if (hasNextLAnd) {
                        if (hasNextLOr) {
                            eqExpInstruction.setConditionJumpType(Instruction.ConditionJumpType.IAO);
                        }
                        else {
                            eqExpInstruction.setConditionJumpType(Instruction.ConditionJumpType.IAN);
                        }
                    }
                    // 最后一条LAnd
                    else {
                        if (hasNextLOr) {
                            eqExpInstruction.setConditionJumpType(Instruction.ConditionJumpType.IBO);
                        }
                        else {
                            eqExpInstruction.setConditionJumpType(Instruction.ConditionJumpType.IBN);
                        }
                    }
                    break;
                case IF_ELSE:
                    // 有下一条 LAnd
                    if (hasNextLAnd) {
                        if (hasNextLOr) {
                            eqExpInstruction.setConditionJumpType(Instruction.ConditionJumpType.IEAO);
                        }
                        else {
                            eqExpInstruction.setConditionJumpType(Instruction.ConditionJumpType.IEAE);
                        }
                    }
                    // 最后一条LAnd
                    else {
                        if (hasNextLOr) {
                            eqExpInstruction.setConditionJumpType(Instruction.ConditionJumpType.IEBO);
                        }
                        else {
                            eqExpInstruction.setConditionJumpType(Instruction.ConditionJumpType.IEBE);
                        }
                    }
                    break;
                case FOR:
                    // 有下一条LAnd
                    if (hasNextLAnd) {
                        if (hasNextLOr) {
                            eqExpInstruction.setConditionJumpType(Instruction.ConditionJumpType.FAO);
                        }
                        else {
                            eqExpInstruction.setConditionJumpType(Instruction.ConditionJumpType.FAN);
                        }
                    }
                    // 当前是最后一条LAnd
                    else {
                        if (hasNextLOr) {
                            eqExpInstruction.setConditionJumpType(Instruction.ConditionJumpType.FBO);
                        }
                        else {
                            eqExpInstruction.setConditionJumpType(Instruction.ConditionJumpType.FBN);
                        }
                    }
                    break;
            }
            instructions.add(eqExpInstruction);
        }
        return instructions;
    }

    /**
     * 解析EqExp子程序
     * @param hasNext 是否有下一个RelExp
     */
    public Instruction eqExpHandler(EqExpNode eqExpNode, boolean hasNext) {
        // EqExp → RelExp | EqExp ('==' | '!=') RelExp
        String val1 = null;
        StringJoiner instructions = new StringJoiner("\n");
        // 此处子程序构造的instruction
        Instruction instruction = new Instruction(null, instructions);
        if (eqExpNode.getEqExpNode() != null) {
            Instruction eqExpInstruction = eqExpHandler(eqExpNode.getEqExpNode(), true);
            instruction.addInstruction(eqExpInstruction.toString());
            val1 = eqExpInstruction.getLlvmName();
        }
        Instruction relExpInstruction = relExpHandler(eqExpNode.getRelExpNode());
        String val2 = relExpInstruction.getLlvmName();
        instruction.addInstruction(relExpInstruction.toString());
        if (val1 != null) {
            relCal(instruction, val1, val2, eqExpNode.getOpToken().getType());
        }
        // 有下一个RelExp或者传来的类型为I1,说明已经进行过关系运算或正要进行关系运算
        else if (hasNext || Objects.equals(curLocalValTable.getLocalValType(val2), I1)) {
            instruction.setLlvmName(val2);
        }
        else {
            relCal(instruction, val2, "0", TokenType.NEQ);
        }
        return instruction;
    }

    public Instruction relExpHandler(RelExpNode relExpNode) {
        // RelExp → AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp
        String val1 = null;
        StringJoiner instructions = new StringJoiner("\n");
        Instruction instruction = new Instruction(null, instructions);
        if (relExpNode.getRelExpNode() != null) {
            Instruction relExpInstruction = relExpHandler(relExpNode.getRelExpNode());
            val1 = relExpInstruction.getLlvmName();
            instruction.addInstruction(relExpInstruction.toString());
        }
        Instruction addExpInstruction = addExpHandler(relExpNode.getAddExpNode(), false);
        String val2 = addExpInstruction.getLlvmName();
        instruction.addInstruction(addExpInstruction.toString());
        if (val1 != null) {
            relCal(instruction, val1, val2, relExpNode.getOpToken().getType());
        }
        else {
            instruction.setLlvmName(val2);
        }
        return instruction;
    }

    /**
     * 检查参数类型是否需要转换
     */
    public String checkZext(Instruction instruction, String val) {
        if (Objects.equals(curLocalValTable.getLocalValType(val), I1)) {
            AllocElement zextAlloc = zext(val);
            val = zextAlloc.getLlvmName();
            instruction.addInstruction(zextAlloc.getInstruction());
        }
        return val;
    }

    public void relCal(Instruction instruction, String val1, String val2, TokenType opType) {
        String ret = val2;
        // 有之前的RelExp,说明需要进行关系运算
        if (val1 != null) {
            // 检查是否需要改变类型,val1和val2都有可能在Cond里用到,由unaryExp转来,所以要检查
            val1 = checkZext(instruction, val1);
            val2 = checkZext(instruction, val2);
            AllocElement icmpAlloc = icmp(opType, val1, val2);
            ret = icmpAlloc.getLlvmName();
            instruction.addInstruction(icmpAlloc.getInstruction());
        }
        instruction.setLlvmName(ret);
    }


    public Instruction addExpHandler(AddExpNode addExpNode, boolean needAssign) {
        //  AddExp → MulExp | AddExp ('+' | '−') MulExp
        String val1 = null;
        StringJoiner instructions = new StringJoiner("\n");
        Instruction instruction = new Instruction(null, instructions);
        if (addExpNode.getAddExpNode() != null) {
            Instruction addExpInstruction = addExpHandler(addExpNode.getAddExpNode(), needAssign);
            val1 = addExpInstruction.getLlvmName();
            instruction.addInstruction(addExpInstruction.toString());
        }
        Instruction mulExpInstruction = mulExpHandler(addExpNode.getMulExpNode(), needAssign);
        String val2 = mulExpInstruction.getLlvmName();
        instruction.addInstruction(mulExpInstruction.toString());
        if (val1 != null) {
            addCal(instruction, val1, val2, addExpNode.getOpToken().getType());
        }
        else {
            instruction.setLlvmName(val2);
        }
        return instruction;
    }

    public Instruction mulExpHandler(MulExpNode mulExpNode, boolean needAssign) {
        // MulExp → UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
        String val1 = null;
        StringJoiner instructions = new StringJoiner("\n");
        Instruction instruction = new Instruction(null, instructions);
        if (mulExpNode.getMulExpNode() != null) {
            Instruction mulExpInstruction = mulExpHandler(mulExpNode.getMulExpNode(), needAssign);
            val1 = mulExpInstruction.getLlvmName();
            instruction.addInstruction(mulExpInstruction.toString());
        }
        Instruction unaryExpInstruction = unaryExpHandler(mulExpNode.getUnaryExpNode(), needAssign);
        String val2 = unaryExpInstruction.getLlvmName();
        instruction.addInstruction(unaryExpInstruction.toString());
        if (val1 != null) {
            addCal(instruction, val1, val2, mulExpNode.getOpToken().getType());
        }
        else {
            instruction.setLlvmName(val2);
        }
        return instruction;
    }

    public void addCal(Instruction instruction, String val1, String val2, TokenType opType) {
        String ret;
        val1 = checkZext(instruction, val1);
        val2 = checkZext(instruction, val2);
        AllocElement allocElement = calPrint(I32, val1, val2, opType);
        ret = allocElement.getLlvmName();
        instruction.addInstruction(allocElement.getInstruction());
        instruction.setLlvmName(ret);
    }


    public Instruction unaryExpHandler(UnaryExpNode unaryExpNode, boolean needAssign) {
        Instruction instruction = new Instruction(null, new StringJoiner("\n"));
        //  UnaryExp → PrimaryExp | Ident '(' [FuncRParams] ')' | UnaryOp UnaryExp
        if (unaryExpNode.getPrimaryExpNode() != null) {
            Instruction primaryExpInstruction= primaryExpHandler(unaryExpNode.getPrimaryExpNode(), needAssign);
            instruction.addInstruction(primaryExpInstruction.toString());
            instruction.setLlvmName(primaryExpInstruction.getLlvmName());
        }
        else if (unaryExpNode.getUnaryOpNode() != null) {
            // UnaryOp → '+' | '−' | '!'
            // 先把UnaryExp的值得出来
            // 记录运算符
            Stack<String> opStack = new Stack<>();
            UnaryExpNode tmp = unaryExpNode;
            while (tmp.getUnaryOpNode() != null) {
                opStack.push(tmp.getUnaryOpNode().getOpToken().getValue());
                tmp = tmp.getUnaryExpNode();
            }
            // 跳出来说明当前tmp不带有unaryOp,递归调用处理
            Instruction unaryExpInstruction = unaryExpHandler(tmp, needAssign);
            instruction.addInstruction(unaryExpInstruction.toString());
            String rVal = unaryExpInstruction.getLlvmName();
            // 进行运算
            while (!opStack.isEmpty()) {
                String op = opStack.pop();
                // '-' 号 减法运算
                if (Objects.equals(op, "-")) {
                    AllocElement alloc = calPrint(I32, "0", rVal, TokenType.MINU);
                    instruction.addInstruction(alloc.getInstruction());
                    // 更新值
                    rVal = alloc.getLlvmName();
                }
                // ‘!’,非运算,出现在条件表达式
                else if (Objects.equals(op, "!")) {
                    // 记录非计算
                    Instruction notInstruction = not(rVal);
                    instruction.addInstruction(notInstruction.toString());
                    // 更新值
                    rVal = notInstruction.getLlvmName();
                }
                // 加法跳过
            }
            // 设置返回值
            instruction.setLlvmName(rVal);
        }
        else {
            // 函数调用  Ident '(' [FuncRParams] ')'
            List<String> args = null;
            String funcName = unaryExpNode.getIdentToken().getValue();
            // 检查是否有参数
            FuncRParamsNode funcRParamsNode = unaryExpNode.getFuncRParamsNode();
            if (funcRParamsNode != null) {
                args = new ArrayList<>();
                for (ExpNode expNode : funcRParamsNode.getExpNodes()) {
                    Instruction addExpInstruction = addExpHandler(expNode.getAddExpNode(), true);
                    instruction.addInstruction(addExpInstruction.toString());
                    args.add(addExpInstruction.getLlvmName());
                }
            }
            // 无论需不需要赋值,调用有返回值函数都会存储其值
            AllocElement alloc = callFunc(funcName, args);
            instruction.addInstruction(alloc.getInstruction());
            instruction.setLlvmName(alloc.getLlvmName());
        }
        return instruction;
    }

    public Instruction primaryExpHandler(PrimaryExpNode primaryExpNode, boolean needAssign) {
        // PrimaryExp → '(' Exp ')' | Number | LVal
        if (primaryExpNode.getExpNode() != null) {
            return addExpHandler(primaryExpNode.getExpNode().getAddExpNode(), needAssign);
        }
        else if (primaryExpNode.getNumberNode() != null) {
            // 是常数,直接返回
            String llvmName = primaryExpNode.getNumberNode().getIntConstToken().getValue();
            return new Instruction(llvmName, null);
            // TODO 检查调用addExp 的 needAssign
        }
        else {
            // LVal
            // 或许可以直接返回符号对应的值
            // 找到其对应标号,进行load
            String name = primaryExpNode.getlValNode().getIdentToken().getValue();
            // 可能是ValSymbol或FuncParamSymbol
            Symbol symbol = findSymbol(name);
            int dim;
            if (symbol instanceof ValSymbol) {
                dim = ((ValSymbol) symbol).getDimension();
            }
            else {
                dim = ((FuncParamSymbol) symbol).getDimension();
            }
            String llvmName = symbol.getLlvmName();
            Instruction instruction = new Instruction(null, new StringJoiner("\n"));
            if (dim == 0) {
                // 非数组
                AllocElement alloc = load(I32, llvmName);
                String allocLlvmName = alloc.getLlvmName();
                instruction.addInstruction(alloc.getInstruction());
                instruction.setLlvmName(allocLlvmName);
                return instruction;
            }
            else {
                // 数组
                return null;
            }
        }
    }

}
