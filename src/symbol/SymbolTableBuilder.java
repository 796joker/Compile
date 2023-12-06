package symbol;

import error.ErrorHandler;
import error.ErrorType;
import frontend.Parser;
import node.*;
import token.Token;
import token.TokenType;

import java.util.ArrayList;
import java.util.List;

public class SymbolTableBuilder {
    private static final SymbolTableBuilder symbolTableBuilder = new SymbolTableBuilder();
    private int curLayer;
    private SymbolTable curSymbolTable;
    private List<SymbolTable> symbolTables;

    private ErrorHandler errorHandler;

    private int forLayer;
    private FuncSymbol.ReturnType funcType;

    private SymbolTableBuilder() {
    }

    public static SymbolTableBuilder getSymbolTableBuilder() {
        return symbolTableBuilder;
    }

    public List<SymbolTable> getSymbolTables() {
        return symbolTables;
    }

    /**
     * 初始化符号表建立器
     */
    public void init() {
        this.curLayer = 0;
        this.symbolTables = new ArrayList<>();
        this.errorHandler = ErrorHandler.getErrorHandler();
        this.forLayer = 0;
        this.funcType = null;
    }

    public void breakORContinueCheck(Token token) {
        // 报错行号为‘break’与’continue’所在行号
        if (forLayer == 0) {
            errorHandler.addError(token.getLineNum(), ErrorType.m);
        }
    }


    /**
     * 查看符号表中是否有定义
     * @param checkRepeat 是否检查重定义
     */
    public boolean isDefine(String name, boolean checkRepeat) {
        // 先看当前符号表是否有定义
        if (curSymbolTable.contains(name)) {
            return true;
        }
        // 当前符号表无定义,若是检查是否重定义,那么已经可以返回false;
        // 若只是单纯查找是否有定义,则须递归查找
        else if (!checkRepeat) {
            // 递归查看父符号表是否有定义
            SymbolTable temp = curSymbolTable.getParent();
            while (temp != null) {
                if (temp.contains(name)) {
                    return true;
                }
                temp = temp.getParent();
            }
        }
        return false;
    }

    /**
     * 获取名称对应符号
     * @return 存在返回true,否则返回false
     */
    public Symbol getSymbol(String name) {
        SymbolTable temp = curSymbolTable;
        while (temp != null) {
            // 一旦找到立刻返回,确保找到的是最内层的定义
            if ((temp.getSymbol(name) != null)) {
                return temp.getSymbol(name);
            }
            temp = temp.getParent();
        }
        return null;
    }

    /**
     * 检查函数参数类型
     * @param lineNum 函数Ident所在行号
     */
    public void checkFuncRParam(List<FuncParamSymbol> funcParamSymbols, List<ExpNode> funcRParams, int lineNum) {
        // 遍历各个参数ExpNode,与符号表Symbol对应
        for (int i=0; i<funcRParams.size(); i++) {
            FuncParamSymbol funcParamSymbol = funcParamSymbols.get(i);
            int needDimension = funcParamSymbol.getDimension();
            // Exp -> AddExp
            // 统合所有维度情况
            // 注意:常量数组 arr 不能作为参数传入到函数中常量数组 arr 不能作为参数传入到函数中
            // 注意: 评测样例中不会出现数组越界情况,无需考虑
            addExpParamAnalyze(funcRParams.get(i).getAddExpNode(), needDimension, lineNum);
        }
    }

    public void addExpParamAnalyze(AddExpNode addExpNode, int needDimension, int lineNum) {
        // AddExp → MulExp | AddExp ('+' | '−') MulExp
        if (addExpNode.getAddExpNode() != null) {
            addExpParamAnalyze(addExpNode.getAddExpNode(), needDimension, lineNum);
        }
        mulExpParamAnalyze(addExpNode.getMulExpNode(), needDimension, lineNum);
    }

    public void mulExpParamAnalyze(MulExpNode mulExpNode, int needDimension, int lineNum) {
        // MulExp → UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
        if (mulExpNode.getMulExpNode() != null) {
            mulExpParamAnalyze(mulExpNode.getMulExpNode(), needDimension, lineNum);
        }
        unaryExpParamAnalyze(mulExpNode.getUnaryExpNode(), needDimension, lineNum);
    }

