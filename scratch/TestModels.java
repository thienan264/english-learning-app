import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestModels {
    public static void main(String[] args) throws Exception {
        String envContent = new String(Files.readAllBytes(Paths.get("/Users/thienan/Desktop/englishlearning/.env")));
        String apiKey = "";
        Pattern pattern = Pattern.compile("GEMINI_API_KEY=(.+)");
        Matcher matcher = pattern.matcher(envContent);
        if (matcher.find()) apiKey = matcher.group(1).trim();

        String[] models = {
            "gemini-1.5-flash",
            "gemini-1.5-pro",
            "gemini-3.5-flash",
            "gemini-3.5-flash-lite",
            "gemini-3.8-flash",
            "gemini-flash-latest"
        };

        String jsonPayload = "{\"contents\":[{\"parts\":[{\"text\":\"Hello, are you working?\"}]}]}";

        for (String model : models) {
            System.out.print("Testing " + model + "... ");
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
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }
}
