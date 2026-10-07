package com.project.englishlearning.controller;
import com.project.englishlearning.entity.SiteActivity;
import com.project.englishlearning.repository.SiteActivityRepository;
import jakarta.servlet.http.*;
import org.springframework.web.bind.annotation.*;
@RestController
public class StudyActivityController {
    private final SiteActivityRepository repository;
    public StudyActivityController(SiteActivityRepository repository){this.repository=repository;}
    @PostMapping("/api/activity/study")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void heartbeat(HttpServletRequest request){
        var session=request.getSession(false);
        if(session==null || request.getUserPrincipal()==null) return;
        synchronized(session){
            long now=System.currentTimeMillis();
            Long page=(Long)session.getAttribute("studyPageSeenAt");
            if(page==null || now-page>12*60*60*1000L) return;
            Long previous=(Long)session.getAttribute("studyHeartbeatAt");
            if(previous!=null && now-previous<25000) return;
            session.setAttribute("studyHeartbeatAt",now);
            if(previous!=null && now-previous<=40000) repository.save(new SiteActivity(0,(now-previous)/1000));
        }
    }
}
