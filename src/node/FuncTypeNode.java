package node;

import token.Token;
import utils.IOUitils;

public class FuncTypeNode {
    // FuncType → 'void' | 'int'

    private static final String LABEL = "<FuncType>\n";
    private Token token;
    public FuncTypeNode(Token token) {
        this.token = token;
    }
    public Token getToken() {
        return token;
    }

    public void show() {
        IOUitils.output(token.toString());
        IOUitils.output(LABEL);
    }
}
