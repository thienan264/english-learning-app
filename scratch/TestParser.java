import com.project.englishlearning.service.LocalExamParserService;
import org.springframework.mock.web.MockMultipartFile;
import java.io.File;
import java.nio.file.Files;

public class TestParser {
    public static void main(String[] args) throws Exception {
        LocalExamParserService parser = new LocalExamParserService();
        File f = new File("/Users/thienan/.gemini/antigravity/brain/5c80865e-55e1-4f6f-8213-1864d53c1f54/.user_uploaded/media_1790597970247.pdf");
        byte[] content = Files.readAllBytes(f.toPath());
        MockMultipartFile mockFile = new MockMultipartFile("file", f.getName(), "application/pdf", content);
        
        try {
            String json = parser.parseExamLocally(mockFile, "READING");
            System.out.println(json);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
