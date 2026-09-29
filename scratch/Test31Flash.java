import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Test31Flash {
    public static void main(String[] args) throws Exception {
        String envContent = new String(Files.readAllBytes(Paths.get("/Users/thienan/Desktop/englishlearning/.env")));
        String apiKey = "";
        Pattern pattern = Pattern.compile("GEMINI_API_KEY=(.+)");
        Matcher matcher = pattern.matcher(envContent);
        if (matcher.find()) apiKey = matcher.group(1).trim();

        // Create a large text payload (approx 5000 chars)
        StringBuilder sb = new StringBuilder();
        sb.append("Please extract this text into JSON. ");
        for (int i=0; i<5000; i++) {
            sb.append("This is a long passage for reading comprehension. It contains many words. ");
        }
        
        String jsonPayload = "{\"contents\":[{\"parts\":[{\"text\":\"" + sb.toString() + "\"}]}]}";

        String model = "gemini-3.1-flash-lite";
        
        System.out.print("Testing " + model + " with large payload... ");
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
