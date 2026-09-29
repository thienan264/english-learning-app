import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestPrompt {
    public static void main(String[] args) throws Exception {
        String envContent = new String(Files.readAllBytes(Paths.get("/Users/thienan/Desktop/englishlearning/.env")));
        String apiKey = "";
        Pattern pattern = Pattern.compile("GEMINI_API_KEY=(.+)");
        Matcher matcher = pattern.matcher(envContent);
        if (matcher.find()) apiKey = matcher.group(1).trim();

        String lessonType = "READING";
        String systemPrompt = "You are an expert IELTS/English Exam Parser. Your task is to extract passages and questions from the provided document and output STRICTLY in JSON format.\n" +
                "The exam type is: " + lessonType + ".\n" +
                "Extract passages (or audio contexts for Listening) into 'passages'.\n" +
                "Group questions under 'questionGroups', linking them to the passage index using 'passageIndex'.\n" +
                "Allowed questionType values: 'MULTIPLE_CHOICE_SINGLE', 'MULTIPLE_CHOICE_MULTI', 'FILL_IN_THE_BLANK', 'TRUE_FALSE_NOT_GIVEN', 'MATCHING_INFORMATION', 'SUMMARY_COMPLETION'.\n" +
                "Important Rules:\n" +
                "- For MULTIPLE_CHOICE_SINGLE, extract choices into the 'answers' array. Mark 'isCorrect': true for the right answer if known (or pick a random one if unknown, but set aiWarning = true on the group).\n" +
                "- For FILL_IN_THE_BLANK, extract the expected word into 'correctAnswer' if known.\n" +
                "- Add an 'aiWarning': boolean flag to each QuestionGroup. Set it to true if you are unsure about the classification, if the text is blurry, or if the answers are missing. Set to false if you are confident.\n" +
                "- Output ONLY valid JSON matching this structure exactly (NO Markdown blocks like ```json):\n" +
                "{\n" +
                "  \"lessonType\": \"" + lessonType + "\",\n" +
                "  \"passages\": [\n" +
                "    { \"label\": \"Passage 1\", \"title\": \"...\", \"content\": \"...\" }\n" +
                "  ],\n" +
                "  \"questionGroups\": [\n" +
                "    {\n" +
                "      \"passageIndex\": 0,\n" +
                "      \"questionType\": \"MULTIPLE_CHOICE_SINGLE\",\n" +
                "      \"instruction\": \"Choose the correct letter...\",\n" +
                "      \"questionRange\": \"Questions 1-3\",\n" +
                "      \"aiWarning\": false,\n" +
                "      \"questions\": [\n" +
                "        {\n" +
                "          \"questionText\": \"What is...?\",\n" +
                "          \"correctAnswer\": \"\",\n" +
                "          \"answers\": [\n" +
                "             { \"label\": \"A\", \"answerText\": \"Choice A\", \"isCorrect\": true },\n" +
                "             { \"label\": \"B\", \"answerText\": \"Choice B\", \"isCorrect\": false }\n" +
                "          ]\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}\n\nDocument text:\nPassage 1\nCats are great.\nQuestions 1-2\n1. What are cats?\nA. Great\nB. Bad\n2. Cats are bad. (True/False)";
                
        // Escape the prompt for JSON
        String escapedPrompt = systemPrompt.replace("\"", "\\\"").replace("\n", "\\n");
        String jsonPayload = "{\"contents\":[{\"parts\":[{\"text\":\"" + escapedPrompt + "\"}]}]}";

        String model = "gemini-3.1-flash-lite";
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
        if (status == 200) {
            java.util.Scanner scanner = new java.util.Scanner(con.getInputStream()).useDelimiter("\\A");
            System.out.println(scanner.hasNext() ? scanner.next() : "");
        } else {
            java.util.Scanner scanner = new java.util.Scanner(con.getErrorStream()).useDelimiter("\\A");
            System.out.println(scanner.hasNext() ? scanner.next() : "");
        }
    }
}
