import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.ResponseEntity;

public class TestControllerResponse {
    public static void main(String[] args) throws Exception {
        String json = "{\"lessonType\":\"READING\",\"passages\":[]}";
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(json);
        ResponseEntity<JsonNode> r = ResponseEntity.ok(root);
        System.out.println(r.getBody().getClass().getName());
    }
}
