package com.project.englishlearning.config;
import com.project.englishlearning.entity.Course;
import com.project.englishlearning.repository.*;
import com.project.englishlearning.service.CourseAccessService;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import java.util.Map;
@Component
public class CourseAccessInterceptor implements HandlerInterceptor {
    private final CourseRepository courses;
    private final LessonRepository lessons;
    private final FlashcardRepository flashcards;
    private final UserRepository users;
    private final CourseAccessService access;
    public CourseAccessInterceptor(CourseRepository courses, LessonRepository lessons, FlashcardRepository flashcards,
            UserRepository users, CourseAccessService access) {
        this.courses=courses; this.lessons=lessons; this.flashcards=flashcards; this.users=users; this.access=access;
    }
    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        @SuppressWarnings("unchecked") Map<String,String> vars = (Map<String,String>)request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (vars == null) return true;
        Course course = null;
        if (vars.containsKey("courseId")) course = courses.findById(Long.valueOf(vars.get("courseId"))).orElse(null);
        else if (vars.containsKey("lessonId")) {
            var lesson = lessons.findById(Long.valueOf(vars.get("lessonId"))).orElse(null);
            if (lesson != null) course = lesson.getModule() != null ? lesson.getModule().getCourse() : lesson.getCourse();
        } else if (vars.containsKey("flashcardId")) {
            var card = flashcards.findById(Long.valueOf(vars.get("flashcardId"))).orElse(null);
            if (card != null) course = card.getCourse();
        }
        if (course == null) { response.sendError(404); return false; }
        var principal = request.getUserPrincipal();
        var user = principal == null ? null : users.findByUsername(principal.getName()).orElse(null);
        if (access.canLearn(user, course)) return true;
        if (request.getMethod().equals("GET") && !request.getRequestURI().contains("/status")) {
            response.sendRedirect(request.getContextPath() + "/courses/" + course.getId() + "?locked");
        } else { response.sendError(403, "Bạn cần mua hoặc gia hạn khóa học để sử dụng nội dung này."); }
        return false;
    }
}
