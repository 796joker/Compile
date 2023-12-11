package utils;

import error.DefError;
import error.ErrorHandler;
import frontend.Lexer;
import frontend.Parser;
import llvm.Generator;
import symbol.SymbolTableBuilder;
import token.Token;

import java.io.*;
import java.util.Comparator;
import java.util.List;
import java.util.StringJoiner;

public class Analyzer {
    private static final String OUTPUT_FILE_PATH = "output.txt";
    private static final String INPUT_FILE_PATH = "testfile.txt";
    private static final String ERROR_FILE_PATH = "error.txt";
    private static final String LLVM_FILE_PATH = "llvm_ir.txt";
    public static void lexerAnalyze(boolean needShowInfo) {
        try(BufferedReader bf = new BufferedReader(new FileReader(INPUT_FILE_PATH))){
            StringJoiner sourceJoiner = new StringJoiner("\n");
            String line;
            // 用readline读入无需考虑换行符不匹配这一问题
            while ((line = bf.readLine()) != null) {
                sourceJoiner.add(line);
            }
            // 输入Lexer
            Lexer lexer = Lexer.getLexer();
            Token token;
            // 初始化lexer
            lexer.init(sourceJoiner.toString());
            if (needShowInfo) {
                try(BufferedWriter bw = new BufferedWriter(new FileWriter(OUTPUT_FILE_PATH, true))) {
                    while ((token = lexer.next()) != null) {
                        bw.write(token.toString());
                    }
                    bw.flush();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else {
                while (true) {
                    if (lexer.next() == null) {
                        break;
                    }
                }
            }
        }
        catch (IOException e) {
            System.out.println("lexer analyzer open testfile.txt failed!\n");
        }
    }

    public static void parserAnalyze(boolean needShowInfo) {
        Parser.getPARSER().init();
        Parser.getPARSER().analyze();
        if (needShowInfo) {
            Parser.getPARSER().show();
        }
    }

    public static void symbolAnalyze(boolean needShowInfo) {
        SymbolTableBuilder symbolTableBuilder = SymbolTableBuilder.getSymbolTableBuilder();
        symbolTableBuilder.init();
        symbolTableBuilder.buildSymbolTable();
        List<DefError> errorList = ErrorHandler.getErrorHandler().getErrorList();
        if (needShowInfo) {
            // 行数从小到大
            errorList.sort(Comparator.comparingInt(DefError::getLineNum));
           try (BufferedWriter bw = new BufferedWriter(new FileWriter(ERROR_FILE_PATH))) {
               for (DefError defError : errorList) {
                   bw.write(defError.toString());
                   bw.flush();
               }
           }
           catch (IOException e) {
               e.printStackTrace();
           }
        }
    }

    public static void llvmIRAnalyze(boolean needShowInfo) {
        Generator generator = Generator.getGenerator();
        generator.init();
        generator.generate();
        String ret = generator.getRet();
        if (needShowInfo) {
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(LLVM_FILE_PATH))) {
                bw.write(ret);
                bw.flush();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
