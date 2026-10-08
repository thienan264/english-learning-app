package com.project.englishlearning;

import com.project.englishlearning.dto.exam.ExamDTO;
import com.project.englishlearning.service.LocalExamParserService;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CourseContentPackTests {
    @Test void everyReadingListeningWordUsesActualParserAndPreservesMetadata() throws Exception {
        Path root=Path.of("docs/course-content-pack");
        ObjectMapper mapper=new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,false);
        JsonNode manifest=mapper.readTree(root.resolve("manifest.json").toFile());
        var parser=new LocalExamParserService();
        int exams=0,questions=0,writing=0;
        Set<String> files=new HashSet<>();
        for(JsonNode entry:manifest) {
            Path word=root.resolve(entry.get("word").asText());
            assertTrue(Files.exists(word),word.toString()); assertTrue(files.add(word.toString()));
            if(entry.get("skill").asText().equals("WRITING")) {
                writing++;assertTrue(Files.size(root.resolve(entry.get("chart").asText()))>1000);continue;
            }
            exams++;
            var upload=new MockMultipartFile("file",word.getFileName().toString(),"application/vnd.openxmlformats-officedocument.wordprocessingml.document",Files.readAllBytes(word));
            var json=mapper.readTree(parser.parseExamLocally(upload,entry.get("skill").asText()));
            assertEquals(2,json.get("parserVersion").asInt());
            int count=0;List<String> answers=new ArrayList<>();
            for(JsonNode group:json.get("questionGroups")) {
                int passage=group.get("passageIndex").asInt();
                assertTrue(passage>=0 && passage<json.get("passages").size(),word.toString());
                for(JsonNode q:group.get("questions")) {
                    count++;questions++;answers.add(q.get("correctAnswer").asText());
                    assertTrue(Set.of("BEGINNER","INTERMEDIATE","ADVANCED").contains(q.get("learningLevel").asText()),word.toString());
                    assertFalse(q.get("competencyTag").asText().isBlank());assertFalse(q.get("explanation").asText().isBlank());
                    assertFalse(q.get("questionText").asText().contains("[LEVEL]"));assertFalse(q.get("questionText").asText().contains("[COMPETENCY]"));
                    if(group.get("questionType").asText().equals("MULTIPLE_CHOICE_SINGLE")) {
                        assertEquals(4,q.get("answers").size());int correct=0;
                        for(JsonNode a:q.get("answers")) if(a.get("isCorrect").asBoolean()) {
                            correct++;assertEquals(q.get("correctAnswer").asText(),a.get("label").asText());
                        }
                        assertEquals(1,correct,word.toString());
                    } else assertTrue(Set.of("TRUE","FALSE","NOT GIVEN").contains(q.get("correctAnswer").asText()));
                }
            }
            assertEquals(entry.get("question_count").asInt(),count,word.toString());
            assertEquals(entry.get("answer_key").asText(),String.join(" ",answers));
            // Bind the same object shape used by the save endpoint, excluding parserVersion metadata.
            ((com.fasterxml.jackson.databind.node.ObjectNode)json).remove("parserVersion");
            ExamDTO dto=mapper.treeToValue(json,ExamDTO.class);
            assertFalse(dto.getQuestionGroups().get(0).getQuestions().get(0).getExplanation().isBlank());
            if(entry.get("skill").asText().equals("LISTENING")) {
                assertTrue(Files.size(root.resolve(entry.get("audio").asText()))>1000);
                assertTrue(entry.get("audio_seconds").asDouble()>10);
            }
        }
        assertEquals(29,exams);assertEquals(3,writing);assertEquals(159,questions);
    }
}
