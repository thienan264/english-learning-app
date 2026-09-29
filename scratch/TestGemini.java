import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestGemini {
    public static void main(String[] args) throws Exception {
        String envContent = new String(Files.readAllBytes(Paths.get("/Users/thienan/Desktop/englishlearning/.env")));
        String apiKey = "";
        Pattern pattern = Pattern.compile("GEMINI_API_KEY=(.+)");
        Matcher matcher = pattern.matcher(envContent);
        if (matcher.find()) {
            apiKey = matcher.group(1).trim();
        }
        
        URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models?key=" + apiKey);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        
        int status = con.getResponseCode();
        BufferedReader in = new BufferedReader(new InputStreamReader(status > 299 ? con.getErrorStream() : con.getInputStream()));
        String inputLine;
        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine).append("\n");
        }
        in.close();
        System.out.println(content.toString());
    }
}
