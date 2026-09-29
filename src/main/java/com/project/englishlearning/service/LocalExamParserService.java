package com.project.englishlearning.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Service
public class LocalExamParserService {

    public String parseExamLocally(MultipartFile file, String lessonType) throws Exception {
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        String fullText = "";

        if (filename.endsWith(".docx")) {
            try (InputStream is = file.getInputStream();
                 XWPFDocument document = new XWPFDocument(is);
                 XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
                fullText = extractor.getText();
            }
        } else if (filename.endsWith(".pdf")) {
            try (InputStream is = file.getInputStream();
                 PDDocument document = org.apache.pdfbox.Loader.loadPDF(is.readAllBytes())) {
                PDFTextStripper stripper = new PDFTextStripper();
                fullText = stripper.getText(document);
            }
        } else {
            throw new Exception("Chỉ hỗ trợ file Word (.docx) hoặc PDF dạng chữ.");
        }

        if (fullText == null || fullText.trim().isEmpty()) {
            throw new Exception("File trống hoặc PDF ảnh scan. Tính năng Strict Template yêu cầu file có chứa văn bản gốc.");
        }

        return buildJsonFromTemplate(fullText, lessonType);
    }

    private String buildJsonFromTemplate(String text, String lessonType) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        root.put("lessonType", lessonType);
        
        ArrayNode passagesNode = mapper.createArrayNode();
        ArrayNode questionGroupsNode = mapper.createArrayNode();

        String[] lines = text.split("\\r?\\n");
        
        int currentPassageIndex = -1; // -1 means none
        ObjectNode currentPassage = null;
        
        ObjectNode currentQuestionGroup = null;
        ArrayNode currentQuestionsArray = null;
        ObjectNode currentQuestion = null;
        ArrayNode currentAnswersArray = null;

        String context = "NONE"; 
        // Contexts: NONE, CONTENT, INSTRUCTION, QUESTION

        for (String rawLine : lines) {
            String line = rawLine.trim();
            
            if (line.isEmpty()) {
                if (context.equals("CONTENT") && currentPassage != null) {
                    String old = currentPassage.get("content").asText();
                    currentPassage.put("content", old + "\n\n");
                }
                continue;
            }

            if (line.startsWith("[PASSAGE]")) {
                currentPassageIndex++;
                currentPassage = mapper.createObjectNode();
                currentPassage.put("label", "Passage " + (currentPassageIndex + 1));
                currentPassage.put("title", "");
                currentPassage.put("content", "");
                passagesNode.add(currentPassage);
                context = "NONE";
                continue;
            }
            if (line.startsWith("[TITLE]")) {
                if (currentPassage != null) {
                    currentPassage.put("title", line.substring(7).trim());
                }
                context = "NONE";
                continue;
            }
            if (line.startsWith("[CONTENT]")) {
                context = "CONTENT";
                continue;
            }
            if (line.startsWith("[GROUP]")) {
                currentQuestionGroup = mapper.createObjectNode();
                currentQuestionGroup.put("passageIndex", Math.max(0, currentPassageIndex)); 
                currentQuestionGroup.put("questionType", "MULTIPLE_CHOICE_SINGLE"); // default
                currentQuestionGroup.put("instruction", "");
                currentQuestionGroup.put("questionRange", "Questions");
                currentQuestionGroup.put("aiWarning", false); // Không cần cảnh báo vì đây là Strict Template!

                currentQuestionsArray = mapper.createArrayNode();
                currentQuestionGroup.set("questions", currentQuestionsArray);
                questionGroupsNode.add(currentQuestionGroup);
                
                currentQuestion = null;
                context = "NONE";
                continue;
            }
            if (line.startsWith("[TYPE]")) {
                if (currentQuestionGroup != null) {
                    currentQuestionGroup.put("questionType", line.substring(6).trim());
                }
                context = "NONE";
                continue;
            }
            if (line.startsWith("[INSTRUCTION]")) {
                context = "INSTRUCTION";
                String instText = line.substring(13).trim();
                if (currentQuestionGroup != null && !instText.isEmpty()) {
                    currentQuestionGroup.put("instruction", instText);
                }
                continue;
            }
            if (line.startsWith("[Q]")) {
                context = "QUESTION";
                currentQuestion = mapper.createObjectNode();
                currentQuestion.put("questionText", line.substring(3).trim());
                currentQuestion.put("correctAnswer", "");
                
                currentAnswersArray = mapper.createArrayNode();
                currentQuestion.set("answers", currentAnswersArray);
                
                if (currentQuestionsArray != null) {
                    currentQuestionsArray.add(currentQuestion);
                }
                continue;
            }
            if (line.startsWith("[OPT]")) {
                context = "NONE";
                if (currentQuestion != null && currentAnswersArray != null) {
                    ObjectNode opt = mapper.createObjectNode();
                    String optText = line.substring(5).trim();
                    opt.put("label", optText.length() > 0 ? String.valueOf(optText.charAt(0)) : "");
                    opt.put("answerText", optText);
                    opt.put("isCorrect", false);
                    currentAnswersArray.add(opt);
                }
                continue;
            }
            if (line.startsWith("[ANS]")) {
                context = "NONE";
                if (currentQuestion != null) {
                    String ansText = line.substring(5).trim();
                    currentQuestion.put("correctAnswer", ansText);
                    
                    // Nếu là Multiple Choice, đánh dấu option nào đúng
                    if (currentAnswersArray != null) {
                        for (int i = 0; i < currentAnswersArray.size(); i++) {
                            ObjectNode optNode = (ObjectNode) currentAnswersArray.get(i);
                            if (optNode.get("answerText").asText().startsWith(ansText) || optNode.get("label").asText().equalsIgnoreCase(ansText)) {
                                optNode.put("isCorrect", true);
                            }
                        }
                    }
                }
                continue;
            }

            // Xử lý multiline text cho các khối đang mở
            if (context.equals("CONTENT") && currentPassage != null) {
                String old = currentPassage.get("content").asText();
                currentPassage.put("content", old + (old.isEmpty() ? "" : "\n") + rawLine); // giữ nguyên khoảng trắng
            } else if (context.equals("INSTRUCTION") && currentQuestionGroup != null) {
                String old = currentQuestionGroup.get("instruction").asText();
                currentQuestionGroup.put("instruction", old + "\n" + rawLine.trim());
            } else if (context.equals("QUESTION") && currentQuestion != null) {
                String old = currentQuestion.get("questionText").asText();
                currentQuestion.put("questionText", old + "\n" + rawLine.trim());
            }
        }

        root.set("passages", passagesNode);
        root.set("questionGroups", questionGroupsNode);

        return mapper.writeValueAsString(root);
    }
}
