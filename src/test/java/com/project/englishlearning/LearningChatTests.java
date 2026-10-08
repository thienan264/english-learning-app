package com.project.englishlearning;
import com.project.englishlearning.controller.LearningChatController;
import com.project.englishlearning.dto.*;
import com.project.englishlearning.dto.LearningChat.*;
import com.project.englishlearning.entity.*;
import com.project.englishlearning.repository.*;
import com.project.englishlearning.service.*;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class LearningChatTests {
 <T>T fake(Class<T> c){return mock(c,withSettings().mockMaker(org.mockito.MockMakers.SUBCLASS));}
 LearningSummary summary(){return new LearningSummary(null,List.of(new LearningSummary.Skill("READING","Reading",null,"Chưa rõ","Chưa đủ dữ liệu",null,null,0,0,0,List.of())),List.of(),0,0,0,null);}
 Context context(){return new Context(summary(),List.of(new CatalogLesson("lesson-95","Đầu vào","Đánh giá","Beginner","READING","BEGINNER","Đánh giá","PLACEMENT",true,List.of("DETAIL"))),List.of(new Action("lesson-95","Đầu vào","/lessons/95","Miễn phí"),new Action("profile","Hồ sơ","/profile/learning-summary","Kết quả của bạn")));}
 @Test void realSummaryTimestampSerializesBeforeProviderCall() throws Exception {
  var original=context();var old=original.learner();
  var dated=new LearningSummary(old.goal(),old.skills(),old.courses(),0,0,0,java.time.LocalDateTime.of(2026,10,8,21,30));
  var payload=new Context(dated,original.lessons(),original.actions());
  assertTrue(LearningChatService.serializeContext(payload).contains("2026-10-08T21:30"));
 }
 @Test void pricesRespectFreeCoursesAndSaleWindow(){
  var c=new Course();c.setIsFree(false);c.setPrice(new java.math.BigDecimal("500000"));c.setSalePrice(new java.math.BigDecimal("350000"));
  var now=java.time.LocalDateTime.of(2026,10,8,12,0);c.setSaleStartDate(now.minusDays(1));c.setSaleEndDate(now.plusDays(1));
  assertEquals(new java.math.BigDecimal("350000"),LearningChatContextService.currentPrice(c,now));
  assertEquals(new java.math.BigDecimal("500000"),LearningChatContextService.currentPrice(c,now.plusDays(2)));
  assertEquals(new java.math.BigDecimal("500000"),LearningChatContextService.currentPrice(c,now.minusDays(2)));
  c.setIsFree(true);assertEquals(java.math.BigDecimal.ZERO,LearningChatContextService.currentPrice(c,now));
  c.setIsFree(false);c.setPrice(null);c.setSalePrice(null);assertNull(LearningChatContextService.currentPrice(c,now));
 }
 @Test void discountContextIncludesOriginalPriceAndOnlyActiveDeadline() throws Exception {
  var now=java.time.LocalDateTime.of(2026,10,8,21,0);var c=new Course();c.setId(8L);c.setTitle("IELTS — Luyện đề và sửa điểm yếu");c.setIsFree(false);c.setPrice(new java.math.BigDecimal("500000"));c.setSalePrice(new java.math.BigDecimal("350000"));c.setSaleStartDate(now.minusDays(1));c.setSaleEndDate(now.plusDays(1));
  var info=LearningChatContextService.courseInfo(c,true,now);
  assertTrue(info.discountActive());assertEquals(new java.math.BigDecimal("30.0"),info.discountPercent());assertEquals(c.getSaleEndDate(),info.discountEndsAt());assertEquals(c.getPrice(),info.originalPrice());
  var base=context();String json=LearningChatService.serializeContext(new Context(base.learner(),base.lessons(),base.actions(),List.of(info)));
  assertTrue(json.contains("\"discountActive\":true"));assertTrue(json.contains("500000"));assertTrue(json.contains("350000"));assertTrue(json.contains("2026-10-09T21:00"));
  var expired=LearningChatContextService.courseInfo(c,true,now.plusDays(2));assertFalse(expired.discountActive());assertNull(expired.discountEndsAt());
  assertFalse(LearningChatContextService.courseInfo(c,true,now.minusDays(2)).discountActive());
  c.setIsFree(true);assertFalse(LearningChatContextService.courseInfo(c,true,now).discountActive());
  c.setIsFree(false);c.setSaleEndDate(null);assertTrue(LearningChatContextService.courseInfo(c,true,now).discountActive());assertNull(LearningChatContextService.courseInfo(c,true,now).discountEndsAt());
 }
 Course offerCourse(long id){var c=new Course();c.setId(id);c.setTitle("Khóa "+id);c.setIsFree(false);c.setPrice(new java.math.BigDecimal("500000"));c.setSalePrice(new java.math.BigDecimal("350000"));return c;}
 @Test void promotionsExcludeEnrolledCoursesAndKeepServerPriceCards(){
  var now=java.time.LocalDateTime.now();var owned=LearningChatContextService.courseInfo(offerCourse(1L),true,now,true);var unowned=LearningChatContextService.courseInfo(offerCourse(2L),false,now,false);
  var base=context();var reply=LearningChatService.promotionReply(new Context(base.learner(),base.lessons(),base.actions(),List.of(owned,unowned)));
  assertEquals(1,reply.actions().size());assertEquals("course-2",reply.actions().get(0).id());assertEquals(new java.math.BigDecimal("350000"),reply.actions().get(0).course().currentPrice());
  var onlyOwned=LearningChatService.promotionReply(new Context(base.learner(),base.lessons(),base.actions(),List.of(owned)));
  assertTrue(onlyOwned.actions().isEmpty());assertTrue(onlyOwned.message().contains("không cần mua lại"));
 }
 @Test void providerCannotIntroduceUnverifiedLinks() throws Exception {
  var reply=LearningChatService.parse("{\"message\":\"Hãy làm bài đầu vào\",\"actionIds\":[\"https://evil.test\",\"lesson-95\",\"lesson-95\",\"course-999\"]}",context());
  assertEquals(1,reply.actions().size());assertEquals("/lessons/95",reply.actions().get(0).url());
  assertThrows(Exception.class,()->LearningChatService.parse("{\"message\":\"https://evil.test\"}",context()));
 }
 @Test void unavailableProviderGivesHonestPlacementFallback(){
  var reply=LearningChatService.fallback(context());assertTrue(reply.fallback());assertTrue(reply.message().contains("chưa kết nối"));assertTrue(reply.actions().stream().anyMatch(a->a.id().equals("lesson-95")));
 }
 @Test void catalogExcludesEmptyLessonsAndNeverIncludesAnswerKeys() throws Exception {
  var summaries=fake(LearningSummaryService.class);var lessons=fake(LessonRepository.class);var groups=fake(QuestionGroupRepository.class);var access=fake(CourseAccessService.class);var writing=fake(WritingExamRepository.class);
  var u=new User();u.setId(1L);when(summaries.summarize(u)).thenReturn(summary());var course=new Course();course.setId(2L);course.setTitle("Foundation");course.setLevel("Beginner");
  var l=new Lesson();l.setId(95L);l.setTitle("Profile");l.setCourse(course);l.setSkillType("READING");l.setAssessmentRole("PLACEMENT");l.setLearningObjective("Find details");
  var empty=new Lesson();empty.setId(96L);empty.setCourse(course);empty.setSkillType("READING");empty.setAssessmentRole("PLACEMENT");empty.setLearningObjective("Empty");
  var q=new Question();q.setQuestionText("secret question");q.setCorrectAnswer("secret answer");q.setExplanation("secret explanation");q.setCompetencyTag("DETAIL");l.getQuestions().add(q);
  when(lessons.findAll()).thenReturn(List.of(l,empty));when(access.canLearn(u,course)).thenReturn(true);
  var result=new LearningChatContextService(summaries,lessons,groups,access,writing).build(u);assertEquals(1,result.lessons().size());
  String json=new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(result);assertFalse(json.contains("secret"));assertTrue(json.contains("DETAIL"));
 }
 @Test void historyBelongsToAuthenticatedUserAndInputIsBounded() throws Exception {
  var users=fake(UserRepository.class);var chat=fake(LearningChatService.class);var a=new User();a.setId(1L);var b=new User();b.setId(2L);
  when(users.findByUsername("a")).thenReturn(Optional.of(a));when(users.findByUsername("b")).thenReturn(Optional.of(b));when(chat.answer(eq(a),eq("hello"),anyList())).thenReturn(new Reply("answer",List.of(new Action("course-8","Khóa ưu đãi","/courses/8","Xem khóa học",LearningChatContextService.courseInfo(offerCourse(8L),true,java.time.LocalDateTime.now()))),false));
  var mvc=MockMvcBuilders.standaloneSetup(new LearningChatController(users,chat)).build();var session=new MockHttpSession();var authA=new UsernamePasswordAuthenticationToken("a","n",List.of());var authB=new UsernamePasswordAuthenticationToken("b","n",List.of());
  mvc.perform(post("/api/learning-chat").session(session).principal(authA).contentType("application/json").content("{\"message\":\"hello\"}")).andExpect(status().isOk());
  mvc.perform(get("/api/learning-chat").session(session).principal(authA)).andExpect(status().isOk()).andExpect(jsonPath("$[1].actions[0].url").value("/courses/8")).andExpect(jsonPath("$[1].actions[0].course.currentPrice").value(350000));
  mvc.perform(get("/api/learning-chat").session(session).principal(authB).param("userId","1")).andExpect(content().json("[]"));
  mvc.perform(get("/api/learning-chat")).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/learning-chat").session(session).principal(authA).contentType("application/json").content("{\"message\":\""+"x".repeat(1501)+"\"}")).andExpect(status().isBadRequest());
  mvc.perform(delete("/api/learning-chat").session(session).principal(authA)).andExpect(status().isOk());
  mvc.perform(get("/api/learning-chat").session(session).principal(authA)).andExpect(content().json("[]"));
 }
 @Test void chatPageRenders() throws Exception {
  var users=fake(UserRepository.class);when(users.findByUsername("student")).thenReturn(Optional.of(new User()));
  var mvc=MockMvcBuilders.standaloneSetup(new LearningChatController(users,fake(LearningChatService.class))).setViewResolvers(new AdvisorySubmissionTests.TestConfig().viewResolver()).build();
  mvc.perform(get("/chat").principal(new UsernamePasswordAuthenticationToken("student","n",List.of()))).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("chat-input")));
 }
}
