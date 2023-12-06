package node;

public class DeclNode {
    // Decl → ConstDecl | VarDecl
    private ConstDeclNode constDeclNode;
    private VarDeclNode varDeclNode;

    public DeclNode(ConstDeclNode constDeclNode, VarDeclNode varDeclNode) {
        this.constDeclNode = constDeclNode;
        this.varDeclNode = varDeclNode;
    }

    public ConstDeclNode getConstDeclNode() {
        return constDeclNode;
    }

    public VarDeclNode getVarDeclNode() {
        return varDeclNode;
    }


    public void show() {
        if (constDeclNode != null) {
            constDeclNode.show();
        }
        if (varDeclNode != null) {
            varDeclNode.show();
        }
    }
}
