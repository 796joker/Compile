package node;
import token.Token;
import utils.IOUitils;

import java.util.List;

public class ConstDeclNode {
    // ConstDecl → 'const' BType ConstDef { ',' ConstDef } ';'
    private static final String LABEL = "<ConstDecl>\n";
    private Token constToken;
    private BTypeNode bTypeNode;
    private List<ConstDefNode> constDefNodes;
    private List<Token> commas;
    private Token semicolonToken;

    public Token getConstToken() {
        return constToken;
    }

    public BTypeNode getbTypeNode() {
        return bTypeNode;
    }

    public List<ConstDefNode> getConstDefNodes() {
        return constDefNodes;
    }

    public List<Token> getCommas() {
        return commas;
    }

    public Token getSemicolonToken() {
        return semicolonToken;
    }

    public ConstDeclNode(Token constToken, BTypeNode bTypeNode, List<ConstDefNode> constDefNodes, List<Token> commas, Token semicnToken) {
        this.constToken = constToken;
        this.bTypeNode = bTypeNode;
        this.constDefNodes = constDefNodes;
        this.commas = commas;
        this.semicolonToken = semicnToken;
    }

    public void show() {
        IOUitils.output(constToken.toString());
        bTypeNode.show();
        int bound = constDefNodes.size() - 1;
        for (int i=0; i<=bound; i++) {
            constDefNodes.get(i).show();
            if (i == bound) {
                break;
            }
            IOUitils.output(commas.get(i).toString());
        }
        IOUitils.output(semicolonToken.toString());
        IOUitils.output(LABEL);
    }
}
