package node;
import token.Token;
import utils.IOUitils;

import java.util.List;

public class VarDefNode {
    // VarDef → Ident { '[' ConstExp ']' }
    // | Ident { '[' ConstExp ']' } '=' InitVal

    private static final String LABEL = "<VarDef>\n";
    private Token identToken;
    private List<Token> leftBracketTokens;
    private List<ConstExpNode> constExpNodes;
    private List<Token> rightBracketTokens;
    private Token assignToken;
    private InitValNode initValNode;

    public VarDefNode(Token identToken, List<Token> leftBracketTokens, List<ConstExpNode> constExpNodes, List<Token> rightBracketTokens, Token assignToken, InitValNode initValNode) {
        this.identToken = identToken;
        this.leftBracketTokens = leftBracketTokens;
        this.constExpNodes = constExpNodes;
        this.rightBracketTokens = rightBracketTokens;
        this.assignToken = assignToken;
        this.initValNode = initValNode;
    }

    public Token getIdentToken() {
        return identToken;
    }

    public List<ConstExpNode> getConstExpNodes() {
        return constExpNodes;
    }

    public InitValNode getInitValNode() {
        return initValNode;
    }

    public List<Token> getLeftBracketTokens() {
        return leftBracketTokens;
    }

    public void show() {
        // VarDef → Ident { '[' ConstExp ']' }
        // | Ident { '[' ConstExp ']' } '=' InitVal
        IOUitils.output(identToken.toString());
        for (int i=0; i<leftBracketTokens.size(); i++) {
            IOUitils.output(leftBracketTokens.get(i).toString());
            constExpNodes.get(i).show();
            IOUitils.output(rightBracketTokens.get(i).toString());
        }
        if (assignToken != null) {
            IOUitils.output(assignToken.toString());
            initValNode.show();
        }
        IOUitils.output(LABEL);
    }
}
