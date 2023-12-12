package frontend;

import error.ErrorHandler;
import error.ErrorType;
import node.*;
import token.Token;
import token.TokenType;

import java.util.ArrayList;
import java.util.List;

public class Parser {
    private static final Parser PARSER = new Parser();

    public static Parser getPARSER() {
        return PARSER;
    }

    private Token currentToken;
    private CompUnitNode compUnitNode;
    private int index;
    private List<Token> tokens;
    private final Lexer lexer = Lexer.getLexer();
    private final ErrorHandler errorHandler = ErrorHandler.getErrorHandler();

    public void init() {
        index = 0;
        tokens = lexer.getTokens();
        currentToken = tokens.get(index);
    }

    /**
     * 检查是否Exp
     */
    public boolean checkExp() {
        // [Exp] ';' F = { (,Number,Ident,+,-,! }
        return switch (currentToken.getType()) {
            case LPARENT, INTCON, IDENFR, PLUS, MINU, NOT -> true;
            default -> false;
        };
    }


    public CompUnitNode getCompUnitNode() {
        return compUnitNode;
    }

    public void analyze() {
        this.compUnitNode = compUnit();
    }

    public void show() {
        compUnitNode.show();
    }

    /**
     * 匹配Token终结符
     */
    public Token expect(TokenType type) {
        // 句子由终结符组成，所有语法树最终调用此函数来处理终结符
        // 故在这里进行index++即可
        // System.out.printf("%s", currentToken.toString());
        if (currentToken.getType() == type) {
            Token ret = currentToken;
            if (index < tokens.size() - 1) {
                currentToken = tokens.get(++index);
            }
            return ret;
        }
        // 进行语法错误处理,报错行号为分号前一个非终结符所在行号
        else {
            int lineNum = tokens.get(index - 1).getLineNum();
            // 当不匹配时,当前字符继续匹配下一个语法成分,不要跳到下一个字符
            switch (type) {
                case SEMICN -> errorHandler.addError(lineNum, ErrorType.i);
                case RPARENT -> errorHandler.addError(lineNum, ErrorType.j);
                case RBRACK -> errorHandler.addError(lineNum, ErrorType.k);
                default -> {
                }
                // TODO 语法成分缺失错误
            }
            return null;
        }
    }

    public CompUnitNode compUnit() {
        List<DeclNode> declNodes = new ArrayList<>();
        List<FuncDefNode> funcDefNodes = new ArrayList<>();
        // {Decl} const int ident | int ident
        while (tokens.get(index + 1).getType() != TokenType.MAINTK && tokens.get(index + 2).getType() != TokenType.LPARENT) {
            DeclNode declNode = decl();
            declNodes.add(declNode);
        }
        // {FuncDef} void | int ident (
        while (tokens.get(index + 1).getType() != TokenType.MAINTK) {
            FuncDefNode funcDefNode = funcDef();
            funcDefNodes.add(funcDefNode);
        }
        // MainFuncDef int main (
        MainFuncDefNode mainFuncDefNode = mainFuncDef();
        return new CompUnitNode(declNodes, funcDefNodes, mainFuncDefNode);
    }

    public DeclNode decl() {
        // ConstDecl
        if (tokens.get(index).getType() == TokenType.CONSTTK) {
            ConstDeclNode constDeclNode = constDecl();
            return new DeclNode(constDeclNode, null);
        }
        // varDecl
        else if (tokens.get(index).getType() == TokenType.INTTK) {
            VarDeclNode varDeclNode = varDecl();
            return new DeclNode(null, varDeclNode);
        }
        return null;
    }

    public FuncDefNode funcDef() {
        // FuncDef → FuncType Ident '(' [FuncFParams] ')' Block
        FuncTypeNode funcTypeNode = funcType();
        Token identToken = expect(TokenType.IDENFR);
        Token leftParentToken = expect(TokenType.LPARENT);
        FuncFParamsNode funcFParamsNode = null;
        if (currentToken.getType() == TokenType.INTTK) {
            funcFParamsNode = funcFParams();
        }
        Token rightParentToken = expect(TokenType.RPARENT);
        BlockNode blockNode = block();
        return new FuncDefNode(funcTypeNode, identToken, leftParentToken, funcFParamsNode, rightParentToken, blockNode);
    }

