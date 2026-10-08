package com.project.englishlearning.dto;
import java.util.List;
public final class LearningChat {
 private LearningChat() {}
 public record Action(String id,String title,String url,String reason,CatalogCourse course) {
  public Action(String id,String title,String url,String reason){this(id,title,url,reason,null);}
 }
 public record Reply(String message,List<Action> actions,boolean fallback) {}
 public record Request(String message) {}
 public record CatalogLesson(String id,String title,String courseTitle,String courseLevel,String skill,String level,String objective,String role,boolean accessible,List<String> competencies) {}
 public record CatalogCourse(String id,String title,String level,java.math.BigDecimal currentPrice,String currency,boolean free,boolean accessible,java.math.BigDecimal originalPrice,boolean discountActive,java.math.BigDecimal discountPercent,java.time.LocalDateTime discountEndsAt,boolean enrolled) {}
 public record Context(LearningSummary learner,List<CatalogLesson> lessons,List<Action> actions,List<CatalogCourse> courses) {
  public Context(LearningSummary learner,List<CatalogLesson> lessons,List<Action> actions){this(learner,lessons,actions,List.of());}
 }
}
