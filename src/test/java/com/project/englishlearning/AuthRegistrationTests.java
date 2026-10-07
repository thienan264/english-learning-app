package com.project.englishlearning;

import com.project.englishlearning.controller.AuthController;
import com.project.englishlearning.repository.UserRepository;
import com.project.englishlearning.service.EmailVerificationService;
import org.junit.jupiter.api.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.mock.web.MockHttpSession;
import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthRegistrationTests {
    UserRepository users;
    EmailVerificationService verification;
    MockMvc mvc;
    private <T> T fake(Class<T> type) { return mock(type, withSettings().mockMaker(org.mockito.MockMakers.SUBCLASS)); }
    @BeforeEach void setup() {
        users = fake(UserRepository.class); verification = fake(EmailVerificationService.class);
        mvc = MockMvcBuilders.standaloneSetup(new AuthController(users,
            new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(4), verification))
            .setViewResolvers(new AdvisorySubmissionTests.TestConfig().viewResolver()).build();
    }
    @Test void registerAndLoginRenderNewDesign() throws Exception {
        mvc.perform(get("/register")).andExpect(status().isOk())
            .andExpect(content().string(containsString("/css/auth.css")))
            .andExpect(content().string(containsString("name=\"verificationCode\"")))
            .andExpect(content().string(containsString("/register/send-code")));
        mvc.perform(get("/login")).andExpect(status().isOk())
            .andExpect(content().string(containsString("autocomplete=\"current-password\"")));
    }
    org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder registration() {
        return post("/register").param("username", "newstudent").param("fullName", "Học sinh")
            .param("email", "new@example.com").param("password", "Strong123!")
            .param("confirmPassword", "Strong123!").param("verificationCode", "123456");
    }
    @Test void registrationCannotBypassCodeVerification() throws Exception {
        when(verification.createVerifiedUser(any(), any(), any())).thenReturn("Mã xác nhận không đúng.");
        mvc.perform(registration()).andExpect(status().isOk()).andExpect(view().name("register"))
            .andExpect(content().string(containsString("Mã xác nhận không đúng.")));
        verify(users, never()).save(any()); verify(users, never()).saveAndFlush(any());
    }
    @Test void verifiedRegistrationRedirectsToLogin() throws Exception {
        mvc.perform(registration()).andExpect(redirectedUrl("/login?registered"));
        verify(verification).createVerifiedUser(argThat(u -> u.getEmail().equals("new@example.com")
            && u.getRole().equals("ROLE_STUDENT") && !u.getPassword().equals("Strong123!")), eq("123456"), anyString());
    }
    @Test void sendCodeHasSessionCooldown() throws Exception {
        var session = new MockHttpSession();
        mvc.perform(post("/register/send-code").session(session).param("email", "new@example.com"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.retryAfter").value(60));
        mvc.perform(post("/register/send-code").session(session).param("email", "other@example.com"))
            .andExpect(status().isTooManyRequests());
        verify(verification, times(1)).sendCode(anyString(), anyString());
    }
    @Test void mailFailureIsShownWithoutClaimingSuccess() throws Exception {
        when(verification.sendCode(anyString(), anyString())).thenThrow(new org.springframework.mail.MailSendException("smtp failed"));
        mvc.perform(post("/register/send-code").param("email", "new@example.com"))
            .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.message").value(containsString("Chưa gửi được")));
    }
}