    public void unaryExpParamAnalyze(UnaryExpNode unaryExpNode, int needDimension, int lineNum) {
        //  UnaryExp → PrimaryExp | Ident '(' [FuncRParams] ')' | UnaryOp UnaryExp
        // 数组地址不会进行运算,无需考虑;就算运算,也不会影响维数
        if (unaryExpNode.getUnaryExpNode() != null) {
            unaryExpParamAnalyze(unaryExpNode.getUnaryExpNode(), needDimension, lineNum);
        }
        else if (unaryExpNode.getPrimaryExpNode() != null) {
            primaryExpParamAnalyze(unaryExpNode.getPrimaryExpNode(), needDimension, lineNum);
        }
        else {
            // 需要分析调用函数的返回值:若为void,那么不应在此出现;若为int,即为0维度,不影响,无需进一步处理
            // 先检查此函数调用(函数是否已经定义,调用的参数是否合法等)
            unaryExpHandler(unaryExpNode, false);
            Symbol funcSymbol;
            funcSymbol = getSymbol(unaryExpNode.getIdentToken().getValue());
            // 若未定义,直接返回,错误已经在上述检查中处理过
            if (funcSymbol == null) {
                return;
            }
            // 已经定义,检查返回类型
            if (((FuncSymbol) funcSymbol).getReturnType() == FuncSymbol.ReturnType.VOID) {
                // 参数类型不匹配,总之不会是0维度
                errorHandler.addError(lineNum, ErrorType.e);
            }
            else if (((FuncSymbol) funcSymbol).getReturnType() == FuncSymbol.ReturnType.INT) {
                if (needDimension != 0) {
                    errorHandler.addError(lineNum, ErrorType.e);
                }
            }
        }
    }

    public void primaryExpParamAnalyze(PrimaryExpNode primaryExpNode, int needDimension, int lineNum) {
        //  PrimaryExp → '(' Exp ')' | LVal | Number
        // Number不用分析,任意维度加Number0维度不改变维度数,且Number不存在定义问题
        if (primaryExpNode.getExpNode() != null) {
            addExpParamAnalyze(primaryExpNode.getExpNode().getAddExpNode(), needDimension, lineNum);
        }
        else if (primaryExpNode.getlValNode() != null) {
            lValParamAnalyze(primaryExpNode.getlValNode(), needDimension, lineNum);
        }
        // 出现Number必然不出现数组,故此参数必为0维
        else if (primaryExpNode.getNumberNode() != null) {
            if (needDimension != 0) {
                errorHandler.addError(lineNum, ErrorType.e);
            }
        }
    }

    public void lValParamAnalyze(LValNode lValNode, int needDimension, int lineNum) {
        // LVal → Ident {'[' Exp ']'}
        String paramName = lValNode.getIdentToken().getValue();
        Symbol valSymbol;
        // 传入参数未定义,假装没错,下一个参数
        if ((valSymbol = getSymbol(paramName)) == null) {
            errorHandler.addError(lValNode.getIdentToken().getLineNum(), ErrorType.c);
            return;
        }
        // TODO 传入数组为常量,语义错误
        // 参数已定义,开始分析其可能存在的Exp,即未定义问题应该等应该在维度问题之前,因为当Exp不正确时,讨论维度是无意义的
        for (ExpNode expNode : lValNode.getExpNodes()) {
            addExpHandler(expNode.getAddExpNode(), false);
        }
        // 检查维度是否匹配
        int valDimension = 0;
        if (valSymbol.getClass() == ValSymbol.class) {
            valDimension = ((ValSymbol) valSymbol).getDimension();
        }
        else if (valSymbol.getClass() == FuncParamSymbol.class) {
            valDimension = ((FuncParamSymbol) valSymbol).getDimension();
        }
        // LVal → Ident {'[' Exp ']'} , 查看调用参数的层级
        int valLayer = lValNode.getExpNodes().size();
        // 维度不匹配,报错行号为函数调用语句的函数名所在行数
        if (valDimension - valLayer != needDimension) {
            errorHandler.addError(lineNum, ErrorType.e);
        }
    }

