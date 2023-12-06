package node;
import token.Token;
import utils.IOUitils;

import java.util.List;

public class ConstInitValNode {
    //  ConstInitVal → ConstExp
    // | '{' [ ConstInitVal { ',' ConstInitVal } ] '}'

    private static final String LABEL = "<ConstInitVal>\n";
    private ConstExpNode constExpNode;
    private Token leftBraceToken;
    private List<ConstInitValNode> constInitValNodes;
    private List<Token> commas;
    private Token rightBraceToken;

    public ConstExpNode getConstExpNode() {
        return constExpNode;
    }

    public Token getLeftBraceToken() {
        return leftBraceToken;
    }

    public List<ConstInitValNode> getConstInitValNodes() {
        return constInitValNodes;
    }

    public List<Token> getCommas() {
        return commas;
    }

    public Token getRightBraceToken() {
        return rightBraceToken;
    }

    public ConstInitValNode(ConstExpNode constExpNode, Token leftBraceToken, List<ConstInitValNode> constInitValNodes, List<Token> commas, Token rightBraceToken) {
        this.constExpNode = constExpNode;
        this.leftBraceToken = leftBraceToken;
        this.constInitValNodes = constInitValNodes;
        this.commas = commas;
        this.rightBraceToken = rightBraceToken;
    }

    public void show() {
        if (leftBraceToken != null) {
            // '{' [ ConstInitVal { ',' ConstInitVal } ] '}'
            IOUitils.output(leftBraceToken.toString());
            int bound = constInitValNodes.size() - 1;
            for (int i=0; i<=bound; i++) {
                constInitValNodes.get(i).show();
                if (i == bound) {
                    break;
                }
                IOUitils.output(commas.get(i).toString());
            }
            IOUitils.output(rightBraceToken.toString());
        }
        else {
            constExpNode.show();
        }
        IOUitils.output(LABEL);
    }
}
