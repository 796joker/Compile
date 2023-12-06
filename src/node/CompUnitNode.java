package node;

import utils.IOUitils;

import java.util.List;

public class CompUnitNode {
    // CompUnit → {Decl} {FuncDef} MainFuncDef
    private static final String LABEL = "<CompUnit>\n";
    private List<DeclNode> declNodes;
    private List<FuncDefNode> funcDefNodes;
    private MainFuncDefNode mainFuncDefNode;

    public CompUnitNode(List<DeclNode> declNodes, List<FuncDefNode> funcDefNodes, MainFuncDefNode mainFuncDefNode) {
        this.declNodes = declNodes;
        this.funcDefNodes = funcDefNodes;
        this.mainFuncDefNode = mainFuncDefNode;

    }

    public List<DeclNode> getDeclNodes() {
        return declNodes;
    }

    public List<FuncDefNode> getFuncDefNodes() {
        return funcDefNodes;
    }

    public MainFuncDefNode getMainFuncDefNode() {
        return mainFuncDefNode;
    }

    public void show() {
        for (DeclNode declNode : declNodes) {
            declNode.show();
        }
        for (FuncDefNode funcDefNode : funcDefNodes) {
            funcDefNode.show();
        }
        mainFuncDefNode.show();
        IOUitils.output(LABEL);
    }
}
