package node;

import token.Token;
import utils.IOUitils;

public class PrimaryExpNode {
    // PrimaryExp → '(' Exp ')' | LVal | Number

    private static final String LABEL = "<PrimaryExp>\n";
    private Token leftBracketToken = null;
    private ExpNode expNode = null;
    private Token rightBracketToken = null;
    private LValNode lValNode = null;
    private NumberNode numberNode = null;

    public PrimaryExpNode(Token leftBracketToken, ExpNode expNode, Token rightBracketToken) {
        this.leftBracketToken = leftBracketToken;
        this.expNode = expNode;
        this.rightBracketToken = rightBracketToken;
    }

    public PrimaryExpNode(LValNode lValNode) {
        this.lValNode = lValNode;
    }

    public PrimaryExpNode(NumberNode numberNode) {
        this.numberNode = numberNode;
    }

    public ExpNode getExpNode() {
        return expNode;
    }

    public LValNode getlValNode() {
        return lValNode;
    }

    public NumberNode getNumberNode() {
        return numberNode;
    }

    public void show() {
        if (leftBracketToken != null) {
            IOUitils.output(leftBracketToken.toString());
            expNode.show();
            IOUitils.output(rightBracketToken.toString());
        }
        else if (lValNode != null) {
            lValNode.show();
        }
        else if (numberNode != null) {
            numberNode.show();
        }
        IOUitils.output(LABEL);
    }
}
