package node;

import token.Token;
import utils.IOUitils;

public class ForStmtNode {
    // ForStmt → LVal '=' Exp

    private static final String LABEL = "<ForStmt>\n";
    private LValNode lValNode;
    private Token assignToken;
    private ExpNode expNode;

    public ForStmtNode(LValNode lValNode, Token assignToken, ExpNode expNode) {
        this.lValNode = lValNode;
        this.assignToken = assignToken;
        this.expNode = expNode;
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

    public void show() {
        lValNode.show();
        IOUitils.output(assignToken.toString());
        expNode.show();
        IOUitils.output(LABEL);
    }
}