    public MainFuncDefNode mainFuncDef() {
        // MainFuncDef → 'int' 'main' '(' ')' Block
        Token intToken = expect(TokenType.INTTK);
        Token mainToken = expect(TokenType.MAINTK);
        Token leftParentToken = expect(TokenType.LPARENT);
        Token rightParentToken = expect(TokenType.RPARENT);
        BlockNode blockNode = block();
        return new MainFuncDefNode(intToken, mainToken, leftParentToken, rightParentToken, blockNode);
    }

    public ConstDeclNode constDecl() {
        // ConstDecl → 'const' BType ConstDef { ',' ConstDef } ';'
        Token constToken = expect(TokenType.CONSTTK);
        BTypeNode bTypeNode = bType();
        List<ConstDefNode> constDefNodes = new ArrayList<>();
        List<Token> commas = new ArrayList<>();
        ConstDefNode constDefNode = constDef();
        constDefNodes.add(constDefNode);
        while (currentToken.getType() == TokenType.COMMA) {
            commas.add(currentToken);
            currentToken = tokens.get(++index);
            constDefNode = constDef();
            constDefNodes.add(constDefNode);
        }
        Token semicolonToken = expect(TokenType.SEMICN);
        return new ConstDeclNode(constToken, bTypeNode, constDefNodes, commas, semicolonToken);
    }

    public VarDeclNode varDecl() {
        // VarDecl → BType VarDef { ',' VarDef } ';'
        BTypeNode bTypeNode = bType();
        List<VarDefNode> varDefNodes = new ArrayList<>();
        VarDefNode varDefNode = varDef();
        varDefNodes.add(varDefNode);
        List<Token> commas = new ArrayList<>();
        while (currentToken.getType() == TokenType.COMMA) {
            commas.add(currentToken);
            currentToken = tokens.get(++index);
            varDefNode = varDef();
            varDefNodes.add(varDefNode);
        }
        Token semicolon = expect(TokenType.SEMICN);
        return new VarDeclNode(bTypeNode, varDefNodes, commas, semicolon);
    }

    public FuncTypeNode funcType() {
        //  FuncType → 'void' | 'int'
        if (currentToken.getType() == TokenType.VOIDTK || currentToken.getType() == TokenType.INTTK) {
            Token token = currentToken;
            currentToken = tokens.get(++index);
            return new FuncTypeNode(token);
        } else {
            // TODO 错误处理
            return null;
        }
    }

    public FuncFParamsNode funcFParams() {
        // FuncFParams → FuncFParam { ',' FuncFParam }
        List<FuncFParamNode> funcFParamNodes = new ArrayList<>();
        List<Token> commas = new ArrayList<>();
        FuncFParamNode funcFParamNode = funcFParam();
        funcFParamNodes.add(funcFParamNode);
        // ','不会缺少,可以用
        while (currentToken.getType() == TokenType.COMMA) {
            commas.add(currentToken);
            currentToken = tokens.get(++index);
            funcFParamNode = funcFParam();
            funcFParamNodes.add(funcFParamNode);
        }
        return new FuncFParamsNode(funcFParamNodes, commas);
    }

    public BlockNode block() {
        // Block → '{' { BlockItem } '}'
        Token leftBraceToken = expect(TokenType.LBRACE);
        List<BlockItemNode> blockItemNodes = new ArrayList<>();
        BlockItemNode blockItemNode;
        while (currentToken.getType() != TokenType.RBRACE) {
            blockItemNode = blockItem();
            blockItemNodes.add(blockItemNode);
        }
        Token rightBraceToken = expect(TokenType.RBRACE);
        return new BlockNode(leftBraceToken, blockItemNodes, rightBraceToken);
    }

    public BTypeNode bType() {
        // BType → 'int'
        Token intToken = expect(TokenType.INTTK);
        return new BTypeNode(intToken);
    }

