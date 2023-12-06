package frontend;
import error.ErrorHandler;
import error.ErrorType;
import token.*;

import token.TokenType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Lexer {
    private static final Lexer LEXER = new Lexer();
    public static Lexer getLexer(){
        return LEXER;
    }
    private List<Token> tokens = new ArrayList<>();
    private String source = null;
    private int curPos = -1;
    private int lineNum = -1;
    private int maxLength = -1;
    private Token token = null;
    private ErrorHandler errorHandler = ErrorHandler.getErrorHandler();
    /**
     * 返回lexer解析出的tokens
     * @return tokens数组
     */
    public List<Token> getTokens(){
        return tokens;
    }

    private Map<String, TokenType> keywords = new HashMap<>(){
        {
            put("if", TokenType.IFTK);
            put("else", TokenType.ELSETK);
            put("main", TokenType.MAINTK);
            put("return", TokenType.RETURNTK);
            put("for", TokenType.FORTK);
            put("const", TokenType.CONSTTK);
            put("continue", TokenType.CONTINUETK);
            put("break", TokenType.BREAKTK);
            put("int", TokenType.INTTK);
            put("void", TokenType.VOIDTK);
            put("getint", TokenType.GETINTTK);
            put("printf", TokenType.PRINTFTK);
        }
    };

    public void init(String source) {
        this.source = source;
        this.curPos = -1;
        this.lineNum = 1;
        this.token = null;
        this.maxLength = source.length();
    }

    public void reset() {
        this.source = null;
        this.curPos = -1;
        this.lineNum = -1;
        this.token = null;
        this.maxLength = -1;
    }

    public Token next(){
        this.curPos++;
        if (this.curPos >= this.maxLength) {
            return null;
        }
        char currentChar = source.charAt(this.curPos);
        char nextChar = this.curPos+1 < this.maxLength ? source.charAt(this.curPos+1) : '\0';
        if(currentChar == '\n') {
            this.lineNum++;
            return this.next();
        }
        else if (currentChar == '_' || Character.isLetter(currentChar)) { // 标识符或关键字，_或字母开头
            StringBuilder value = new StringBuilder();
            value.append(currentChar);
            for (this.curPos=this.curPos+1; this.curPos<this.maxLength; this.curPos++) {
                currentChar = source.charAt(this.curPos);
                if (Character.isLetter(currentChar) || Character.isDigit(currentChar) || currentChar == '_') {
                    value.append(currentChar);
                }
                else {
                    this.curPos = this.curPos - 1;
                    break;
                }
            }
            this.token = new Token(keywords.getOrDefault(value.toString(), TokenType.IDENFR), lineNum, value.toString());
            tokens.add(this.token);
            return this.token;
        }
        else if (Character.isDigit(currentChar)) { // 数字串
            StringBuilder value = new StringBuilder();
            value.append(currentChar);
            for (this.curPos=this.curPos+1; this.curPos<this.maxLength; this.curPos++) {
                currentChar = source.charAt(this.curPos);
                if (Character.isDigit(currentChar)) {
                    value.append(currentChar);
                }
                else {
                    // 回退到合法串结尾
                    this.curPos = this.curPos - 1;
                    break;
                }
            }
            this.token = new Token(keywords.getOrDefault(value.toString(), TokenType.INTCON), lineNum, value.toString());
            tokens.add(this.token);
            return this.token;
        }
        else if (currentChar == '"') { // 字符串
            StringBuilder value = new StringBuilder();
            value.append(currentChar);
            for (this.curPos=this.curPos+1; this.curPos<this.maxLength; this.curPos++) {
                currentChar = source.charAt(this.curPos);
                if (currentChar == '"') {
                    // 无需回退,记录字符串值
                    value.append(currentChar);
                    this.token = new Token(TokenType.STRCON, lineNum, value.toString());
                    tokens.add(this.token);
                    return this.token;
                }
                else if (currentChar == 32 || currentChar == 33 || (currentChar >= 40 && currentChar <= 126)) {
                    // \n
                    if (currentChar == '\\' && source.charAt(this.curPos+1) != 'n') {
                        // TODO 错误处理
                        errorHandler.addError(lineNum, ErrorType.a);
                    }
                    value.append(currentChar);
                }
                // %d
                else if (currentChar == '%') {
                    if (source.charAt(this.curPos+1) == 'd') {
                        value.append("%d");
                        this.curPos++;
                    }
                    // 检查是否有连续%
                    else if (source.charAt(this.curPos+1) == '%') {
                        // 后面那个也不合法,由于一行只有一个错误,所以这两个应属于同一错误,不会恶意换行
                        if (source.charAt(this.curPos+2) != 'd') {
                            this.curPos = this.curPos++;
                        }
                        // 后面的合法,正常读取
                        // 无论如何都要添加错误
                        errorHandler.addError(lineNum, ErrorType.a);
                    }
                }
                else {
                    // TODO 错误处理
                    errorHandler.addError(lineNum, ErrorType.a);
                }
            }
        }
        else if (currentChar == '!') { // ! 或 !=
            if (nextChar == '=') {
                this.curPos++;
                this.token = new Token(TokenType.NEQ, lineNum, "!=");
                tokens.add(this.token);
                return this.token;
            }
            else {
                this.token = new Token(TokenType.NOT, lineNum, "!");
                tokens.add(this.token);
                return this.token;
            }
        }
        else if (currentChar == '&') { // &&
            if (nextChar == '&') {
                this.curPos++;
                this.token = new Token(TokenType.AND, lineNum, "&&");
                tokens.add(this.token);
                return this.token;
            }
            else {
                // TODO 错误处理
            }
        }
        else if (currentChar == '|') {
            if (nextChar == '|') {
                this.curPos++;
                this.token = new Token(TokenType.OR, lineNum, "||");
                tokens.add(this.token);
                return this.token;
            }
            else {
                // TODO 错误处理
            }
        }
        else if (currentChar == '/') { // '/'注释 或 '/*'多行注释 或 '/'除号
            if (nextChar == '/') {
                // 单行注释，找换行符
                int index = source.indexOf('\n', this.curPos+1);
                if (index == -1) {
                    // 没找到换行符：说明注释直接到结尾 TODO 之后再看看有没有错误
                    this.curPos = this.maxLength - 1;
                    return null;
                }
                else {
                    // 定位到‘\n’
                    this.curPos = index;
                    this.lineNum++;
                    // 进行下一次解析
                    return this.next();
                }
            }
            else if (nextChar == '*') {
                // 多行注释,找*/,同时注意换行
                int end = source.indexOf("*/", this.curPos+1);
                if (end == -1) {
                    // TODO 错误处理
                }
                int index = source.indexOf('\n', this.curPos+1);
                // 此行结束无换行 或 注释在换行前结束，移动到注释结束处;否则行数++
                while (index != -1 && index < end) {
                    this.lineNum++;
                    this.curPos = index;
                    index = source.indexOf('\n', this.curPos+1);
                }
                // 跳到注释后
                this.curPos = end + 1;
                return this.next();
            }
            else {
                this.token = new Token(TokenType.DIV, lineNum, "/");
                tokens.add(this.token);
                return this.token;
            }
        }
        else if (currentChar == ',') {
            this.token = new Token(TokenType.COMMA, lineNum, ",");
            tokens.add(this.token);
            return this.token;
        }
        else if (currentChar == ';') {
            this.token = new Token(TokenType.SEMICN, lineNum, ";");
            tokens.add(this.token);
            return this.token;
        }
        else if (currentChar == '(') {
            this.token = new Token(TokenType.LPARENT, lineNum, "(");
            tokens.add(this.token);
            return this.token;
        }
        else if (currentChar == ')') {
            this.token = new Token(TokenType.RPARENT, lineNum, ")");
            tokens.add(this.token);
            return this.token;
        }
        else if (currentChar == '[') {
            this.token = new Token(TokenType.LBRACK, lineNum, "[");
            tokens.add(this.token);
            return this.token;
        }
        else if (currentChar == ']') {
            this.token = new Token(TokenType.RBRACK, lineNum, "]");
            tokens.add(this.token);
            return this.token;
        }
        else if (currentChar == '{') {
            this.token = new Token(TokenType.LBRACE, lineNum, "{");
            tokens.add(this.token);
            return this.token;
        }
        else if (currentChar == '}') {
            this.token = new Token(TokenType.RBRACE, lineNum, "}");
            tokens.add(this.token);
            return this.token;
        }
        else if (currentChar == '*') {
            this.token = new Token(TokenType.MULT, lineNum, "*");
            tokens.add(this.token);
            return this.token;
        }
        else if (currentChar == '+') {
            this.token = new Token(TokenType.PLUS, lineNum, "+");
            tokens.add(this.token);
            return this.token;
        }
        else if (currentChar == '-') {
            this.token = new Token(TokenType.MINU, lineNum, "-");
            tokens.add(this.token);
            return this.token;
        }
        else if (currentChar == '%') {
            this.token = new Token(TokenType.MOD, lineNum, "%");
            tokens.add(this.token);
            return this.token;
        }
        else if (currentChar == '<') {
            if (nextChar == '=') {
                this.curPos++;
                this.token = new Token(TokenType.LEQ, lineNum, "<=");
                tokens.add(this.token);
                return this.token;
            }
            else {
                this.token = new Token(TokenType.LSS, lineNum, "<");
                tokens.add(this.token);
                return this.token;
            }
        }
        else if (currentChar == '>') {
            if (nextChar == '=') {
                this.curPos++;
                this.token = new Token(TokenType.GEQ, lineNum, ">=");
                tokens.add(this.token);
                return this.token;
            }
            else {
                this.token = new Token(TokenType.GRE, lineNum, ">");
                tokens.add(this.token);
                return this.token;
            }
        }
        else if (currentChar == '=') {
            if (nextChar == '=') {
                this.curPos++;
                this.token = new Token(TokenType.EQL, lineNum, "==");
                tokens.add(this.token);
                return this.token;
            }
            else {
                this.token = new Token(TokenType.ASSIGN, lineNum, "=");
                tokens.add(this.token);
                return this.token;
            }
        }
        else if (currentChar == ' ' || currentChar == '\t') { // 空格或者制表符跳过
            return this.next();
        }
        else {
            // TODO 错误处理
            return null;
        }
        return null;
    }

}
