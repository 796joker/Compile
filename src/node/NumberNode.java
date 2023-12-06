package node;

import token.Token;
import utils.IOUitils;

public class NumberNode {
    // Number → IntConst

    private static final String LABEL = "<Number>\n";
    private Token intConstToken;

    public NumberNode(Token intConstToken) {
        this.intConstToken = intConstToken;
    }

    public Token getIntConstToken() {
        return intConstToken;
    }

    public void show() {
        IOUitils.output(intConstToken.toString());
        IOUitils.output(LABEL);
    }
}
