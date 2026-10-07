package com.project.englishlearning.config;
import com.project.englishlearning.entity.SiteActivity;
import com.project.englishlearning.repository.SiteActivityRepository;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
@Component
public class SiteActivityInterceptor implements HandlerInterceptor {
    private final SiteActivityRepository repository;
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger(SiteActivityInterceptor.class);
    public SiteActivityInterceptor(SiteActivityRepository repository){this.repository=repository;}
    public static boolean isStudyPath(String path){return path.startsWith("/learn/") || path.startsWith("/lessons/") || path.matches("/courses/[^/]+/(study-flashcards|vocabulary-quiz)");}
    @Override public void afterCompletion(HttpServletRequest request,HttpServletResponse response,Object handler,Exception exception){
        String path=request.getRequestURI().substring(request.getContextPath().length());
        if (!(handler instanceof HandlerMethod) || !"GET".equals(request.getMethod()) || response.getStatus()!=200 || exception!=null || path.startsWith("/admin") || response.getContentType()==null || !response.getContentType().contains("text/html")) return;
        if(isStudyPath(path) && request.getUserPrincipal()!=null) request.getSession().setAttribute("studyPageSeenAt",System.currentTimeMillis());
        try {repository.save(new SiteActivity(1,0));} catch(RuntimeException failure){LOG.warn("Unable to record page view",failure);}
    }
}
