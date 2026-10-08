package com.project.englishlearning;
import com.project.englishlearning.controller.StudentTestController;
import com.project.englishlearning.entity.*;
import com.project.englishlearning.repository.*;
import com.project.englishlearning.service.*;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.ui.ExtendedModelMap;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;
class ReadingResultTests {
    LessonRepository lessons; UserRepository users; TestResultRepository results;
    QuestionGroupRepository groups; StudentLearningService learning;
    StudentTestController controller; Lesson lesson; User user; TestResult saved; MockMvc mvc;
    <T> T fake(Class<T> type) { return mock(type,withSettings().mockMaker(org.mockito.MockMakers.SUBCLASS)); }
    @BeforeEach void setup() {
        lessons=fake(LessonRepository.class);users=fake(UserRepository.class);results=fake(TestResultRepository.class);
        groups=fake(QuestionGroupRepository.class);learning=fake(StudentLearningService.class);
        controller=new StudentTestController(lessons,fake(ExamPassageRepository.class),groups,users,results,fake(IeltsScoringService.class),learning);
        user=new User();user.setId(7L);user.setUsername("student");
        Course course=new Course();course.setId(5L);
        lesson=new Lesson();lesson.setId(3L);lesson.setCourse(course);lesson.setTitle("Đọc hồ sơ cá nhân");lesson.setAssessmentRole("PRACTICE");
        when(users.findByUsername("student")).thenReturn(Optional.of(user));when(lessons.findById(3L)).thenReturn(Optional.of(lesson));
        when(results.save(any())).thenAnswer(i -> {saved=i.getArgument(0);saved.setId(11L);return saved;});
        mvc=MockMvcBuilders.standaloneSetup(controller).setViewResolvers(new AdvisorySubmissionTests.TestConfig().viewResolver()).build();
    }
    @Test void submissionRecordsHumanReadableAnswerAndRedirectsToDetails() throws Exception {
        Question q=new Question();q.setId(1L);q.setQuestionText("Where?");q.setQuestionType("MULTIPLE_CHOICE_SINGLE");q.setExplanation("I live in Hanoi");q.setCompetencyTag("DETAIL");
        Answer answer=new Answer();answer.setId(42L);answer.setLabel("B");answer.setAnswerText("Hanoi");answer.setIsCorrect(true);q.getAnswers().add(answer);
        QuestionGroup group=new QuestionGroup();group.setQuestionType(QuestionType.MULTIPLE_CHOICE_SINGLE);group.getQuestions().add(q);
        when(groups.findByLessonIdOrderByOrderIndexAsc(3L)).thenReturn(List.of(group));
        mvc.perform(post("/lessons/3/submit").principal(new UsernamePasswordAuthenticationToken("student",""))
                .param("question_1","42")).andExpect(redirectedUrl("/lessons/3/result/11"));
        assertEquals(10.0,saved.getScore());assertEquals(1,saved.getCorrectAnswers());
        assertTrue(saved.getDetailedResultJson().contains("B. Hanoi"));assertTrue(saved.getDetailedResultJson().contains("I live in Hanoi"));
        verify(learning).recordTestResult(saved);
    }
    @Test void resultShowsScoreCorrectWrongAndExplanation() throws Exception {
        var result=new TestResult();result.setId(11L);result.setLesson(lesson);result.setUser(user);
        result.setTotalQuestions(5);result.setCorrectAnswers(2);result.setScore(3.5);
        result.setDetailedResultJson("[{\"questionText\":\"Where?\",\"submittedValue\":\"B. Hanoi\",\"correctAnswer\":\"Hanoi\",\"isCorrect\":true,\"explanation\":\"Evidence one\"},{\"questionText\":\"Who?\",\"submittedValue\":\"A. Teacher\",\"correctAnswer\":\"Nurse\",\"isCorrect\":false,\"explanation\":\"Evidence two\"}]");
        when(results.findById(11L)).thenReturn(Optional.of(result));
        mvc.perform(get("/lessons/3/result/11").principal(new UsernamePasswordAuthenticationToken("student","")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("4.0")))
                .andExpect(content().string(containsString("Đúng"))).andExpect(content().string(containsString("Sai")))
                .andExpect(content().string(containsString("Evidence two"))).andExpect(content().string(containsString("40.0%")));
        mvc.perform(get("/lessons/99/result/11").principal(new UsernamePasswordAuthenticationToken("student","")))
                .andExpect(redirectedUrl("/"));
        user=new User();user.setId(99L);when(users.findByUsername("student")).thenReturn(Optional.of(user));
        mvc.perform(get("/lessons/3/result/11").principal(new UsernamePasswordAuthenticationToken("student",""))).andExpect(redirectedUrl("/"));
    }
    @Test void practiceCompletionAndFinalThresholdAreDifferent() {
        var enrollments=fake(UserCourseEnrollmentRepository.class);var progressRepo=fake(UserLessonProgressRepository.class);
        var courses=fake(CourseRepository.class);var modules=fake(ModuleRepository.class);
        var service=new StudentLearningService(enrollments,progressRepo,courses,modules,lessons);
        var module=new com.project.englishlearning.entity.Module();module.setCourse(lesson.getCourse());module.getLessons().add(lesson);lesson.setModule(module);
        var enrollment=new UserCourseEnrollment();when(enrollments.findByUserIdAndCourseId(7L,5L)).thenReturn(Optional.of(enrollment));
        when(modules.findByCourseIdOrderByOrderIndexAsc(5L)).thenReturn(List.of(module));
        var progress=new UserLessonProgress();progress.setLesson(lesson);progress.setUser(user);progress.setStatus("IN_PROGRESS");
        when(progressRepo.findByUserIdAndLessonId(7L,3L)).thenReturn(Optional.of(progress));
        var result=new TestResult();result.setLesson(lesson);result.setUser(user);result.setCorrectAnswers(1);result.setTotalQuestions(5);
        service.recordTestResult(result);assertEquals("COMPLETED",progress.getStatus());assertEquals(2.0,progress.getHighestScore());
        progress.setStatus("IN_PROGRESS");lesson.setAssessmentRole("FINAL");
        service.recordTestResult(result);assertEquals("IN_PROGRESS",progress.getStatus());
        result.setCorrectAnswers(4);service.recordTestResult(result);assertEquals("COMPLETED",progress.getStatus());assertEquals(8.0,progress.getHighestScore());
    }
    @org.springframework.stereotype.Controller
    static class DashboardFixture {
        final Map<String,Object> data;
        DashboardFixture(Map<String,Object> data) { this.data=data; }
        @org.springframework.web.bind.annotation.GetMapping("/fixture")
        String render(org.springframework.ui.Model model) { model.addAllAttributes(data); return "student/learning-dashboard"; }
    }
    @Test void dashboardRendersHistoryScoreLinkAndGreenCheck() throws Exception {
        lesson.setLessonType("MOCK_TEST");lesson.setSkillType("READING");
        var module=new com.project.englishlearning.entity.Module();module.setId(1L);module.setTitle("Học và luyện tập");
        module.setCourse(lesson.getCourse());module.getLessons().add(lesson);lesson.setModule(module);
        var result=new TestResult();result.setId(11L);result.setLesson(lesson);result.setCorrectAnswers(2);result.setTotalQuestions(5);result.setScore(3.5);
        var enrollment=new UserCourseEnrollment();enrollment.setCompletionPercentage(20.0);
        var progress=new UserLessonProgress();progress.setStatus("COMPLETED");
        Map<String,Object> data=new HashMap<>();data.put("course",lesson.getCourse());data.put("modules",List.of(module));
        data.put("activeLesson",lesson);data.put("progressMap",Map.of(3L,progress));data.put("enrollment",enrollment);
        data.put("isExpired",false);data.put("daysUntilExpiration",-1L);data.put("testHistory",List.of(result));
        var dashboard=MockMvcBuilders.standaloneSetup(new DashboardFixture(data))
                .setViewResolvers(new AdvisorySubmissionTests.TestConfig().viewResolver()).build();
        dashboard.perform(get("/fixture")).andExpect(status().isOk())
                .andExpect(content().string(containsString("4.0 / 10")))
                .andExpect(content().string(containsString("/lessons/3/result/11")))
                .andExpect(content().string(containsString("bi-check-circle-fill")));
    }

