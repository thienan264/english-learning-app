import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestLargeRequest {
    public static void main(String[] args) throws Exception {
        String envContent = new String(Files.readAllBytes(Paths.get("/Users/thienan/Desktop/englishlearning/.env")));
        String apiKey = "";
        Pattern pattern = Pattern.compile("GEMINI_API_KEY=(.+)");
        Matcher matcher = pattern.matcher(envContent);
        if (matcher.find()) apiKey = matcher.group(1).trim();

        // Create a large text payload (approx 3000 chars)
        StringBuilder sb = new StringBuilder();
        sb.append("Please extract this text into JSON. ");
        for (int i=0; i<3000; i++) {
            sb.append("This is a long passage for reading comprehension. It contains many words. ");
        }
        
        String jsonPayload = "{\"contents\":[{\"parts\":[{\"text\":\"" + sb.toString() + "\"}]}]}";

        System.out.print("Testing gemini-3.8-flash with large payload... ");
        try {
            URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent?key=" + apiKey);
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
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
