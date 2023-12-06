package node;
import token.Token;
import utils.IOUitils;

import java.util.List;

public class VarDeclNode {
    // VarDecl → BType VarDef { ',' VarDef } ';'

    private static final String LABEL = "<VarDecl>\n";
    private BTypeNode bTypeNode;
    private List<VarDefNode> varDefNodes;
    private List<Token> commas;
    private Token semicolonToken;

    public VarDeclNode(BTypeNode bTypeNode, List<VarDefNode> varDefNodes, List<Token> commas, Token semicolonToken) {
        this.bTypeNode = bTypeNode;
        this.varDefNodes = varDefNodes;
        this.commas = commas;
        this.semicolonToken = semicolonToken;
    }

    public List<VarDefNode> getVarDefNodes() {
        return varDefNodes;
    }

    public void show() {
        // VarDecl → BType VarDef { ',' VarDef } ';'
        bTypeNode.show();
        int bound = varDefNodes.size() - 1;
        for (int i=0; i<=bound; i++) {
            varDefNodes.get(i).show();
            if (i == bound) {
                break;
            }
            IOUitils.output(commas.get(i).toString());
        }
        IOUitils.output(semicolonToken.toString());
        IOUitils.output(LABEL);
    }
}
