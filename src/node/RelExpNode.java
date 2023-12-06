package node;

import token.Token;
import utils.IOUitils;

public class RelExpNode {
    // RelExp → AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp

    private static final String LABEL = "<RelExp>\n";
    private AddExpNode addExpNode;
    private RelExpNode relExpNode;
    private Token opToken;

    public RelExpNode(AddExpNode addExpNode) {
        this.addExpNode = addExpNode;
    }

    public RelExpNode(AddExpNode addExpNode, RelExpNode relExpNode, Token opToken) {
        this.addExpNode = addExpNode;
        this.relExpNode = relExpNode;
        this.opToken = opToken;
    }

    public AddExpNode getAddExpNode() {
        return addExpNode;
    }

    public RelExpNode getRelExpNode() {
        return relExpNode;
    }

    public Token getOpToken() {
        return opToken;
    }

    public void show() {
        if (relExpNode != null) {
            relExpNode.show();
            IOUitils.output(opToken.toString());
        }
        addExpNode.show();
        IOUitils.output(LABEL);
    }
}
