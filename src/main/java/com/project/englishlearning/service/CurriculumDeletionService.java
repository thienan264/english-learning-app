package com.project.englishlearning.service;
import com.project.englishlearning.entity.*;
import com.project.englishlearning.entity.Module;
import jakarta.persistence.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Service
public class CurriculumDeletionService {
 @PersistenceContext private EntityManager em;
 private long count(String query,Long id){return em.createQuery(query,Long.class).setParameter("id",id).getSingleResult();}
 private void fail(String message){throw new DataConstraintViolation(message);}
 private Course course(Long id){var c=em.find(Course.class,id,LockModeType.PESSIMISTIC_WRITE);if(c==null)fail("Khóa học không còn tồn tại.");return c;}
 private void checkLesson(Long id){
  if(count("select count(r) from TestResult r where r.lesson.id=:id",id)>0 || count("select count(w) from WritingSubmission w where w.lesson.id=:id",id)>0 || count("select count(p) from UserLessonProgress p where p.lesson.id=:id and (p.status in ('IN_PROGRESS','COMPLETED') or p.lastAccessedAt is not null)",id)>0)
   fail("Bài học đã có lịch sử học hoặc bài làm nên không thể xóa. Bạn có thể sửa nội dung bài hiện tại.");
 }
 @Transactional public void deleteLesson(Long courseId,Long lessonId){
  course(courseId);var l=em.find(Lesson.class,lessonId);if(l==null || l.getModule()==null || !Objects.equals(l.getModule().getCourse().getId(),courseId))fail("Bài học không thuộc khóa đang quản lý.");checkLesson(lessonId);em.remove(l);em.flush();
 }
 @Transactional public void deleteModule(Long courseId,Long moduleId){
  course(courseId);var m=em.find(Module.class,moduleId);if(m==null || !Objects.equals(m.getCourse().getId(),courseId))fail("Chương không thuộc khóa đang quản lý.");
  for(var l:m.getLessons())checkLesson(l.getId());em.remove(m);em.flush();
 }
 @Transactional public void deleteCourse(Long courseId){
  var c=course(courseId);
  if(count("select count(e) from UserCourseEnrollment e where e.course.id=:id",courseId)>0 || count("select count(o) from CourseOrder o where o.course.id=:id",courseId)>0 || count("select count(r) from CourseReview r where r.course.id=:id",courseId)>0 || count("select count(f) from FlashcardTestResult f where f.course.id=:id",courseId)>0 || count("select count(p) from UserFlashcardProgress p where p.flashcard.course.id=:id",courseId)>0)
   fail("Khóa học đã có đăng ký, đơn hàng hoặc lịch sử sử dụng nên không thể xóa. Lịch sử học và thanh toán được giữ nguyên.");
  var lessons=em.createQuery("select l from Lesson l left join l.module m where l.course.id=:id or m.course.id=:id",Lesson.class).setParameter("id",courseId).getResultList();
  for(var l:lessons)checkLesson(l.getId());
  var modules=em.createQuery("select m from Module m where m.course.id=:id",Module.class).setParameter("id",courseId).getResultList();
  for(var m:modules)em.remove(m);
  for(var l:lessons)if(l.getModule()==null)em.remove(l);
  for(var f:em.createQuery("select f from Flashcard f where f.course.id=:id",Flashcard.class).setParameter("id",courseId).getResultList())em.remove(f);
  em.flush();em.remove(c);em.flush();
 }
}
