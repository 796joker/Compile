package node;

import java.util.List;
import token.Token;
import utils.IOUitils;

public class LValNode {
    //  LVal → Ident {'[' Exp ']'}

    private static final String LABEL = "<LVal>\n";
    private Token identToken;
    private List<Token> leftBracketTokens;
    private List<ExpNode> expNodes;
    private List<Token> rightBracketTokens;

    public LValNode(Token identToken, List<Token> leftBracketTokens, List<ExpNode> expNodes, List<Token> rightBracketTokens) {
        this.identToken = identToken;
        this.leftBracketTokens = leftBracketTokens;
        this.expNodes = expNodes;
        this.rightBracketTokens = rightBracketTokens;
    }

    public Token getIdentToken() {
        return identToken;
    }

    public List<ExpNode> getExpNodes() {
        return expNodes;
    }

    public void show() {
        // LVal → Ident {'[' Exp ']'}
        IOUitils.output(identToken.toString());
        for (int i=0; i<expNodes.size(); i++) {
            IOUitils.output(leftBracketTokens.get(i).toString());
            expNodes.get(i).show();
            IOUitils.output(rightBracketTokens.get(i).toString());
        }
        IOUitils.output(LABEL);
    }
}
