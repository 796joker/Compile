package node;
import token.Token;
import utils.IOUitils;

import java.util.List;

public class FuncFParamNode {
    // FuncFParam → BType Ident ['[' ']' { '[' ConstExp ']' }]

    private static final String LABEL = "<FuncFParam>\n";
    private BTypeNode bTypeNode;
    private Token identToken;
    private List<Token> leftBracketTokens;
    private List<Token> rightBracketTokens;
    private ConstExpNode constExpNode;

    public FuncFParamNode(BTypeNode bTypeNode, Token identToken, List<Token> leftBracketTokens, List<Token> rightBracketTokens, ConstExpNode constExpNode) {
        this.bTypeNode = bTypeNode;
        this.identToken = identToken;
        this.leftBracketTokens = leftBracketTokens;
        this.rightBracketTokens = rightBracketTokens;
        this.constExpNode = constExpNode;
    }

    public BTypeNode getbTypeNode() {
        return bTypeNode;
    }

    public Token getIdentToken() {
        return identToken;
    }

    public List<Token> getLeftBracketTokens() {
        return leftBracketTokens;
    }

    public List<Token> getRightBracketTokens() {
        return rightBracketTokens;
    }

    public ConstExpNode getConstExpNode() {
        return constExpNode;
    }

    public void show() {
        bTypeNode.show();
        IOUitils.output(identToken.toString());
        for (int i=0; i<leftBracketTokens.size(); i++) {
            IOUitils.output(leftBracketTokens.get(i).toString());
            if (i==1) {
                constExpNode.show();
            }
            IOUitils.output(rightBracketTokens.get(i).toString());
        }
        IOUitils.output(LABEL);
    }
}
