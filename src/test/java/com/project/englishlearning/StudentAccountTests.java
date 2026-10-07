package com.project.englishlearning;

import com.project.englishlearning.controller.StudentProfileController;
import com.project.englishlearning.entity.*;
import com.project.englishlearning.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.mock.web.MockMultipartFile;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

class StudentAccountTests {
    UserRepository users;
    CourseOrderRepository orders;
    UserLessonProgressRepository progress;
    User user;
    MockMvc mvc;
    final UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("student", "password",
        java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_STUDENT")));
    private <T> T fake(Class<T> type) {
        return mock(type, withSettings().mockMaker(org.mockito.MockMakers.SUBCLASS));
    }
    @BeforeEach void setup() {
        users = fake(UserRepository.class);
        orders = fake(CourseOrderRepository.class);
        progress = fake(UserLessonProgressRepository.class);
        user = new User(); user.setId(7L); user.setUsername("student"); user.setRole("ROLE_STUDENT");
        user.setEmail("student@example.com"); user.setFullName("Học sinh");
        when(users.findByUsername("student")).thenReturn(java.util.Optional.of(user));
        var controller = new StudentProfileController(users, fake(TestResultRepository.class),
            fake(FlashcardTestResultRepository.class), fake(WritingSubmissionRepository.class),
            fake(UserCourseEnrollmentRepository.class), new org.modelmapper.ModelMapper(),
            new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(), orders, progress);
        mvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new com.project.englishlearning.controller.AccountNavbarAdvice(users))
            .setViewResolvers(new AdvisorySubmissionTests.TestConfig().viewResolver()).build();
    }
    @Test void accountIsSeparateAndRoleIsTranslated() throws Exception {
        mvc.perform(get("/profile").principal(auth)).andExpect(status().isOk())
            .andExpect(content().string(containsString("Tài khoản học sinh")))
            .andExpect(content().string(not(containsString("ROLE_STUDENT"))))
            .andExpect(content().string(containsString("name=\"avatar\"")))
            .andExpect(content().string(not(containsString("Lịch sử bài tập Nghe"))));
        verifyNoInteractions(orders, progress);
    }
    @Test void navbarDisplaysCurrentUserAvatar() throws Exception {
        user.setAvatarUrl("/uploads/avatars/example.png");
        mvc.perform(get("/profile").principal(auth)).andExpect(status().isOk())
            .andExpect(content().string(containsString("src=\"/uploads/avatars/example.png\" class=\"account-avatar me-1\"")))
            .andExpect(content().string(containsString("<span aria-hidden=\"true\" hidden=\"hidden\">👤</span>")));
    }
    @Test void paymentsAreScopedToCurrentUser() throws Exception {
        CourseOrder order = new CourseOrder(); order.setOrderCode("ORDER-7");
        order.setAmount(new java.math.BigDecimal("200000")); order.setStatus("PAID");
        when(orders.findByUserIdOrderByCreatedAtDesc(7L)).thenReturn(java.util.List.of(order));
        mvc.perform(get("/profile/payments").principal(auth)).andExpect(status().isOk())
            .andExpect(content().string(containsString("ORDER-7")))
            .andExpect(content().string(containsString("Đã thanh toán")));
        verify(orders).findByUserIdOrderByCreatedAtDesc(7L);
        verify(orders, never()).findAll();
    }
    @Test void emptyLearningHistoryRenders() throws Exception {
        mvc.perform(get("/profile/learning-history").principal(auth)).andExpect(status().isOk())
            .andExpect(content().string(containsString("Chưa có bài học nào.")))
            .andExpect(content().string(containsString("/profile/learning-history")));
        verify(progress).findByUserId(7L);
    }
    @Test void emptyPaymentHistoryRenders() throws Exception {
        mvc.perform(get("/profile/payments").principal(auth)).andExpect(status().isOk())
            .andExpect(content().string(containsString("Bạn chưa có lịch sử thanh toán.")));
    }
    @Test void populatedLessonHistoryKeepsStatusDateAndLearningLink() throws Exception {
        var course = new Course(); course.setId(2L); course.setTitle("Khóa học thử");
        var module = new com.project.englishlearning.entity.Module(); module.setCourse(course);
        var lesson = new Lesson(); lesson.setId(3L); lesson.setTitle("Bài đã học"); lesson.setModule(module);
        var record = new UserLessonProgress(); record.setLesson(lesson); record.setStatus("COMPLETED");
        record.setLastAccessedAt(java.time.LocalDateTime.of(2026, 10, 7, 16, 0));
        when(progress.findByUserId(7L)).thenReturn(java.util.List.of(record));
        mvc.perform(get("/profile/learning-history").principal(auth)).andExpect(status().isOk())
            .andExpect(content().string(containsString("Bài đã học")))
            .andExpect(content().string(containsString("Đã hoàn thành")))
            .andExpect(content().string(containsString("07/10/2026 16:00")))
            .andExpect(content().string(containsString("/learn/course/2?lessonId=3")));
    }
    @Test void personalDetailsPersistWithoutChangingRole() throws Exception {
        mvc.perform(multipart("/profile/update").principal(auth).param("fullName", "Tên mới")
            .param("email", "student@example.com").param("phone", "0900000000")
            .param("dateOfBirth", "2000-01-02").param("city", "Hồ Chí Minh")
            .param("learningGoal", "IELTS 7.0")).andExpect(redirectedUrl("/profile"));
        verify(users).save(user);
        Assertions.assertEquals("0900000000", user.getPhone());
        Assertions.assertEquals(java.time.LocalDate.of(2000, 1, 2), user.getDateOfBirth());
        Assertions.assertEquals("IELTS 7.0", user.getLearningGoal());
        Assertions.assertEquals("ROLE_STUDENT", user.getRole());
    }
    @Test void invalidAvatarDoesNotSave() throws Exception {
        mvc.perform(multipart("/profile/update")
            .file(new MockMultipartFile("avatar", "fake.png", "image/png", "not an image".getBytes()))
            .principal(auth).param("fullName", "Tên mới").param("email", "student@example.com"))
            .andExpect(redirectedUrl("/profile")).andExpect(flash().attributeExists("error"));
        verify(users, never()).save(any());
    }
    @Test void validAvatarIsStoredAndCanBeRemoved() throws Exception {
        var bytes = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(2, 2, java.awt.image.BufferedImage.TYPE_INT_RGB), "png", bytes);
        mvc.perform(multipart("/profile/update")
            .file(new MockMultipartFile("avatar", "avatar.png", "image/png", bytes.toByteArray()))
            .principal(auth).param("fullName", "Học sinh").param("email", "student@example.com"))
            .andExpect(redirectedUrl("/profile")).andExpect(flash().attributeExists("success"));
        Assertions.assertTrue(user.getAvatarUrl().startsWith("/uploads/avatars/"));
        var path = java.nio.file.Path.of(user.getAvatarUrl().substring(1));
        try {
            Assertions.assertNotNull(javax.imageio.ImageIO.read(path.toFile()));
            mvc.perform(multipart("/profile/update").principal(auth)
                .param("fullName", "Học sinh").param("email", "student@example.com")
                .param("removeAvatar", "true")).andExpect(flash().attributeExists("success"));
            Assertions.assertNull(user.getAvatarUrl());
        } finally { java.nio.file.Files.deleteIfExists(path); }
    }
    @Test void malformedBirthdayDoesNotCauseServerError() throws Exception {
        mvc.perform(multipart("/profile/update").principal(auth)
            .param("fullName", "Học sinh").param("email", "student@example.com")
            .param("dateOfBirth", "invalid")).andExpect(redirectedUrl("/profile"))
            .andExpect(flash().attributeExists("error"));
        verify(users, never()).save(any());
    }
}
