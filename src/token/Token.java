package token;

public class Token {
    private TokenType type;
    private int lineNum;

    public TokenType getType() {
        return type;
    }

    public int getLineNum() {
        return lineNum;
    }

    public void setType(TokenType type) {
        this.type = type;
    }

    public void setLineNum(int lineNum) {
        this.lineNum = lineNum;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    private String value;

    public Token(TokenType type, int lineNum, String value){
        this.type = type;
        this.lineNum = lineNum;
        this.value = value;
    }

    /**
     * token.Token toString方法
     * @return 返回Token结果表达:
     * 单词类别码 单词的字符/字符串形式(中间仅用一个空格间隔)
     */
    @Override
    public String toString() {
        return type.toString() + " " + value + "\n";
    }

    public String debugInfo() {
        return type.toString() + " " + value + " " + lineNum + "\n";
    }

}