    /**
     * 建立符号表
     */
    public void buildSymbolTable() {
        SymbolTable globalSymbolTable = new SymbolTable(curLayer, null);
        curSymbolTable = globalSymbolTable;
        this.symbolTables.add(globalSymbolTable);
        CompUnitNode compUnitNode = Parser.getPARSER().getCompUnitNode();
        List<DeclNode> declNodes = compUnitNode.getDeclNodes();
        List<FuncDefNode> funcDefNodes = compUnitNode.getFuncDefNodes();
        MainFuncDefNode mainFuncDefNode = compUnitNode.getMainFuncDefNode();
        // 处理Val定义
        for (DeclNode declNode : declNodes) {
            declHandler(declNode);
        }
        // 处理Func定义
        for (FuncDefNode funcDefNode : funcDefNodes) {
            FuncDefHandler(funcDefNode);
        }
        List<BlockItemNode> blockItemNodes = mainFuncDefNode.getBlockNode().getBlockItemNodes();
        // 处理Main函数里的blockItem块
        curLayer++;
        SymbolTable childSymbolTable = new SymbolTable(curLayer, curSymbolTable);
        curSymbolTable.addChild(childSymbolTable);
        curSymbolTable = childSymbolTable;
        this.symbolTables.add(curSymbolTable);
        // main函数返回类型为int
        funcType = FuncSymbol.ReturnType.INT;
        for (BlockItemNode blockItemNode : blockItemNodes) {
            blockHandler(blockItemNode);
        }
        if (funcType == FuncSymbol.ReturnType.INT) {
            // main函数缺少返回语句
            errorHandler.addError(mainFuncDefNode.getBlockNode().getRightBraceToken().getLineNum(), ErrorType.g);
        }
        curLayer--;
        curSymbolTable = curSymbolTable.getParent();
    }

    public void declHandler(DeclNode declNode) {
        // Decl → ConstDecl | VarDecl
        if (declNode.getConstDeclNode() != null) {
            ConstDeclNode constDeclNode = declNode.getConstDeclNode();
            for (ConstDefNode constDefNode : constDeclNode.getConstDefNodes()) {
                constDefHandler(constDefNode);
            }
        }
        if (declNode.getVarDeclNode() != null) {
            VarDeclNode varDeclNode = declNode.getVarDeclNode();
            for (VarDefNode varDefNode : varDeclNode.getVarDefNodes()) {
                varDefHandler(varDefNode);
            }
        }
    }

    public void FuncDefHandler(FuncDefNode funcDefNode) {
        //  FuncDef → FuncType Ident '(' [FuncFParams] ')' Block
        String funcName = funcDefNode.getIdentToken().getValue();
        // 检查函数名是否重定义,报错行号为<Ident>所在行数
        if (isDefine(funcName, true)) {
            errorHandler.addError(funcDefNode.getIdentToken().getLineNum(), ErrorType.b);
            // 重定义则后续无需分析
            return;
        }
        // 记录函数返回值类型
        FuncTypeNode funcTypeNode = funcDefNode.getFuncTypeNode();
        FuncSymbol.ReturnType returnType;
        // FuncType → int | void
        if (funcTypeNode.getToken().getType() == TokenType.VOIDTK) {
            returnType = FuncSymbol.ReturnType.VOID;
            funcType = FuncSymbol.ReturnType.VOID;
        }
        else {
            returnType = FuncSymbol.ReturnType.INT;
            funcType = FuncSymbol.ReturnType.INT;
        }
        // 注意: 需要加深一层符号表,FuncParam和Block都在此层内
        curLayer++;
        SymbolTable childSymbolTable = new SymbolTable(curLayer, curSymbolTable);
        curSymbolTable.addChild(childSymbolTable);
        curSymbolTable = childSymbolTable;
        this.symbolTables.add(curSymbolTable);
        // FuncFParams → FuncFParam { ',' FuncFParam }
        List<FuncParamSymbol> funcParamSymbols = new ArrayList<>();
        if (funcDefNode.getFuncFParamsNode() != null) {
            List<FuncFParamNode> funcFParamNodes = funcDefNode.getFuncFParamsNode().getFuncFParamNodes();
            // 遍历函数形参,记录参数维度与参数名字
            for (FuncFParamNode funcFParamNode : funcFParamNodes) {
                FuncParamSymbol funcParamSymbol = funcParamHandler(funcFParamNode);
                if (funcParamSymbol == null) {
                    // 若funcParamSymbol为null,那么必然是函数形参中存在未定义或者重定义问题,
                    // 此时不应该影响下一个形参的分析,以达到错误局部化,但每当这中错误出现,
                    // 此形参仍然将被记录,
                    continue;
                }
                funcParamSymbols.add(funcParamSymbol);
            }
        }
        // 加入Func所在符号表,先把当前函数体层级符号表记下
        SymbolTable funcBlockSymbolTable = curSymbolTable;
        curSymbolTable = curSymbolTable.getParent();
        // 为了能够递归调用,将函数定义先加入符号表,再分析函数体
        FuncSymbol funcSymbol = new FuncSymbol(funcName, funcParamSymbols, returnType, funcParamSymbols.size());
        curSymbolTable.addSymbol(funcSymbol);
        // 转回函数体层级符号表
        curSymbolTable = funcBlockSymbolTable;
        // Block
        for (BlockItemNode blockItemNode : funcDefNode.getBlockNode().getBlockItemNodes()) {
            blockHandler(blockItemNode);
        }
        // 如果此时funcType为int,说明无返回值,报错
        if (funcType == FuncSymbol.ReturnType.INT) {
            // 报错行号为函数结尾的’}’
            errorHandler.addError(funcDefNode.getBlockNode().getRightBraceToken().getLineNum(), ErrorType.g);
        }
        // 重置funcType
        funcType = null;
        // 记录完毕参数后并分析完Block后,层级回退
        curLayer--;
        curSymbolTable = curSymbolTable.getParent();
    }

