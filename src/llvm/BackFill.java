package llvm;

import java.util.Objects;
import java.util.Stack;

import static llvm.Utils.unconditionalJump;

/**
 * @author AS
 */
public class BackFill {
    /**
     * 记录是否需要回填continue语句
     */
    private Stack<Integer> needBackFillC;
    /**
     * 记录是否需要回填break语句
     */
    private Stack<Integer> needBackFillB;

    /**
     * 记录需要回填的continue的Instruction
     */
    private Stack<Instruction> needBackFillIC;
    /**
     * 记录需要回填的break语句的Instruction
     */
    private Stack<Instruction> needBackFillIB;

    private BackFill() {
    }
    private static final BackFill BACK_FILL = new BackFill();

    public static BackFill getBackFill() {
        return BACK_FILL;
    }

    /**
     * 初始化
     */
    public void init() {
        this.needBackFillIB = new Stack<>();
        this.needBackFillIC = new Stack<>();
        this.needBackFillB = new Stack<>();
        this.needBackFillC = new Stack<>();
    }

    private void addStackNum(Stack<Integer> stack) {
        Integer pop = stack.pop();
        pop += 1;
        stack.push(pop);
    }

    /**
     * 进入一层for循环,默认需要回填的语句数目为0
     */
    public void enterForInit() {
        this.needBackFillB.push(0);
        this.needBackFillC.push(0);
    }

    /**
     * 添加需要回填的continue语句
     */
    public void addNeedBackFillIC(Instruction continueInstruction) {
        this.needBackFillIC.add(continueInstruction);
        addStackNum(needBackFillC);
    }

    /**
     * 添加需要回填的break语句
     */
    public void addNeedBackFillIB(Instruction breakInstruction) {
        this.needBackFillIB.add(breakInstruction);
        addStackNum(this.needBackFillB);
    }

    /**
     * 进行break语句回填
     */
    public void backFillBreak(String nextLabel) {
        int needBackFillNum = needBackFillB.pop();
        while (needBackFillNum != 0) {
            // 需要回填,回填到nextLabel
            Instruction needFillBreak = needBackFillIB.pop();
            // 回填到nextLabel
            needFillBreak.addInstruction(unconditionalJump(nextLabel).toString());
            needBackFillNum--;
        }
    }

    /**
     * 进行continue语句回填
     */
    public void backFillContinue(String forStepLabel, String forCondLabel, String forBodyLabel) {
        int needBackFillNum = needBackFillC.pop();
        while (needBackFillNum != 0) {
            // 需要回填,回填到nextLabel
            Instruction needFillContinue = needBackFillIC.pop();
            // 需要回填,回填到forStep(如果有),否则回填到forCond(如果有),否则回填到forBody
            needFillContinue.addInstruction(unconditionalJump(Objects.requireNonNullElseGet(forStepLabel, () -> Objects.requireNonNullElse(forCondLabel, forBodyLabel))).toString());
            needBackFillNum--;
        }
    }
}
