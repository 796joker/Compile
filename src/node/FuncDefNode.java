package node;

import token.Token;
import utils.IOUitils;

public class FuncDefNode {
    // FuncDef → FuncType Ident '(' [FuncFParams] ')' Block

    private static final String LABEL = "<FuncDef>\n";
    private FuncTypeNode funcTypeNode;
    private Token identToken;
    private Token leftParentToken;
    private FuncFParamsNode funcFParamsNode;
    private Token rightParentToken;
    private BlockNode blockNode;

    public FuncDefNode(FuncTypeNode funcTypeNode, Token identToken, Token leftParentToken, FuncFParamsNode funcFParamsNode, Token rightParentToken, BlockNode blockNode) {
        this.funcTypeNode = funcTypeNode;
        this.identToken = identToken;
        this.leftParentToken = leftParentToken;
        this.funcFParamsNode = funcFParamsNode;
        this.rightParentToken = rightParentToken;
        this.blockNode = blockNode;
    }

    public FuncTypeNode getFuncTypeNode() {
        return funcTypeNode;
    }

    public Token getIdentToken() {
        return identToken;
    }

    public FuncFParamsNode getFuncFParamsNode() {
        return funcFParamsNode;
    }

    public BlockNode getBlockNode() {
        return blockNode;
    }

    public Token getRightParentToken() {
        return rightParentToken;
    }

    public void show() {
        funcTypeNode.show();
        IOUitils.output(identToken.toString());
        IOUitils.output(leftParentToken.toString());
        if (funcFParamsNode != null) {
            funcFParamsNode.show();
        }
        IOUitils.output(rightParentToken.toString());
        blockNode.show();
        IOUitils.output(LABEL);
    }
}
