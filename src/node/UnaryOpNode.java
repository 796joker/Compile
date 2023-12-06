package node;

import token.Token;
import utils.IOUitils;

public class UnaryOpNode {
    // UnaryOp → '+' | '−' | '!'

    private static final String LABEL = "<UnaryOp>\n";
    private Token opToken;

    public UnaryOpNode(Token opToken) {
        this.opToken = opToken;
    }

    public Token getOpToken() {
        return opToken;
    }

    public void show() {
        IOUitils.output(opToken.toString());
        IOUitils.output(LABEL);
    }
}
