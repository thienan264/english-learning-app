package com.project.englishlearning;

import com.project.englishlearning.controller.*;
import com.project.englishlearning.entity.*;
import com.project.englishlearning.repository.*;
import com.project.englishlearning.service.*;
import org.junit.jupiter.api.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

class ExpertWritingTests {
    WritingSubmissionRepository submissions; UserRepository users; WritingExamRepository exams;
    NotificationRepository notifications; UserCourseEnrollmentRepository enrollments; LessonRepository lessons;
    ExpertWritingService service; User user; Course course; Lesson lesson; MockMvc student, admin;
    <T> T fake(Class<T> type) { return mock(type,withSettings().mockMaker(org.mockito.MockMakers.SUBCLASS)); }
    @BeforeEach void setup() {
        submissions=fake(WritingSubmissionRepository.class);users=fake(UserRepository.class);exams=fake(WritingExamRepository.class);
        notifications=fake(NotificationRepository.class);enrollments=fake(UserCourseEnrollmentRepository.class);lessons=fake(LessonRepository.class);
        service=new ExpertWritingService(submissions,users,exams,notifications,new CourseAccessService(enrollments));
        user=new User();user.setId(7L);user.setUsername("student");user.setFullName("Học viên");
        course=new Course();course.setId(1L);course.setTitle("Paid Writing");course.setIsFree(false);
        var module=new com.project.englishlearning.entity.Module();module.setCourse(course);
        lesson=new Lesson();lesson.setId(3L);lesson.setTitle("Writing Test");lesson.setModule(module);lesson.setLessonType("MOCK_TEST");lesson.setSkillType("WRITING");lesson.setContent("Original prompt");
        when(users.findByUsername("student")).thenReturn(Optional.of(user));when(users.lockForWriting(7L)).thenReturn(Optional.of(user));
        when(lessons.findById(3L)).thenReturn(Optional.of(lesson));
        when(enrollments.findByUserIdAndCourseId(7L,1L)).thenReturn(Optional.of(new UserCourseEnrollment()));
        when(submissions.saveAndFlush(any())).thenAnswer(i -> { WritingSubmission s=i.getArgument(0);s.setId(11L);return s; });
        student=MockMvcBuilders.standaloneSetup(new StudentWritingController(lessons,users,submissions,exams,fake(GeminiAiService.class),service))
            .setViewResolvers(new AdvisorySubmissionTests.TestConfig().viewResolver()).build();
        admin=MockMvcBuilders.standaloneSetup(new AdminExpertWritingController(submissions,service))
            .setViewResolvers(new AdvisorySubmissionTests.TestConfig().viewResolver()).build();
    }
    com.project.englishlearning.dto.ExpertWritingReviewForm form(Double band,String feedback,String corrections,String suggested) {
        var form=new com.project.englishlearning.dto.ExpertWritingReviewForm();form.setTask2Band(band);form.setTask2Feedback(feedback);form.setTask2Corrections(corrections);form.setTask2Suggested(suggested);return form;
    }
    WritingSubmission submitted(String status,int attempt) {
        var s=new WritingSubmission();s.setId(11L);s.setUser(user);s.setLesson(lesson);s.setGradingMode("EXPERT");s.setStatus(status);s.setExpertAttempt(attempt);
        s.setSubmissionText("Original essay");s.setPromptSnapshot("Original prompt");return s;
    }
    void history(WritingSubmission... rows) {
        when(submissions.findByUserIdAndLessonIdAndGradingModeOrderBySubmittedAtAsc(7L,3L,"EXPERT")).thenReturn(List.of(rows));
    }
    @Test void firstSubmissionSavesSnapshotAndLocksSecondUntilResponse() {
        var s=service.submit(user,lesson,"Essay one","Essay two",null);
        assertEquals("EXPERT",s.getGradingMode());assertEquals(1,s.getExpertAttempt());assertEquals("WAITING_REVIEW",s.getStatus());
        assertEquals("Original prompt",s.getPromptSnapshot());assertTrue(s.getSubmissionText().contains("Essay two"));
        assertNull(s.getBandScore());verify(users).lockForWriting(7L);
        history(s);assertFalse(service.availability(user,lesson).isCanSubmit());
        assertEquals(409,assertThrows(ResponseStatusException.class,() -> service.submit(user,lesson,"Again",null,null)).getStatusCode().value());
        verify(submissions,times(1)).saveAndFlush(any());verifyNoInteractions(notifications);
    }
    @Test void reviewedFirstAllowsSecondButThirdIsRejected() {
        history(submitted("REVIEWED",1));
        assertTrue(service.availability(user,lesson).isCanSubmit());
        var second=service.submit(user,lesson,null,null,"Revised essay");assertEquals(2,second.getExpertAttempt());
        second.setStatus("REVIEWED");history(submitted("REVIEWED",1),second);
        assertEquals(409,assertThrows(ResponseStatusException.class,() -> service.submit(user,lesson,null,null,"Third essay")).getStatusCode().value());
    }
    @Test void freeUnpurchasedExpiredAndRevokedCoursesCannotSubmit() {
        course.setIsFree(true);assertEquals(403,assertThrows(ResponseStatusException.class,() -> service.submit(user,lesson,"Essay",null,null)).getStatusCode().value());
        course.setIsFree(false);when(enrollments.findByUserIdAndCourseId(7L,1L)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class,() -> service.submit(user,lesson,"Essay",null,null));
        var e=new UserCourseEnrollment();e.setExpiresAt(java.time.LocalDateTime.now().minusDays(1));when(enrollments.findByUserIdAndCourseId(7L,1L)).thenReturn(Optional.of(e));
        assertThrows(ResponseStatusException.class,() -> service.submit(user,lesson,"Essay",null,null));
        e.setExpiresAt(null);e.setStatus("REVOKED");assertThrows(ResponseStatusException.class,() -> service.submit(user,lesson,"Essay",null,null));
        verify(submissions,never()).saveAndFlush(any());
    }
    @Test void emptyOversizedAndWrongLessonSubmissionsAreRejectedWithoutConsumingQuota() {
        assertThrows(ResponseStatusException.class,() -> service.submit(user,lesson," ",null,null));
        assertThrows(ResponseStatusException.class,() -> service.submit(user,lesson,"x".repeat(20001),null,null));
        lesson.setSkillType("READING");assertThrows(ResponseStatusException.class,() -> service.submit(user,lesson,"Essay",null,null));
        verify(submissions,never()).saveAndFlush(any());
    }
    @Test void feedbackSavedWithOneNotificationAndDuplicateResponseIsRejected() {
        var s=submitted("WAITING_REVIEW",1);when(submissions.lockForReview(11L)).thenReturn(Optional.of(s));
        service.respondTasks(11L,"admin",form(6.5,"Good structure","Fix tense","Improved essay"));
        assertEquals("REVIEWED",s.getStatus());assertEquals(6.5,s.getBandScore());assertEquals("admin",s.getReviewedBy());assertNotNull(s.getEvaluatedAt());
        var captor=org.mockito.ArgumentCaptor.forClass(Notification.class);verify(notifications).save(captor.capture());
        assertEquals(user,captor.getValue().getUser());assertEquals("/lessons/3/writing/result/11",captor.getValue().getUrl());
        assertThrows(ResponseStatusException.class,() -> service.respondTasks(11L,"admin",form(7.0,"new","new","new")));
        verify(notifications,times(1)).save(any());
    }
    @Test void invalidScoresAndIncompleteFeedbackCannotMarkReviewedOrNotify() {
        var s=submitted("WAITING_REVIEW",1);when(submissions.lockForReview(11L)).thenReturn(Optional.of(s));
        for(Double band:Arrays.asList(null,-1.0,9.5,6.3,Double.NaN,Double.POSITIVE_INFINITY))
            assertThrows(ResponseStatusException.class,() -> service.respondTasks(11L,"admin",form(band,"Feedback","Corrections","Suggested")));
        assertThrows(ResponseStatusException.class,() -> service.respondTasks(11L,"admin",form(6.5," ","Corrections","Suggested")));
        assertEquals("WAITING_REVIEW",s.getStatus());verifyNoInteractions(notifications);verify(submissions,never()).save(any());
    }
    @Test void writingPageShowsQuotaAndDisablesPendingRequest() throws Exception {
        student.perform(get("/lessons/3/writing").principal(new UsernamePasswordAuthenticationToken("student","")))
            .andExpect(status().isOk()).andExpect(content().string(containsString("Lần 1/2")));
        history(submitted("WAITING_REVIEW",1));
        student.perform(get("/lessons/3/writing").principal(new UsernamePasswordAuthenticationToken("student","")))
            .andExpect(content().string(containsString("Đang chờ phản hồi lần 1"))).andExpect(content().string(containsString("disabled=\"disabled\"")));
    }
    @Test void privateResultsCannotBeViewedOrPolledByAnotherStudentOrWrongLesson() throws Exception {
        var s=submitted("WAITING_REVIEW",1);when(submissions.findById(11L)).thenReturn(Optional.of(s));
        student.perform(get("/lessons/3/writing/result/11").principal(new UsernamePasswordAuthenticationToken("other",""))).andExpect(status().isForbidden());
        student.perform(get("/lessons/4/writing/submissions/11/status").principal(new UsernamePasswordAuthenticationToken("student",""))).andExpect(status().isForbidden());
        student.perform(get("/lessons/3/writing/result/11").principal(new UsernamePasswordAuthenticationToken("student","")))
            .andExpect(status().isOk()).andExpect(content().string(containsString("Original essay")));
    }
    @Test void adminListAndFeedbackPagesRenderAndExcludeAiSubmissions() throws Exception {
        var s=submitted("WAITING_REVIEW",1);when(submissions.findById(11L)).thenReturn(Optional.of(s));when(submissions.lockForReview(11L)).thenReturn(Optional.of(s));
        when(submissions.findByGradingModeOrderBySubmittedAtDesc("EXPERT")).thenReturn(List.of(s));
        admin.perform(get("/admin/writing/reviews")).andExpect(status().isOk()).andExpect(content().string(containsString("Writing Test")));
        admin.perform(get("/admin/writing/reviews/11")).andExpect(status().isOk()).andExpect(content().string(containsString("Original essay")));
        admin.perform(post("/admin/writing/reviews/11/respond").principal(new UsernamePasswordAuthenticationToken("admin",""))
            .param("task2Band","6.5").param("task2Feedback","Good structure").param("task2Corrections","Fix tense").param("task2Suggested","Improved essay"))
            .andExpect(redirectedUrl("/admin/writing/reviews/11"));
        admin.perform(get("/admin/writing/reviews/11")).andExpect(status().isOk()).andExpect(content().string(containsString("Improved essay")));
        student.perform(get("/lessons/3/writing/result/11").principal(new UsernamePasswordAuthenticationToken("student","")))
            .andExpect(status().isOk()).andExpect(content().string(containsString("6.5"))).andExpect(content().string(containsString("Viết lại và gửi lần 2")));
        s.setGradingMode(null);admin.perform(get("/admin/writing/reviews/11")).andExpect(status().isNotFound());
    }
    @Test void expertEndpointRejectsRepeatRequestsAndFreeCourseButtonIsAbsent() throws Exception {
        var principal=new UsernamePasswordAuthenticationToken("student","");
        student.perform(post("/lessons/3/writing/expert-submit").principal(principal).param("task1Essay","My essay"))
            .andExpect(status().isAccepted()).andExpect(jsonPath("$.submissionId").value(11));
        history(submitted("WAITING_REVIEW",1));
        student.perform(post("/lessons/3/writing/expert-submit").principal(principal).param("task1Essay","Again"))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("Đang chờ phản hồi lần 1"));
        course.setIsFree(true);
        String html=student.perform(get("/lessons/3/writing").principal(principal)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertFalse(html.contains("<button id=\"expertSubmitBtn\""));assertTrue(html.contains("Nộp Bài &amp; Nhận Điểm AI") || html.contains("Nộp Bài & Nhận Điểm AI"));
    }
    @Test void examTasksAreStoredSeparatelyAndPromptIsSnapshot() {
        var exam=new WritingExam();var task1=new WritingTask();var task2=new WritingTask();
        task1.setInstruction("Chart prompt");task2.setInstruction("Discussion prompt");exam.setTask1(task1);exam.setTask2(task2);
        when(exams.findByLessonId(3L)).thenReturn(Optional.of(exam));
        var s=service.submit(user,lesson,"Task one","Task two",null);
        task2.setInstruction("Changed prompt");
        assertEquals("Task one",s.getTask1Essay());assertEquals("Task two",s.getTask2Essay());
        assertTrue(s.getPromptSnapshot().contains("Discussion prompt"));assertFalse(s.getPromptSnapshot().contains("Changed prompt"));
    }
    @Test void savedResultsRemainReadableAfterCourseAccessExpires() throws Exception {
        var s=submitted("REVIEWED",1);s.setBandScore(6.5);s.setEvaluatedAt(java.time.LocalDateTime.now());
        when(submissions.findById(11L)).thenReturn(Optional.of(s));
        var enrollment=new UserCourseEnrollment();enrollment.setExpiresAt(java.time.LocalDateTime.now().minusDays(1));
        when(enrollments.findByUserIdAndCourseId(7L,1L)).thenReturn(Optional.of(enrollment));
        var guard=new com.project.englishlearning.config.CourseAccessInterceptor(fake(CourseRepository.class),lessons,fake(FlashcardRepository.class),users,new CourseAccessService(enrollments));
        var request=new org.springframework.mock.web.MockHttpServletRequest("GET","/lessons/3/writing/result/11");
        request.setUserPrincipal(new UsernamePasswordAuthenticationToken("student",""));
        request.setAttribute(org.springframework.web.servlet.HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE,Map.of("lessonId","3"));
        assertTrue(guard.preHandle(request,new org.springframework.mock.web.MockHttpServletResponse(),new Object()));
        student.perform(get("/lessons/3/writing/result/11").principal(new UsernamePasswordAuthenticationToken("student","")))
            .andExpect(status().isOk()).andExpect(content().string(containsString("Original essay")));
        request.setMethod("POST");request.setRequestURI("/lessons/3/writing/expert-submit");
        assertFalse(guard.preHandle(request,new org.springframework.mock.web.MockHttpServletResponse(),new Object()));
    }

