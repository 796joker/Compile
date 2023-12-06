package node;

import token.Token;
import utils.IOUitils;

public class UnaryExpNode {
    // UnaryExp → PrimaryExp | Ident '(' [FuncRParams] ')'

    private static final String LABEL = "<UnaryExp>\n";
    private PrimaryExpNode primaryExpNode = null;
    private Token identToken = null;
    private Token leftParentToken = null;
    private FuncRParamsNode funcRParamsNode = null;
    private Token rightParentToken = null;
    private UnaryOpNode unaryOpNode = null;
    private UnaryExpNode unaryExpNode = null;

    public UnaryExpNode(PrimaryExpNode primaryExpNode) {
        this.primaryExpNode = primaryExpNode;
    }

    public UnaryExpNode(Token identToken, Token leftParentToken, FuncRParamsNode funcRParamsNode, Token rightParentToken) {
        this.identToken = identToken;
        this.leftParentToken = leftParentToken;
        this.funcRParamsNode = funcRParamsNode;
        this.rightParentToken = rightParentToken;
    }

    public UnaryExpNode(UnaryOpNode unaryOpNode, UnaryExpNode unaryExpNode) {
        this.unaryOpNode = unaryOpNode;
        this.unaryExpNode = unaryExpNode;
    }

    public PrimaryExpNode getPrimaryExpNode() {
        return primaryExpNode;
    }

    public Token getIdentToken() {
        return identToken;
    }

    public Token getLeftParentToken() {
        return leftParentToken;
    }

    public FuncRParamsNode getFuncRParamsNode() {
        return funcRParamsNode;
    }

    public Token getRightParentToken() {
        return rightParentToken;
    }

    public UnaryOpNode getUnaryOpNode() {
        return unaryOpNode;
    }

    public UnaryExpNode getUnaryExpNode() {
        return unaryExpNode;
    }

    public void show() {
        // UnaryExp → PrimaryExp
        // | Ident '(' [FuncRParams] ')'
        // | UnaryOp UnaryExp
        if (primaryExpNode != null) {
            primaryExpNode.show();
        }
        else if (identToken != null) {
            IOUitils.output(identToken.toString());
            IOUitils.output(leftParentToken.toString());
            if (funcRParamsNode != null) {
                funcRParamsNode.show();
            }
            IOUitils.output(rightParentToken.toString());
        }
        else {
            unaryOpNode.show();
            unaryExpNode.show();
        }
        IOUitils.output(LABEL);
    }
}
