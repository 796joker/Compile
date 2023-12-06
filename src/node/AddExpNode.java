package node;
import token.Token;
import utils.IOUitils;

public class AddExpNode {
    // AddExp → MulExp | AddExp ('+' | '−') MulExp

    private static final String LABEL = "<AddExp>\n";
    private MulExpNode mulExpNode;
    private AddExpNode addExpNode;
    private Token opToken;

    public AddExpNode(MulExpNode mulExpNode) {
        this.mulExpNode = mulExpNode;
    }

    public AddExpNode(MulExpNode mulExpNode, AddExpNode addExpNode, Token opToken) {
        this.mulExpNode = mulExpNode;
        this.addExpNode = addExpNode;
        this.opToken = opToken;
    }

    public MulExpNode getMulExpNode() {
        return mulExpNode;
    }

    public AddExpNode getAddExpNode() {
        return addExpNode;
    }

    public Token getOpToken() {
        return opToken;
    }

    public void show() {
        if (addExpNode != null) {
            addExpNode.show();
            IOUitils.output(opToken.toString());
        }
        mulExpNode.show();
        IOUitils.output(LABEL);
    }
}
