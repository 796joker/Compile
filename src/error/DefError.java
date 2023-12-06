package error;

public class DefError {
    private int lineNum;
    private ErrorType type;

    public DefError(int lineNum, ErrorType type) {
        this.lineNum = lineNum;
        this.type = type;
    }

    public int getLineNum() {
        return lineNum;
    }

    public void setLineNum(int lineNum) {
        this.lineNum = lineNum;
    }

    public ErrorType getType() {
        return type;
    }

    public void setType(ErrorType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return lineNum + " " + type.toString() + "\n";
    }
}
