package com.project.englishlearning;
import com.project.englishlearning.config.LockedAccountFilter;
import com.project.englishlearning.entity.User;
import com.project.englishlearning.repository.UserRepository;
import com.project.englishlearning.service.CustomUserDetailsService;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class LockedAccountTests {
    UserRepository users;User user;
    @BeforeEach void setup(){
        users=mock(UserRepository.class,withSettings().mockMaker(org.mockito.MockMakers.SUBCLASS));
        user=new User();user.setUsername("student");user.setRole("ROLE_STUDENT");user.setPassword(new BCryptPasswordEncoder(4).encode("password"));
        when(users.findByUsername("student")).thenReturn(Optional.of(user));
    }
    @AfterEach void cleanup(){SecurityContextHolder.clearContext();}
    @Test void lockedAccountCannotAuthenticateEvenWithCorrectPasswordAndCanLoginAfterUnlock(){
        var provider=new DaoAuthenticationProvider(new CustomUserDetailsService(users));provider.setPasswordEncoder(new BCryptPasswordEncoder(4));
        user.setStatus("LOCKED");
        assertThrows(LockedException.class,()->provider.authenticate(UsernamePasswordAuthenticationToken.unauthenticated("student","password")));
        user.setStatus("ACTIVE");
        assertTrue(provider.authenticate(UsernamePasswordAuthenticationToken.unauthenticated("student","password")).isAuthenticated());
    }
    @Test void lockRevokesExistingSessionBeforeControllerRuns() throws Exception {
        user.setStatus("LOCKED");
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated("student","",java.util.List.of()));
        var request=new MockHttpServletRequest("GET","/profile");var session=request.getSession();var response=new MockHttpServletResponse();var chain=new MockFilterChain();
        new LockedAccountFilter(users).doFilter(request,response,chain);
        assertEquals("/login?locked",response.getRedirectedUrl());assertNull(chain.getRequest());assertNull(SecurityContextHolder.getContext().getAuthentication());assertTrue(((MockHttpSession)session).isInvalid());
    }
    @Test void lockedAccountCannotSubmitOrCallApi() throws Exception {
        user.setStatus("LOCKED");
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated("student","",java.util.List.of()));
        var request=new MockHttpServletRequest("POST","/api/activity/study");var response=new MockHttpServletResponse();var chain=new MockFilterChain();
        new LockedAccountFilter(users).doFilter(request,response,chain);
        assertEquals(403,response.getStatus());assertNull(chain.getRequest());
    }
    @Test void activeAccountAndGuestContinueNormally() throws Exception {
        var filter=new LockedAccountFilter(users);
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated("student","",java.util.List.of()));
        var request=new MockHttpServletRequest("GET","/profile");var chain=new MockFilterChain();filter.doFilter(request,new MockHttpServletResponse(),chain);assertNotNull(chain.getRequest());
        SecurityContextHolder.clearContext();chain=new MockFilterChain();filter.doFilter(new MockHttpServletRequest("GET","/"),new MockHttpServletResponse(),chain);assertNotNull(chain.getRequest());
    }
}
