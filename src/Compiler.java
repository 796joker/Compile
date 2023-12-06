import error.ErrorHandler;
import frontend.Lexer;
import frontend.Parser;
import utils.Analyzer;

public class Compiler {
    public static void main(String[] args)  {
        // ErrorHandle init
        ErrorHandler.getErrorHandler().init();
        // Lexer Step
        Analyzer.lexerAnalyze(false);
        // Parser Step
        Analyzer.parserAnalyze(false);
        // SymbolAndError
        Analyzer.symbolAnalyze(false);
        // IR
        Analyzer.llvmIRAnalyze(true);
    }
}
