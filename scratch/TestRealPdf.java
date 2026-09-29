import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestRealPdf {
    public static void main(String[] args) throws Exception {
        String envContent = new String(Files.readAllBytes(Paths.get("/Users/thienan/Desktop/englishlearning/.env")));
        String apiKey = "";
        Pattern pattern = Pattern.compile("GEMINI_API_KEY=(.+)");
        Matcher matcher = pattern.matcher(envContent);
        if (matcher.find()) apiKey = matcher.group(1).trim();

        byte[] pdfBytes = Files.readAllBytes(Paths.get("/Users/thienan/.gemini/antigravity/brain/5c80865e-55e1-4f6f-8213-1864d53c1f54/.user_uploaded/media_1790597970247.pdf"));
        String base64Pdf = Base64.getEncoder().encodeToString(pdfBytes);
        
        String jsonPayload = "{\"contents\":[{\"parts\":[{\"inlineData\":{\"mimeType\":\"application/pdf\",\"data\":\"" + base64Pdf + "\"}},{\"text\":\"Extract passages and questions to JSON.\"}]}]}";

        String model = "gemini-3.1-flash-lite";
        System.out.println("Testing " + model + " with REAL PDF...");
        try {
            URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setDoOutput(true);
            
            try(OutputStream os = con.getOutputStream()) {
                byte[] input = jsonPayload.getBytes("utf-8");
                os.write(input, 0, input.length);
            }
            
            int status = con.getResponseCode();
            System.out.println("Status: " + status);
            
            java.util.Scanner scanner;
            if (status >= 400) {
                scanner = new java.util.Scanner(con.getErrorStream()).useDelimiter("\\A");
            } else {
                scanner = new java.util.Scanner(con.getInputStream()).useDelimiter("\\A");
            }
            System.out.println(scanner.hasNext() ? scanner.next() : "");
            
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
