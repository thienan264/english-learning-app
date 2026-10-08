package com.project.englishlearning;

import com.project.englishlearning.config.SecurityConfig;
import com.project.englishlearning.controller.*;
import com.project.englishlearning.repository.*;
import com.project.englishlearning.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes=ExpertWritingSecurityTests.Config.class)
class ExpertWritingSecurityTests {
    static <T> T fake(Class<T> type) { return mock(type,withSettings().mockMaker(org.mockito.MockMakers.SUBCLASS)); }
    @Configuration @EnableWebMvc
    @Import({SecurityConfig.class,AdminExpertWritingController.class,StudentWritingController.class})
    static class Config {
        @Bean UserRepository users() { return fake(UserRepository.class); }
        @Bean WritingSubmissionRepository submissions() { return fake(WritingSubmissionRepository.class); }
        @Bean LessonRepository lessons() { return fake(LessonRepository.class); }
        @Bean WritingExamRepository exams() { return fake(WritingExamRepository.class); }
        @Bean GeminiAiService ai() { return fake(GeminiAiService.class); }
        @Bean ExpertWritingService expert() { return fake(ExpertWritingService.class); }
        @Bean org.thymeleaf.spring6.view.ThymeleafViewResolver views() { return new AdvisorySubmissionTests.TestConfig().viewResolver(); }
    }
    @Autowired WebApplicationContext context;
    @Autowired ExpertWritingService expert;
    @Autowired UserRepository users;
    MockMvc mvc;
    @BeforeEach void setup() { reset(expert,users);
        for(String name:java.util.List.of("student","admin")) {
            var account=new com.project.englishlearning.entity.User();account.setUsername(name);account.setStatus("ACTIVE");
            when(users.findByUsername(name)).thenReturn(java.util.Optional.of(account));
        }
        mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build(); }
    @Test void guestsMustLoginAndStudentsCannotOpenAdminReviewPages() throws Exception {
        mvc.perform(get("/admin/writing/reviews")).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/admin/writing/reviews").with(user("student").roles("STUDENT"))).andExpect(status().isForbidden());
        mvc.perform(post("/admin/writing/reviews/11/respond").with(user("student").roles("STUDENT")).with(csrf()))
            .andExpect(status().isForbidden());
        verifyNoInteractions(expert);
    }
    @Test void bothSubmissionAndAdminResponseRequireCsrf() throws Exception {
        mvc.perform(post("/lessons/3/writing/expert-submit").with(user("student").roles("STUDENT"))).andExpect(status().isForbidden());
        mvc.perform(post("/admin/writing/reviews/11/respond").with(user("admin").roles("ADMIN"))).andExpect(status().isForbidden());
        verifyNoInteractions(expert);
    }
    @Test void adminWithCsrfCanSendFeedback() throws Exception {
        mvc.perform(post("/admin/writing/reviews/11/respond").with(user("admin").roles("ADMIN")).with(csrf())
            .param("task2Band","7.0").param("task2Feedback","Feedback").param("task2Corrections","Corrections").param("task2Suggested","Suggested"))
            .andExpect(redirectedUrl("/admin/writing/reviews/11"));
        var form=org.mockito.ArgumentCaptor.forClass(com.project.englishlearning.dto.ExpertWritingReviewForm.class);
        verify(expert).respondTasks(eq(11L),eq("admin"),form.capture());
        org.junit.jupiter.api.Assertions.assertEquals(7.0,form.getValue().getTask2Band());
        org.junit.jupiter.api.Assertions.assertEquals("Suggested",form.getValue().getTask2Suggested());
    }
}