    @Test void eachTaskStoresItsOwnBandFeedbackAndSuggestionsWithCombinedBand() {
        var s=submitted("WAITING_REVIEW",1);s.setTask1Essay("Chart essay");s.setTask2Essay("He go home. He go home.");
        when(submissions.lockForReview(11L)).thenReturn(Optional.of(s));
        var form=form(7.0,"Task two feedback","Task two extra","Task two improved");
        form.setTask1Band(6.0);form.setTask1Feedback("Task one feedback");form.setTask1Suggested("Task one improved");
        form.setAnnotationsJson("[{\"task\":2,\"start\":12,\"end\":23,\"original\":\"He go home.\",\"correction\":\"He goes home.\",\"explanation\":\"Subject–verb agreement\"}]");
        service.respondTasks(11L,"admin",form);
        assertEquals(6.0,s.getTask1Overall());assertEquals(7.0,s.getTask2Overall());assertEquals(6.5,s.getBandScore());
        var review=service.review(s);assertEquals("Task one feedback",review.task1().feedback());assertEquals("Task two feedback",review.task2().feedback());
        assertEquals("Task one improved",review.task1().suggested());assertEquals("Task two improved",review.task2().suggested());
        assertEquals(12,review.annotations().getFirst().start());assertEquals("He goes home.",review.annotations().getFirst().correction());
        verify(notifications).save(any());
    }
    @Test void everySubmittedTaskRequiresItsOwnGradeBeforeAnyFeedbackIsSent() {
        var s=submitted("WAITING_REVIEW",1);s.setTask1Essay("Chart essay");s.setTask2Essay("Discussion essay");
        when(submissions.lockForReview(11L)).thenReturn(Optional.of(s));
        assertThrows(ResponseStatusException.class,() -> service.respondTasks(11L,"admin",form(7.0,"Task two feedback","","Suggested")));
        assertEquals("WAITING_REVIEW",s.getStatus());assertNull(s.getExpertReviewJson());verifyNoInteractions(notifications);
    }
    @Test void forgedOutOfRangeAndOverlappingAnnotationsAreRejected() {
        var s=submitted("WAITING_REVIEW",1);when(submissions.lockForReview(11L)).thenReturn(Optional.of(s));
        for(String annotations:List.of("not-json","null",
            "[{\"task\":2,\"start\":0,\"end\":500,\"original\":\"Original essay\",\"correction\":\"Fixed\"}]",
            "[{\"task\":2,\"start\":0,\"end\":8,\"original\":\"Forged!!\",\"correction\":\"Fixed\"}]",
            "[{\"task\":1,\"start\":0,\"end\":8,\"original\":\"Original\",\"correction\":\"Fixed\"}]",
            "[{\"task\":2,\"start\":0,\"end\":8,\"original\":\"Original\",\"correction\":\"Fixed\"},{\"task\":2,\"start\":0,\"end\":8,\"original\":\"Original\",\"correction\":\"Again\"}]")) {
            var form=form(6.5,"Feedback","","Suggested");form.setAnnotationsJson(annotations);
            assertThrows(ResponseStatusException.class,() -> service.respondTasks(11L,"admin",form));
        }
        assertEquals("WAITING_REVIEW",s.getStatus());verify(submissions,never()).save(any());verifyNoInteractions(notifications);
    }
    @Test void separateTaskFormsAndSavedAnnotationDataRenderSafely() throws Exception {
        var s=submitted("WAITING_REVIEW",1);s.setTask1Essay("Chart text");s.setTask2Essay("He go home.");
        when(submissions.findById(11L)).thenReturn(Optional.of(s));when(submissions.lockForReview(11L)).thenReturn(Optional.of(s));
        admin.perform(get("/admin/writing/reviews/11")).andExpect(status().isOk())
            .andExpect(content().string(containsString("name=\"task1Band\""))).andExpect(content().string(containsString("name=\"task2Band\"")))
            .andExpect(content().string(containsString("Bôi chọn đoạn cần sửa")));
        var form=form(7.0,"Task 2 feedback","","Task 2 improved");form.setTask1Band(6.0);form.setTask1Feedback("Task 1 feedback");form.setTask1Suggested("Task 1 improved");
        form.setAnnotationsJson("[{\"task\":2,\"start\":0,\"end\":11,\"original\":\"He go home.\",\"correction\":\"<script>alert(1)</script>\",\"explanation\":\"Grammar\"}]");
        service.respondTasks(11L,"admin",form);
        String html=student.perform(get("/lessons/3/writing/result/11").principal(new UsernamePasswordAuthenticationToken("student",""))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(html.contains("Task 1 feedback"));assertTrue(html.contains("Task 2 feedback"));assertTrue(html.contains("Task 1 improved"));assertTrue(html.contains("Task 2 improved"));
        assertTrue(html.contains("&lt;script&gt;"));assertFalse(html.contains("<script>alert(1)</script>"));assertTrue(html.contains("expert-writing-annotations.js"));
    }
    @Test void legacyFeedbackRemainsVisibleAndInvalidDraftIsPreserved() throws Exception {
        var s=submitted("REVIEWED",1);s.setBandScore(6.0);s.setFeedback("Earlier feedback");s.setExpertCorrections("Earlier corrections");s.setSuggestedEssay("Earlier improved essay");s.setEvaluatedAt(java.time.LocalDateTime.now());
        when(submissions.findById(11L)).thenReturn(Optional.of(s));
        student.perform(get("/lessons/3/writing/result/11").principal(new UsernamePasswordAuthenticationToken("student","")))
            .andExpect(status().isOk()).andExpect(content().string(containsString("Earlier feedback"))).andExpect(content().string(containsString("Earlier improved essay")));
        s.setStatus("WAITING_REVIEW");when(submissions.lockForReview(11L)).thenReturn(Optional.of(s));
        admin.perform(post("/admin/writing/reviews/11/respond").principal(new UsernamePasswordAuthenticationToken("admin",""))
            .param("task2Band","6.3").param("task2Feedback","Draft feedback").param("task2Suggested","Draft suggested"))
            .andExpect(redirectedUrl("/admin/writing/reviews/11")).andExpect(flash().attributeExists("reviewForm","error"));
        verifyNoInteractions(notifications);
    }

    @Test void browserOffsetsMatchStoredCrLfAndCrParagraphsWithoutChangingOriginalEssays() {
        String raw="First paragraph.\r\n\r\nHe go home.\rAnother paragraph.";
        String displayed="First paragraph.\n\nHe go home.\nAnother paragraph.";
        var s=submitted("WAITING_REVIEW",1);s.setTask1Essay(raw);s.setTask2Essay(raw);
        when(submissions.lockForReview(11L)).thenReturn(Optional.of(s));
        assertEquals(displayed,s.getExpertTask1Text());assertEquals(displayed,s.getExpertTask2Text());
        int start=displayed.indexOf("He go home.");
        var form=form(7.0,"Task 2 feedback","","Task 2 suggestion");form.setTask1Band(6.0);form.setTask1Feedback("Task 1 feedback");form.setTask1Suggested("Task 1 suggestion");
        form.setAnnotationsJson("[{\"task\":1,\"start\":"+start+",\"end\":"+(start+11)+",\"original\":\"He go home.\",\"correction\":\"He goes home.\"},{\"task\":2,\"start\":"+start+",\"end\":"+(start+11)+",\"original\":\"He go home.\",\"correction\":\"He goes home.\"}]");
        service.respondTasks(11L,"admin",form);
        assertEquals("REVIEWED",s.getStatus());assertEquals(2,service.review(s).annotations().size());
        assertEquals(raw,s.getTask1Essay());assertEquals(raw,s.getTask2Essay());
        var legacy=submitted("WAITING_REVIEW",1);legacy.setSubmissionText(raw);assertEquals(displayed,legacy.getExpertTask2Text());
    }

}
