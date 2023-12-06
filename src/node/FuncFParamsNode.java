package node;
import token.Token;
import utils.IOUitils;

import java.util.List;

public class FuncFParamsNode {
    // FuncFParams → FuncFParam { ',' FuncFParam }

    private static final String LABEL = "<FuncFParams>\n";
    private List<FuncFParamNode> funcFParamNodes;
    private List<Token> commas;

    public FuncFParamsNode(List<FuncFParamNode> funcFParamNodes, List<Token> commas) {
        this.funcFParamNodes = funcFParamNodes;
        this.commas = commas;
    }

    public List<FuncFParamNode> getFuncFParamNodes() {
        return funcFParamNodes;
    }

    public void show() {
        int bound = funcFParamNodes.size() - 1;
        for (int i=0; i<=bound; i++) {
            funcFParamNodes.get(i).show();
            if (i == bound) {
                break;
            }
            IOUitils.output(commas.get(i).toString());
        }
        IOUitils.output(LABEL);
    }
}
