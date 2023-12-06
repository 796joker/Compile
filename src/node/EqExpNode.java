package node;

import token.Token;
import utils.IOUitils;

public class EqExpNode {
    // EqExp → RelExp | EqExp ('==' | '!=') RelExp

    private static final String LABEL = "<EqExp>\n";
    private RelExpNode relExpNode;
    private EqExpNode eqExpNode;
    private Token opToken;

    public EqExpNode(RelExpNode relExpNode) {
        this.relExpNode = relExpNode;
    }

    public EqExpNode(RelExpNode relExpNode, EqExpNode eqExpNode, Token opToken) {
        this.relExpNode = relExpNode;
        this.eqExpNode = eqExpNode;
        this.opToken = opToken;
    }

    public EqExpNode getEqExpNode() {
        return eqExpNode;
    }

    public RelExpNode getRelExpNode() {
        return relExpNode;
    }

    public Token getOpToken() {
        return opToken;
    }

    public void show() {
        if (eqExpNode != null) {
            eqExpNode.show();
            IOUitils.output(opToken.toString());
        }
        relExpNode.show();
        IOUitils.output(LABEL);
    }
}
