package node;

import token.Token;
import utils.IOUitils;

public class MulExpNode {
    // MulExp → UnaryExp | MulExp ('*' | '/' | '%') UnaryExp

    private static final String LABEL = "<MulExp>\n";
    private UnaryExpNode unaryExpNode = null;
    private MulExpNode mulExpNode = null;
    private Token opToken = null;

    public MulExpNode(UnaryExpNode unaryExpNode) {
        this.unaryExpNode = unaryExpNode;
    }

    public MulExpNode(UnaryExpNode unaryExpNode, MulExpNode mulExpNode, Token opToken) {
        this.unaryExpNode = unaryExpNode;
        this.mulExpNode = mulExpNode;
        this.opToken = opToken;
    }

    public UnaryExpNode getUnaryExpNode() {
        return unaryExpNode;
    }

    public MulExpNode getMulExpNode() {
        return mulExpNode;
    }

    public Token getOpToken() {
        return opToken;
    }

    public void show() {
        if (mulExpNode != null) {
            mulExpNode.show();
            IOUitils.output(opToken.toString());
        }
        unaryExpNode.show();
        IOUitils.output(LABEL);
    }
}
