package llvm;

import frontend.Parser;
import node.*;
import symbol.FuncParamSymbol;
import symbol.FuncSymbol;
import symbol.Symbol;
import symbol.ValSymbol;
import token.TokenType;

import java.util.*;

import static llvm.Utils.conditionalJump;
import static llvm.Utils.unconditionalJump;

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
    private static final String I1 = "i1";
    private static final String I32POINT = "i32*";

    private static final String ZEROINITIALIZER = "zeroinitializer";

    private static final String LBRACE = "{";
    private static final String RBRACE = "}";

    private static final String TAB = "    ";

    /**
     * BackFill类实例
     */
    private static final BackFill BACK_FILL = BackFill.getBackFill();
    /**
     * Utils工具类实例
     */
    private static final Utils UTILS = Utils.getUTILS();
    /**
     * Jump工具类实例
     */
    private static final Jump JUMP = Jump.getJUMP();
    private StringJoiner ret;

    private static final Generator GENERATOR = new Generator();

    public static Generator getGenerator() {
        return GENERATOR;
    }

    public String getRet() {
        return ret.toString();
    }

    /**
     * 初始化
     */
    public void init() {
        this.ret = new StringJoiner("\n");
        this.condTypes = new Stack<>();
        // 初始化工具类
        UTILS.init();
        BACK_FILL.init();
        JUMP.init();
    }



    /**
     * 初始化全局数组
     * @param constInitValNode 用于初始化的常量初始值
     * @param initValNode 用于初始化的变量初始值
     * @param stringJoiner 指令拼装者
     * @param isConst 是否常量
     * @param innerDim 二维数组的第二位维数表示,若是一维数组则为null
     */
    public boolean initGlobalArr(ConstInitValNode constInitValNode, InitValNode initValNode, StringJoiner stringJoiner, boolean isConst, String innerDim, List<Integer> constValues) {
        boolean allZeroFlag = true;
        ArrayList<Integer> initValList = new ArrayList<>();
        int size;
        int initVal;
        if (isConst) {
            List<ConstInitValNode> constInitValNodes = constInitValNode.getConstInitValNodes();
            size = constInitValNodes.size();
            for (ConstInitValNode ele : constInitValNodes) {
                initVal = UTILS.constAddExpHandler(ele.getConstExpNode().getAddExpNode());
                // 变量才记录
                constValues.add(initVal);
                if (initVal != 0) {
                    allZeroFlag = false;
                }
                initValList.add(initVal);
            }
        }
        else {
            List<InitValNode> initValNodes = initValNode.getInitValNodes();
            size = initValNodes.size();
            for (InitValNode ele : initValNodes) {
                initVal = UTILS.constAddExpHandler(ele.getExpNode().getAddExpNode());
                if (initVal != 0) {
                    allZeroFlag = false;
                }
                initValList.add(initVal);
            }
        }
        // 如果全零,则用zeroinitializer
        if (allZeroFlag) {
            if (innerDim != null) {
                stringJoiner.add(innerDim + " " + ZEROINITIALIZER);
            }
            else {
                stringJoiner.add(ZEROINITIALIZER);
            }
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


    /**
     * 获取语法分析时生成的AST,遍历各个成分进行分析
     */
    public void generate() {
        // 加入库函数定义
        // declare i32 @getint() ; declare void @putint(i32) ; declare void @putch(i32)
        ret.add("declare i32 @getint()");
        ret.add("declare void @putint(i32)");
        ret.add("declare void @putch(i32)");

        CompUnitNode compUnitNode = Parser.getPARSER().getCompUnitNode();
        List<DeclNode> declNodes = compUnitNode.getDeclNodes();
        for (DeclNode declNode : declNodes) {
            ret.add(globalDeclHandler(declNode).toString());
        }
        ret.add("");
        List<FuncDefNode> funcDefNodes = compUnitNode.getFuncDefNodes();
        for (FuncDefNode funcDefNode : funcDefNodes) {
            ret.add(funcDefHandler(funcDefNode).toString());
            ret.add("");
        }
        // 主函数中指令已经加入到ret中了
        MainFuncDefNode mainFuncDefNode = compUnitNode.getMainFuncDefNode();
        mainFuncHandler(mainFuncDefNode);
    }

    /**
     * 处理数组维度
     * @param dimension 数组维数
     * @return ret[innerDim, llvmType]
     */
    public String[] dimHandler(String[] dim, int dimension, List<ConstExpNode> constExpNodes) {
        // 计算各个维度维数
        String llvmType;
        for (int i=0; i<dimension; i++) {
            ConstExpNode constExpNode = constExpNodes.get(i);
            dim[i] = String.valueOf(UTILS.constAddExpHandler(constExpNode.getAddExpNode()));
        }
        // 记录维度
        // 内层维度,一维即本身,二维是第二维的维度
        String innerDim = null;
        if (dimension == 1) {
            llvmType = "[" + dim[0] + " x " + I32 + "]";
        }
        else {
            innerDim = "[" + dim[1] + " x " + I32 + "]";
            llvmType = "[" + dim[0] + " x " + innerDim + "]";
        }
        return new String[]{innerDim, llvmType};
    }

    /**
     * 获取数组元素
     * @param arrInfo 封装了数组信息的对象
     * @param dim 要取的维度的值
     */
    public Instruction getElementPtr(ArrInfo arrInfo, String[] dim) {
        Instruction instruction = new Instruction(null, new StringJoiner("\n"));
        String[] arrSymbolInfo = arrInfo.getArrSymbolInfo();
        int[] dimensionInfo = arrInfo.getDimensionInfo();
        String innerDim = arrSymbolInfo[0], arrType = arrSymbolInfo[1], arrName = arrSymbolInfo[2];
        int dimension = dimensionInfo[0], getDimension = dimensionInfo[1];
        // %3 = getelementptr [5 x [7 x i32]], [5 x [7 x i32]]* @a, i32 0, i32 3, i32 4 二维数组取两维
        // %60 = getelementptr inbounds [2 x [2 x i32]], [2 x [2 x i32]]* %26, i64 0, i64 0 二维数组取一维
        // 一维数组只能取一维
        // 取到的类型
        String getType;
        // 当取二维数组时,取到其innerDim的指针
        // 取二维数组的第一个维度还是第二个维度是一样的,无非是只改变dim[0]
        // 还是dim[0],dim[1]都改变,取到的都是I32POINT,一维数组也一样
        if (dimension == 2 && getDimension == 0) {
            getType = innerDim + "*";
        }
        else {
            getType = I32POINT;
        }

        // 注意,对于函数参数中的数组来说,由于不能使用**的形式,所以需要先load一次
        // %7 = load i32*, i32** %5, align 8
        // %8 = getelementptr inbounds i32, i32* %7, i64 0
        // %10 = load [2 x i32]*, [2 x i32]** %6, align 8
        // %11 = getelementptr inbounds [2 x i32], [2 x i32]* %10, i64 0
        // %12 = getelementptr inbounds [2 x i32], [2 x i32]* %11, i64 0, i64 0
        // 是否属于参数类型数组
        boolean isParamArr = arrType.contains("*");
        if (isParamArr) {
            AllocElement load = UTILS.load(arrType, arrName);
            instruction.addInstruction(load.getInstruction());
            // 用加载好的arrName替换
            arrName = load.getLlvmName();
            // 加载后去除*
            arrType = arrType.replace("*", "");
        }
        String tmp = UTILS.alloc(getType).getLlvmName();
        StringJoiner stringJoiner = new StringJoiner(", ");
        stringJoiner.add(TAB + tmp + " = getelementptr " + arrType);
        stringJoiner.add(arrType + "* " + arrName);
        // 对于参数类型数组来说,由于维度减少了,步长需要减少一步
        if (!isParamArr) {
            stringJoiner.add(I32 + " 0");
        }
        stringJoiner.add(I32 + " " + dim[0]);
        // 当取二维数组且不是取整个数组时
        if (dimension == 2 && getDimension != 0) {
            stringJoiner.add(I32 + " " + dim[1]);
        }
        instruction.addInstruction(stringJoiner.toString());
        instruction.setLlvmName(tmp);
        return instruction;
    }

    /**
     * 数组处理函数
     * @param dimension ConstExp数量,即数组维数
     * @return dimRet[innerDim, llvmType]
     */
    public String[] globalArrHandler(StringJoiner stringJoiner, List<ConstExpNode> constExpNodes, int dimension, InitValNode initValNode, ConstInitValNode constInitValNode, boolean isConst, String[] dim, List<Integer> constValues) {
        // VarDef → Ident { '[' ConstExp ']' } | Ident { '[' ConstExp ']' } '=' InitVal
        String[] dimRet = dimHandler(dim, dimension, constExpNodes);
        String innerDim = dimRet[0];
        String llvmType = dimRet[1];
        // 指令拼接类型
        stringJoiner.add(llvmType);
        // 非全局常量,且无初值,默认全零
        if (!isConst && initValNode == null) {
            stringJoiner.add(ZEROINITIALIZER);
        }
        // 有初值
        else {
            // 一维
            if (dimension == 1) {
                if (isConst) {
                    initGlobalArr(constInitValNode, initValNode, stringJoiner, true, null, constValues);
                }
                else {
                    initGlobalArr(constInitValNode, initValNode, stringJoiner, false, null, null);
                }
            }
            // 二维
            else {
                boolean allZeroFlag = true;
                StringJoiner initValSj = new StringJoiner(", ", "[", "]");
                // 常量的初值表达式
                if (isConst) {
                    for (ConstInitValNode ele : constInitValNode.getConstInitValNodes()) {
                        List<Integer> innerConstValues = new ArrayList<>();
                        boolean innerFlag = initGlobalArr(ele, null, initValSj, true, innerDim, innerConstValues);
                        if (!innerFlag) {
                            allZeroFlag = false;
                        }
                        constValues.addAll(innerConstValues);
                    }
                }
                // 变量的初值表达式
                else {
                    for (InitValNode ele : initValNode.getInitValNodes()) {
                        boolean innerFlag = initGlobalArr(null, ele, initValSj, false, innerDim, null);
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
        return dimRet;
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
        String llvmName;
        String name;
        int size;
        // 全局变量
        if (varDeclNode != null) {
            // @b = dso_local global i32 5
            List<VarDefNode> varDefNodes = varDeclNode.getVarDefNodes();
            for (VarDefNode varDefNode : varDefNodes) {
                // 遍历变量定义
                name = varDefNode.getIdentToken().getValue();
                llvmName = "@" + name;
                size = varDefNode.getConstExpNodes().size();
                InitValNode initValNode = varDefNode.getInitValNode();
                // 用于拼接指令
                StringJoiner stringJoiner = new StringJoiner(" ", "", "");
                stringJoiner.add(llvmName).add("=").add("dso_local").add("global");
                // 非数组
                if (size == 0) {
                    // 查看是否有初值, 没有初始值的默认为0
                    constValue = 0;
                    llvmType = I32;
                    if (initValNode != null) {
                        AddExpNode addExpNode = varDefNode.getInitValNode().getExpNode().getAddExpNode();
                        constValue = UTILS.constAddExpHandler(addExpNode);
                    }
                    // 记录初值
                    stringJoiner.add(I32).add(String.valueOf(constValue));
                }
                // 数组
                else {
                    String[] dim = {"0", "0"};
                    String[] dimRet = globalArrHandler(stringJoiner, varDefNode.getConstExpNodes(), size, initValNode, null, false, dim, null);
                    llvmType = dimRet[1];
                    ((ValSymbol) UTILS.findSymbol(name, name)).setInnerSize(Integer.parseInt(dim[1]));
                }
                // 定义完毕,记录符号表
                UTILS.setSymbolLLVMInfo(name, llvmType, llvmName, constValue, name);
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
                llvmName = "@" + name;
                size = constDefNode.getConstExpNodes().size();
                ConstInitValNode constInitValNode = constDefNode.getConstInitValNode();
                // 用于拼接指令
                StringJoiner stringJoiner = new StringJoiner(" ", "", "");
                stringJoiner.add(llvmName).add("=").add("dso_local").add("constant");
                List<Integer> constValues = null;
                // 非数组
                if (size == 0) {
                    constValue = UTILS.constAddExpHandler(constInitValNode.getConstExpNode().getAddExpNode());
                    llvmType = I32;
                    // 记录初值
                    stringJoiner.add(I32).add(String.valueOf(constValue));
                }
                // 数组
                else {
                    String[] dim = {"0", "0"};
                    constValues = new ArrayList<>();
                    String[] dimRet = globalArrHandler(stringJoiner, constDefNode.getConstExpNodes(), size, null, constInitValNode, true, dim, constValues);
                    llvmType = dimRet[1];
                    ((ValSymbol) UTILS.findSymbol(name, name)).setInnerSize(Integer.parseInt(dim[1]));
                }
                // 定义完毕,记录符号表
                UTILS.setSymbolLLVMInfo(name, llvmType, llvmName, constValue, name);
                UTILS.findSymbol(name, null).setConst();
                if (size != 0) {
                    UTILS.addConstArrVal(UTILS.findSymbol(name, null), constValues);
                }
                // 记录指令
                instruction.addInstruction(stringJoiner.toString());
            }
        }
        return instruction;
    }


    public Instruction funcDefHandler(FuncDefNode funcDefNode) {
        //  FuncDef → FuncType Ident '(' [FuncFParams] ')' Block
        // define dso_local i32 @aaa(i32 %0, i32 %1) {block} 有返回值
        // define dso_local void @ab() {block} 无返回值
        Instruction instruction = new Instruction(null, new StringJoiner("\n"));
        StringJoiner funcDef = new StringJoiner(" ", "", "");
        funcDef.add("define").add("dso_local");
        // 函数返回值类型
        TokenType funcType = funcDefNode.getFuncTypeNode().getToken().getType();
        if (funcType == TokenType.VOIDTK) {
            funcDef.add("void");
        }
        else {
            funcDef.add(I32);
        }
        // 函数名
        String funcName = funcDefNode.getIdentToken().getValue();
        FuncSymbol funcSymbol = (FuncSymbol) UTILS.findSymbol(funcName, funcName);
        List<FuncParamSymbol> funcParamSymbols = funcSymbol.getFuncParamSymbols();

        // 进入参数分析tmpRegisterNum需要重置,每次申请寄存器先++,从0开始,所以初始值置为-1
        UTILS.enterNewLocalArea();
        // 进入参数表后,SymbolTable层数加1
        UTILS.enterNextSymbolTable();

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
                String paramType = UTILS.getParamType(funcFParamNode);
                // 申请寄存器,在申请时已经存入局部变量表
                AllocElement alloc = UTILS.alloc(paramType);
                String llvmName = alloc.getLlvmName();
                paramNames.add(llvmName);
                funcFParams.add(paramType + " " + llvmName);
                // 此处不用记录参数寄存器编号(llvmName),因为参数的值存储的寄存器还要在之后申请
                // 但是需要记录函数参数符号定义的类型,以便后续使用
                funcParamSymbols.get(i).setLlvmType(paramType);
            }
            // 函数本身占一个寄存器编号
            UTILS.addRegisterNum();
            funcDef.add("@" + funcName + funcFParams);
            funcDef.add(LBRACE);
            instruction.addInstruction(funcDef.toString());
            // 在分析函数体之前,需要先申请临时寄存器将函数的参数存储下来
            for (int i=0; i<funcFParamNodes.size(); i++) {
                FuncFParamNode funcFParamNode = funcFParamNodes.get(i);
                String sName = paramNames.get(i);
                //  %4 = alloca [3 x i32]*
                // store [3 x i32]* %2, [3 x i32]* * %4
                String paramType = UTILS.getParamType(funcFParamNode);
                // 申请寄存器
                AllocElement allocAlloc = UTILS.alloc(paramType);
                String dName = allocAlloc.getLlvmName();
                instruction.addInstruction(allocAlloc.getInstruction());
                String storeAlloc = UTILS.store(paramType, sName, paramType + "*", dName);
                instruction.addInstruction(storeAlloc);
                // 记录到符号表中
                String funcFParamName = funcFParamNode.getIdentToken().getValue();
                // 标记参数load后临时变量的名字
                UTILS.findSymbol(funcFParamName, funcFParamName).setLlvmName(dName);
                // 记录到局部变量表中
                UTILS.addLocalVal(dName, paramType);
            }
        }
        // 无参数,寄存器直接++
        else {
            UTILS.addRegisterNum();
            funcDef.add("@" + funcName + funcFParams);
            funcDef.add(LBRACE);
            instruction.addInstruction(funcDef.toString());
        }

        // 参数定义完,函数名已经可以使用
        funcSymbol.setLlvmName("@" + funcName);

        // 分析函数体
        List<BlockItemNode> blockItemNodes = funcDefNode.getBlockNode().getBlockItemNodes();
        for (BlockItemNode blockItemNode : blockItemNodes) {
            Instruction block = blockItemHandler(blockItemNode, null);
            // blockItem里的Stmt里的Block类型需要注意整合指令
            if (block.isBlock()) {
                block.unionNeedBack();
            }
            instruction.addInstruction(block.toString());
        }
        // 如果void函数最后一句未显式给出返回语句,需要手动添加
        if (funcType == TokenType.VOIDTK) {
            int size = blockItemNodes.size();
            // void类型的空函数体,可优化,符号表进行标记,在之后遇到的地方忽略
            if (size == 0) {
                UTILS.findSymbol(funcName, null).ignore();
                UTILS.exitCurSymbolTable();
                // 返回空指令
                return new Instruction(null, new StringJoiner("\n"));
            }
            else {
                BlockItemNode lastBlockItem = blockItemNodes.get(size - 1);
                if (lastBlockItem.getStmtNode() == null || lastBlockItem.getStmtNode().getStmtType() != StmtNode.StmtType.RETURN) {
                    instruction.addInstruction(TAB + "ret void");
                }
            }
        }
        instruction.addInstruction(RBRACE);
        // 结束函数分析,SymbolTable回退,出栈当前符号表的孩子符号
        UTILS.exitCurSymbolTable();
        return instruction;
    }

    public void mainFuncHandler(MainFuncDefNode mainFuncDefNode) {
        // MainFuncDef → 'int' 'main' '(' ')' Block
        String mainFuncHeader = "define dso_local i32 @main()";
        ret.add(mainFuncHeader + LBRACE);
        List<BlockItemNode> blockItemNodes = mainFuncDefNode.getBlockNode().getBlockItemNodes();
        // 重置临时变量数字, main函数先占一个0,临时局部变量从1开始
        UTILS.enterNewLocalArea();
        UTILS.addRegisterNum();
        // 进入block层数++,main函数的SymbolTable是全局函数表中的子符号表的最后一个
        UTILS.enterNextSymbolTable();
        Instruction blockItemInstruction;
        for (BlockItemNode blockItemNode : blockItemNodes) {
            // 主函数分析里得到的blockItem返回的指令集直接加入结果就好,没有回填的说法
            // 但是仍然可能存在无for的Block, 如 if-Block else-Block 和 纯 Block,所以需要聚合
            blockItemInstruction = blockItemHandler(blockItemNode, null);
            blockItemInstruction.unionNeedBack();
            ret.add(blockItemInstruction.toString());
        }
        // 退出block层数--,回退到全局符号表
        UTILS.exitCurSymbolTable();
        ret.add(RBRACE);
    }

    /**
     * 初始化局部数组
     */
    public void initLocalArr(ConstInitValNode constInitValNode, InitValNode initValNode, Instruction instruction, ArrInfo arrInfo, String dimOne, String dimTwo, List<Integer> constValues) {
        // 获取初始值
        String sName;
        if (constInitValNode != null) {
            int constValue = UTILS.constAddExpHandler(constInitValNode.getConstExpNode().getAddExpNode());
            constValues.add(constValue);
            sName = String.valueOf(constValue);
        }
        else {
            Instruction addExpInstruction = addExpHandler(initValNode.getExpNode().getAddExpNode(), true);
            sName = addExpInstruction.getLlvmName();
            instruction.addInstruction(addExpInstruction.toString());
        }
        // 取出数组元素,对于数组初始化来说,要获取的维数和数组维数是一样的
        String[] dim = {dimOne, dimTwo};
        Instruction elementPtr = getElementPtr(arrInfo, dim);
        instruction.addInstruction(elementPtr.toString());
        String dName = elementPtr.getLlvmName();
        // 将初始值存入
        String store = UTILS.store(I32, sName, I32POINT, dName);
        instruction.addInstruction(store);
    }

    /**
     * 拼接数组函数调用所用信息对象
     * @return 函数信息对象
     */
    public ArrInfo getArrInfo(ConstDefNode constDefNode, VarDefNode varDefNode, Instruction instruction, int[] dimInt) {
        List<ConstExpNode> constExpNodes;
        // 定义数组维度的ConstExp
        if (constDefNode != null) {
            constExpNodes = constDefNode.getConstExpNodes();
        }
        else {
            constExpNodes = varDefNode.getConstExpNodes();
        }
        // 数组维数
        int size = constExpNodes.size();
        // 计算各个维度维数
        String[] dimStr = {"0", "0"};
        String[] dimRet = dimHandler(dimStr, size, constExpNodes);
        String innerDim = dimRet[0];
        String llvmType = dimRet[1];

        dimInt[0] = Integer.parseInt(dimStr[0]);
        dimInt[1] = Integer.parseInt(dimStr[1]);

        // 申请局部变量
        AllocElement alloc = UTILS.alloc(llvmType);
        String llvmName = alloc.getLlvmName();
        instruction.addInstruction(alloc.getInstruction());

        // 拼装函数调用数组 [innerDim, arrType, arrName]
        String[] arrSymbolInfo = {innerDim, llvmType, llvmName};
        // 用于初始化的调用,获取维数与数组维数相同
        int[] dimensionInfo = {size, size};
        return new ArrInfo(arrSymbolInfo, dimensionInfo);
    }


    public Instruction blockItemHandler(BlockItemNode blockItemNode, Jump.JumpType jumpType) {
        // 指令集合
        Instruction instruction = new Instruction(null, new StringJoiner("\n"));
        //  BlockItem → Decl | Stmt
        DeclNode declNode = blockItemNode.getDeclNode();
        // Decl语句
        if (declNode != null) {
            VarDeclNode varDeclNode = declNode.getVarDeclNode();
            // 局部变量
            if (varDeclNode != null) {
                for (VarDefNode varDefNode : varDeclNode.getVarDefNodes()) {
                    //  VarDef → Ident { '[' ConstExp ']' } | Ident { '[' ConstExp ']' } '=' InitVal
                    int size = varDefNode.getConstExpNodes().size();
                    String name = varDefNode.getIdentToken().getValue();
                    InitValNode initValNode = varDefNode.getInitValNode();
                    String llvmType;
                    String llvmName;
                    // 非数组
                    if (size == 0) {
                        // 申请空间,记录alloc指令
                        llvmType = I32;
                        AllocElement alloc = UTILS.alloc(llvmType);
                        instruction.addInstruction(alloc.getInstruction());
                        llvmName = alloc.getLlvmName();

                        // 是否有初值
                        if (initValNode != null) {
                            Instruction addExpInstruction = addExpHandler(varDefNode.getInitValNode().getExpNode().getAddExpNode(), true);
                            // 记录addExp分析的指令
                            instruction.addInstruction(addExpInstruction.toString());
                            String sName = addExpInstruction.getLlvmName();
                            // 记录store指令 store i32 %0, i32 * %4
                            instruction.addInstruction(UTILS.store(I32, sName, I32POINT, llvmName));
                        }
                    }
                    // 数组
                    else {
                        int[] dimInt = {0, 0};
                        ArrInfo arrInfo = getArrInfo(null, varDefNode, instruction, dimInt);
                        llvmType = arrInfo.getArrSymbolInfo()[1];
                        llvmName = arrInfo.getArrSymbolInfo()[2];
                        // 有初始值
                        if (initValNode != null) {
                            if (size == 1) {
                                for (int i=0; i<dimInt[0]; i++) {
                                    InitValNode ele = initValNode.getInitValNodes().get(i);
                                    initLocalArr(null, ele, instruction, arrInfo, String.valueOf(i), null, null);
                                }
                            }
                            else {
                                for (int i=0; i<dimInt[0]; i++) {
                                    InitValNode eleOne = initValNode.getInitValNodes().get(i);
                                    for (int j=0; j<dimInt[1]; j++) {
                                        // 获取初始值
                                        InitValNode eleTwo = eleOne.getInitValNodes().get(j);
                                        initLocalArr(null, eleTwo, instruction, arrInfo, String.valueOf(i), String.valueOf(j), null);
                                    }
                                }
                            }
                        }

                    }
                    // 记录符号表
                    UTILS.setSymbolLLVMInfo(name, llvmType, llvmName, 0, name);
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
                    String name = constDefNode.getIdentToken().getValue();
                    int size = constDefNode.getConstExpNodes().size();
                    ConstInitValNode constInitValNode = constDefNode.getConstInitValNode();
                    String llvmName;
                    String llvmType;
                    int constValue = 0;
                    List<Integer> constValues = null;
                    if (size == 0) {
                        // 申请空间
                        llvmType = I32;
                        AllocElement alloc = UTILS.alloc(llvmType);
                        instruction.addInstruction(alloc.getInstruction());
                        llvmName = alloc.getLlvmName();

                        // 记录addExp分析的指令
                        constValue = UTILS.constAddExpHandler(constDefNode.getConstInitValNode().getConstExpNode().getAddExpNode());
                        String sName = String.valueOf(constValue);

                        // 记录store指令
                        instruction.addInstruction(UTILS.store(I32, sName, I32POINT, llvmName));
                    }
                    // 数组
                    else {
                        int[] dimInt = {0, 0};
                        ArrInfo arrInfo = getArrInfo(constDefNode, null, instruction, dimInt);
                        llvmType = arrInfo.getArrSymbolInfo()[1];
                        llvmName = arrInfo.getArrSymbolInfo()[2];
                        constValues = new ArrayList<>();
                        if (size == 1) {
                            for (int i=0; i<dimInt[0]; i++) {
                                // 获取初始值
                                ConstInitValNode ele = constInitValNode.getConstInitValNodes().get(i);
                                initLocalArr(ele, null, instruction, arrInfo, String.valueOf(i), null, constValues);
                            }
                        }
                        else {
                            ((ValSymbol)UTILS.findSymbol(name, name)).setInnerSize(dimInt[1]);
                            for (int i=0; i<dimInt[0]; i++) {
                                ConstInitValNode eleOne = constInitValNode.getConstInitValNodes().get(i);
                                for (int j=0; j<dimInt[1]; j++) {
                                    // 获取初始值
                                    List<Integer> innerConstValues = new ArrayList<>();
                                    ConstInitValNode eleTwo = eleOne.getConstInitValNodes().get(j);
                                    initLocalArr(eleTwo, null, instruction, arrInfo, String.valueOf(i), String.valueOf(j), innerConstValues);
                                    constValues.addAll(innerConstValues);
                                }
                            }
                        }
                    }
                    // 记录符号表
                    UTILS.setSymbolLLVMInfo(name, llvmType, llvmName, constValue, name);
                    UTILS.findSymbol(name, null).setConst();
                    // 需要在设置完信息后再记录,否则哈希值会产生变化
                    if (size != 0) {
                        UTILS.addConstArrVal(UTILS.findSymbol(name, null), constValues);
                    }
                }
            }
        }
        // Stmt语句
        else {
            StmtNode stmtNode = blockItemNode.getStmtNode();
            // 此处不能聚合为Block类型的,要预留嵌套回填,直接返回即可
            return stmtHandler(stmtNode, jumpType);
        }
        return instruction;
    }

    /**
     * 数组LVal
     */
    public void arrLValHandler(LValNode lValNode, Symbol symbol, String sName, Instruction instruction) {
        // LVal → Ident {'[' Exp ']'}
        String[] dim = {"0", "0"};
        Instruction addExpInstruction;
        // 数组维数
        List<ExpNode> expNodes = lValNode.getExpNodes();
        // 要获取的维数
        int getDimension = expNodes.size();
        for (int i=0; i<getDimension; i++) {
            ExpNode expNode = expNodes.get(i);
            addExpInstruction = addExpHandler(expNode.getAddExpNode(), true);
            instruction.addInstruction(addExpInstruction.toString());
            dim[i] = addExpInstruction.getLlvmName();
        }
        String[] arrSymbolInfo = {symbol.getInnerDim(), symbol.getLlvmType(), symbol.getLlvmName()};
        int[] dimensionInfo = {symbol.getDimension(), getDimension};
        ArrInfo arrInfo = new ArrInfo(arrSymbolInfo, dimensionInfo);

        // 取出数组元素
        Instruction elementPtr = getElementPtr(arrInfo, dim);
        instruction.addInstruction(elementPtr.toString());
        String dName = elementPtr.getLlvmName();
        // 存入值
        String store = UTILS.store(I32, sName, I32POINT, dName);
        instruction.addInstruction(store);
        instruction.setLlvmName(dName);
    }


    public Instruction stmtHandler(StmtNode stmtNode, Jump.JumpType jumpType) {
        Instruction instruction = new Instruction(null, new StringJoiner("\n"));
        Instruction addExpInstruction;
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
                UTILS.enterNextSymbolTable();
                List<BlockItemNode> blockItemNodes = stmtNode.getBlockNode().getBlockItemNodes();
                Instruction blockInstruction = new Instruction(null, new StringJoiner("\n"));
                List<Instruction> blockInstructions = new ArrayList<>();
                StmtNode tmpStmt;
                for (BlockItemNode blockItemNode : blockItemNodes) {
                    // 遍历加入每一个BlockItem获得的指令集
                    blockInstructions.add(blockItemHandler(blockItemNode, jumpType));
                    // 如果是一个Block里的continue或break语句,那么后面的都不用再分析了
                    if ((tmpStmt = blockItemNode.getStmtNode()) != null) {
                        if (tmpStmt.getStmtType() == StmtNode.StmtType.CONTINUE || tmpStmt.getStmtType() == StmtNode.StmtType.BREAK) {
                            break;
                        }
                    }
                }
                // 退出Block层数--,出栈当前符号表的孩子序号
                UTILS.exitCurSymbolTable();
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
                LValNode lValNode = stmtNode.getlValNode();
                String name = lValNode.getIdentToken().getValue();
                Symbol symbol = UTILS.findSymbol(name, null);
                String llvmName = symbol.getLlvmName();
                int dimension = symbol.getDimension();
                // 非数组
                if (dimension == 0) {
                    instruction.addInstruction(UTILS.store(I32, sName, I32POINT, llvmName));
                }
                // 数组
                else {
                    arrLValHandler(lValNode, symbol, sName, instruction);
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
                        instruction.addInstruction(UTILS.callFunc("putint", args).getInstruction());
                        i++;
                    }
                    else if (curChar == '\\') {
                        // 换行符输出
                        int ch = '\n';
                        String argVal = String.valueOf(ch);
                        List<String> args = new ArrayList<>(){{
                            add(argVal);
                        }};
                        instruction.addInstruction(UTILS.callFunc("putch", args).getInstruction());
                        i++;
                    }
                    else {
                        // 普通字符输出
                        String argVal = String.valueOf((int) curChar);
                        List<String> args = new ArrayList<>(){{
                            add(argVal);
                        }};
                        instruction.addInstruction(UTILS.callFunc("putch", args).getInstruction());
                    }
                }
                break;
            case CONTINUE:
                // 查看在哪种Block里,打上标记
                JUMP.setJumpFlag(jumpType);
                // 需要回填,返回的是对象,可以之后回填,打上标记即可
                BACK_FILL.addNeedBackFillIC(instruction);
                return instruction;
            case BREAK:
                // 查看在哪种Block里,打上标记
                JUMP.setJumpFlag(jumpType);
                // 需要回填,打上标记
                BACK_FILL.addNeedBackFillIB(instruction);
                return instruction;
            case IF:
                //  'if' '(' Cond ')' Stmt [ 'else' Stmt ]

                // 记录条件类型,以便条件表达式处理子程序进行跳转
                if (stmtNode.getElesToken() == null) {
                    condTypes.push(CondType.IF);
                }
                else {
                    condTypes.push(CondType.IF_ELSE);
                }

                // Cond, ifCond是不用直接跳转的,按顺序分析即可
                List<Instruction> ifCondInstructions = lOrExpHandler(stmtNode.getCondNode().getlOrExpNode());

                // ifBody, 默认无直接跳转
                JUMP.initJump(Jump.JumpType.IF);
                String ifBodyLabel = UTILS.allocLabel();
                // 若是Block类型,可能需要回填
                StmtNode forStmtNode = stmtNode.getStmtNodes().get(0);
                Instruction ifBodyInstructions = stmtHandler(stmtNode.getStmtNodes().get(0), Jump.JumpType.IF);
                // 若是Block类型,标记;此时ifBodyInstructions本身已经有标记
                boolean ifBodyB = ifBodyInstructions.isBlock();
                // 若不是Block类型, Continue和Break也要归类为Block类型
                if (forStmtNode.getStmtType() == StmtNode.StmtType.BREAK || forStmtNode.getStmtType() == StmtNode.StmtType.CONTINUE) {
                    ifBodyB = true;
                }
                // 此处若是Block类型,其所有指令存储在ifBodyInstructions.needBackInstructions中

                // elseBody
                Instruction elseBodyInstructions = null;
                String elseBodyLabel = null;
                boolean elseBodyB = false;
                if (stmtNode.getElesToken() != null) {
                    JUMP.initJump(Jump.JumpType.ELSE);
                    elseBodyLabel= UTILS.allocLabel();
                    elseBodyInstructions = stmtHandler(stmtNode.getStmtNodes().get(1), Jump.JumpType.ELSE);
                    // 若是Block类型,标记,之后聚合
                    if (elseBodyInstructions.isBlock()) {
                        elseBodyB = true;
                    }
                }

                // nextLabel, 加上Label即可,下一条语句stmt会按它自己的处理执行
                nextLabel = UTILS.allocLabel();

                // 进行回填
                // ifCond语句里的跳转语句回填,根据标记好的语句类型进行回填
                for (Instruction ifCondInstruction : ifCondInstructions) {
                    String nextLAndLabel = ifCondInstruction.getNextLAndLabel();
                    String nextLOrLabel = ifCondInstruction.getNextLOrLabel();
                    String cond = ifCondInstruction.getLlvmName();
                    switch (ifCondInstruction.getConditionJumpType()) {
                        case IAN -> ifCondInstruction.addInstruction(conditionalJump(cond, nextLAndLabel, nextLabel).toString());
                        case IAO, IEAO -> ifCondInstruction.addInstruction(conditionalJump(cond, nextLAndLabel, nextLOrLabel).toString());
                        case IBN -> ifCondInstruction.addInstruction(conditionalJump(cond, ifBodyLabel, nextLabel).toString());
                        case IBO, IEBO -> ifCondInstruction.addInstruction(conditionalJump(cond, ifBodyLabel, nextLOrLabel).toString());
                        case IEAE -> ifCondInstruction.addInstruction(conditionalJump(cond, nextLAndLabel, elseBodyLabel).toString());
                        case IEBE -> ifCondInstruction.addInstruction(conditionalJump(cond, ifBodyLabel, elseBodyLabel).toString());
                    }
                }
                // 如果当前if语句已经有跳转语句,不用回填
                if (JUMP.isNotJump(Jump.JumpType.IF)) {
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
                    if (JUMP.isNotJump(Jump.JumpType.ELSE)) {
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
                        instruction.addInstruction(UTILS.addLabel(ifCondInstruction.getNextLabel()).toString());
                    }
                }

                // ifBody
                instruction.addInstruction(UTILS.addLabel(ifBodyLabel).toString());
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
                        instruction.addNeedBackInstruction(UTILS.addLabel(elseBodyLabel));
                        instruction.addNeedBackInstruction(elseBodyInstructions);
                    }
                    else {
                        instruction.addInstruction(UTILS.addLabel(elseBodyLabel).toString());
                        instruction.addInstruction(elseBodyInstructions.toString());
                    }
                }

                // 肯定有下一条语句,就算是void函数,也有ret void
                // 注意,因为顺序是 ifCond -> ifBody -> {elseBody} -> nextLabel
                // 所以如果ifBody和elseBody语句需要回填,那么nextLabel也应该回填
                if (instruction.isBlock()) {
                    instruction.addNeedBackInstruction(UTILS.addLabel(nextLabel));
                }
                else {
                    instruction.addInstruction(UTILS.addLabel(nextLabel).toString());
                }

                // 分析完成,弹出条件类型
                condTypes.pop();

                break;

            case FOR:
                // 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt

                // 记录条件类型,以便条件表达式处理子程序进行跳转
                condTypes.push(CondType.FOR);
                // 假设不用continue和break回填
                BACK_FILL.enterForInit();

                // ForStmtOne,初始化语句
                if (stmtNode.getForStmtOne() != null) {
                    // 初始化语句直接添加即可
                    instruction.addInstruction(forStmtHandler(stmtNode.getForStmtOne()).getInstructions().toString());
                }
                // forCond,条件判断语句,不一定有,如果没有,应该直接跳转到forBody
                List<Instruction> forCondInstructions = null;
                String forCondLabel = null;
                if (stmtNode.getCondNode() != null) {
                    forCondLabel = UTILS.allocLabel();
                    forCondInstructions = lOrExpHandler(stmtNode.getCondNode().getlOrExpNode());
                }

                // forBody, 循环体语句
                String forBodyLabel = UTILS.allocLabel();
                StmtNode forBodyStmt = stmtNode.getStmtNode();
                Instruction forBodyInstruction;
                JUMP.initJump(Jump.JumpType.FOR);
                // 直接为跳转语句非Block
                if (forBodyStmt.getStmtType() == StmtNode.StmtType.CONTINUE || forBodyStmt.getStmtType() == StmtNode.StmtType.BREAK) {
                    forBodyInstruction = stmtHandler(forBodyStmt, Jump.JumpType.FOR);
                }
                // Block
                else if (forBodyStmt.getStmtType() == StmtNode.StmtType.BLOCK) {
                    // 进行Block分析时, Block → '{' { BlockItem } '}', BlockItem → Decl | Stmt
                    // Decl语句不必管 主要是Stmt里的Continue和Break语句会导致回填;
                    // 另外,当Stmt语句再导出Block时,若还是在同一层for循环里,里面的Continue和Break语句也会导致回填
                    // 因此Block进行分析时,里面的每一个BlockItem的指令集合都要进行保留,留待回到for循环回填完毕再统合
                    forBodyInstruction = stmtHandler(forBodyStmt, Jump.JumpType.FOR);
                }
                // 除了跳转语句和Block以外的语句
                else {
                    forBodyInstruction = stmtHandler(forBodyStmt, Jump.JumpType.FOR);
                }

                // forStep, 步长语句
                Instruction forStepInstruction = null;
                String forStepLabel = null;
                if (stmtNode.getForStmtTwo() != null) {
                    forStepLabel = UTILS.allocLabel();
                    forStepInstruction = forStmtHandler(stmtNode.getForStmtTwo());
                }

                // nextLabel
                nextLabel = UTILS.allocLabel();

                // 进行回填操作
                // forCond如果有的话, 对每一个跳转语句进行回填
                if (forCondLabel != null) {
                    // 遍历forCondInstruction的所有跳转语句,进行回填
                    for (Instruction forCondInstruction : forCondInstructions) {
                        // 根据eqExp的类型,进行跳转语句的回填
                        forCondInstruction.backFillConditionalJump(forBodyLabel, nextLabel);
                    }
                }

                // forStep, 最后一条语句是跳转语句,跳转到forCond(如果有),否则跳转到循环体,需要回填
                if (forStepLabel != null) {
                    forStepInstruction.addInstruction(unconditionalJump(Objects.requireNonNullElse(forCondLabel, forBodyLabel)).toString());
                }

                // 进行可能有的Continue和Break的回填
                // continue回填
                BACK_FILL.backFillContinue(forStepLabel, forCondLabel, forBodyLabel);

                // break回填
                BACK_FILL.backFillBreak(nextLabel);

                // 除了forBody外语句回填完毕,现在统合可能为Block类型的循环体
                if (forBodyInstruction.isBlock()) {
                    // 其内嵌套所有可能的回填,并在此一次性解决for循环块内的回填
                    forBodyInstruction.unionNeedBack();
                }

                // forBody, 最后一条语句是跳转语句,需要回填
                // 因为可能是Block类型的Stmt,所以需要在其他回填结束后聚合再回填
                // 如果为直接跳转语句,那么不需要填额外的跳转语句; 否则需要填跳转语句
                if (JUMP.isNotJump(Jump.JumpType.FOR)) {
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
                    instruction.addInstruction(UTILS.addLabel(forCondLabel).toString());
                    for (Instruction forCondInstruction : forCondInstructions) {
                        instruction.addInstruction(forCondInstruction.toString());
                        // 如果有下一个条件判断的基本块,那么加入指令
                        if (forCondInstruction.getNextLabel() != null) {
                            instruction.addInstruction(UTILS.addLabel(forCondInstruction.getNextLabel()).toString());
                        }
                    }
                }
                // forBody
                instruction.addInstruction(UTILS.addLabel(forBodyLabel).toString());
                instruction.addInstruction(forBodyInstruction.toString());
                // forStep
                if (forStepLabel != null) {
                    instruction.addInstruction(UTILS.addLabel(forStepLabel).toString());
                    instruction.addInstruction(forStepInstruction.toString());
                }

                // nextLabel, 直接加入指令集合即可,不用管下一条语句的详细分析过程
                instruction.addInstruction(UTILS.addLabel(nextLabel).toString());

                // 分析完成,弹出条件类型
                condTypes.pop();

                break;

            case GET:
                // LVal '=' 'getint''('')'
                AllocElement allocElement = UTILS.callFunc("getint", null);
                String rVal = allocElement.getLlvmName();
                // 记录函数调用指令
                instruction.addInstruction(allocElement.getInstruction());
                // 通过标识符名称找到其在中间代码中的名称(全局变量为@*,局部变量为%*)
                lValNode = stmtNode.getlValNode();
                name = lValNode.getIdentToken().getValue();
                symbol = UTILS.findSymbol(name, null);
                String dName = symbol.getLlvmName();
                dimension = symbol.getDimension();
                // 非数组
                if (dimension == 0) {
                    // 记录store指令
                    instruction.addInstruction(UTILS.store(I32, rVal, I32POINT, dName));
                }
                // 数组
                else {
                    arrLValHandler(lValNode, symbol, rVal, instruction);
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
                // 查看在哪种Block里,打上标记
                JUMP.setJumpFlag(jumpType);
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
        LValNode lValNode = forStmtOne.getlValNode();
        String name = lValNode.getIdentToken().getValue();
        ValSymbol valSymbol = (ValSymbol) UTILS.findSymbol(name, null);
        int dimension = valSymbol.getDimension();
        // 非数组
        if (dimension == 0) {
            // 记录store指令
            instruction.addInstruction(UTILS.store(I32, rVal, I32POINT, valSymbol.getLlvmName()));
        }
        // 数组
        else {
            arrLValHandler(lValNode, valSymbol, rVal, instruction);
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
                String nextLOrLabel = UTILS.allocLabel();
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
                String nextLAnd = UTILS.allocLabel();
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
        else if (hasNext || Objects.equals(UTILS.getLocalValType(val2), I1)) {
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
        if (Objects.equals(UTILS.getLocalValType(val), I1)) {
            AllocElement zextAlloc = UTILS.zext(val);
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
            AllocElement icmpAlloc = UTILS.icmp(opType, val1, val2);
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
        AllocElement allocElement = UTILS.calPrint(I32, val1, val2, opType);
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
                    AllocElement alloc = UTILS.calPrint(I32, "0", rVal, TokenType.MINU);
                    instruction.addInstruction(alloc.getInstruction());
                    // 更新值
                    rVal = alloc.getLlvmName();
                }
                // ‘!’,非运算,出现在条件表达式
                else if (Objects.equals(op, "!")) {
                    // 记录非计算
                    Instruction notInstruction = UTILS.not(rVal);
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
            // 如果已经被忽略,那么直接跳过分析即可
            if (UTILS.findSymbol(funcName, null).isIgnored()) {
                return instruction;
            }
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
            AllocElement alloc = UTILS.callFunc(funcName, args);
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
            LValNode lValNode = primaryExpNode.getlValNode();
            int getDimension = lValNode.getExpNodes().size();
            String name = lValNode.getIdentToken().getValue();
            // 可能是ValSymbol或FuncParamSymbol
            Symbol symbol = UTILS.findSymbol(name, null);
            String llvmName = symbol.getLlvmName();
            String retName;
            Instruction instruction = new Instruction(null, new StringJoiner("\n"));
            // size不可以用来判断是否是数组,得用symbol
            int dimension = symbol.getDimension();
            if (dimension == 0) {
                // 非数组
                // 如果是常数,可以直接获取值
                if (symbol.isConst()) {
                    int constValue = UTILS.getConstValue(symbol);
                    instruction.setLlvmName(String.valueOf(constValue));
                }
                // 否则进行load
                else {
                    AllocElement load = UTILS.load(I32, llvmName);
                    retName = load.getLlvmName();
                    instruction.addInstruction(load.getInstruction());
                    instruction.setLlvmName(retName);
                }
            }
            else if (dimension > 0){
                // 数组
                String[] dim = {"0", "0"};
                for (int i=0; i<getDimension; i++) {
                    ExpNode expNode = lValNode.getExpNodes().get(i);
                    Instruction addExpInstruction = addExpHandler(expNode.getAddExpNode(), true);
                    instruction.addInstruction(addExpInstruction.toString());
                    dim[i] = addExpInstruction.getLlvmName();
                }
                // 如果是取元素, 且非函数参数,且位置取值都是常数, 且是常量, 那么可以直接获取值
                if (symbol.isConst() && getDimension == dimension && symbol.getClass() != FuncParamSymbol.class) {
                    try {
                        int i = Integer.parseInt(dim[0]);
                        int j = Integer.parseInt(dim[1]);
                        return new Instruction(UTILS.getConstArrEle(symbol, i, j), new StringJoiner("\n"));
                    }
                    catch (NumberFormatException ignored) {
                    }
                }
                // 组装函数调用所用信息
                String[] arrSymbolInfo = {symbol.getInnerDim(), symbol.getLlvmType(), symbol.getLlvmName()};
                int[] dimensionInfo = {dimension, getDimension};
                ArrInfo arrInfo = new ArrInfo(arrSymbolInfo, dimensionInfo);

                Instruction elementPtr = getElementPtr(arrInfo, dim);
                instruction.addInstruction(elementPtr.toString());
                // 数组lVal,只有获取维度等于数组维度的才load
                if (dimension == getDimension) {
                    AllocElement load = UTILS.load(I32, elementPtr.getLlvmName());
                    instruction.addInstruction(load.getInstruction());
                    retName = load.getLlvmName();
                }
                // 否则直接返回获取到的数组地址即可
                else {
                    retName = elementPtr.getLlvmName();
                }
                instruction.setLlvmName(retName);
            }
            return instruction;
        }
    }
}
