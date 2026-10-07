package com.project.englishlearning;
import com.project.englishlearning.controller.NotificationController;
import com.project.englishlearning.entity.*;
import com.project.englishlearning.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class NotificationPageTests {
    NotificationRepository notifications;
    MockMvc mvc;
    final UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("student","pw",java.util.List.of());
    @BeforeEach void setup() {
        notifications=mock(NotificationRepository.class,withSettings().mockMaker(org.mockito.MockMakers.SUBCLASS));
        var users=mock(UserRepository.class,withSettings().mockMaker(org.mockito.MockMakers.SUBCLASS));
        var user=new User();user.setId(7L);user.setUsername("student");
        when(users.findByUsername("student")).thenReturn(java.util.Optional.of(user));
        mvc=MockMvcBuilders.standaloneSetup(new NotificationController(notifications,users))
            .setViewResolvers(new AdvisorySubmissionTests.TestConfig().viewResolver()).build();
    }
    @Test void emptyInboxRendersNewDesignAndFilters() throws Exception {
        mvc.perform(get("/notifications").principal(auth)).andExpect(status().isOk())
            .andExpect(content().string(containsString("Bạn chưa có thông báo nào.")))
            .andExpect(content().string(containsString("data-notification-filter=\"unread\"")))
            .andExpect(content().string(containsString("/css/account-pages.css")));
        verify(notifications).findByUserIdOrderByCreatedAtDesc(7L);
    }
    @Test void populatedInboxKeepsUnreadStateAndReadLinks() throws Exception {
        var unread=new Notification();unread.setId(10L);unread.setTitle("Khóa học mới");unread.setMessage("Đã mở khóa.");unread.setIsRead(false);
        var read=new Notification();read.setId(11L);read.setTitle("Bài học hoàn tất");read.setMessage("Tiếp tục học.");read.setIsRead(true);
        when(notifications.findByUserIdOrderByCreatedAtDesc(7L)).thenReturn(java.util.List.of(unread,read));
        mvc.perform(get("/notifications").principal(auth)).andExpect(status().isOk())
            .andExpect(content().string(containsString("/api/notifications/10/read")))
            .andExpect(content().string(containsString("data-unread=\"true\"")))
            .andExpect(content().string(containsString("data-unread=\"false\"")))
            .andExpect(model().attribute("unreadCount",1L));
    }
}
