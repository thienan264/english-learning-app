package com.project.englishlearning.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Base64;

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

            String response = restTemplate.postForObject(apiUrl, request, String.class);
            return extractTextFromResponse(response, mapper);

        } catch (Exception e) {
            return handleError(e);
        }
    }

    public String evaluateWritingDetailed(String task1Prompt, String task1Essay,
                                          String task2Prompt, String task2Essay) {
        String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite:generateContent?key=" + apiKey;

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("You are an expert ex-IELTS examiner with 20 years of experience. ");
        promptBuilder.append("Your task is to evaluate the student's writing based on the strict IELTS scoring rubrics.\n\n");

        promptBuilder.append("IMPORTANT RULES:\n");
        promptBuilder.append("1. If the student's essay is random gibberish (e.g. 'asdfgh'), a copy of the prompt, or completely unrelated to the topic, set isOffTopic to true, assign all scores to 0, and explain why in generalFeedback.\n");
        promptBuilder.append("2. Score each task independently on 4 criteria (0.0-9.0, in 0.5 increments).\n");
        promptBuilder.append("3. For Task 1: criteria are taskAchievement, coherenceCohesion, lexicalResource, grammaticalRange.\n");
        promptBuilder.append("4. For Task 2: criteria are taskResponse, coherenceCohesion, lexicalResource, grammaticalRange.\n");
        promptBuilder.append("5. Overall Band = (Task1Overall * 1/3) + (Task2Overall * 2/3), rounded to nearest 0.5.\n");
        promptBuilder.append("6. Provide generalFeedback in Vietnamese for each task.\n");
        promptBuilder.append("7. List specific errors with corrections in detailedErrors array.\n");
        promptBuilder.append("8. Return ONLY valid JSON, no markdown, no explanation outside JSON.\n\n");

        if (task1Prompt != null && !task1Prompt.isEmpty()) {
            promptBuilder.append("=== TASK 1 ===\n");
            promptBuilder.append("Đề bài Task 1: ").append(task1Prompt).append("\n");
            promptBuilder.append("Bài làm Task 1: ").append(task1Essay != null ? task1Essay : "(Không có bài làm)").append("\n\n");
        }
        if (task2Prompt != null && !task2Prompt.isEmpty()) {
            promptBuilder.append("=== TASK 2 ===\n");
            promptBuilder.append("Đề bài Task 2: ").append(task2Prompt).append("\n");
            promptBuilder.append("Bài làm Task 2: ").append(task2Essay != null ? task2Essay : "(Không có bài làm)").append("\n\n");
        }

        promptBuilder.append("Return JSON in this EXACT structure:\n");
        promptBuilder.append("{\n");
        promptBuilder.append("  \"isOffTopic\": false,\n");
        promptBuilder.append("  \"overallBand\": 6.5,\n");
        promptBuilder.append("  \"task1Evaluation\": {\n");
        promptBuilder.append("    \"bandScore\": 6.0,\n");
        promptBuilder.append("    \"criteria\": {\n");
        promptBuilder.append("      \"taskAchievement\": 6.0,\n");
        promptBuilder.append("      \"coherenceCohesion\": 6.0,\n");
        promptBuilder.append("      \"lexicalResource\": 6.5,\n");
        promptBuilder.append("      \"grammaticalRange\": 5.5\n");
        promptBuilder.append("    },\n");
        promptBuilder.append("    \"generalFeedback\": \"Nhận xét bằng tiếng Việt...\",\n");
        promptBuilder.append("    \"detailedErrors\": [\n");
        promptBuilder.append("      {\"originalText\": \"sai\", \"errorType\": \"Grammar\", \"correction\": \"đúng\", \"explanation\": \"giải thích\"}\n");
        promptBuilder.append("    ]\n");
        promptBuilder.append("  },\n");
        promptBuilder.append("  \"task2Evaluation\": {\n");
        promptBuilder.append("    \"bandScore\": 7.0,\n");
        promptBuilder.append("    \"criteria\": {\n");
        promptBuilder.append("      \"taskResponse\": 7.0,\n");
        promptBuilder.append("      \"coherenceCohesion\": 7.0,\n");
        promptBuilder.append("      \"lexicalResource\": 6.5,\n");
        promptBuilder.append("      \"grammaticalRange\": 6.5\n");
        promptBuilder.append("    },\n");
        promptBuilder.append("    \"generalFeedback\": \"Nhận xét bằng tiếng Việt...\",\n");
        promptBuilder.append("    \"detailedErrors\": []\n");
        promptBuilder.append("  },\n");
        promptBuilder.append("  \"suggestedEssay\": \"Bài viết mẫu nâng cấp...\"\n");
        promptBuilder.append("}");

        try {
            ObjectMapper mapper = new ObjectMapper();
            com.fasterxml.jackson.databind.node.ObjectNode textPart = mapper.createObjectNode();
            textPart.put("text", promptBuilder.toString());

            com.fasterxml.jackson.databind.node.ArrayNode parts = mapper.createArrayNode();
            parts.add(textPart);

            com.fasterxml.jackson.databind.node.ObjectNode contentItem = mapper.createObjectNode();
            contentItem.set("parts", parts);

            com.fasterxml.jackson.databind.node.ArrayNode contents = mapper.createArrayNode();
            contents.add(contentItem);

            com.fasterxml.jackson.databind.node.ObjectNode requestBody = mapper.createObjectNode();
            requestBody.set("contents", contents);

            String requestBodyJson = mapper.writeValueAsString(requestBody);
            HttpEntity<String> request = new HttpEntity<>(requestBodyJson, headers);

            String response = restTemplate.postForObject(apiUrl, request, String.class);
            return extractTextFromResponse(response, mapper);

        } catch (Exception e) {
            return handleError(e);
        }
    }

    public String extractExamFromFile(MultipartFile file, String lessonType) throws Exception {
        String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite:generateContent?key=" + apiKey;

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ObjectMapper mapper = new ObjectMapper();
        ArrayNode parts = mapper.createArrayNode();

        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        
        if (filename.endsWith(".docx")) {
            try (InputStream is = file.getInputStream();
                 XWPFDocument document = new XWPFDocument(is);
                 XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
                String text = extractor.getText();
                ObjectNode textPart = mapper.createObjectNode();
                textPart.put("text", "Dưới đây là nội dung văn bản đề thi:\n\n" + text);
                parts.add(textPart);
            }
        } else if (filename.endsWith(".pdf") || filename.endsWith(".png") || filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
            String base64Data = Base64.getEncoder().encodeToString(file.getBytes());
            ObjectNode filePart = mapper.createObjectNode();
            ObjectNode inlineData = mapper.createObjectNode();
            String mimeType = file.getContentType();
            if (mimeType == null || mimeType.isEmpty()) {
                mimeType = filename.endsWith(".pdf") ? "application/pdf" : "image/jpeg";
            }
            inlineData.put("mimeType", mimeType);
            inlineData.put("data", base64Data);
            filePart.set("inlineData", inlineData);
            parts.add(filePart);
        } else {
            throw new Exception("Vui lòng tải lên file định dạng .pdf, .docx, hoặc hình ảnh.");
        }

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
                "}";

        ObjectNode promptPart = mapper.createObjectNode();
        promptPart.put("text", systemPrompt);
        parts.add(promptPart);

        ObjectNode contentItem = mapper.createObjectNode();
        contentItem.set("parts", parts);

        ArrayNode contents = mapper.createArrayNode();
        contents.add(contentItem);

        ObjectNode requestBody = mapper.createObjectNode();
        requestBody.set("contents", contents);

        String requestBodyJson = mapper.writeValueAsString(requestBody);
        HttpEntity<String> request = new HttpEntity<>(requestBodyJson, headers);

        try {
            String response = restTemplate.postForObject(apiUrl, request, String.class);
            return extractTextFromResponse(response, mapper);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            String errorResponse = e.getResponseBodyAsString();
            throw new Exception("Lỗi API Gemini: " + e.getStatusCode() + " - " + errorResponse);
        } catch (Exception e) {
            throw new Exception("Lỗi gọi API: " + e.getMessage());
        }
    }

    private String extractTextFromResponse(String response, ObjectMapper mapper) throws Exception {
        JsonNode root = mapper.readTree(response);
        JsonNode candidate = root.path("candidates").get(0);
        
        if (candidate.has("finishReason")) {
            String reason = candidate.path("finishReason").asText();
            if (!"STOP".equals(reason)) {
                if ("RECITATION".equals(reason)) {
                    throw new Exception("Google AI chặn bóc tách file này vì lý do BẢN QUYỀN (RECITATION). File PDF này chứa nội dung của đề thi IELTS có bản quyền đang được lưu hành trên mạng. Vui lòng sử dụng file tự biên soạn hoặc file khác.");
                } else if ("SAFETY".equals(reason)) {
                    throw new Exception("Google AI chặn file vì lý do an toàn (SAFETY).");
                }
                throw new Exception("Bị chặn bởi AI (Lý do: " + reason + ").");
            }
        }
        
        JsonNode parts = candidate.path("content").path("parts");
        if (parts.isMissingNode() || parts.isEmpty()) {
            throw new Exception("AI không trả về dữ liệu. Có thể do file quá mờ hoặc có cấu trúc bất thường.");
        }
        
        String aiResponseText = parts.get(0).path("text").asText();

        aiResponseText = aiResponseText.trim();
        if (aiResponseText.startsWith("```")) {
            aiResponseText = aiResponseText.replaceAll("^```(?:json)?\\n?", "").replaceAll("```$", "").trim();
        }
        return aiResponseText;
    }

    private String handleError(Exception e) {
        if (e instanceof HttpClientErrorException) {
            HttpClientErrorException clientError = (HttpClientErrorException) e;
            System.err.println("=== LỖI GEMINI API (Client Error) ===");
            System.err.println("HTTP Status: " + clientError.getStatusCode());
            System.err.println("Response Body: " + clientError.getResponseBodyAsString());
            return "{\"bandScore\": 0.0, \"feedback\": \"Lỗi API: " + clientError.getStatusCode() + "\"}";
        } else if (e instanceof HttpServerErrorException) {
            HttpServerErrorException serverError = (HttpServerErrorException) e;
            System.err.println("=== LỖI GEMINI API (Server Error) ===");
            System.err.println("HTTP Status: " + serverError.getStatusCode());
            return "{\"bandScore\": 0.0, \"feedback\": \"Hệ thống AI đang bảo trì, vui lòng thử lại sau.\"}";
        } else {
            e.printStackTrace();
            return "{\"bandScore\": 0.0, \"feedback\": \"Lỗi hệ thống: " + e.getMessage() + "\"}";
        }
    }
}
