import com.project.englishlearning.service.LocalExamParserService;
import org.springframework.mock.web.MockMultipartFile;
import java.io.File;
import java.nio.file.Files;

public class TestDirect {
    public static void main(String[] args) throws Exception {
        LocalExamParserService parser = new LocalExamParserService();
        File f = new File("/Users/thienan/Desktop/IELTS_Strict_Template_Test.docx");
        byte[] content = Files.readAllBytes(f.toPath());
        MockMultipartFile mockFile = new MockMultipartFile("file", f.getName(), "application/vnd.openxmlformats-officedocument.wordprocessingml.document", content);
        
        try {
            String json = parser.parseExamLocally(mockFile, "READING");
            System.out.println(json);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
