package com.project.englishlearning;
import com.project.englishlearning.controller.AdminExamController;
import com.project.englishlearning.dto.exam.ExamDTO;
import com.project.englishlearning.entity.*;
import com.project.englishlearning.repository.*;
import com.project.englishlearning.service.*;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class ExamMetadataTests {
    LessonRepository lessons; ExamPassageRepository passages; QuestionGroupRepository groups;
    AdminExamController controller; Lesson lesson;
    <T> T fake(Class<T> type) { return mock(type, withSettings().mockMaker(org.mockito.MockMakers.SUBCLASS)); }
    @BeforeEach void setup() {
        lessons=fake(LessonRepository.class); passages=fake(ExamPassageRepository.class); groups=fake(QuestionGroupRepository.class);
        controller=new AdminExamController(lessons,passages,groups,fake(QuestionRepository.class),fake(GeminiAiService.class),fake(LocalExamParserService.class));
        lesson=new Lesson(); lesson.setSkillType("READING");
        when(lessons.findById(1L)).thenReturn(Optional.of(lesson));
        when(passages.findByLessonIdOrderByOrderIndexAsc(1L)).thenReturn(List.of());
        when(groups.findByLessonIdOrderByOrderIndexAsc(1L)).thenReturn(List.of());
    }
    @Test void preservesMetadataAndExplanationAcrossSaveAndRead() {
        var question=new ExamDTO.ExamQuestionDTO(); question.setQuestionText("Where?"); question.setCompetencyTag("DETAIL");
        question.setLearningLevel("BEGINNER"); question.setExplanation("Evidence");
        var group=new ExamDTO.QuestionGroupDTO(); group.setQuestionType("MULTIPLE_CHOICE_SINGLE"); group.setQuestions(List.of(question));
        var dto=new ExamDTO(); dto.setQuestionGroups(List.of(group));
        var captured=org.mockito.ArgumentCaptor.forClass(QuestionGroup.class);
        assertEquals(200,controller.saveExamStructure(1L,dto).getStatusCode().value());
        verify(groups).save(captured.capture());
        when(groups.findByLessonIdOrderByOrderIndexAsc(1L)).thenReturn(List.of(captured.getValue()));
        var loaded=controller.getExamData(1L).getQuestionGroups().get(0).getQuestions().get(0);
        assertEquals("DETAIL",loaded.getCompetencyTag()); assertEquals("BEGINNER",loaded.getLearningLevel());
        assertEquals("Evidence",loaded.getExplanation());
    }
    @Test void rejectsUnknownMetadataBeforeDeletingOldExam() {
        var question=new ExamDTO.ExamQuestionDTO(); question.setCompetencyTag("UNKNOWN");
        var group=new ExamDTO.QuestionGroupDTO(); group.setQuestions(List.of(question));
        var dto=new ExamDTO(); dto.setQuestionGroups(List.of(group));
        assertEquals(400,controller.saveExamStructure(1L,dto).getStatusCode().value());
        verify(groups,never()).deleteAll(any()); verify(passages,never()).deleteAll(any());
    }
    @Test void oldQuestionsWithoutMetadataStillSave() {
        var question=new ExamDTO.ExamQuestionDTO(); question.setQuestionText("Legacy");
        var group=new ExamDTO.QuestionGroupDTO(); group.setQuestionType("MULTIPLE_CHOICE_SINGLE"); group.setQuestions(List.of(question));
        var dto=new ExamDTO(); dto.setQuestionGroups(List.of(group));
        assertEquals(200,controller.saveExamStructure(1L,dto).getStatusCode().value());
    }
}