    public FuncParamSymbol funcParamHandler(FuncFParamNode funcFParamNode) {
        // FuncFParam → BType Ident ['[' ']' { '[' ConstExp ']' }]
        // 注意: 参数名字不能重复
        String paramName = funcFParamNode.getIdentToken().getValue();
        // 检查是否重定义,若重定义了,仍然需要继续向后分析,由于一行只有一个错误,不分析也行
        if (isDefine(paramName, true)) {
            errorHandler.addError(funcFParamNode.getIdentToken().getLineNum(), ErrorType.b);
        }
        // 参数维度
        int dimension = funcFParamNode.getLeftBracketTokens().size();
        // 即使是递归分析出错了,也应该加入符号表中,所以无需判断是否出错
        if (funcFParamNode.getConstExpNode() != null) {
            // ConstExp → AddExp
            addExpHandler(funcFParamNode.getConstExpNode().getAddExpNode(), true);
        }
        // 加入符号表
        FuncParamSymbol funcParamSymbol = new FuncParamSymbol(paramName, dimension);
        curSymbolTable.addSymbol(funcParamSymbol);
        return funcParamSymbol;
    }


    public void blockHandler(BlockItemNode blockItemNode) {
        // BlockItem → Decl | Stmt
        if (blockItemNode.getDeclNode() != null) {
            declHandler(blockItemNode.getDeclNode());
        }
        if (blockItemNode.getStmtNode() != null) {
            stmtHandler(blockItemNode.getStmtNode());
        }
    }

    public void constDefHandler(ConstDefNode constDefNode) {
        // ConstDef → Ident { '[' ConstExp ']' } '=' ConstInitVal
        String constName = constDefNode.getIdentToken().getValue();
        // 检查是否重定义,重定义则无需继续分析
        if (isDefine(constName, true)) {
            errorHandler.addError(constDefNode.getIdentToken().getLineNum(), ErrorType.b);
            return;
        }
        int dimension = constDefNode.getLeftBrackets().size();
        List<ConstExpNode> constExpNodes = constDefNode.getConstExpNodes();
        for (ConstExpNode constExpNode : constExpNodes) {
            //  ConstExp → AddExp
            addExpHandler(constExpNode.getAddExpNode(), true);
        }
        // 处理ConstInitVal初始化值
        constInitValHandler(constDefNode.getConstInitValNode());
        // 加入当前符号表
        curSymbolTable.addSymbol(new ValSymbol(constName, true, dimension));
    }

    public void varDefHandler(VarDefNode varDefNode) {
        // VarDef → Ident { '[' ConstExp ']' }
        // | Ident { '[' ConstExp ']' } '=' InitVal
        String varName = varDefNode.getIdentToken().getValue();
        // 是否重定义,若重定义了,那么此定义无效,不用继续往下分析
        if (isDefine(varName, true)) {
            errorHandler.addError(varDefNode.getIdentToken().getLineNum(), ErrorType.b);
            return;
        }
        int dimension = varDefNode.getConstExpNodes().size();
        // 处理ConstExp
        for (ConstExpNode constExpNode : varDefNode.getConstExpNodes()) {
            // ConstExp → AddExp
            addExpHandler(constExpNode.getAddExpNode(), true);
        }
        // 处理InitVal
        if (varDefNode.getInitValNode() != null) {
            initValHandler(varDefNode.getInitValNode());
        }
        // 加入符号表
        curSymbolTable.addSymbol(new ValSymbol(varName, false, dimension));
    }