    public ConstDefNode constDef() {
        // ConstDef → Ident { '[' ConstExp ']' } '=' ConstInitVal
        Token identToken = expect(TokenType.IDENFR);
        List<Token> leftBracketTokens = new ArrayList<>();
        List<Token> rightBracketTokens = new ArrayList<>();
        List<ConstExpNode> constExpNodes = new ArrayList<>();
        while (currentToken.getType() == TokenType.LBRACK) {
            Token leftBracketToken = expect(TokenType.LBRACK);
            leftBracketTokens.add(leftBracketToken);
            ConstExpNode constExpNode = constExp();
            constExpNodes.add(constExpNode);
            Token rightBracketToken = expect(TokenType.RBRACK);
            rightBracketTokens.add(rightBracketToken);
        }
        Token assignToken = expect(TokenType.ASSIGN);
        ConstInitValNode constInitValNode = constInitVal();
        return new ConstDefNode(identToken, leftBracketTokens, constExpNodes, rightBracketTokens, assignToken, constInitValNode);
    }

    public VarDefNode varDef() {
        // VarDef → Ident { '[' ConstExp ']' }
        // | Ident { '[' ConstExp ']' } '=' InitVal
        Token identToken = expect(TokenType.IDENFR);
        List<Token> leftBracketTokens = new ArrayList<>();
        List<Token> rightBracketTokens = new ArrayList<>();
        List<ConstExpNode> constExpNodes = new ArrayList<>();
        while (currentToken.getType() == TokenType.LBRACK) {
            Token leftBracketToken = expect(TokenType.LBRACK);
            leftBracketTokens.add(leftBracketToken);
            ConstExpNode constExpNode = constExp();
            constExpNodes.add(constExpNode);
            Token rightBracketToken = expect(TokenType.RBRACK);
            rightBracketTokens.add(rightBracketToken);
        }
        Token assignToken = null;
        InitValNode initValNode = null;
        if (currentToken.getType() == TokenType.ASSIGN) {
            assignToken = expect(TokenType.ASSIGN);
            initValNode = initVal();
        }
        return new VarDefNode(identToken, leftBracketTokens, constExpNodes, rightBracketTokens, assignToken, initValNode);
    }

    public FuncFParamNode funcFParam() {
        // FuncFParam → BType Ident ['[' ']' { '[' ConstExp ']' }]
        BTypeNode bTypeNode = bType();
        Token identToken = expect(TokenType.IDENFR);
        List<Token> leftBracketTokens = new ArrayList<>();
        List<Token> rightBracketTokens = new ArrayList<>();
        // 不是参数间隔',', 也不是函数参数定义结尾')',而是'[',这个不会缺少
        if (currentToken.getType() == TokenType.LBRACK) {
            Token leftBracketToken = expect(TokenType.LBRACK);
            leftBracketTokens.add(leftBracketToken);
            Token rightBracketToken = expect(TokenType.RBRACK);
            rightBracketTokens.add(rightBracketToken);
        }
        ConstExpNode constExpNode = null;
        if (currentToken.getType() == TokenType.LBRACK) {
            Token leftBracketToken = expect(TokenType.LBRACK);
            leftBracketTokens.add(leftBracketToken);
            // ConstExp
            constExpNode = constExp();
            Token rightBracketToken = expect(TokenType.RBRACK);
            rightBracketTokens.add(rightBracketToken);
        }
        return new FuncFParamNode(bTypeNode, identToken, leftBracketTokens, rightBracketTokens, constExpNode);
    }

    public BlockItemNode blockItem() {
        // BlockItem → Decl | Stmt
        // FIRST(Decl) = {const, int}
        // FIRST(Stmt) = {Ident, +, -, !,}
        // {, if, for, break, continue, return, printf, (, Number}
        DeclNode declNode = null;
        StmtNode stmtNode = null;
        if (currentToken.getType() == TokenType.CONSTTK || currentToken.getType() == TokenType.INTTK) {
            declNode = decl();
        } else {
            stmtNode = stmt();
        }
        return new BlockItemNode(declNode, stmtNode);
    }

    public ConstExpNode constExp() {
        // ConstExp → AddExp
        // 是否常量Ident要归到语义分析
        AddExpNode addExpNode = addExp();
        return new ConstExpNode(addExpNode);
    }

