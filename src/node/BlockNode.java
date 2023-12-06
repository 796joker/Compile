package node;
import token.Token;
import utils.IOUitils;

import java.util.List;

public class BlockNode {
    // Block → '{' { BlockItem } '}'
    private static final String LABEL = "<Block>\n";
    private Token leftBraceToken;
    private List<BlockItemNode> blockItemNodes;
    private Token rightBraceToken;

    public BlockNode(Token leftBraceToken, List<BlockItemNode> blockItemNodes, Token rightBraceToken) {
        this.leftBraceToken = leftBraceToken;
        this.blockItemNodes = blockItemNodes;
        this.rightBraceToken = rightBraceToken;
    }

    public List<BlockItemNode> getBlockItemNodes() {
        return blockItemNodes;
    }

    public Token getRightBraceToken() {
        return rightBraceToken;
    }


    public void show() {
        IOUitils.output(leftBraceToken.toString());
        for (BlockItemNode blockItemNode : blockItemNodes) {
            blockItemNode.show();
        }
        IOUitils.output(rightBraceToken.toString());
        IOUitils.output(LABEL);
    }

}