    /**
     * 处理addExp, ConstExp | Exp -> AddExp
     * @param constCheck 是否需要常量检查
     */
    public void addExpHandler(AddExpNode addExpNode, boolean constCheck) {
        //  AddExp → MulExp | AddExp ('+' | '−') MulExp
        // 处理AddExp
        if (addExpNode.getAddExpNode() != null) {
            addExpHandler(addExpNode.getAddExpNode(), constCheck);
        }
        // 处理MulExp
        mulExpHandler(addExpNode.getMulExpNode(), constCheck);
    }

    public void mulExpHandler(MulExpNode mulExpNode, boolean constCheck) {
        // MulExp → UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
        // 处理MulExp
        if (mulExpNode.getMulExpNode() != null) {
            mulExpHandler(mulExpNode.getMulExpNode(), constCheck);
        }
        // 处理UnaryExp
        unaryExpHandler(mulExpNode.getUnaryExpNode(), constCheck);
    }

    public void unaryExpHandler(UnaryExpNode unaryExpNode, boolean constCheck) {
        // UnaryExp → PrimaryExp | Ident '(' [FuncRParams] ')' | UnaryOp UnaryExp
        // 处理UnaryExp
        if (unaryExpNode.getUnaryExpNode() != null) {
            unaryExpHandler(unaryExpNode.getUnaryExpNode(), constCheck);
        }
        // 处理PrimaryExp
        else if (unaryExpNode.getPrimaryExpNode() != null) {
            primaryExpHandler(unaryExpNode.getPrimaryExpNode(), constCheck);
        }
        // 处理Ident '(' [FuncRParams] ')'
        // 注意:此处的处理即为对函数调用的处理,但是在函数实参中可能递归的出现函数调用,因此此函数需要递归判断
        else if (unaryExpNode.getIdentToken() != null) {
            int lineNum = unaryExpNode.getIdentToken().getLineNum();
            // 检查函数是否定义, 报错行号为<Ident>所在行数
            Symbol funcSymbol;
            // 未定义,无需往下分析
            if ((funcSymbol = getSymbol(unaryExpNode.getIdentToken().getValue())) == null) {
                errorHandler.addError(lineNum, ErrorType.c);
                return;
            }
            // 若有参数，检查函数参数是否匹配
            if (unaryExpNode.getFuncRParamsNode() != null) {
                // FuncRParams → Exp { ',' Exp }
                List<ExpNode> expNodes = unaryExpNode.getFuncRParamsNode().getExpNodes();
                // 检查参数个数是否匹配,当参数个数不匹配时,自然也无法判断缺少哪个位置的参数,
                // 因此就不用继续往下分析参数类型是否匹配
                if (expNodes.size() != ((FuncSymbol) funcSymbol).getParamNum()) {
                    errorHandler.addError(unaryExpNode.getIdentToken().getLineNum(), ErrorType.d);
                    return;
                }
                // 检查参数类型是否匹配,因为BType->int,所以此处类型指的是参数的维数
                checkFuncRParam(((FuncSymbol) funcSymbol).getFuncParamSymbols(), expNodes, lineNum);
            }
            // 若无参数,但定义有,也算是参数个数不匹配
            else {
                if (((FuncSymbol) funcSymbol).getParamNum() != 0) {
                    errorHandler.addError(unaryExpNode.getIdentToken().getLineNum(), ErrorType.d);
                }
            }
        }
    }

    public void primaryExpHandler(PrimaryExpNode primaryExpNode, boolean constCheck) {
        // PrimaryExp → '(' Exp ')' | LVal | Number
        if (primaryExpNode.getExpNode() != null) {
            addExpHandler(primaryExpNode.getExpNode().getAddExpNode(), constCheck);
        }
        else if (primaryExpNode.getlValNode() != null) {
            lValHandler(primaryExpNode.getlValNode(), constCheck);
        }
        // 如果是Number,那么无需处理
    }

