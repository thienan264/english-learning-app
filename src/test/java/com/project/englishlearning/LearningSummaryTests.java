package com.project.englishlearning;
import com.project.englishlearning.controller.LearningSummaryController;
import com.project.englishlearning.service.LearningSummaryService;
import com.project.englishlearning.entity.*;
import com.project.englishlearning.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.time.LocalDateTime;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

class LearningSummaryTests {
    TestResultRepository tests;UserCourseEnrollmentRepository enrollments;UserLessonProgressRepository progress;
    UserRepository users;LessonRepository lessons;LearningSummaryService service;User user;
    <T>T fake(Class<T>type){return mock(type,withSettings().mockMaker(org.mockito.MockMakers.SUBCLASS));}
    @BeforeEach void setup(){
        tests=fake(TestResultRepository.class);enrollments=fake(UserCourseEnrollmentRepository.class);progress=fake(UserLessonProgressRepository.class);
        users=fake(UserRepository.class);lessons=fake(LessonRepository.class);
        service=new LearningSummaryService(tests,enrollments,progress);user=new User();user.setId(7L);user.setUsername("student");user.setPassword("secret");user.setEmail("private@example.com");
        when(tests.findByUserIdOrderByCompletedAtDesc(7L)).thenReturn(List.of());
        when(enrollments.findByUserId(7L)).thenReturn(List.of());when(progress.findByUserId(7L)).thenReturn(List.of());
        when(users.findByUsername("student")).thenReturn(Optional.of(user));when(lessons.findByAssessmentRoleOrderByIdAsc("PLACEMENT")).thenReturn(List.of());
    }
    TestResult attempt(long lessonId,String skill,String role,String... levels){
        var lesson=new Lesson();lesson.setId(lessonId);lesson.setSkillType(skill);lesson.setAssessmentRole(role);
        var result=new TestResult();result.setId(lessonId+100);result.setUser(user);result.setLesson(lesson);result.setCompletedAt(LocalDateTime.now().minusHours(1));result.setTotalQuestions(levels.length);
        List<String> rows=new ArrayList<>();
        for(int i=0;i<levels.length;i++) rows.add("{\"questionId\":"+i+",\"learningLevel\":\""+levels[i]+"\",\"competencyTag\":\"DETAIL\",\"isCorrect\":true}");
        result.setDetailedResultJson("["+String.join(",",rows)+"]");return result;
    }
    TestResult placement(){return attempt(1,"READING","PLACEMENT","BEGINNER","BEGINNER","BEGINNER","INTERMEDIATE","INTERMEDIATE","INTERMEDIATE","ADVANCED","ADVANCED","ADVANCED");}
    @Test void newLearnerHasUnknownLevels(){
        var summary=service.summarize(user);assertNull(summary.skills().get(0).suggestedLevel());assertNull(summary.skills().get(1).suggestedLevel());
        assertTrue(summary.courses().isEmpty());
    }
    @Test void placementUsesQuestionLevelsEvenIfLessonIsBeginner(){
        var test=placement();test.getLesson().setLearningLevel("BEGINNER");
        when(tests.findByUserIdOrderByCompletedAtDesc(7L)).thenReturn(List.of(test));
        var summary=service.summarize(user);assertEquals("ADVANCED",summary.skills().get(0).suggestedLevel());
        assertNull(summary.skills().get(1).suggestedLevel());assertEquals(9,summary.skills().get(0).questions());
    }
    @Test void repeatsAreNotCountedAndPracticeDoesNotSetLevel(){
        var older=attempt(1,"READING","PRACTICE","BEGINNER","BEGINNER","BEGINNER","BEGINNER","BEGINNER");
        var recent=attempt(1,"READING","PRACTICE","BEGINNER","BEGINNER","BEGINNER","BEGINNER","BEGINNER");recent.setCompletedAt(LocalDateTime.now());
        recent.setDetailedResultJson(recent.getDetailedResultJson().replace("true","false"));
        when(tests.findByUserIdOrderByCompletedAtDesc(7L)).thenReturn(List.of(older,recent));
        var summary=service.summarize(user);assertEquals(5,summary.skills().get(0).questions());assertEquals(0,summary.skills().get(0).correct());
        assertNull(summary.skills().get(0).suggestedLevel());assertEquals("REVIEW",summary.skills().get(0).competencies().get(0).status());
    }
    @Test void staleMalformedMissingLabelsAndOtherUsersDoNotSupplyEvidence(){
        var stale=placement();stale.setCompletedAt(LocalDateTime.now().minusDays(100));
        var malformed=attempt(2,"READING","PRACTICE","BEGINNER");malformed.setDetailedResultJson("broken");
        var unknown=attempt(3,"READING","PRACTICE","BEGINNER");unknown.setDetailedResultJson("[{\"isCorrect\":true}]");
        var foreign=attempt(4,"READING","PRACTICE","BEGINNER");var other=new User();other.setId(8L);foreign.setUser(other);
        when(tests.findByUserIdOrderByCompletedAtDesc(7L)).thenReturn(List.of(stale,malformed,unknown,foreign));
        var summary=service.summarize(user);assertEquals(0,summary.skills().get(0).questions());assertEquals(4,summary.excludedAttempts());assertEquals(1,summary.legacyQuestions());
    }
    @Test void contradictoryPlacementDoesNotSuggestLevel(){
        var test=placement();test.setDetailedResultJson(test.getDetailedResultJson().replace("\"BEGINNER\",\"competencyTag\":\"DETAIL\",\"isCorrect\":true","\"BEGINNER\",\"competencyTag\":\"DETAIL\",\"isCorrect\":false"));
        when(tests.findByUserIdOrderByCompletedAtDesc(7L)).thenReturn(List.of(test));
        var skill=service.summarize(user).skills().get(0);assertNull(skill.suggestedLevel());assertTrue(skill.basis().contains("chưa nhất quán"));
    }
    @Test void finalAndExpiryAreHandledSeparately(){
        var test=attempt(2,"LISTENING","FINAL","BEGINNER","BEGINNER","BEGINNER","BEGINNER","BEGINNER","BEGINNER");
        when(tests.findByUserIdOrderByCompletedAtDesc(7L)).thenReturn(List.of(test));
        var course=new Course();course.setId(5L);course.setTitle("Foundation");var enrollment=new UserCourseEnrollment();enrollment.setUser(user);enrollment.setCourse(course);enrollment.setExpiresAt(LocalDateTime.now().minusDays(1));
        when(enrollments.findByUserId(7L)).thenReturn(List.of(enrollment));
        var summary=service.summarize(user);assertEquals("INTERMEDIATE",summary.skills().get(1).suggestedLevel());assertEquals("EXPIRED",summary.courses().get(0).status());
    }
    @Test void pageRendersAndJsonOnlyUsesLoggedInUser()throws Exception{
        var mvc=MockMvcBuilders.standaloneSetup(new LearningSummaryController(users,service,lessons))
                .setViewResolvers(new AdvisorySubmissionTests.TestConfig().viewResolver()).build();
        var auth=new UsernamePasswordAuthenticationToken("student","",List.of());
        mvc.perform(get("/profile/learning-summary").principal(auth)).andExpect(status().isOk()).andExpect(content().string(containsString("Chưa đủ dữ liệu")));
        mvc.perform(get("/profile/learning-summary/data").param("userId","8").principal(auth)).andExpect(status().isOk())
                .andExpect(jsonPath("$.skills[0].levelLabel").value("Chưa đủ dữ liệu"))
                .andExpect(jsonPath("$.password").doesNotExist()).andExpect(jsonPath("$.email").doesNotExist());
        mvc.perform(get("/profile/learning-summary/data")).andExpect(status().isUnauthorized());
        verify(tests,never()).findByUserIdOrderByCompletedAtDesc(8L);
    }
}
