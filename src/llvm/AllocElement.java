package llvm;

import java.util.Objects;

public class AllocElement {
    private final String llvmName;
    private final String instruction;

    public AllocElement(String llvmName, String instruction) {
        this.llvmName = llvmName;
        this.instruction = instruction;
    }

    public String getLlvmName() {
        return llvmName;
    }

    public String getInstruction() {
        return Objects.requireNonNullElse(instruction, "");
    }
}