    public void lValHandler(LValNode lValNode, boolean constCheck) {
        //  LVal → Ident {'[' Exp ']'}
        // 是否已定义
        String name = lValNode.getIdentToken().getValue();
        if (!isDefine(name, false)) {
            // 报错行号为<Ident>所在行数
            errorHandler.addError(lValNode.getIdentToken().getLineNum(), ErrorType.c);
        }
        // 分析可能有的Exp
        for (ExpNode expNode : lValNode.getExpNodes()) {
            addExpHandler(expNode.getAddExpNode(), constCheck);
        }
        // TODO 是否符合维度是语义问题
    }


    public void constInitValHandler(ConstInitValNode constInitValNode) {
        //  ConstInitVal → ConstExp | '{' [ ConstInitVal { ',' ConstInitVal } ] '}'
        // 处理ConstExp
        if (constInitValNode.getConstExpNode() != null) {
            addExpHandler(constInitValNode.getConstExpNode().getAddExpNode(), true);
        }
        // 处理ConstInitVal
        else {
            for (ConstInitValNode ele : constInitValNode.getConstInitValNodes()) {
                constInitValHandler(ele);
            }
        }
    }

    public void initValHandler(InitValNode initValNode) {
        // InitVal → Exp | '{' [ InitVal { ',' InitVal } ] '}'
        if (initValNode.getExpNode() != null) {
            addExpHandler(initValNode.getExpNode().getAddExpNode(), false);
        }
        else {
            for (InitValNode ele : initValNode.getInitValNodes()) {
                initValHandler(ele);
            }
        }
    }

    public void stmtHandler(StmtNode stmtNode) {
        // Stmt → LVal = Exp; | [Exp]; | Block
        boolean constCheck = false;
        Token identToken;
        String name;
        Symbol valSymbol;
        switch (stmtNode.getStmtType()) {
            case ASSIGN:
                // 检查是否修改常量值
                identToken= stmtNode.getlValNode().getIdentToken();
                name = identToken.getValue();
                if ((valSymbol = getSymbol(name)) != null) {
                    // 有定义且不是函数定义里的函数参数
                    if (valSymbol.getClass() != FuncParamSymbol.class) {
                        if (((ValSymbol) valSymbol).isConst()) {
                            // 报错行号为<LVal>所在行号
                            errorHandler.addError(identToken.getLineNum(), ErrorType.h);
                        }
                    }
                }
                // 由于一行只有一个错误,所以一个LVal不会同时未定义和为常量或其余Exp有问题
                lValHandler(stmtNode.getlValNode(), constCheck);
                addExpHandler(stmtNode.getExpNode().getAddExpNode(), constCheck);
                break;
            case EXP:
                if (stmtNode.getExpNode() != null) {
                    addExpHandler(stmtNode.getExpNode().getAddExpNode(), constCheck);
                }
                break;
            case BLOCK:
                curLayer++;
                SymbolTable childSymbolTable = new SymbolTable(curLayer, curSymbolTable);
                curSymbolTable.addChild(childSymbolTable);
                curSymbolTable = childSymbolTable;
                this.symbolTables.add(curSymbolTable);
                for (BlockItemNode blockItemNode : stmtNode.getBlockNode().getBlockItemNodes()) {
                    blockHandler(blockItemNode);
                }
                curLayer--;
                curSymbolTable = curSymbolTable.getParent();
                break;
            case IF:
                // 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
                lOrExpHandler(stmtNode.getCondNode().getlOrExpNode(), constCheck);
                stmtHandler(stmtNode.getStmtNodes().get(0));
                if (stmtNode.getElesToken() != null) {
                    stmtHandler(stmtNode.getStmtNodes().get(1));
                }
                break;
            case FOR:
                // 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
                // for层级++
                forLayer++;
                if (stmtNode.getForStmtOne() != null) {
                    forStmtHandler(stmtNode.getForStmtOne());
                }
                if (stmtNode.getCondNode() != null) {
                    lOrExpHandler(stmtNode.getCondNode().getlOrExpNode(), constCheck);
                }
                if (stmtNode.getForStmtTwo() != null) {
                    forStmtHandler(stmtNode.getForStmtTwo());
                }
                stmtHandler(stmtNode.getStmtNode());
                forLayer--;
                break;
            // | break; | continue; | return [Exp];
            // |  LVal '=' 'getint''('')'';'
            // | 'printf''('FormatString{','Exp}')'';'
            case BREAK:
            case CONTINUE:
                breakORContinueCheck(stmtNode.getBreakTokenOrContinueToken());
                break;
            case RETURN:
                // 对于int,如果有return,一定在末尾,直接将funcType置为null
                if (funcType == FuncSymbol.ReturnType.INT) {
                    funcType = null;
                }
                // 对于void,每次出现return,考虑是否有返回值,有则报错
                else if (funcType == FuncSymbol.ReturnType.VOID){
                    if (stmtNode.getExpNode() != null) {
                        // 报错行号为‘return’所在行号
                        errorHandler.addError(stmtNode.getReturnToken().getLineNum(), ErrorType.f);
                    }
                }
                if (stmtNode.getExpNode() != null) {
                    addExpHandler(stmtNode.getExpNode().getAddExpNode(), constCheck);
                }
                break;
            case GET:
                // 检查是否修改常量值
                identToken = stmtNode.getlValNode().getIdentToken();
                name = identToken.getValue();
                if ((valSymbol = getSymbol(name)) != null) {
                    // 有定义且不是函数定义里的函数参数
                    if (valSymbol.getClass() != FuncParamSymbol.class) {
                        if (((ValSymbol) valSymbol).isConst()) {
                            // 报错行号为<LVal>所在行号
                            errorHandler.addError(identToken.getLineNum(), ErrorType.h);
                        }
                    }
                }
                lValHandler(stmtNode.getlValNode(), constCheck);
                break;
            case PRINT:
                formatStringHandler(stmtNode);
                break;
            default:
                // TODO 未知错误
                break;
        }
    }

