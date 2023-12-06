package node;

import token.Token;
import utils.IOUitils;

import java.util.List;

public class InitValNode {

    private static final String LABEL = "<InitVal>\n";
    private ExpNode expNode;
    private Token leftBraceToken;
    private List<InitValNode> initValNodes;
    private List<Token> commas;
    private Token rightBraceToken;

    public InitValNode(ExpNode expNode, Token leftBraceToken, List<InitValNode> initValNodes, List<Token> commas, Token rightBraceToken) {
        this.expNode = expNode;
        this.leftBraceToken = leftBraceToken;
        this.initValNodes = initValNodes;
        this.commas = commas;
        this.rightBraceToken = rightBraceToken;
    }

    public ExpNode getExpNode() {
        return expNode;
    }

    public List<InitValNode> getInitValNodes() {
        return initValNodes;
    }

    public List<Token> getCommas() {
        return commas;
    }

    public void show() {
        // InitVal → Exp | '{' [ InitVal { ',' InitVal } ] '}'
        if (leftBraceToken != null) {
            IOUitils.output(leftBraceToken.toString());
            int bound = initValNodes.size() - 1;
            for (int i=0; i<=bound; i++) {
                initValNodes.get(i).show();
                if (i == bound) {
                    break;
                }
                IOUitils.output(commas.get(i).toString());
            }
            IOUitils.output(rightBraceToken.toString());
        }
        else {
            expNode.show();
        }
        IOUitils.output(LABEL);
    }
}
