package com.project.englishlearning;
import com.project.englishlearning.service.*;
import com.project.englishlearning.controller.LearningStatisticsController;
import com.project.englishlearning.repository.*;
import com.project.englishlearning.entity.*;
import com.project.englishlearning.dto.LearningSummary;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class LearningStatisticsTests {
 <T>T fake(Class<T> c){return mock(c,withSettings().mockMaker(org.mockito.MockMakers.SUBCLASS));}
 TestResult result(User u,long id,int correct,int total,LocalDateTime date){var lesson=new Lesson();lesson.setId(5L);lesson.setTitle("Practice");lesson.setSkillType("READING");var r=new TestResult();r.setId(id);r.setUser(u);r.setLesson(lesson);r.setCorrectAnswers(correct);r.setTotalQuestions(total);r.setCompletedAt(date);return r;}
 @Test void isolatesDataCountsActivityAndPreservesSameLessonHistory(){
  var results=fake(TestResultRepository.class);var writing=fake(WritingSubmissionRepository.class);var summaries=fake(LearningSummaryService.class);var user=new User();user.setId(1L);var foreign=new User();foreign.setId(2L);var now=LocalDateTime.now();
  when(results.findByUserIdOrderByCompletedAtDesc(1L)).thenReturn(List.of(result(user,2,4,5,now.minusMinutes(1)),result(user,1,2,5,now.minusDays(1)),result(foreign,3,5,5,now.minusMinutes(1)),result(user,4,8,5,now.minusMinutes(1)),result(user,5,3,5,now.plusDays(1))));
  var submission=new WritingSubmission();submission.setUser(user);submission.setSubmittedAt(now.minusMinutes(2));when(writing.findByUserId(1L)).thenReturn(List.of(submission));
  var summary=new LearningSummary(null,List.of(),List.of(),3,0,0,now);when(summaries.summarize(user)).thenReturn(summary);
  var data=new LearningStatisticsService(results,writing,summaries).build(user);assertEquals(2,data.attempts().size());assertEquals(40,data.attempts().get(0).accuracy());assertEquals(80,data.attempts().get(1).accuracy());assertEquals(28,data.activity().size());assertEquals(3,data.totalAttempts());assertEquals(2,data.activeDays());assertEquals(2,data.activity().get(27).attempts());
 }
 @Test void emptyPageRendersAndEndpointUsesOnlyLoggedInUser() throws Exception {
  var users=fake(UserRepository.class);var stats=fake(LearningStatisticsService.class);var user=new User();user.setId(1L);when(users.findByUsername("student")).thenReturn(Optional.of(user));
  when(stats.build(user)).thenReturn(new LearningStatisticsService.Statistics(new LearningSummary(null,List.of(),List.of(),0,0,0,null),List.of(),List.of(),0,0,0));
  var mvc=MockMvcBuilders.standaloneSetup(new LearningStatisticsController(users,stats)).setViewResolvers(new AdvisorySubmissionTests.TestConfig().viewResolver()).build();
  mvc.perform(get("/profile/statistics")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("attempt-trend")));
  mvc.perform(get("/profile/statistics/data")).andExpect(status().isUnauthorized());
  mvc.perform(get("/profile/statistics/data").param("userId","2").principal(new UsernamePasswordAuthenticationToken("student","n",List.of()))).andExpect(status().isOk()).andExpect(jsonPath("$.totalAttempts").value(0));verify(stats).build(user);
 }
}
