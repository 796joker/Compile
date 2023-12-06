package node;

import utils.IOUitils;

public class ConstExpNode {
    // ConstExp → AddExp 注：使用的Ident 必须是常量
    private static final String LABEL = "<ConstExp>\n";
    private AddExpNode addExpNode;

    public ConstExpNode(AddExpNode addExpNode) {
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