    public ConstInitValNode constInitVal() {
        // ConstInitVal → ConstExp
        // | '{' [ ConstInitVal { ',' ConstInitVal } ] '}'
        ConstExpNode constExpNode = null;
        List<ConstInitValNode> constInitValNodes = new ArrayList<>();
        List<Token> commas = new ArrayList<>();
        Token leftBraceToken = null;
        Token rightBraceToken = null;
        if (currentToken.getType() == TokenType.LBRACE) {
            leftBraceToken = expect(TokenType.LBRACE);
            while (currentToken.getType() != TokenType.RBRACE) {
                ConstInitValNode constInitValNode = constInitVal();
                constInitValNodes.add(constInitValNode);
                // 不是结尾,还有初始值
                if (currentToken.getType() != TokenType.RBRACE) {
                    Token comma = expect(TokenType.COMMA);
                    commas.add(comma);
                }
            }
            rightBraceToken = expect(TokenType.RBRACE);
        } else {
            // ConstExp
            constExpNode = constExp();
        }
        return new ConstInitValNode(constExpNode, leftBraceToken, constInitValNodes, commas, rightBraceToken);
    }

    public InitValNode initVal() {
        // InitVal → Exp | '{' [ InitVal { ',' InitVal } ] '}'
        ExpNode expNode = null;
        Token leftBraceToken = null;
        List<Token> commas = new ArrayList<>();
        List<InitValNode> initValNodes = new ArrayList<>();
        Token rightBraceToken = null;
        if (currentToken.getType() == TokenType.LBRACE) {
            leftBraceToken = expect(TokenType.LBRACE);
            while (currentToken.getType() != TokenType.RBRACE) {
                InitValNode initValNode = initVal();
                initValNodes.add(initValNode);
                // 判断是否结束
                if (currentToken.getType() != TokenType.RBRACE) {
                    Token comma = expect(TokenType.COMMA);
                    commas.add(comma);
                }
            }
            rightBraceToken = expect(TokenType.RBRACE);
        } else {
            expNode = exp();
        }
        return new InitValNode(expNode, leftBraceToken, initValNodes, commas, rightBraceToken);
    }

