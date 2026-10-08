package com.project.englishlearning;
import com.project.englishlearning.controller.*;
import com.project.englishlearning.entity.*;
import com.project.englishlearning.repository.*;
import com.project.englishlearning.service.*;
import com.project.englishlearning.dto.CourseDTO;
import org.junit.jupiter.api.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;

class CourseExperienceTests {
    CourseRepository courses; UserRepository users; UserCourseEnrollmentRepository enrollments;
    ModuleRepository modules; CourseReviewRepository reviews; CourseOrderRepository orders;
    CourseReviewService reviewService; CourseAccessService access; Course course; User user; MockMvc mvc;
    private <T> T fake(Class<T> type) { return mock(type, withSettings().mockMaker(org.mockito.MockMakers.SUBCLASS)); }
    @BeforeEach void setup() {
        courses=fake(CourseRepository.class); users=fake(UserRepository.class); enrollments=fake(UserCourseEnrollmentRepository.class);
        modules=fake(ModuleRepository.class); reviews=fake(CourseReviewRepository.class); orders=fake(CourseOrderRepository.class);
        reviewService=new CourseReviewService(reviews,orders); access=new CourseAccessService(enrollments);
        course=new Course();course.setId(1L);course.setTitle("Khóa trả phí");course.setIsFree(false);course.setLevel("Beginner");course.setPrice(new java.math.BigDecimal("200000"));course.setAccessDurationMonths(2);
        user=new User();user.setId(7L);user.setUsername("student");user.setFullName("Học viên");
        when(courses.findById(1L)).thenReturn(Optional.of(course));when(users.findByUsername("student")).thenReturn(Optional.of(user));
        var module=new com.project.englishlearning.entity.Module();module.setId(2L);module.setTitle("Chương giới thiệu");module.setCourse(course);
        var lesson=new Lesson();lesson.setId(3L);lesson.setTitle("Bài phát âm");lesson.setLessonType("THEORY");lesson.setModule(module);module.getLessons().add(lesson);
        when(modules.findByCourseIdOrderByOrderIndexAsc(1L)).thenReturn(List.of(module));
        mvc=MockMvcBuilders.standaloneSetup(new StudentCourseController(courses,users,enrollments,modules,reviews,reviewService,access))
            .setViewResolvers(new AdvisorySubmissionTests.TestConfig().viewResolver()).build();
    }
    @Test void guestCanSeeOutlineWithoutLessonContentOrEnrollment() throws Exception {
        mvc.perform(get("/courses/1")).andExpect(status().isOk())
            .andExpect(content().string(containsString("Bài phát âm")))
            .andExpect(content().string(containsString("Đang khóa")))
            .andExpect(content().string(containsString("/courses/1/study-flashcards")))
            .andExpect(content().string(not(containsString("/learn/course/1?lessonId=3"))))
            .andExpect(content().string(containsString("2 tháng kể từ khi mở khóa")));
        verify(enrollments,never()).save(any());
    }
    @Test void paidStudentCanLearnAndReview() throws Exception {
        var e=new UserCourseEnrollment();e.setExpiresAt(java.time.LocalDateTime.now().plusDays(60));
        when(enrollments.findByUserIdAndCourseId(7L,1L)).thenReturn(Optional.of(e));
        when(orders.existsByUserIdAndCourseIdAndStatus(7L,1L,"PAID")).thenReturn(true);
        mvc.perform(get("/courses/1").principal(new UsernamePasswordAuthenticationToken("student","pw")))
            .andExpect(status().isOk()).andExpect(content().string(containsString("/learn/course/1?lessonId=3")))
            .andExpect(content().string(containsString("name=\"rating\"")));
    }
    @Test void expiredStudentSeesLockedOutlineAndRenewal() throws Exception {
        var e=new UserCourseEnrollment();e.setExpiresAt(java.time.LocalDateTime.now().minusDays(1));
        when(enrollments.findByUserIdAndCourseId(7L,1L)).thenReturn(Optional.of(e));
        mvc.perform(get("/courses/1").principal(new UsernamePasswordAuthenticationToken("student","pw")))
            .andExpect(status().isOk()).andExpect(content().string(containsString("Gia hạn")))
            .andExpect(content().string(containsString("Đang khóa")));
    }
    @Test void onlyActualPaidBuyersCanReviewAndRatingIsValidated() {
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->reviewService.submit(user,course,5,"great"));
        verify(reviews,never()).saveAndFlush(any());
        when(orders.existsByUserIdAndCourseIdAndStatus(7L,1L,"PAID")).thenReturn(true);
        assertThrows(IllegalArgumentException.class,()->reviewService.submit(user,course,6,"great"));
        assertThrows(IllegalArgumentException.class,()->reviewService.submit(user,course,3,"a".repeat(1501)));
        reviewService.submit(user,course,0,"First review");
        var captured=org.mockito.ArgumentCaptor.forClass(CourseReview.class);verify(reviews).saveAndFlush(captured.capture());
        var existing=captured.getValue();existing.setVisible(false);
        when(reviews.findByCourseIdAndUserId(1L,7L)).thenReturn(Optional.of(existing));
        reviewService.submit(user,course,5,"Updated review");assertEquals(5,existing.getRating());assertFalse(existing.isVisible());
        verify(reviews,times(2)).saveAndFlush(existing);
    }
    @Test void statisticsUsePaidOrdersAndVisibleReviewAggregate() {
        when(orders.countPurchasesByCourse()).thenReturn(Collections.singletonList(new Object[]{1L,9L}));
        when(reviews.ratingStats()).thenReturn(Collections.singletonList(new Object[]{1L,4.5,2L}));
        var stats=reviewService.statistics().get(1L);assertEquals(9,stats.purchases());assertEquals(4.5,stats.rating());assertEquals(2,stats.reviews());
    }
    @Test void accessRequiresActiveEnrollmentForPaidCourses() {
        assertFalse(access.canLearn(user,course));
        var e=new UserCourseEnrollment();when(enrollments.findByUserIdAndCourseId(7L,1L)).thenReturn(Optional.of(e));
        assertTrue(access.canLearn(user,course));e.setStatus("REVOKED");assertFalse(access.canLearn(user,course));
        e.setStatus("IN_PROGRESS");e.setExpiresAt(java.time.LocalDateTime.now().minusSeconds(1));assertFalse(access.canLearn(user,course));
        course.setIsFree(true);assertTrue(access.canLearn(user,course));assertFalse(access.canLearn(null,course));
    }
    @Test void flashcardPreviewDoesNotLoadOrWriteStudentProgress() throws Exception {
        var flashcards=fake(FlashcardService.class);var progress=fake(UserFlashcardProgressRepository.class);
        var preview=MockMvcBuilders.standaloneSetup(new StudentFlashcardController(flashcards,courses,users,progress,access))
            .setViewResolvers(new AdvisorySubmissionTests.TestConfig().viewResolver()).build();
        preview.perform(get("/courses/1/study-flashcards")).andExpect(status().isOk())
            .andExpect(content().string(containsString("data-preview=\"true\"")))
            .andExpect(content().string(containsString("Bạn đang tham khảo từ vựng")));
        verifyNoInteractions(progress);
    }
    @Test void homeCardIsCompactAndLinksToPreview() throws Exception {
        var service=fake(CourseService.class);var dto=new CourseDTO();dto.setId(1L);dto.setTitle("Khóa trả phí");dto.setLevel("Beginner");dto.setIsFree(false);dto.setPrice(new java.math.BigDecimal("200000"));dto.setDescription("LONG_CARD_DESCRIPTION");dto.setPurchaseCount(9);dto.setAverageRating(4.5);dto.setReviewCount(2);dto.setAccessDurationMonths(2);
        when(service.getAllCoursesDTO()).thenReturn(List.of(dto));
        var home=MockMvcBuilders.standaloneSetup(new HomeController(service,users)).setViewResolvers(new AdvisorySubmissionTests.TestConfig().viewResolver()).build();
        home.perform(get("/")).andExpect(status().isOk()).andExpect(content().string(containsString("Tham khảo khóa học")))
            .andExpect(content().string(not(containsString("LONG_CARD_DESCRIPTION"))))
            .andExpect(content().string(containsString("9 lượt mua")))
            .andExpect(content().string(not(containsString("/payment/checkout/1"))));
    }
    @Test void serverBlocksDirectPaidLessonSubmissions() throws Exception {
        var lessons=fake(LessonRepository.class);var flashcards=fake(FlashcardRepository.class);
        var lesson=new Lesson();var module=new com.project.englishlearning.entity.Module();module.setCourse(course);lesson.setModule(module);
        when(lessons.findById(3L)).thenReturn(Optional.of(lesson));
        var guard=new com.project.englishlearning.config.CourseAccessInterceptor(courses,lessons,flashcards,users,access);
        var request=new org.springframework.mock.web.MockHttpServletRequest("POST","/lessons/3/submit");
        request.setUserPrincipal(new UsernamePasswordAuthenticationToken("student","pw"));
        request.setAttribute(org.springframework.web.servlet.HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE,Map.of("lessonId","3"));
        var response=new org.springframework.mock.web.MockHttpServletResponse();
        assertFalse(guard.preHandle(request,response,new Object()));assertEquals(403,response.getStatus());
    }
    @Test void adminReviewListRendersAndVisibilityCanBeUpdated() throws Exception {
        var review=new CourseReview();review.setId(10L);review.setCourse(course);review.setUser(user);review.setRating(5);review.setComment("Helpful");
        when(reviews.adminReviews()).thenReturn(List.of(review));when(reviews.findById(10L)).thenReturn(Optional.of(review));
        var admin=MockMvcBuilders.standaloneSetup(new AdminCourseReviewController(reviews,courses)).setViewResolvers(new AdvisorySubmissionTests.TestConfig().viewResolver()).build();
        admin.perform(get("/admin/reviews")).andExpect(status().isOk()).andExpect(content().string(containsString("Helpful")));
        admin.perform(post("/admin/reviews/10/visibility").param("visible","false")).andExpect(redirectedUrl("/admin/reviews"));
        assertFalse(review.isVisible());verify(reviews).save(review);
    }
    @Test void adminCourseListDisplaysTheSameRatingsAndPurchases() throws Exception {
        var service=fake(CourseService.class);when(service.getAllCourses()).thenReturn(List.of(course));
        when(orders.countPurchasesByCourse()).thenReturn(Collections.singletonList(new Object[]{1L,9L}));
        when(reviews.ratingStats()).thenReturn(Collections.singletonList(new Object[]{1L,4.5,2L}));
        var admin=MockMvcBuilders.standaloneSetup(new AdminCourseController(service,orders,fake(NotificationRepository.class),users,reviewService))
            .setViewResolvers(new AdvisorySubmissionTests.TestConfig().viewResolver()).build();
        admin.perform(get("/admin/courses")).andExpect(status().isOk())
            .andExpect(content().string(containsString("4.5/5 (2)")))
            .andExpect(content().string(containsString("/admin/reviews")));
    }

    @Test void adminReviewsFilterByCourseAndKeepFilterAfterModeration() throws Exception {
        var review=new CourseReview();review.setId(10L);review.setCourse(course);review.setUser(user);review.setRating(4);review.setComment("ONLY_SELECTED_COURSE");
        when(courses.findAll()).thenReturn(List.of(course));
        when(reviews.adminReviewsByCourseId(1L)).thenReturn(List.of(review));
        when(reviews.findById(10L)).thenReturn(Optional.of(review));
        var admin=MockMvcBuilders.standaloneSetup(new AdminCourseReviewController(reviews,courses))
            .setViewResolvers(new AdvisorySubmissionTests.TestConfig().viewResolver()).build();
        admin.perform(get("/admin/reviews").param("courseId","1")).andExpect(status().isOk())
            .andExpect(content().string(containsString("ONLY_SELECTED_COURSE")))
            .andExpect(content().string(containsString("name=\"courseId\" value=\"1\"")));
        verify(reviews,never()).adminReviews();
        admin.perform(post("/admin/reviews/10/visibility").param("visible","false").param("courseId","1"))
            .andExpect(redirectedUrl("/admin/reviews?courseId=1"));
        admin.perform(get("/admin/reviews").param("courseId","999")).andExpect(status().isNotFound());
    }
    @Test void dashboardShowsPriceAndCompactReviewLinkForTheCorrectCourse() throws Exception {
        when(orders.getTopSellingCourses()).thenReturn(Collections.singletonList(new Object[]{course.getTitle(),9L,1L,course.getPrice()}));
        when(orders.countPurchasesByCourse()).thenReturn(Collections.singletonList(new Object[]{1L,9L}));
        when(reviews.ratingStats()).thenReturn(Collections.singletonList(new Object[]{1L,4.5,2L}));
        var admin=MockMvcBuilders.standaloneSetup(new AdminController(users,courses,fake(LessonRepository.class),fake(TestResultRepository.class),fake(WritingSubmissionRepository.class),orders,enrollments,fake(PaymentTransactionRepository.class),reviewService,fake(SiteActivityRepository.class)))
            .setViewResolvers(new AdvisorySubmissionTests.TestConfig().viewResolver()).build();
        admin.perform(get("/admin/dashboard")).andExpect(status().isOk())
            .andExpect(content().string(containsString("200,000 ₫")))
            .andExpect(content().string(containsString("/admin/reviews?courseId=1")))
            .andExpect(content().string(containsString("4.5/5")))
            .andExpect(content().string(containsString("href=\"#quick-actions\"")))
            .andExpect(content().string(containsString("id=\"visitsChart\"")))
            .andExpect(content().string(containsString("id=\"studyHoursChart\"")))
            .andExpect(content().string(containsString("id=\"registrationsChart\"")))
            .andExpect(content().string(containsString("id=\"purchasesChart\"")));
    }

    @Test void adminAccountsSortNewestByDefaultAndAllowOldestFirst() throws Exception {
        var admin=MockMvcBuilders.standaloneSetup(new AdminController(users,courses,fake(LessonRepository.class),fake(TestResultRepository.class),fake(WritingSubmissionRepository.class),orders,enrollments,fake(PaymentTransactionRepository.class),reviewService,fake(SiteActivityRepository.class)))
            .setViewResolvers(new AdvisorySubmissionTests.TestConfig().viewResolver()).build();
        admin.perform(get("/admin/users")).andExpect(status().isOk())
            .andExpect(model().attribute("accountSort","newest"))
            .andExpect(content().string(containsString("Mới nhất trước")));
        verify(users).findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC,"createdAt","id"));
        admin.perform(get("/admin/users").param("sort","oldest")).andExpect(status().isOk()).andExpect(model().attribute("accountSort","oldest"));
        verify(users).findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC,"createdAt","id"));
        admin.perform(get("/admin/users").param("sort","invalid")).andExpect(status().isOk()).andExpect(model().attribute("accountSort","newest"));
    }

    @Test void courseBuilderLinksMockTestsDirectlyToTheCorrectEditor() throws Exception {
        var module=new com.project.englishlearning.entity.Module();module.setId(2L);module.setTitle("Đề luyện kỹ năng");module.setCourse(course);
        for (int i=0;i<3;i++) {
            var lesson=new Lesson();lesson.setId(20L+i);lesson.setTitle("Test "+i);lesson.setLessonType("MOCK_TEST");lesson.setSkillType(List.of("WRITING","READING","LISTENING").get(i));lesson.setModule(module);module.getLessons().add(lesson);
        }
        when(modules.findByCourseIdOrderByOrderIndexAsc(1L)).thenReturn(List.of(module));
        var admin=MockMvcBuilders.standaloneSetup(new AdminCourseBuilderController(courses,modules,fake(LessonRepository.class)))
            .setViewResolvers(new AdvisorySubmissionTests.TestConfig().viewResolver()).build();
        admin.perform(get("/admin/courses/1/builder")).andExpect(status().isOk())
            .andExpect(content().string(containsString("/admin/writing/lessons/20/writing-builder")))
            .andExpect(content().string(containsString("/admin/lessons/21/exam-builder")))
            .andExpect(content().string(containsString("/admin/lessons/22/exam-builder")))
            .andExpect(content().string(not(containsString("Soạn nội dung tại Ngân hàng đề thi"))));
    }

    @Test void promotionCountdownUsesTheSameDeadlineInAllHomePlacementsAndDetails() throws Exception {
        var end=java.time.LocalDateTime.now().plusDays(2).withNano(0);
        var dto=new CourseDTO();dto.setId(1L);dto.setTitle("Promotion");dto.setLevel("Beginner");dto.setIsFree(false);dto.setPrice(new java.math.BigDecimal("200000"));dto.setSalePrice(new java.math.BigDecimal("150000"));dto.setSaleEndDate(end);
        var service=fake(CourseService.class);when(service.getAllCoursesDTO()).thenReturn(List.of(dto));
        var home=MockMvcBuilders.standaloneSetup(new HomeController(service,users)).setViewResolvers(new AdvisorySubmissionTests.TestConfig().viewResolver()).build();
        String deadline=end.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))+"+07:00";
        var html=home.perform(get("/")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertEquals(3,html.split("data-offer-end=",-1).length-1);
        assertTrue(html.contains(deadline));
        course.setSalePrice(new java.math.BigDecimal("150000"));course.setSaleEndDate(end);
        mvc.perform(get("/courses/1")).andExpect(status().isOk()).andExpect(content().string(containsString(deadline)));
        course.setSaleEndDate(java.time.LocalDateTime.now().minusSeconds(1));
        mvc.perform(get("/courses/1")).andExpect(status().isOk()).andExpect(content().string(not(containsString("data-offer-end="))));
    }

    @Test void purchasedCoursesKeepLearningCardsButDisappearFromEveryPromotionPlacement() throws Exception {
        var dto=new CourseDTO();dto.setId(1L);dto.setTitle("Purchased course");dto.setLevel("Beginner");dto.setIsFree(false);dto.setPrice(new java.math.BigDecimal("200000"));dto.setSalePrice(new java.math.BigDecimal("150000"));dto.setSaleEndDate(java.time.LocalDateTime.now().plusDays(2));
        dto.setPurchased(true);dto.setIsEnrolled(true);
        var service=fake(CourseService.class);when(service.getAllCoursesDTOForUser(7L)).thenReturn(List.of(dto));
        var home=MockMvcBuilders.standaloneSetup(new HomeController(service,users)).setViewResolvers(new AdvisorySubmissionTests.TestConfig().viewResolver()).build();
        var html=home.perform(get("/").principal(new UsernamePasswordAuthenticationToken("student","",List.of()))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(html.contains("Purchased course"));
        assertTrue(html.contains("Tiếp tục học"));
        assertTrue(html.contains("data-on-sale=\"false\""));
        assertFalse(html.contains("data-offer-end="));
        dto.setIsEnrolled(false);
        assertFalse(dto.isPromotionAvailable());
        dto.setPurchased(false);
        assertTrue(dto.isPromotionAvailable());
        dto.setIsEnrolled(true);dto.setIsExpired(true);
        assertFalse(dto.isPromotionAvailable());
    }

    @Test void previouslyPaidCourseDetailsHideCountdownEvenWithoutCurrentEnrollment() throws Exception {
        course.setSalePrice(new java.math.BigDecimal("150000"));course.setSaleEndDate(java.time.LocalDateTime.now().plusDays(2));
        when(orders.existsByUserIdAndCourseIdAndStatus(7L,1L,"PAID")).thenReturn(true);
        mvc.perform(get("/courses/1").principal(new UsernamePasswordAuthenticationToken("student","")))
            .andExpect(status().isOk()).andExpect(content().string(not(containsString("data-offer-end="))));
        mvc.perform(get("/courses/1")).andExpect(status().isOk()).andExpect(content().string(containsString("data-offer-end=")));
    }

    @Test void adminStudentDetailsShowAllSavedProfileFieldsAndHandleMissingInformation() throws Exception {
        user.setPhone("0901234567");user.setCity("Đà Nẵng");user.setLearningGoal("IELTS 7.0");user.setDateOfBirth(java.time.LocalDate.of(2004,5,20));user.setAvatarUrl("/uploads/avatars/student.png");user.setRole("ROLE_STUDENT");
        when(users.findById(7L)).thenReturn(Optional.of(user));
        var admin=MockMvcBuilders.standaloneSetup(new AdminController(users,courses,fake(LessonRepository.class),fake(TestResultRepository.class),fake(WritingSubmissionRepository.class),orders,enrollments,fake(PaymentTransactionRepository.class),reviewService,fake(SiteActivityRepository.class)))
            .setViewResolvers(new AdvisorySubmissionTests.TestConfig().viewResolver()).build();
        admin.perform(get("/admin/users/7")).andExpect(status().isOk())
            .andExpect(content().string(containsString("0901234567")))
            .andExpect(content().string(containsString("Đà Nẵng")))
            .andExpect(content().string(containsString("IELTS 7.0")))
            .andExpect(content().string(containsString("20/05/2004")))
            .andExpect(content().string(containsString("/uploads/avatars/student.png")))
            .andExpect(content().string(containsString("Tài khoản học sinh")));
        user.setAvatarUrl(null);user.setPhone(null);user.setCity(null);user.setLearningGoal(null);user.setDateOfBirth(null);
        admin.perform(get("/admin/users/7")).andExpect(status().isOk())
            .andExpect(content().string(containsString("Chưa cập nhật")))
            .andExpect(content().string(containsString("Chưa có ảnh đại diện")));
    }

    @Test void guestAdvisoryHighlightSitsBetweenHotOffersAndCourseRoadmap() throws Exception {
        var service=fake(CourseService.class);
        var home=MockMvcBuilders.standaloneSetup(new HomeController(service,users)).setViewResolvers(new AdvisorySubmissionTests.TestConfig().viewResolver()).build();
        String html=home.perform(get("/")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        int hot=html.indexOf("id=\"hotCoursesCarousel\""),highlight=html.indexOf("class=\"guest-advisory-highlight\""),courses=html.indexOf("id=\"courses\"");
        // With no promotions the hot carousel is omitted, but insertion remains before the roadmap.
        assertTrue(highlight>=0 && highlight<courses);assertTrue(hot<0 || hot<highlight);
        assertTrue(html.contains("/images/advisory-learning.png"));
        assertEquals(2,html.split("action=\"/advisory/submit\"",-1).length-1);
    }
    @Test void orderFilterIncludesFullNameAndUsernameWithMissingUserHandled() throws Exception {
        user.setFullName("Trần Thiên Ân");
        var order=new CourseOrder();order.setId(1L);order.setUser(user);order.setCourse(course);order.setOrderCode("12345");order.setStatus("PENDING");
        var missingUser=new CourseOrder();missingUser.setId(2L);missingUser.setOrderCode("67890");missingUser.setStatus("FAILED");
        when(orders.findAll(org.mockito.ArgumentMatchers.any(org.springframework.data.domain.Sort.class))).thenReturn(List.of(order,missingUser));
        var admin=new AdminOrderController(orders,fake(PaymentTransactionRepository.class),enrollments,fake(AdminNotificationRepository.class),fake(NotificationRepository.class));
        var page=MockMvcBuilders.standaloneSetup(admin).setViewResolvers(new AdvisorySubmissionTests.TestConfig().viewResolver()).build();
        page.perform(get("/admin/orders")).andExpect(status().isOk())
            .andExpect(content().string(containsString("data-name-filter=\"true\"")))
            .andExpect(content().string(containsString("data-student-name=\"student Trần Thiên Ân\"")))
            .andExpect(content().string(containsString("N/A")));
    }
}
