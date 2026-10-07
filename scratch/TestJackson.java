import com.fasterxml.jackson.databind.ObjectMapper;
public class TestJackson {
    public static class Notification {
        private Boolean isRead = false;
        public Boolean getIsRead() { return isRead; }
    }
    public static void main(String[] args) throws Exception {
        System.out.println(new ObjectMapper().writeValueAsString(new Notification()));
    }
}