    public StmtNode stmt() {
        //  Stmt → LVal '=' Exp ';' F = {Ident}
        //| [Exp] ';' F = { (,Number,Ident,+,-,! }
        //| Block F = { { }
        //| 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
        //| 'for' '(' [ForStmt] ';' [Cond] ';' [forStmt] ')' Stmt
        //| 'break' ';' | 'continue' ';'
        //| 'return' [Exp] ';'
        //| LVal '=' 'getint''('')'';' F = {Ident}
        //| 'printf''('FormatString{','Exp}')'';'

        if (currentToken.getType() == TokenType.IDENFR) {
            // 若是Exp,此解析当然可行;若是LVal,此解析也能导出LVal
            ExpNode expNode = exp();
            // 因为';'可能缺失,所以需要判断是不是等号
            if (currentToken.getType() != TokenType.ASSIGN) {
                // [Exp] ';'
                Token semicolonToken = expect(TokenType.SEMICN);
                return new StmtNode(StmtNode.StmtType.EXP, expNode, semicolonToken);
            }
            else {
                LValNode lValNode = expNode.getAddExpNode().getMulExpNode().getUnaryExpNode().getPrimaryExpNode().getlValNode();
                Token assignToken = expect(TokenType.ASSIGN);
                if (currentToken.getType() == TokenType.GETINTTK) {
                    // LVal '=' 'getint''('')'';'
                    Token getIntToken = expect(TokenType.GETINTTK);
                    Token leftParentToken = expect(TokenType.LPARENT);
                    Token rightParentToken = expect(TokenType.RPARENT);
                    Token semicolonToken = expect(TokenType.SEMICN);
                    return new StmtNode(StmtNode.StmtType.GET, lValNode, assignToken, getIntToken, leftParentToken, rightParentToken, semicolonToken);
                } else {
                    // LVal '=' Exp ';'
                    ExpNode lExpNode = exp();
                    Token semicolonToken = expect(TokenType.SEMICN);
                    return new StmtNode(StmtNode.StmtType.ASSIGN, lValNode, assignToken, lExpNode, semicolonToken);
                }
            }
        } else if (currentToken.getType() == TokenType.LBRACE) {
            // Stmt -> Block
            BlockNode blockNode = block();
            return new StmtNode(StmtNode.StmtType.BLOCK, blockNode);
        } else if (currentToken.getType() == TokenType.IFTK) {
            // Stmt -> 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
            Token ifToken = expect(TokenType.IFTK);
            Token leftParentToken = expect(TokenType.LPARENT);
            CondNode condNode = cond();
            Token rightParentToken = expect(TokenType.RPARENT);
            List<StmtNode> stmtNodes = new ArrayList<>();
            StmtNode stmtNode = stmt();
            stmtNodes.add(stmtNode);
            Token elseToken = null;
            if (currentToken.getType() == TokenType.ELSETK) {
                elseToken = expect(TokenType.ELSETK);
                stmtNode = stmt();
                stmtNodes.add(stmtNode);
            }
            return new StmtNode(StmtNode.StmtType.IF, ifToken, leftParentToken, condNode, rightParentToken, stmtNodes, elseToken);
        } else if (currentToken.getType() == TokenType.BREAKTK) {
            // 'break' ';'
            Token breakToken = expect(TokenType.BREAKTK);
            Token semicolonToken = expect(TokenType.SEMICN);
            return new StmtNode(StmtNode.StmtType.BREAK, semicolonToken, breakToken);
        } else if (currentToken.getType() == TokenType.CONTINUETK) {
            // 'continue' ';'
            Token continueToken = expect(TokenType.CONTINUETK);
            Token semicolonToken = expect(TokenType.SEMICN);
            return new StmtNode(StmtNode.StmtType.CONTINUE, semicolonToken, continueToken);
        } else if (currentToken.getType() == TokenType.RETURNTK) {
            // 'return' [Exp] ';'
            Token returnToken = expect(TokenType.RETURNTK);
            ExpNode expNode = null;
            // ';'可能缺失,判断是否Exp
            if (checkExp()) {
                expNode = exp();
            }
            Token semicolonToken = expect(TokenType.SEMICN);
            return new StmtNode(StmtNode.StmtType.RETURN, returnToken, expNode, semicolonToken);
        } else if (currentToken.getType() == TokenType.PRINTFTK) {
            // 'printf''('FormatString{','Exp}')'';'
            Token printfToken = expect(TokenType.PRINTFTK);
            Token leftParentToken = expect(TokenType.LPARENT);
            Token formatStringToken = expect(TokenType.STRCON);
            List<Token> commas = new ArrayList<>();
            List<ExpNode> expNodes = new ArrayList<>();
            // ')'可能缺失,不可用于判断; 但','不会缺失
            while (currentToken.getType() == TokenType.COMMA) {
                Token comma = expect(TokenType.COMMA);
                commas.add(comma);
                ExpNode expNode = exp();
                expNodes.add(expNode);
            }
            Token rightParentToken = expect(TokenType.RPARENT);
            Token semicolonToken = expect(TokenType.SEMICN);
            return new StmtNode(StmtNode.StmtType.PRINT, printfToken, leftParentToken, rightParentToken, formatStringToken, commas, expNodes, semicolonToken);
        } else if (currentToken.getType() == TokenType.FORTK) {
            // 'for' '(' [ForStmt] ';' [Cond] ';' [forStmt] ')' Stmt
            Token forToken = expect(TokenType.FORTK);
            Token leftParentToken = expect(TokenType.LPARENT);
            List<Token> semicolonTokens = new ArrayList<>();
            ForStmtNode forStmtOne = null, forStmtTwo = null;
            CondNode condNode = null;
            // ';'可能缺失,不可用于判断
            // //  ForStmt → LVal '=' Exp; LVal → Ident {'[' Exp ']'}
            // Cond开头为Ident的情况有:LVal, Ident '(' [FuncRParams] ')'
            if ((currentToken.getType() == TokenType.IDENFR) && (tokens.get(index+1).getType() != TokenType.LPARENT)) {
                // 此时尚不能确定是ForStmt还是Cond,为了之后出错回退,记录当前index
                int temp_index = index;
                forStmtOne = forStmt();
                if (forStmtOne == null) {
                    // 说明是Cond,进行回退,此时缺失了第一个';',正好对上下面的判断
                    index = temp_index;
                    currentToken = tokens.get(index);
                }
            }
            Token semicolonToken = expect(TokenType.SEMICN);
            semicolonTokens.add(semicolonToken);
            //  Cond → LOrExp LOrExp的FIRST集与Exp相同
            // 注意:这里不能简单地判断是Exp就进行Cond的分析,若缺失了第二个';'会导致判断错误
            LValNode lValNode;
            if (checkExp()) {
                // 若是Ident, LVal -> Ident{[Exp]}, Ident({FuncRParams})
                if (currentToken.getType() == TokenType.IDENFR) {
                    // 是函数调用,那么不可能是ForStmt -> LVal = Exp
                    if (tokens.get(index+1).getType() == TokenType.LPARENT) {
                        condNode = cond();
                    }
                    // 否则,先按LVal分析
                    else {
                        int backIndex = index;
                        lValNode = lVal();
                        // 判断后面是否有'=',若有,证明是ForStmt
                        if(currentToken.getType() == TokenType.ASSIGN) {
                            Token assignToken = expect(TokenType.ASSIGN);
                            ExpNode expNode = exp();
                            // 这里的如果是ForStmt一定是第二句,因为有第一句必然已经被识别
                            forStmtTwo = new ForStmtNode(lValNode, assignToken, expNode);
                        }
                        // 没有等号,说明是Cond,回退分析
                        else {
                            index = backIndex;
                            currentToken = tokens.get(index);
                            condNode = cond();
                        }
                    }
                }
                // 不是Ident,必为Cond
                else {
                    condNode = cond();
                }
            }
            semicolonToken = expect(TokenType.SEMICN);
            semicolonTokens.add(semicolonToken);
            // ')'可能缺失,不可用于判断
            // 如果已经在上面Cond被判断过,说明缺失第二个分号,此时后面必然不是Ident,所以仍然可以判断
            if (currentToken.getType() == TokenType.IDENFR) {
                forStmtTwo = forStmt();
            }
            Token rightParentToken = expect(TokenType.RPARENT);
            StmtNode stmtNode = stmt();
            return new StmtNode(StmtNode.StmtType.FOR, forToken, leftParentToken, forStmtOne, semicolonTokens, condNode, forStmtTwo, rightParentToken, stmtNode);
        } else if (currentToken.getType() == TokenType.SEMICN) {
            // [Exp] ';'
            Token semicolonToken = expect(TokenType.SEMICN);
            return new StmtNode(StmtNode.StmtType.EXP, (ExpNode) null, semicolonToken);
        } else {
            // [Exp] ';'
            ExpNode expNode = exp();
            Token semicolonToken = expect(TokenType.SEMICN);
            return new StmtNode(StmtNode.StmtType.EXP, expNode, semicolonToken);
        }
    }


