package com.project.englishlearning;

import com.project.englishlearning.config.SecurityConfig;
import com.project.englishlearning.controller.AdvisoryController;
import com.project.englishlearning.controller.AdminAdvisoryController;
import com.project.englishlearning.entity.AdvisoryRequest;
import com.project.englishlearning.repository.AdvisoryRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = AdvisorySubmissionTests.TestConfig.class)
class AdvisorySubmissionTests {
    @Configuration
    @EnableWebMvc
    @Import({SecurityConfig.class, AdvisoryController.class, AdminAdvisoryController.class,
        com.project.englishlearning.controller.AuthController.class})
    static class TestConfig {
        @Bean
        com.project.englishlearning.repository.UserRepository users() {
            return mock(com.project.englishlearning.repository.UserRepository.class,
                withSettings().mockMaker(org.mockito.MockMakers.SUBCLASS));
        }

        @Bean
        com.project.englishlearning.service.EmailVerificationService verification() {
            return mock(com.project.englishlearning.service.EmailVerificationService.class,
                withSettings().mockMaker(org.mockito.MockMakers.SUBCLASS));
        }
        @Bean
        AdvisoryRequestRepository repository() {
            return mock(AdvisoryRequestRepository.class,
                withSettings().mockMaker(org.mockito.MockMakers.SUBCLASS));
        }

        @Bean
        org.thymeleaf.spring6.view.ThymeleafViewResolver viewResolver() {
            var templates = new org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver();
            templates.setPrefix("classpath:/templates/");
            templates.setSuffix(".html");
            templates.setCharacterEncoding("UTF-8");
            templates.setApplicationContext(new org.springframework.context.support.StaticApplicationContext());
            var engine = new org.thymeleaf.spring6.SpringTemplateEngine();
            engine.setTemplateResolver(templates);
            var resolver = new org.thymeleaf.spring6.view.ThymeleafViewResolver();
            resolver.setTemplateEngine(engine);
            resolver.setCharacterEncoding("UTF-8");
            return resolver;
        }
    }

    @Autowired WebApplicationContext context;
    @Autowired AdvisoryRequestRepository repository;
    @Autowired com.project.englishlearning.repository.UserRepository users;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        reset(repository, users);
        var admin=new com.project.englishlearning.entity.User();admin.setUsername("admin");admin.setRole("ROLE_ADMIN");
        when(users.findByUsername("admin")).thenReturn(java.util.Optional.of(admin));
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void guestCanSubmitAdvisory() throws Exception {
        mvc.perform(post("/advisory/submit").with(csrf())
                .param("fullName", "Khách thử nghiệm")
                .param("phone", "0900000000")
                .param("area", "Tp. Hồ Chí Minh")
                .param("subject", "IELTS"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/#advisory-section"))
            .andExpect(flash().attribute("successAdvisory",
                "Đăng ký nhận tư vấn thành công! Chúng tôi sẽ liên hệ với bạn trong thời gian sớm nhất."));

        var saved = ArgumentCaptor.forClass(AdvisoryRequest.class);
        verify(repository).save(saved.capture());
        assertEquals("Khách thử nghiệm", saved.getValue().getFullName());
        assertEquals("0900000000", saved.getValue().getPhone());
        assertEquals("Tp. Hồ Chí Minh", saved.getValue().getArea());
        assertEquals("IELTS", saved.getValue().getSubject());
    }

    @Test
    void submissionStillRequiresCsrf() throws Exception {
        mvc.perform(post("/advisory/submit"))
            .andExpect(status().isForbidden());
        verifyNoInteractions(repository);
    }

    @Test
    void guestCanSendVerificationCodeWithCsrf() throws Exception {
        mvc.perform(post("/register/send-code").with(csrf()).param("email", "guest@example.com"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.retryAfter").value(60));
    }

    @Test
    void verificationEmailEndpointRequiresCsrf() throws Exception {
        mvc.perform(post("/register/send-code").param("email", "guest@example.com"))
            .andExpect(status().isForbidden());
    }

    @Test
    void adminStillRequiresLogin() throws Exception {
        mvc.perform(get("/admin/advisory"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"));
    }

    @Test
    void adminCanOpenEmptyAdvisoryList() throws Exception {
        when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(java.util.List.of());
        mvc.perform(get("/admin/advisories").with(
                org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa có yêu cầu tư vấn nào.")));
    }

    @Test
    void adminCanViewSubmittedAdvisory() throws Exception {
        var request = new AdvisoryRequest();
        request.setId(1L);
        request.setFullName("Khách thử nghiệm");
        request.setPhone("0900000000");
        request.setArea("Tp. Hồ Chí Minh");
        request.setSubject("IELTS");
        request.setStatus("PENDING");
        request.setCreatedAt(java.time.LocalDateTime.of(2026, 10, 7, 14, 0));
        when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(java.util.List.of(request));
        mvc.perform(get("/admin/advisories").with(
                org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Khách thử nghiệm")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("07/10/2026 14:00")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("/admin/advisories/1/status")));
    }
}
