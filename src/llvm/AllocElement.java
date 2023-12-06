package llvm;

import java.util.Objects;

public class AllocElement {
    String llvmName;
    String instruction;

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
