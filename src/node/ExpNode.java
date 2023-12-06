package node;

import utils.IOUitils;

public class ExpNode {
    //  Exp → AddExp

    private static final String LABEL = "<Exp>\n";
    private AddExpNode addExpNode;

    public ExpNode(AddExpNode addExpNode) {
        this.addExpNode = addExpNode;
    }

    public AddExpNode getAddExpNode() {
        return addExpNode;
    }

    public void show() {
        addExpNode.show();
        IOUitils.output(LABEL);
    }
}
