package com.project.englishlearning;

import com.project.englishlearning.entity.Lesson;
import com.project.englishlearning.entity.WritingSubmission;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.StringTemplateResolver;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class WritingHistoryDisplayTests {
    @Test void historyShowsSavedBandAndDistinguishesPendingAndFailedGrading() throws Exception {
        String page=Files.readString(Path.of("src/main/resources/templates/student/learning-dashboard.html"));
        int rowStart=page.indexOf("<tr th:each=\"hist, stat : ${writingHistory}\">");
        String row=page.substring(rowStart,page.indexOf("</tr>",rowStart)+5);
        // Render the score row without context-relative navigation in this non-web test.
        row=row.replaceAll("th:href=\"[^\"]*\"", "");
        var engine=new SpringTemplateEngine();engine.setTemplateResolver(new StringTemplateResolver());
        var lesson=new Lesson();lesson.setId(3L);
        var context=new Context();context.setVariable("activeLesson",lesson);
        for(String state:List.of("COMPLETED","EVALUATED","REVIEWED","WAITING_REVIEW","EVALUATING","ERROR")) {
            var submission=new WritingSubmission();submission.setId(1L);submission.setStatus(state);
            submission.setBandScore(6.5);submission.setSubmittedAt(LocalDateTime.now());
            context.setVariable("writingHistory",List.of(submission));
            String html=engine.process(row,context);
            if(state.equals("COMPLETED") || state.equals("EVALUATED") || state.equals("REVIEWED")) {
                assertTrue(html.contains(">6.5</span>"));assertTrue(html.contains("Đã chấm"));
            } else {
                assertFalse(html.contains(">6.5</span>"));
                assertTrue(html.contains(state.equals("ERROR") ? "Chấm lỗi" : (state.equals("WAITING_REVIEW") ? "Chờ chuyên gia" : "Chờ AI...")));
                assertFalse(html.contains("Đã chấm"));
            }
        }
    }
}
