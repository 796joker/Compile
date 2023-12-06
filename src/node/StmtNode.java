package node;

import token.Token;
import utils.IOUitils;

import java.util.List;

public class StmtNode {
    // Stmt → LVal '=' Exp ';'
    //| [Exp] ';'
    //| Block
    //| 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
    //| 'for' '(' [ForStmt] ';' [Cond] ';' [forStmt] ')' Stmt
    //| 'break' ';' | 'continue' ';'
    //| 'return' [Exp] ';' // 1.有Exp 2.无Exp
    //| LVal '=' 'getint''('')'';'
    //| 'printf''('FormatString{','Exp}')'';'

    /**
     * 为了区分构造函数,编写内部类
     */
    public enum StmtType{
        ASSIGN,
        EXP,
        BLOCK,
        IF,
        FOR,
        BREAK,
        CONTINUE,
        RETURN,
        GET,
        PRINT
    }


    private static final String LABEL = "<Stmt>\n";
    private StmtType stmtType;
    private LValNode lValNode;
    private Token assignToken;
    private ExpNode expNode;
    private Token semicolonToken;
    private BlockNode blockNode;
    private Token ifToken;
    private Token leftParentToken;
    private CondNode condNode;
    private Token rightParentToken;
    private List<StmtNode> stmtNodes;
    private Token elesToken;
    private Token forToken;
    private ForStmtNode forStmtOne;
    private ForStmtNode forStmtTwo;
    private StmtNode stmtNode;
    private List<Token> semicolonTokens;
    private Token breakTokenOrContinueToken;
    private Token returnToken;
    private Token getIntToken;
    private Token printfToken;
    private Token formatStringToken;
    private List<Token> commas;
    private List<ExpNode> expNodes;

    public StmtNode(StmtType stmtType, LValNode lValNode, Token assignToken, ExpNode expNode, Token semicolonToken) {
        // LVal '=' Exp ';'
        this.stmtType = stmtType;
        this.lValNode = lValNode;
        this.assignToken = assignToken;
        this.expNode = expNode;
        this.semicolonToken = semicolonToken;
    }

    public StmtNode(StmtType stmtType, ExpNode expNode, Token semicolonToken) {
        //  [Exp] ';'
        this.stmtType = stmtType;
        this.expNode = expNode;
        this.semicolonToken = semicolonToken;
    }

    public StmtNode(StmtType stmtType, BlockNode blockNode) {
        // Block
        this.stmtType = stmtType;
        this.blockNode = blockNode;
    }

    public StmtNode(StmtType stmtType, Token ifToken, Token leftParentToken, CondNode condNode, Token rightParentToken, List<StmtNode> stmtNodes, Token elesToken) {
        // 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
        this.stmtType = stmtType;
        this.ifToken = ifToken;
        this.leftParentToken = leftParentToken;
        this.condNode = condNode;
        this.rightParentToken = rightParentToken;
        this.stmtNodes = stmtNodes;
        this.elesToken = elesToken;
    }

    public StmtNode(StmtType stmtType, Token forToken, Token leftParentToken, ForStmtNode forStmtOne, List<Token> semicolonTokens, CondNode condNode, ForStmtNode forStmtTwo, Token rightParentToken, StmtNode stmtNode) {
        // 'for' '(' [ForStmt] ';' [Cond] ';' [forStmt] ')' Stmt
        this.stmtType = stmtType;
        this.leftParentToken = leftParentToken;
        this.forStmtOne = forStmtOne;
        this.condNode = condNode;
        this.forStmtTwo = forStmtTwo;
        this.rightParentToken = rightParentToken;
        this.stmtNode = stmtNode;
        this.forToken = forToken;
        this.semicolonTokens = semicolonTokens;
    }


    public StmtNode(StmtType stmtType, Token semicolonToken, Token breakTokenOrContinueToken) {
        // 'break' ';' | 'continue' ';'
        this.stmtType = stmtType;
        this.semicolonToken = semicolonToken;
        this.breakTokenOrContinueToken = breakTokenOrContinueToken;
    }

    public StmtNode(StmtType stmtType,  Token returnToken, ExpNode expNode, Token semicolonToken) {
        // 'return' [Exp] ';'
        this.stmtType = stmtType;
        this.expNode = expNode;
        this.semicolonToken = semicolonToken;
        this.returnToken = returnToken;
    }

    public StmtNode(StmtType stmtType, LValNode lValNode, Token assignToken, Token getIntToken, Token leftParentToken, Token rightParentToken, Token semicolonToken) {
        // LVal '=' 'getint''('')'';'
        this.stmtType = stmtType;
        this.lValNode = lValNode;
        this.assignToken = assignToken;
        this.semicolonToken = semicolonToken;
        this.leftParentToken = leftParentToken;
        this.rightParentToken = rightParentToken;
        this.getIntToken = getIntToken;
    }

