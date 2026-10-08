package com.project.englishlearning;
import com.project.englishlearning.entity.*;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.StringTemplateResolver;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class EnrollmentGroupDisplayTests {
 @Test void groupsRenderEachStudentOnceAndRetainCourseActions() throws Exception {
  String page=Files.readString(Path.of("src/main/resources/templates/admin/enrollment-list.html"));
  String fragment=page.substring(page.indexOf("<details class="),page.indexOf("</details>")+10).replaceAll("th:action=\"[^\"]*\"", "");
  var user=new User();user.setId(7L);user.setUsername("anan");user.setFullName("An Nguyễn");user.setEmail("an@example.com");
  var course=new Course();course.setId(2L);course.setTitle("Course A");
  var first=new UserCourseEnrollment();first.setId(1L);first.setUser(user);first.setCourse(course);first.setStatus("IN_PROGRESS");first.setEnrolledAt(LocalDateTime.now());first.setCompletionPercentage(50.0);
  var second=new UserCourseEnrollment();second.setId(2L);second.setUser(user);second.setCourse(course);second.setStatus("REVOKED");second.setEnrolledAt(LocalDateTime.now());second.setCompletionPercentage(0.0);
  var engine=new SpringTemplateEngine();engine.setTemplateResolver(new StringTemplateResolver());
  var context=new Context();context.setVariable("enrollmentNow",LocalDateTime.now());context.setVariable("enrollmentGroups",List.of(List.of(first,second)));
  String html=engine.process(fragment,context);
  assertEquals(1,html.split("<summary>",-1).length-1);assertTrue(html.contains("An Nguyễn"));assertTrue(html.contains("2 khóa học"));
  assertTrue(html.contains("extendModal1"));assertTrue(html.contains("extendModal2"));assertTrue(html.contains("Bị thu hồi"));
 }
}