    @Test void legacyChoiceIdsResolveButNumericFillAnswersStayUnchanged() throws Exception {
        Question choice=new Question();choice.setId(1L);choice.setQuestionType("MULTIPLE_CHOICE_SINGLE");
        Answer answer=new Answer();answer.setId(410L);answer.setLabel("A");answer.setAnswerText("Da Nang");choice.getAnswers().add(answer);
        Question fill=new Question();fill.setId(2L);fill.setQuestionType("FILL_IN_THE_BLANK");
        lesson.getQuestions().add(choice);lesson.getQuestions().add(fill);
        when(groups.findByLessonIdOrderByOrderIndexAsc(3L)).thenReturn(List.of());
        var result=new TestResult();result.setId(11L);result.setUser(user);result.setLesson(lesson);result.setTotalQuestions(2);result.setCorrectAnswers(0);
        result.setDetailedResultJson("[{\"questionId\":1,\"questionText\":\"Where?\",\"submittedValue\":\"410\",\"correctAnswer\":\"Hanoi\",\"isCorrect\":false,\"explanation\":\"Evidence\"},{\"questionId\":2,\"questionText\":\"Number?\",\"submittedValue\":\"410\",\"correctAnswer\":\"400\",\"isCorrect\":false}]");
        when(results.findById(11L)).thenReturn(Optional.of(result));
        var model=new ExtendedModelMap();
        controller.viewTestResult(3L,11L,model,new UsernamePasswordAuthenticationToken("student",""));
        @SuppressWarnings("unchecked") var details=(List<Map<String,Object>>)model.get("detailedAnswers");
        assertEquals("A. Da Nang",details.get(0).get("submittedValue"));
        assertEquals("410",details.get(1).get("submittedValue"));
        assertEquals(false,details.get(0).get("isCorrect"));
        answer.setId(999L);
        model=new ExtendedModelMap();controller.viewTestResult(3L,11L,model,new UsernamePasswordAuthenticationToken("student",""));
        @SuppressWarnings("unchecked") var missing=(List<Map<String,Object>>)model.get("detailedAnswers");
        assertTrue(missing.get(0).get("submittedValue").toString().contains("Không khôi phục"));
    }

}
