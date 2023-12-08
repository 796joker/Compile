import java.io.*;
import java.util.StringJoiner;

public class Test {
    public static void main(String[] args) {
        int num = 2;
        try(BufferedReader bf = new BufferedReader(new FileReader("A/testfile" + num + ".txt"));
            BufferedWriter bw = new BufferedWriter(new FileWriter("testfile.txt"))) {
            String line;
            while ((line = bf.readLine()) != null) {
                bw.write(line + "\n");
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
    }
}
