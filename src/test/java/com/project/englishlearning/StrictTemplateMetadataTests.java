package com.project.englishlearning;
import com.project.englishlearning.service.LocalExamParserService;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import java.nio.file.*;
import java.io.*;
import static org.junit.jupiter.api.Assertions.*;
class StrictTemplateMetadataTests {
    final LocalExamParserService parser=new LocalExamParserService();
    JsonNode parse(byte[] bytes) throws Exception {
        return new ObjectMapper().readTree(parser.parseExamLocally(new MockMultipartFile("file","exam.docx","application/vnd.openxmlformats-officedocument.wordprocessingml.document",bytes),"READING"));
    }
    byte[] word(String text) throws Exception {
        try(var document=new XWPFDocument(); var output=new ByteArrayOutputStream()) {
            for(String line:text.split("\n")) document.createParagraph().createRun().setText(line);
            document.write(output);return output.toByteArray();
        }
    }
    @Test void generatedWordImportsFiveQuestionsWithLabelsAnswersAndExplanations() throws Exception {
        var data=parse(Files.readAllBytes(Path.of("docs/exams/Reading_Beginner_Doc_ho_so_ca_nhan.docx")));
        assertEquals(2,data.get("parserVersion").asInt());
        assertEquals(1,data.get("passages").size());assertEquals(1,data.get("questionGroups").size());
        var questions=data.get("questionGroups").get(0).get("questions");assertEquals(5,questions.size());
        assertEquals("Da Nang",questions.get(0).get("answers").get(0).get("answerText").asText());
        assertEquals("1. Where does Anna live now?",questions.get(0).get("questionText").asText());
        String[] expected={"B","C","A","D","B"};
        for(int i=0;i<5;i++) {
            var q=questions.get(i);assertEquals("BEGINNER",q.get("learningLevel").asText());assertEquals("DETAIL",q.get("competencyTag").asText());
            assertFalse(q.get("explanation").asText().isBlank());assertEquals(expected[i],q.get("correctAnswer").asText());
            int count=0;for(var a:q.get("answers")) if(a.get("isCorrect").asBoolean()) {count++;assertEquals(expected[i],a.get("label").asText());}
            assertEquals(1,count);
        }
        assertFalse(data.get("passages").get(0).get("content").asText().contains("[LEVEL]"));
    }
    @Test void originalTemplateStillImportsFortyQuestionsAndMultipleAnswers() throws Exception {
        byte[] bytes;
        try(var stream=getClass().getResourceAsStream("/exams/strict-legacy.docx")) {bytes=stream.readAllBytes();}
        var data=parse(bytes);assertEquals(3,data.get("passages").size());int count=0;boolean multi=false;
        for(var g:data.get("questionGroups")) for(var q:g.get("questions")) {
            count++;assertEquals("",q.get("competencyTag").asText());
            if(q.get("correctAnswer").asText().equals("B, C")) {
                multi=true;int correct=0;for(var a:q.get("answers")) if(a.get("isCorrect").asBoolean()) correct++;
                assertEquals(2,correct);
            }
        }
        assertEquals(40,count);assertTrue(multi);
    }
    @Test void multilineExplanationDoesNotLeakAndTagsAreNotInherited() throws Exception {
        var data=parse(word("[GROUP]\n[TYPE] FILL_IN_THE_BLANK\n[Q] First?\n[LEVEL] BEGINNER\n[COMPETENCY] DETAIL\n[ANS] 24\n[EXPLANATION] Line one\nLine two\n[Q] Second?\n[ANS] 7"));
        var questions=data.get("questionGroups").get(0).get("questions");
        assertEquals("Line one\nLine two",questions.get(0).get("explanation").asText());
        assertEquals("",questions.get(1).get("competencyTag").asText());
        assertEquals("",questions.get(1).get("learningLevel").asText());
        assertEquals("First?",questions.get(0).get("questionText").asText());
    }
    @Test void invalidOrMisplacedMetadataFailsClearly() throws Exception {
        var error=assertThrows(IllegalArgumentException.class,()->parse(word("[GROUP]\n[Q] Test\n[LEVEL] EASY")));
        assertTrue(error.getMessage().contains("[LEVEL]"));
        assertThrows(IllegalArgumentException.class,()->parse(word("[COMPETENCY] DETAIL")));
    }
}
