import error.ErrorHandler;
import utils.Analyzer;

/**
 * @author AS
 */
public class Compiler {
    public static void main(String[] args)  {
        // ErrorHandle init
        ErrorHandler.getErrorHandler().init();
        // Lexer Step
        Analyzer.lexerAnalyze(false);
        // Parser Step
        Analyzer.parserAnalyze(false);
        // SymbolAndError
        Analyzer.symbolAnalyze(true);
        if (ErrorHandler.getErrorHandler().ifErrorExist()) {
            // IR (没有编译错误才进行目标代码生成)
            Analyzer.llvmIRAnalyze(true);
        }
    }
}
