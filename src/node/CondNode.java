package node;

import utils.IOUitils;

public class CondNode {
    // Cond → LOrExp
    private static final String LABEL = "<Cond>\n";
    private LOrExpNode lOrExpNode;

    public CondNode(LOrExpNode lOrExpNode) {
        this.lOrExpNode = lOrExpNode;
    }

    public LOrExpNode getlOrExpNode() {
        return lOrExpNode;
    }

    public void show() {
        lOrExpNode.show();
        IOUitils.output(LABEL);
    }
}
