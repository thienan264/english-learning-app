package com.project.englishlearning;
import com.project.englishlearning.service.*;
import com.project.englishlearning.entity.*;
import jakarta.persistence.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class BasicDataConstraintsTests {
 <T>T fake(Class<T> c){return mock(c,withSettings().mockMaker(org.mockito.MockMakers.SUBCLASS));}
 Course validCourse(){var c=new Course();c.setId(1L);c.setTitle("Course");c.setIsFree(false);c.setPrice(new BigDecimal("500000"));return c;}
 @Test void validPricingAllowsFutureUpdates(){var c=validCourse();c.setSalePrice(new BigDecimal("350000"));c.setSaleStartDate(LocalDateTime.now());c.setSaleEndDate(LocalDateTime.now().plusDays(1));assertDoesNotThrow(()->CourseDataRules.validate(c));c.setSalePrice(null);c.setAccessDurationMonths(0);assertDoesNotThrow(()->CourseDataRules.validate(c));c.setIsFree(true);c.setPrice(null);assertDoesNotThrow(()->CourseDataRules.validate(c));}
 @Test void invalidPricesDatesAndBlankTitlesAreRejected(){
  var c=validCourse();c.setPrice(new BigDecimal("-1"));assertThrows(DataConstraintViolation.class,()->CourseDataRules.validate(c));
  c.setPrice(new BigDecimal("500000"));c.setSalePrice(new BigDecimal("500001"));assertThrows(DataConstraintViolation.class,()->CourseDataRules.validate(c));
  c.setSalePrice(new BigDecimal("-1"));assertThrows(DataConstraintViolation.class,()->CourseDataRules.validate(c));c.setSalePrice(null);
  c.setSaleStartDate(LocalDateTime.now());c.setSaleEndDate(LocalDateTime.now().minusDays(1));assertThrows(DataConstraintViolation.class,()->CourseDataRules.validate(c));c.setSaleEndDate(null);
  c.setAccessDurationMonths(-1);assertThrows(DataConstraintViolation.class,()->CourseDataRules.validate(c));c.setAccessDurationMonths(null);
  c.setTitle("   ");assertThrows(DataConstraintViolation.class,()->CourseDataRules.validate(c));
 }
 @SuppressWarnings("unchecked") EntityManager manager(boolean hasHistory){
  var em=fake(EntityManager.class);when(em.createQuery(anyString(),eq(Long.class))).thenAnswer(call->{TypedQuery<Long> q=fake(TypedQuery.class);when(q.setParameter(eq("id"),any())).thenReturn(q);when(q.getSingleResult()).thenReturn(hasHistory?1L:0L);return q;});return em;
 }
 @Test void courseWithEnrollmentOrOrdersCannotLoseHistory(){var em=manager(true);when(em.find(Course.class,1L,LockModeType.PESSIMISTIC_WRITE)).thenReturn(validCourse());var service=new CurriculumDeletionService();ReflectionTestUtils.setField(service,"em",em);assertThrows(DataConstraintViolation.class,()->service.deleteCourse(1L));verify(em,never()).remove(any());}
 Lesson lesson(){var l=new Lesson();l.setId(3L);var m=new com.project.englishlearning.entity.Module();m.setCourse(validCourse());l.setModule(m);return l;}
 @Test void lessonWithAttemptsIsProtected(){var em=manager(true);var l=lesson();when(em.find(Course.class,1L,LockModeType.PESSIMISTIC_WRITE)).thenReturn(validCourse());when(em.find(Lesson.class,3L)).thenReturn(l);var service=new CurriculumDeletionService();ReflectionTestUtils.setField(service,"em",em);assertThrows(DataConstraintViolation.class,()->service.deleteLesson(1L,3L));verify(em,never()).remove(any());}
 @Test void unusedLessonCanBeDeletedButWrongCourseCannot(){var em=manager(false);var l=lesson();when(em.find(Course.class,1L,LockModeType.PESSIMISTIC_WRITE)).thenReturn(validCourse());when(em.find(Course.class,2L,LockModeType.PESSIMISTIC_WRITE)).thenReturn(validCourse());when(em.find(Lesson.class,3L)).thenReturn(l);var service=new CurriculumDeletionService();ReflectionTestUtils.setField(service,"em",em);assertThrows(DataConstraintViolation.class,()->service.deleteLesson(2L,3L));verify(em,never()).remove(any());service.deleteLesson(1L,3L);verify(em).remove(l);verify(em).flush();}
 @Test void moduleWithHistoryIsProtectedBeforeAnyDeletion(){
  var em=manager(true);var l=lesson();var m=l.getModule();m.setId(2L);m.getLessons().add(l);
  when(em.find(Course.class,1L,LockModeType.PESSIMISTIC_WRITE)).thenReturn(validCourse());when(em.find(com.project.englishlearning.entity.Module.class,2L)).thenReturn(m);
  var service=new CurriculumDeletionService();ReflectionTestUtils.setField(service,"em",em);
  assertThrows(DataConstraintViolation.class,()->service.deleteModule(1L,2L));verify(em,never()).remove(any());
 }
 @Test void unusedModuleCanBeDeleted(){
  var em=manager(false);var l=lesson();var m=l.getModule();m.setId(2L);m.getLessons().add(l);
  when(em.find(Course.class,1L,LockModeType.PESSIMISTIC_WRITE)).thenReturn(validCourse());when(em.find(com.project.englishlearning.entity.Module.class,2L)).thenReturn(m);
  var service=new CurriculumDeletionService();ReflectionTestUtils.setField(service,"em",em);service.deleteModule(1L,2L);verify(em).remove(m);verify(em).flush();
 }
}
