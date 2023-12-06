import java.util.StringJoiner;

public class Test {
    public static void main(String[] args) {
        StringJoiner sj1 = new StringJoiner("\n");
        sj1.add("1").add("2");
        StringJoiner sj2 = new StringJoiner("\n");
        sj2.add(sj1.toString());
        System.out.println(sj2.toString());
    }
}
