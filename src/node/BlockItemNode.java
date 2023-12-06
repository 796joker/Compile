package node;

public class BlockItemNode {
    // BlockItem → Decl | Stmt
    private DeclNode declNode;
    private StmtNode stmtNode;

    public BlockItemNode(DeclNode declNode, StmtNode stmtNode) {
        this.declNode = declNode;
        this.stmtNode = stmtNode;
    }

    public DeclNode getDeclNode() {
        return declNode;
    }

    public StmtNode getStmtNode() {
        return stmtNode;
    }

    public void show() {
        if (declNode != null) {
            declNode.show();
        }
        if (stmtNode != null) {
            stmtNode.show();
        }
    }
}