    public StmtNode(StmtType stmtType, Token printfToken, Token leftParentToken, Token rightParentToken, Token formatStringToken, List<Token> commas, List<ExpNode> expNodes, Token semicolonToken) {
        // 'printf''('FormatString{','Exp}')'';'
        this.stmtType = stmtType;
        this.semicolonToken = semicolonToken;
        this.leftParentToken = leftParentToken;
        this.rightParentToken = rightParentToken;
        this.printfToken = printfToken;
        this.formatStringToken = formatStringToken;
        this.commas = commas;
        this.expNodes = expNodes;
    }

    public StmtType getStmtType() {
        return stmtType;
    }

    public LValNode getlValNode() {
        return lValNode;
    }

    public Token getAssignToken() {
        return assignToken;
    }

    public ExpNode getExpNode() {
        return expNode;
    }

    public Token getSemicolonToken() {
        return semicolonToken;
    }

    public BlockNode getBlockNode() {
        return blockNode;
    }

    public Token getIfToken() {
        return ifToken;
    }

    public Token getLeftParentToken() {
        return leftParentToken;
    }

    public CondNode getCondNode() {
        return condNode;
    }

    public Token getRightParentToken() {
        return rightParentToken;
    }

    public StmtNode getStmtNode() {
        return stmtNode;
    }

    public List<StmtNode> getStmtNodes() {
        return stmtNodes;
    }

    public Token getElesToken() {
        return elesToken;
    }

    public Token getForToken() {
        return forToken;
    }

    public List<Token> getSemicolonTokens() {
        return semicolonTokens;
    }

    public Token getBreakTokenOrContinueToken() {
        return breakTokenOrContinueToken;
    }

    public Token getReturnToken() {
        return returnToken;
    }

    public Token getGetIntToken() {
        return getIntToken;
    }

    public Token getPrintfToken() {
        return printfToken;
    }

    public Token getFormatStringToken() {
        return formatStringToken;
    }

    public List<Token> getCommas() {
        return commas;
    }

    public List<ExpNode> getExpNodes() {
        return expNodes;
    }

    public ForStmtNode getForStmtOne() {
        return forStmtOne;
    }

    public ForStmtNode getForStmtTwo() {
        return forStmtTwo;
    }

    public void show() {
        if (stmtType == StmtType.ASSIGN) {
            lValNode.show();
            IOUitils.output(assignToken.toString());
            expNode.show();
            IOUitils.output(semicolonToken.toString());
        }
        else if (stmtType == StmtType.EXP) {
            if (expNode != null) {
                expNode.show();
            }
            IOUitils.output(semicolonToken.toString());
        }
        else if (stmtType == StmtType.BLOCK) {
            blockNode.show();
        }
        else if (stmtType == StmtType.IF) {
            // 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
            IOUitils.output(ifToken.toString());
            IOUitils.output(leftParentToken.toString());
            condNode.show();
            IOUitils.output(rightParentToken.toString());
            stmtNodes.get(0).show();
            if (elesToken != null) {
                IOUitils.output(elesToken.toString());
                stmtNodes.get(1).show();
            }
        }
        else if (stmtType == StmtType.FOR) {
            // 'for' '(' [ForStmt] ';' [Cond] ';' [forStmt] ')' Stmt
            IOUitils.output(forToken.toString());
            IOUitils.output(leftParentToken.toString());
            if (forStmtOne != null) {
                forStmtOne.show();
            }
            IOUitils.output(semicolonTokens.get(0).toString());
            if (condNode != null) {
                condNode.show();
            }
            IOUitils.output(semicolonTokens.get(1).toString());
            if (forStmtTwo != null) {
                forStmtTwo.show();
            }
            IOUitils.output(rightParentToken.toString());
            stmtNode.show();
        }
        else if (stmtType == StmtType.BREAK) {
            IOUitils.output(breakTokenOrContinueToken.toString());
            IOUitils.output(semicolonToken.toString());
        }
        else if (stmtType == StmtType.CONTINUE) {
            IOUitils.output(breakTokenOrContinueToken.toString());
            IOUitils.output(semicolonToken.toString());
        }
        else if (stmtType == StmtType.RETURN) {
            // 'return' [Exp] ';'
            IOUitils.output(returnToken.toString());
            if (expNode != null) {
                expNode.show();
            }
            IOUitils.output(semicolonToken.toString());
        }
        else if (stmtType == StmtType.GET) {
            lValNode.show();
            IOUitils.output(assignToken.toString());
            IOUitils.output(getIntToken.toString());
            IOUitils.output(leftParentToken.toString());
            IOUitils.output(rightParentToken.toString());
            IOUitils.output(semicolonToken.toString());
        }
        else if (stmtType == StmtType.PRINT) {
            IOUitils.output(printfToken.toString());
            IOUitils.output(leftParentToken.toString());
            IOUitils.output(formatStringToken.toString());
            for (int i=0; i<expNodes.size(); i++) {
                IOUitils.output(commas.get(i).toString());
                expNodes.get(i).show();
            }
            IOUitils.output(rightParentToken.toString());
            IOUitils.output(semicolonToken.toString());
        }
        IOUitils.output(LABEL);
    }
}
