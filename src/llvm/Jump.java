package llvm;

import java.util.HashMap;
import java.util.Stack;

public class Jump {
    /**
     * 记录当前的forBody是否有直接跳转
     */
    Stack<Boolean> forJump;
    /**
     * 记录当前的ifBody是否有直接跳转
     */
    Stack<Boolean> ifJump;
    /**
     * 记录当前的elseBody是否有直接跳转
     */
    Stack<Boolean> elseJump;

    public enum JumpType{
        IF,
        ELSE,
        FOR
    }

    private HashMap<JumpType, Stack<Boolean>> jumpTypeMap;

    private Jump() {
    }

    private static final Jump JUMP = new Jump();

    public void init() {
        this.ifJump = new Stack<>();
        this.elseJump = new Stack<>();
        this.forJump = new Stack<>();
        this.jumpTypeMap = new HashMap<>(16) {{
            put(JumpType.IF, ifJump);
            put(JumpType.ELSE, elseJump);
            put(JumpType.FOR, forJump);
        }};
    }

    public static Jump getJUMP() {
        return JUMP;
    }

    /**
     * 遇到return,continue,break语句,为当前所在类型打上跳转标记
     */
    public void setJumpFlag(JumpType jumpType) {
        // 函数分析会传null,只有不为null时才标记
        if (jumpType != null) {
            Stack<Boolean> jump = this.jumpTypeMap.get(jumpType);
            jump.pop();
            jump.push(true);
        }
    }

    /**
     * 初始化此类型的jump标记
     * @param jumpType 是哪种类型
     */
    public void initJump(JumpType jumpType) {
        this.jumpTypeMap.get(jumpType).push(false);
    }

    public boolean isNotJump(JumpType jumpType) {
        return !this.jumpTypeMap.get(jumpType).pop();
    }
}
