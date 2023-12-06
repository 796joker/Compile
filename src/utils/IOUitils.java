package utils;

import java.io.*;
import java.util.StringJoiner;


public class IOUitils {
    private static final String OUTPUT_FILE_PATH = "output.txt";
    private static final String INPUT_FILE_PATH = "testfile.txt";


    /**
     * 从"testfile.txt"读入源代码,无需考虑不同的换行符,统一替换为'\n'
     * @return 返回以'\n'拼接的输入字符串
     */
    public static String input() {
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(INPUT_FILE_PATH))) {
            StringJoiner stringJoiner = new StringJoiner("\n");
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                stringJoiner.add(line);
            }
            return stringJoiner.toString();
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * 将字符串输出到"output.txt"
     * @param value 要输出的字符串
     * @return 成功返回true,否则false
     */
    public static boolean output(String value) {
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(OUTPUT_FILE_PATH, true))) {
            bufferedWriter.write(value);
            bufferedWriter.flush();
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
