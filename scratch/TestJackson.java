import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestJackson {
    public static void main(String[] args) throws Exception {
        String json = "{\n  \"lessonType\": \"READING\",\n  \"passages\": []\n}";
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(json);
        System.out.println(rootNode.getClass().getName());
        System.out.println(rootNode.has("passages"));
    }
}
