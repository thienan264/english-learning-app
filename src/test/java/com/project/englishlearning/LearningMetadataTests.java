package com.project.englishlearning;

import com.project.englishlearning.controller.AdminLearningMetadataController;
import com.project.englishlearning.entity.*;
import com.project.englishlearning.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LearningMetadataTests {
    CourseRepository courses;
    LessonRepository lessons;
    Lesson lesson;
    MockMvc mvc;
    @BeforeEach void setup() {
        courses = mock(CourseRepository.class, withSettings().mockMaker(org.mockito.MockMakers.SUBCLASS));
        lessons = mock(LessonRepository.class, withSettings().mockMaker(org.mockito.MockMakers.SUBCLASS));
        Course course = new Course(); course.setId(1L); course.setTitle("Reading");
        var module=new com.project.englishlearning.entity.Module();module.setCourse(course);
        lesson = new Lesson();lesson.setModule(module); lesson.setId(3L); lesson.setCourse(course);
        lesson.setTitle("Thông báo"); lesson.setSkillType("READING"); lesson.setLessonType("MOCK_TEST");
        lesson.setContent("Keep original passage");
        when(courses.findById(1L)).thenReturn(Optional.of(course));
        when(lessons.findById(3L)).thenReturn(Optional.of(lesson));
        when(lessons.findByModuleCourseIdOrderByModuleOrderIndexAscOrderIndexAsc(1L)).thenReturn(List.of(lesson));
        mvc = MockMvcBuilders.standaloneSetup(new AdminLearningMetadataController(courses, lessons))
                .setViewResolvers(new AdvisorySubmissionTests.TestConfig().viewResolver()).build();
    }
    @Test void rendersExistingUnclassifiedLesson() throws Exception {
        mvc.perform(get("/admin/courses/1/learning-metadata"))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa phân loại")));
    }
    @Test void orphanedLegacyLessonCannotBeEditedAsCurrentCurriculum() throws Exception {
        lesson.setModule(null);
        mvc.perform(post("/admin/courses/1/learning-metadata/3").param("learningLevel","BEGINNER").param("assessmentRole","PRACTICE").param("learningObjective","Test"))
            .andExpect(status().isNotFound());
        verify(lessons,never()).save(any());
    }
    @Test void inlineSaveReturnsStatusWithoutRedirecting() throws Exception {
        mvc.perform(post("/admin/courses/1/learning-metadata/3/inline")
                .param("learningLevel","BEGINNER").param("assessmentRole","PRACTICE").param("learningObjective","Find details"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.message").exists());
        assertEquals("Find details",lesson.getLearningObjective());
        mvc.perform(post("/admin/courses/1/learning-metadata/3/inline")
                .param("learningLevel","BAD").param("assessmentRole","PRACTICE").param("learningObjective","Invalid"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").exists());
        assertEquals("Find details",lesson.getLearningObjective());
    }
    @Test void savesMetadataWithoutChangingContent() throws Exception {
        mvc.perform(post("/admin/courses/1/learning-metadata/3")
                .param("learningLevel", "BEGINNER").param("assessmentRole", "PRACTICE")
                .param("learningObjective", "  Tìm thời gian  "))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("success"));
        assertEquals("BEGINNER", lesson.getLearningLevel());
        assertEquals("Tìm thời gian", lesson.getLearningObjective());
        assertEquals("Keep original passage", lesson.getContent());
        verify(lessons).save(lesson);
    }
    @Test void rejectsLessonFromAnotherCourse() throws Exception {
        mvc.perform(post("/admin/courses/2/learning-metadata/3")
                .param("learningLevel", "BEGINNER").param("assessmentRole", "FINAL")
                .param("learningObjective", "Test")).andExpect(status().isNotFound());
        verify(lessons, never()).save(any());
    }
    @Test void rejectsInvalidValuesAndTheoryAssessment() throws Exception {
        for (String level : List.of("INVALID", "BEGINNER")) {
            lesson.setLessonType("THEORY");
            mvc.perform(post("/admin/courses/1/learning-metadata/3")
                    .param("learningLevel", level).param("assessmentRole", "FINAL")
                    .param("learningObjective", "Test"))
                    .andExpect(flash().attributeExists("error"));
        }
        verify(lessons, never()).save(any());
    }
    @Test void allowsClearingUnknownMetadataAndRejectsLongObjective() throws Exception {
        mvc.perform(post("/admin/courses/1/learning-metadata/3")
                .param("learningLevel", "BEGINNER").param("assessmentRole", "PRACTICE")
                .param("learningObjective", "x".repeat(501))).andExpect(flash().attributeExists("error"));
        verify(lessons, never()).save(any());
        lesson.setLearningLevel("BEGINNER");
        mvc.perform(post("/admin/courses/1/learning-metadata/3")
                .param("learningLevel", "").param("assessmentRole", "").param("learningObjective", ""))
                .andExpect(flash().attributeExists("success"));
        assertNull(lesson.getLearningLevel()); assertNull(lesson.getAssessmentRole());
        assertNull(lesson.getLearningObjective());
    }
}
