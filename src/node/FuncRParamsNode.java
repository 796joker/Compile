package node;

import java.util.List;
import token.Token;
import utils.IOUitils;

public class FuncRParamsNode {
    // FuncRParams → Exp { ',' Exp }

    private static final String LABEL = "<FuncRParams>\n";
    private List<ExpNode> expNodes;
    private List<Token> commas;

    public FuncRParamsNode(List<ExpNode> expNodes, List<Token> commas) {
        this.expNodes = expNodes;
        this.commas = commas;
    }

    public List<ExpNode> getExpNodes() {
        return expNodes;
    }

    public List<Token> getCommas() {
        return commas;
    }

    public void show() {
        int bound = expNodes.size() - 1;
        for (int i=0; i<=bound; i++) {
            expNodes.get(i).show();
            if (i == bound) {
                break;
            }
            IOUitils.output(commas.get(i).toString());
        }
        IOUitils.output(LABEL);
    }
}
