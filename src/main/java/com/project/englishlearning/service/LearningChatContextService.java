package com.project.englishlearning.service;
import com.project.englishlearning.dto.LearningChat.*;
import com.project.englishlearning.entity.*;
import com.project.englishlearning.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Service
public class LearningChatContextService {
 private final LearningSummaryService summaries; private final LessonRepository lessons;
 private final WritingExamRepository writing; private final QuestionGroupRepository groups; private final CourseAccessService access;
 public LearningChatContextService(LearningSummaryService s,LessonRepository l,QuestionGroupRepository g,CourseAccessService a,WritingExamRepository w){summaries=s;lessons=l;groups=g;access=a;writing=w;}
 @Transactional(readOnly=true)
 public Context build(User user){
  var summary=summaries.summarize(user); var courses=new LinkedHashMap<Long,CatalogCourse>(); var catalog=new ArrayList<CatalogLesson>();var actions=new ArrayList<Action>();
  actions.add(new Action("profile","Xem hồ sơ học tập","/profile/learning-summary","Kết quả và phần cần ôn của bạn"));
  for(var l:lessons.findAll()){
   if(l.getLearningObjective()==null || l.getLearningObjective().isBlank() || l.getAssessmentRole()==null) continue;
   if(!Set.of("PRACTICE","FINAL","PLACEMENT").contains(l.getAssessmentRole())) continue;
   var course=l.getCourse()!=null?l.getCourse():l.getModule()!=null?l.getModule().getCourse():null;
   if(course==null) continue;
   var qs=new ArrayList<Question>(l.getQuestions());
   for(var g:groups.findByLessonIdOrderByOrderIndexAsc(l.getId())) qs.addAll(g.getQuestions());
   if(Set.of("READING","LISTENING").contains(l.getSkillType()) && qs.isEmpty()) continue;
   if("LISTENING".equals(l.getSkillType()) && (l.getMediaUrl()==null || l.getMediaUrl().isBlank())) continue;
   if("WRITING".equals(l.getSkillType()) && writing.findByLessonId(l.getId()).isEmpty()) continue;
   boolean allowed=access.canLearn(user,course); String id="lesson-"+l.getId();
   catalog.add(new CatalogLesson(id,l.getTitle(),course.getTitle(),course.getLevel(),l.getSkillType(),l.getLearningLevel(),l.getLearningObjective(),l.getAssessmentRole(),allowed,
    qs.stream().map(Question::getCompetencyTag).filter(Objects::nonNull).distinct().sorted().toList()));
   actions.add(new Action(id,l.getTitle(),allowed?"/lessons/"+l.getId():"/courses/"+course.getId(),
     course.getTitle()+" · "+(allowed?"Có thể học":"Xem khóa học và quyền truy cập")));
   String cid="course-"+course.getId();
   if(!"PLACEMENT".equals(l.getAssessmentRole())) courses.putIfAbsent(course.getId(),courseInfo(course,allowed,java.time.LocalDateTime.now(),summary.courses().stream().anyMatch(c->c.id().equals(course.getId()))));
   if(actions.stream().noneMatch(a->a.id().equals(cid))) actions.add(new Action(cid,course.getTitle(),"/courses/"+course.getId(),course.getLevel()+" · "+(allowed?"Có thể học":"Xem thông tin khóa học")));
  }
  for(int i=0;i<actions.size();i++){
   var action=actions.get(i);var info=courses.values().stream().filter(c->c.id().equals(action.id())).findFirst().orElse(null);
   if(info!=null) actions.set(i,new Action(action.id(),action.title(),action.url(),info.enrolled() && info.accessible()?"Bạn đã đăng ký · Tiếp tục học":action.reason(),info));
  }
  catalog.sort(Comparator.comparingInt((CatalogLesson l)->{
   var skill=summary.skills().stream().filter(k->k.code().equals(l.skill())).findFirst().orElse(null);
   int rank=l.accessible()?0:100;
   if(skill!=null && skill.suggestedLevel()==null) return rank+("PLACEMENT".equals(l.role())?0:50);
   rank+="PRACTICE".equals(l.role())?0:30;
   if(skill!=null){rank+=Objects.equals(skill.suggestedLevel(),l.level())?0:20;
    boolean weak=skill.competencies().stream().anyMatch(c->"REVIEW".equals(c.status()) && Objects.equals(c.level(),l.level()) && l.competencies().contains(c.code()));
    rank+=weak?0:10;
   }
   return rank;
  }).thenComparing(CatalogLesson::id));
  return new Context(summary,List.copyOf(catalog),List.copyOf(actions),List.copyOf(courses.values()));
 }
 public static CatalogCourse courseInfo(Course course,boolean accessible,java.time.LocalDateTime now){
  return courseInfo(course,accessible,now,false);
 }
 public static CatalogCourse courseInfo(Course course,boolean accessible,java.time.LocalDateTime now,boolean enrolled){
  var price=currentPrice(course,now);var original=course.getPrice();
  boolean discounted=Boolean.FALSE.equals(course.getIsFree()) && original!=null && original.signum()>0 && price!=null && price.compareTo(original)<0;
  var percent=discounted?original.subtract(price).multiply(java.math.BigDecimal.valueOf(100)).divide(original,1,java.math.RoundingMode.HALF_UP):java.math.BigDecimal.ZERO;
  return new CatalogCourse("course-"+course.getId(),course.getTitle(),course.getLevel(),price,"VND",!Boolean.FALSE.equals(course.getIsFree()),accessible,original,discounted,percent,discounted?course.getSaleEndDate():null,enrolled);
 }
 public static java.math.BigDecimal currentPrice(Course course,java.time.LocalDateTime now){
  if(!Boolean.FALSE.equals(course.getIsFree())) return java.math.BigDecimal.ZERO;
  boolean started=course.getSaleStartDate()==null || !now.isBefore(course.getSaleStartDate());
  boolean ended=course.getSaleEndDate()!=null && now.isAfter(course.getSaleEndDate());
  var price=started && !ended && course.getSalePrice()!=null?course.getSalePrice():course.getPrice();
  return price!=null && price.signum()>=0?price:null;
 }

}