    public ForStmtNode forStmt() {
        LValNode lValNode = lVal();
        Token assignToken = expect(TokenType.ASSIGN);
        if (assignToken == null) {
            // 没有无'='的错误定义,所以这是Cond,返回进行回退
            return null;
        }
        ExpNode expNode = exp();
        return new ForStmtNode(lValNode, assignToken, expNode);
    }

    public AddExpNode addExp() {
        // AddExp → MulExp | AddExp ('+' | '−') MulExp
        MulExpNode mulExpNode = mulExp();
        AddExpNode addExpNode = new AddExpNode(mulExpNode, null, null);
        Token opToken;
        AddExpNode temp;
        while (addOpCheck()) {
            // 说明这个MulExp是AddExp
            opToken = currentToken;
            currentToken = tokens.get(++index);
            mulExpNode = mulExp();
            temp = addExpNode;
            addExpNode = new AddExpNode(mulExpNode, temp, opToken);
        }
        // 若没有进while,说明直接是MulExp,那么直接返回即可
        return addExpNode;
    }

    public ExpNode exp() {
        // Exp → AddExp
        AddExpNode addExpNode = addExp();
        return new ExpNode(addExpNode);
    }

    public CondNode cond() {
        // Cond → LOrExp
        LOrExpNode lOrExpNode = lOrExp();
        return new CondNode(lOrExpNode);
    }

