package llvm;

import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

public class Instruction {
    private String llvmName;
    private StringJoiner instructions;

    private String nextLOrLabel = null;

    private String nextLAndLabel = null;

    private ConditionJumpType conditionJumpType = ConditionJumpType.NONE;

    private String nextLabel = null;

    private boolean isBlock = false;

    /**
     * 用于回填的指令列表
     */
    List<Instruction> needBackInstructions;

    /**
     * 条件跳转类型的枚举类
     */
    public enum ConditionJumpType {
        /**
         * default
         */
        NONE,
        /**
         * if
         * nextLAnd, nextLor
         */
        IAO,
        /**
         * nextLAnd, nextLabel
         */
        IAN,
        /**
         * ifBodyLabel, nextLor
         */
        IBO,
        /**
         * ifBodyLabel, nextLabel
         */
        IBN,
        /**
         * if-else
         * nextLAnd, nextLor
         */
        IEAO,
        /**
         * nextLAnd, elseBodyLabel
         */
        IEAE,
        /**
         * ifBodyLabel, nextLor
         */
        IEBO,
        /**
         * ifBodyLabel, elseBodyLabel
         */
        IEBE,
        /**
         * for
         * nextLAnd, nextLabel
         */

        FAN,
        /**
         * nextLAnd, nextLor
         */
        FAO,
        /**
         * forBodyLabel, nextLabel
         */
        FBN,
        /**
         * forBodyLabel, nextLor
         */
        FBO
    }


    public Instruction(String llvmName, StringJoiner instructions) {
        this.llvmName = llvmName;
        this.instructions = instructions;
    }

    public void setNextLAndLabel(String nextLAndLabel) {
        this.nextLAndLabel = nextLAndLabel;
    }

    public void setNextLOrLabel(String nextLOrLabel) {
        this.nextLOrLabel = nextLOrLabel;
    }

    public void setConditionJumpType(ConditionJumpType conditionJumpType) {
        this.conditionJumpType = conditionJumpType;
    }

    public void setNextLabel(String nextLabel) {
        this.nextLabel = nextLabel;
    }

    public String getNextLabel() {
        return nextLabel;
    }

    public String getLlvmName() {
        return llvmName;
    }

    public StringJoiner getInstructions() {
        return instructions;
    }

    public void addInstruction(String instruction) {
        if (!Objects.equals(instruction, "")) {
            instructions.add(instruction);
        }
    }

    public void setLlvmName(String llvmName) {
        this.llvmName = llvmName;
    }

    public ConditionJumpType getConditionJumpType() {
        return conditionJumpType;
    }

    public String getNextLAndLabel() {
        return nextLAndLabel;
    }

    public String getNextLOrLabel() {
        return nextLOrLabel;
    }

    public void setNeedBackInstructions(List<Instruction> needBackInstructions) {
        this.needBackInstructions = needBackInstructions;
    }

    public void addNeedBackInstruction(Instruction instruction) {
        this.needBackInstructions.add(instruction);
    }

    public List<Instruction> getNeedBackInstructions() {
        return needBackInstructions;
    }

    public void setBlock(boolean block) {
        isBlock = block;
    }

    public boolean isBlock() {
        return isBlock;
    }

    /**
     * 重写toString方法
     * @return 返回指令集合
     */
    @Override
    public String toString() {
        if (instructions != null) {
            return instructions.toString();
        }
        else {
            return "";
        }
    }

    public void unionNeedBack() {
        // 如果是Block块的Instruction,需要在回填完成后统合所有指令
        if (isBlock) {
            for (Instruction needBackInstruction : needBackInstructions) {
                // 如果统合的指令中有Block指令,进行递归统合
                if (needBackInstruction.isBlock()) {
                    needBackInstruction.unionNeedBack();
                }
                // 加入指令集中
                addInstruction(needBackInstruction.toString());
            }
            // 回填完进行标记
            isBlock = false;
        }
    }

}
