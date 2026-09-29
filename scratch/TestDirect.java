import com.project.englishlearning.service.LocalExamParserService;

public class TestDirect {
    public static void main(String[] args) throws Exception {
        LocalExamParserService service = new LocalExamParserService();
        java.lang.reflect.Method method = LocalExamParserService.class.getDeclaredMethod("buildJsonFromText", String.class, String.class);
        method.setAccessible(true);
        String json = (String) method.invoke(service, "This is a random text with no keywords.\nJust normal words.\n", "READING");
        System.out.println(json);
    }
}