    public MulExpNode mulExp() {
        // MulExp → UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
        UnaryExpNode unaryExpNode = unaryExp();
        MulExpNode mulExpNode = new MulExpNode(unaryExpNode, null, null);
        MulExpNode temp;
        Token opToken;
        while (mulOpCheck()) {
            // 说明此时的Unary是MulExp
            opToken = currentToken;
            currentToken = tokens.get(++index);
            unaryExpNode = unaryExp();
            temp = mulExpNode;
            mulExpNode = new MulExpNode(unaryExpNode, temp, opToken);
        }
        // 若没进while,淡出UnaryExp组成,直接返回即可
        return mulExpNode;
    }

    public LOrExpNode lOrExp() {
        // LOrExp → LAndExp | LOrExp '||' LAndExp
        LAndExpNode lAndExpNode = lAndExp();
        LOrExpNode lOrExpNode = new LOrExpNode(lAndExpNode, null, null);
        Token orToken;
        while (currentToken.getType() == TokenType.OR) {
            orToken = currentToken;
            currentToken = tokens.get(++index);
            lAndExpNode = lAndExp();
            lOrExpNode = new LOrExpNode(lAndExpNode, lOrExpNode, orToken);
        }
        return lOrExpNode;
    }

    public UnaryExpNode unaryExp() {
        //  UnaryExp → PrimaryExp  F = { (, Ident, Number }
        // | Ident '(' [FuncRParams] ')'  F = {Ident}
        // | UnaryOp UnaryExp F = { +, -, !}
        if (unaryOpCheck()) {
            // UnaryExp -> UnaryOp UnaryExp
            UnaryOpNode unaryOpNode = unaryOp();
            UnaryExpNode unaryExpNode = unaryExp();
            return new UnaryExpNode(unaryOpNode, unaryExpNode);
        }
        // PrimaryExp -> LVal -> Ident {'[' Exp ']'}
        else if (currentToken.getType() == TokenType.IDENFR) {
            if (tokens.get(index + 1).getType() == TokenType.LPARENT) {
                // Ident '(' [FuncRParams] ')'
                Token identToken = expect(TokenType.IDENFR);
                Token leftParentToken = expect(TokenType.LPARENT);
                FuncRParamsNode funcRParamsNode = null;
                // 注意')'可能缺失,所以需要判断后面是不是Exp
                if (checkExp()) {
                    funcRParamsNode = funcRParams();
                }
                Token rightParentToken = expect(TokenType.RPARENT);
                return new UnaryExpNode(identToken, leftParentToken, funcRParamsNode, rightParentToken);
            } else {
                // UnaryExp → PrimaryExp
                PrimaryExpNode primaryExpNode = primaryExp();
                return new UnaryExpNode(primaryExpNode);
            }
        } else {
            // UnaryExp → PrimaryExp
            PrimaryExpNode primaryExpNode = primaryExp();
            return new UnaryExpNode(primaryExpNode);
        }
    }

    public LAndExpNode lAndExp() {
        // LAndExp → EqExp | LAndExp '&&' EqExp
        EqExpNode eqExpNode = eqExp();
        LAndExpNode lAndExpNode = new LAndExpNode(null, null, eqExpNode);
        LAndExpNode temp;
        Token andToken;
        while (currentToken.getType() == TokenType.AND) {
            andToken = currentToken;
            currentToken = tokens.get(++index);
            eqExpNode = eqExp();
            temp = lAndExpNode;
            lAndExpNode = new LAndExpNode(temp, andToken, eqExpNode);
        }
        return lAndExpNode;
    }

    public UnaryOpNode unaryOp() {
        // UnaryOp → '+' | '−' | '!'
        Token opToken = currentToken;
        currentToken = tokens.get(++index);
        return new UnaryOpNode(opToken);
    }