    public void formatStringHandler(StmtNode stmtNode) {
        String formatString = stmtNode.getFormatStringToken().getValue();
        List<ExpNode> expNodes = stmtNode.getExpNodes();
        int sum = 0, index = -1;
        String dStr = "%d";
        while ((index = formatString.indexOf(dStr, index + 1)) != -1 ) {
            sum++;
        }
        for (ExpNode expNode : expNodes) {
            addExpHandler(expNode.getAddExpNode(), false);
            sum--;
        }
        // exp分析完后发现没有一一对应占位符
        if (sum != 0) {
            // 报错行号为‘printf’所在行号
            errorHandler.addError(stmtNode.getPrintfToken().getLineNum(), ErrorType.l);
        }
    }


    public void forStmtHandler(ForStmtNode forStmtNode) {
        // ForStmt → LVal '=' Exp
        // 检查是否修改常量值
        Token identToken = forStmtNode.getlValNode().getIdentToken();
        String name = identToken.getValue();
        Symbol valSymbol;
        if ((valSymbol = getSymbol(name)) != null) {
            // 有定义,for语句里肯定不是函数定义的参数
            if (((ValSymbol) valSymbol).isConst()) {
                // 报错行号为<LVal>所在行号
                errorHandler.addError(identToken.getLineNum(), ErrorType.h);
            }
        }
        lValHandler(forStmtNode.getlValNode(), false);
        addExpHandler(forStmtNode.getExpNode().getAddExpNode(), false);
    }

    public void lOrExpHandler(LOrExpNode lOrExpNode, boolean constCheck) {
        //  LOrExp → LAndExp | LOrExp '||' LAndExp
        if (lOrExpNode.getlOrExpNode() != null) {
            lOrExpHandler(lOrExpNode.getlOrExpNode(), constCheck);
        }
        lAndExpHandler(lOrExpNode.getlAndExpNode(), constCheck);
    }

    public void lAndExpHandler(LAndExpNode lAndExpNode, boolean constCheck) {
        // LAndExp → EqExp | LAndExp '&&' EqExp
        if (lAndExpNode.getlAndExpNode() != null) {
            lAndExpHandler(lAndExpNode.getlAndExpNode(), constCheck);
        }
        eqExpHandler(lAndExpNode.getEqExpNode(), constCheck);
    }

    public void eqExpHandler(EqExpNode eqExpNode, boolean constCheck) {
        // EqExp → RelExp | EqExp ('==' | '!=') RelExp
        if (eqExpNode.getEqExpNode() != null) {
            eqExpHandler(eqExpNode.getEqExpNode(), constCheck);
        }
        relExpHandler(eqExpNode.getRelExpNode(), constCheck);
    }

    public void relExpHandler(RelExpNode relExpNode, boolean constCheck) {
        //  RelExp → AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp
        if (relExpNode.getRelExpNode() != null) {
            relExpHandler(relExpNode.getRelExpNode(), constCheck);
        }
        addExpHandler(relExpNode.getAddExpNode(), constCheck);
    }
}
