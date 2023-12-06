package node;
import token.Token;
import utils.IOUitils;

public class BTypeNode {
    // BType → 'int'

    private Token intToken;

    public BTypeNode(Token intToken) {
        this.intToken = intToken;
    }

    public void show() {
        IOUitils.output(intToken.toString());
    }

}