    public FuncRParamsNode funcRParams() {
        // FuncRParams → Exp { ',' Exp }
        List<ExpNode> expNodes = new ArrayList<>();
        List<Token> commas = new ArrayList<>();
        ExpNode expNode = exp();
        expNodes.add(expNode);
        while (currentToken.getType() == TokenType.COMMA) {
            Token comma = expect(TokenType.COMMA);
            commas.add(comma);
            expNode = exp();
            expNodes.add(expNode);
        }
        return new FuncRParamsNode(expNodes, commas);
    }

    public PrimaryExpNode primaryExp() {
        // PrimaryExp → '(' Exp ')' | LVal | Number
        if (currentToken.getType() == TokenType.INTCON) {
            NumberNode numberNode = number();
            return new PrimaryExpNode(numberNode);
        } else if (currentToken.getType() == TokenType.IDENFR) {
            LValNode lValNode = lVal();
            return new PrimaryExpNode(lValNode);
        } else {
            Token leftParentToken = expect(TokenType.LPARENT);
            ExpNode expNode = exp();
            Token rightParentToken = expect(TokenType.RPARENT);
            return new PrimaryExpNode(leftParentToken, expNode, rightParentToken);
        }
    }

    public EqExpNode eqExp() {
        //  EqExp → RelExp | EqExp ('==' | '!=') RelExp
        RelExpNode relExpNode = relExp();
        EqExpNode eqExpNode = new EqExpNode(relExpNode, null, null);
        EqExpNode temp;
        Token opToken;
        while (eqOpCheck()) {
            opToken = currentToken;
            currentToken = tokens.get(++index);
            relExpNode = relExp();
            temp = eqExpNode;
            eqExpNode = new EqExpNode(relExpNode, temp, opToken);
        }
        return eqExpNode;
    }


    public NumberNode number() {
        Token intConstToken = expect(TokenType.INTCON);
        return new NumberNode(intConstToken);
    }

    public LValNode lVal() {
        //  LVal → Ident {'[' Exp ']'}
        Token identToken = expect(TokenType.IDENFR);
        List<ExpNode> expNodes = new ArrayList<>();
        List<Token> leftBracketTokens = new ArrayList<>();
        List<Token> rightBracketTokens = new ArrayList<>();
        while (currentToken.getType() == TokenType.LBRACK) {
            Token leftBracketToken = expect(TokenType.LBRACK);
            leftBracketTokens.add(leftBracketToken);
            ExpNode expNode = exp();
            expNodes.add(expNode);
            Token rightBracketToken = expect(TokenType.RBRACK);
            rightBracketTokens.add(rightBracketToken);
        }
        return new LValNode(identToken, leftBracketTokens, expNodes, rightBracketTokens);
    }

    public RelExpNode relExp() {
        // RelExp → AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp
        AddExpNode addExpNode = addExp();
        RelExpNode relExpNode = new RelExpNode(addExpNode, null, null);
        RelExpNode temp;
        Token opToken;
        while (relOpCheck()) {
            opToken = currentToken;
            currentToken = tokens.get(++index);
            addExpNode = addExp();
            temp = relExpNode;
            relExpNode = new RelExpNode(addExpNode, temp, opToken);
        }
        return relExpNode;
    }

    public boolean addOpCheck() {
        return currentToken.getType() == TokenType.PLUS
                || currentToken.getType() == TokenType.MINU;
    }

    public boolean mulOpCheck() {
        return currentToken.getType() == TokenType.MULT
                || currentToken.getType() == TokenType.DIV
                || currentToken.getType() == TokenType.MOD;
    }

    public boolean unaryOpCheck() {
        return currentToken.getType() == TokenType.PLUS
                || currentToken.getType() == TokenType.MINU
                || currentToken.getType() == TokenType.NOT;
    }

    public boolean eqOpCheck() {
        return currentToken.getType() == TokenType.EQL
                || currentToken.getType() == TokenType.NEQ;
    }

    public boolean relOpCheck() {
        return currentToken.getType() == TokenType.LSS
                || currentToken.getType() == TokenType.LEQ
                || currentToken.getType() == TokenType.GRE
                || currentToken.getType() == TokenType.GEQ;
    }
}
