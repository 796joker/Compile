package node;

import token.Token;
import utils.IOUitils;

public class LOrExpNode {
    //  LOrExp → LAndExp | LOrExp '||' LAndExp

    private static final String LABEL = "<LOrExp>\n";
    private LAndExpNode lAndExpNode;
    private LOrExpNode lOrExpNode;
    private Token orToken;

    public LOrExpNode(LAndExpNode lAndExpNode) {
        this.lAndExpNode = lAndExpNode;
    }

    public LOrExpNode(LAndExpNode lAndExpNode, LOrExpNode lOrExpNode, Token orToken) {
        this.lAndExpNode = lAndExpNode;
        this.lOrExpNode = lOrExpNode;
        this.orToken = orToken;
    }

    public LAndExpNode getlAndExpNode() {
        return lAndExpNode;
    }

    public LOrExpNode getlOrExpNode() {
        return lOrExpNode;
    }

    public Token getOrToken() {
        return orToken;
    }

    public void show() {
        if (lOrExpNode != null) {
            lOrExpNode.show();
            IOUitils.output(orToken.toString());
        }
        lAndExpNode.show();
        IOUitils.output(LABEL);
    }
}
