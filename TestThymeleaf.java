import org.thymeleaf.expression.Numbers;
import java.util.Locale;

public class TestThymeleaf {
    public static void main(String[] args) {
        Numbers numbers = new Numbers(Locale.US);
        try {
            numbers.formatDecimal(100, 0, "COMMA", 0, "POINT");
            System.out.println("Success!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
