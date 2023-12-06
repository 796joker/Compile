package node;

import token.Token;
import utils.IOUitils;

public class LAndExpNode {
    // LAndExp → EqExp | LAndExp '&&' EqExp

    private static final String LABEL = "<LAndExp>\n";
    private LAndExpNode lAndExpNode;
    private Token andToken;
    private EqExpNode eqExpNode;

    public LAndExpNode(LAndExpNode lAndExpNode) {
        this.lAndExpNode = lAndExpNode;
    }

    public LAndExpNode(LAndExpNode lAndExpNode, Token andToken, EqExpNode eqExpNode) {
        this.lAndExpNode = lAndExpNode;
        this.andToken = andToken;
        this.eqExpNode = eqExpNode;
    }

    public LAndExpNode getlAndExpNode() {
        return lAndExpNode;
    }

    public Token getAndToken() {
        return andToken;
    }

    public EqExpNode getEqExpNode() {
        return eqExpNode;
    }

    public void show() {
        if (lAndExpNode != null) {
            lAndExpNode.show();
            IOUitils.output(andToken.toString());
        }
        eqExpNode.show();
        IOUitils.output(LABEL);
    }
}
