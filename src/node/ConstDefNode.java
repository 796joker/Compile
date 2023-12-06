package node;
import token.Token;
import utils.IOUitils;

import java.util.List;

public class ConstDefNode {
    // ConstDef → Ident { '[' ConstExp ']' } '=' ConstInitVal
    private static final String LABEL = "<ConstDef>\n";
    private Token identToken;
    private List<Token> leftBrackets;
    private List<ConstExpNode> constExpNodes;
    private List<Token> rightBrackets;
    private Token assignToken;
    private ConstInitValNode constInitValNode;

    public Token getIdentToken() {
        return identToken;
    }

    public List<Token> getLeftBrackets() {
        return leftBrackets;
    }

    public List<ConstExpNode> getConstExpNodes() {
        return constExpNodes;
    }

    public List<Token> getRightBrackets() {
        return rightBrackets;
    }

    public Token getAssignToken() {
        return assignToken;
    }

    public ConstInitValNode getConstInitValNode() {
        return constInitValNode;
    }

    public ConstDefNode(Token identToken, List<Token> leftBrackets, List<ConstExpNode> constExpNodes, List<Token> rightBrackets, Token assignToken, ConstInitValNode constInitValNode) {
        this.identToken = identToken;
        this.leftBrackets = leftBrackets;
        this.constExpNodes = constExpNodes;
        this.rightBrackets = rightBrackets;
        this.assignToken = assignToken;
        this.constInitValNode = constInitValNode;
    }

    public void show() {
        IOUitils.output(identToken.toString());
        for (int i=0; i<constExpNodes.size(); i++) {
            IOUitils.output(leftBrackets.get(i).toString());
            constExpNodes.get(i).show();
            IOUitils.output(rightBrackets.get(i).toString());
        }
        IOUitils.output(assignToken.toString());
        constInitValNode.show();
        IOUitils.output(LABEL);
    }
}
