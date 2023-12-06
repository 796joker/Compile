package node;
import token.Token;
import utils.IOUitils;

public class MainFuncDefNode {
    // MainFuncDef → 'int' 'main' '(' ')' Block

    private static final String LABEL = "<MainFuncDef>\n";
    private Token intToken;
    private Token mainToken;
    private Token leftParentToken;
    private Token rightParentToken;
    private BlockNode blockNode;

    public MainFuncDefNode(Token intToken, Token mainToken, Token leftParentToken, Token rightParentToken, BlockNode blockNode) {
        this.intToken = intToken;
        this.mainToken = mainToken;
        this.leftParentToken = leftParentToken;
        this.rightParentToken = rightParentToken;
        this.blockNode = blockNode;
    }

    public BlockNode getBlockNode() {
        return blockNode;
    }

    public void show() {
        IOUitils.output(intToken.toString());
        IOUitils.output(mainToken.toString());
        IOUitils.output(leftParentToken.toString());
        IOUitils.output(rightParentToken.toString());
        blockNode.show();
        IOUitils.output(LABEL);
    }
}
