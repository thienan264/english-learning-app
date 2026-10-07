package com.project.englishlearning;
import com.project.englishlearning.config.SiteActivityInterceptor;
import com.project.englishlearning.controller.StudyActivityController;
import com.project.englishlearning.repository.SiteActivityRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import org.springframework.web.method.HandlerMethod;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class SiteActivityTests {
    private SiteActivityRepository repository(){return mock(SiteActivityRepository.class,withSettings().mockMaker(org.mockito.MockMakers.SUBCLASS));}
    public String page(){return "page";}
    @Test void recordsSuccessfulHtmlPagesAndIgnoresAdminErrorsAndApi() throws Exception {
        var repo=repository();var interceptor=new SiteActivityInterceptor(repo);
        var request=new MockHttpServletRequest("GET","/learn/course/1");request.setUserPrincipal(()->"student");
        var response=new MockHttpServletResponse();response.setContentType("text/html;charset=UTF-8");
        var handler=new HandlerMethod(this,getClass().getMethod("page"));
        interceptor.afterCompletion(request,response,handler,null);
        assertNotNull(request.getSession().getAttribute("studyPageSeenAt"));verify(repo).save(any());
        request.setRequestURI("/admin/dashboard");interceptor.afterCompletion(request,response,handler,null);
        request.setRequestURI("/learn/course/1");response.setStatus(403);interceptor.afterCompletion(request,response,handler,null);
        response.setStatus(200);response.setContentType("application/json");interceptor.afterCompletion(request,response,handler,null);
        verify(repo,times(1)).save(any());
    }
    @Test void learningHeartbeatDoesNotCreditFirstPingDuplicateTabsOrLongIdleGaps(){
        var repo=repository();var controller=new StudyActivityController(repo);var request=new MockHttpServletRequest();request.setUserPrincipal(()->"student");
        var session=request.getSession();session.setAttribute("studyPageSeenAt",System.currentTimeMillis());
        controller.heartbeat(request);verifyNoInteractions(repo);
        controller.heartbeat(request);verifyNoInteractions(repo);
        session.setAttribute("studyHeartbeatAt",System.currentTimeMillis()-30000);controller.heartbeat(request);verify(repo,times(1)).save(any());
        session.setAttribute("studyHeartbeatAt",System.currentTimeMillis()-300000);controller.heartbeat(request);verify(repo,times(1)).save(any());
        request.setUserPrincipal(null);controller.heartbeat(request);verify(repo,times(1)).save(any());
    }
    @Test void cannotCreditTimeWithoutVisitingALearningPage(){
        var repo=repository();var controller=new StudyActivityController(repo);var request=new MockHttpServletRequest();request.setUserPrincipal(()->"student");request.getSession();
        controller.heartbeat(request);verifyNoInteractions(repo);
    }
}
