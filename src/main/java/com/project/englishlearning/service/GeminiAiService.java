package com.project.englishlearning.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

@Service
public class GeminiAiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    public String evaluateWriting(String topic, String studentEssay) {
        String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite:generateContent?key=" + apiKey;

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

       String prompt = "Bạn là một giám khảo chấm thi IELTS Writing chuyên nghiệp. " +
                "Đề bài: '" + topic + "'. Bài làm của học viên: '" + studentEssay + "'. " +
                "Hãy thực hiện 2 nhiệm vụ sau: " +
                "1. Chấm điểm bài viết theo thang IELTS (từ 0.0 đến 9.0) và đưa ra nhận xét chi tiết bằng tiếng Việt. " +
                "2. Viết lại một bài mẫu gợi ý dựa trên chính ý tưởng gốc của học viên, nhưng nâng cấp từ vựng, ngữ pháp và cấu trúc câu để đạt mức điểm CAO HƠN ĐÚNG 1.0 BAND so với điểm bạn vừa chấm (Nếu điểm chấm là 8.5 hoặc 9.0 thì bài gợi ý giữ nguyên mức 9.0). " +
                "BẮT BUỘC trả về kết quả ĐÚNG theo định dạng JSON dưới đây, KHÔNG BÀO CHỮA, KHÔNG markdown: " +
                "{\"bandScore\": 6.5, \"feedback\": \"Nhận xét...\", \"suggestedEssay\": \"Bài viết nâng cấp...\"}";
        try {
            // Dùng ObjectMapper để build JSON an toàn, tránh lỗi khi bài viết có ký tự đặc biệt
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode textPart = mapper.createObjectNode();
            textPart.put("text", prompt);

            ArrayNode parts = mapper.createArrayNode();
            parts.add(textPart);

            ObjectNode contentItem = mapper.createObjectNode();
            contentItem.set("parts", parts);

            ArrayNode contents = mapper.createArrayNode();
            contents.add(contentItem);

            ObjectNode requestBody = mapper.createObjectNode();
            requestBody.set("contents", contents);

            String requestBodyJson = mapper.writeValueAsString(requestBody);
            HttpEntity<String> request = new HttpEntity<>(requestBodyJson, headers);

            // Gọi Gemini API
            String response = restTemplate.postForObject(apiUrl, request, String.class);

            // Parse response lấy text AI trả về
            JsonNode root = mapper.readTree(response);
            String aiResponseText = root.path("candidates").get(0)
                                        .path("content").path("parts").get(0)
                                        .path("text").asText();

            // Xóa markdown code block nếu AI trả về ```json ... ```
            aiResponseText = aiResponseText.trim();
            if (aiResponseText.startsWith("```")) {
                aiResponseText = aiResponseText.replaceAll("^```[a-zA-Z]*\\n?", "").replaceAll("```$", "").trim();
            }

            return aiResponseText;

        } catch (HttpClientErrorException e) {
            System.err.println("=== LỖI GEMINI API (Client Error) ===");
            System.err.println("HTTP Status: " + e.getStatusCode());
            System.err.println("Response Body: " + e.getResponseBodyAsString());
            System.err.println("====================================");
            return "{\"bandScore\": 0.0, \"feedback\": \"Lỗi API: " + e.getStatusCode() + " - Kiểm tra lại API Key trong application.properties.\"}";
        } catch (HttpServerErrorException e) {
            System.err.println("=== LỖI GEMINI API (Server Error) ===");
            System.err.println("HTTP Status: " + e.getStatusCode());
            System.err.println("Response Body: " + e.getResponseBodyAsString());
            System.err.println("=====================================");
            return "{\"bandScore\": 0.0, \"feedback\": \"Hệ thống AI đang bảo trì, vui lòng thử lại sau.\"}";
        } catch (Exception e) {
            System.err.println("=== LỖI KHÔNG XÁC ĐỊNH KHI GỌI GEMINI ===");
            System.err.println("Exception: " + e.getClass().getName());
            System.err.println("Message: " + e.getMessage());
            System.err.println("==========================================");
            return "{\"bandScore\": 0.0, \"feedback\": \"Lỗi hệ thống: " + e.getMessage() + "\"}";
        }
    }
}