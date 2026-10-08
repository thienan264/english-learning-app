package com.project.englishlearning.service;
import com.project.englishlearning.dto.LearningSummary;
import com.project.englishlearning.entity.*;
import com.project.englishlearning.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
@Service
public class LearningStatisticsService {
 public record Day(String date,int attempts) {}
 public record Attempt(Long id,Long lessonId,String title,String skill,String date,double accuracy,int correct,int total) {}
 public record Statistics(LearningSummary summary,List<Day> activity,List<Attempt> attempts,int writingSubmissions,int activeDays,int totalAttempts) {}
 private final TestResultRepository results;private final WritingSubmissionRepository writing;private final LearningSummaryService summaries;
 public LearningStatisticsService(TestResultRepository r,WritingSubmissionRepository w,LearningSummaryService s){results=r;writing=w;summaries=s;}
 @Transactional(readOnly=true)
 public Statistics build(User user){
  var now=LocalDateTime.now();var start=now.toLocalDate().minusDays(27);var days=new LinkedHashMap<LocalDate,Integer>();for(int i=0;i<28;i++)days.put(start.plusDays(i),0);
  var attempts=new ArrayList<Attempt>();
  for(var r:results.findByUserIdOrderByCompletedAtDesc(user.getId())){
   if(r.getUser()==null || !Objects.equals(user.getId(),r.getUser().getId()) || r.getCompletedAt()==null || r.getCompletedAt().isAfter(now) || r.getLesson()==null || r.getTotalQuestions()==null || r.getTotalQuestions()<=0 || r.getCorrectAnswers()==null || r.getCorrectAnswers()<0 || r.getCorrectAnswers()>r.getTotalQuestions())continue;
   if(!Set.of("READING","LISTENING").contains(r.getLesson().getSkillType()))continue;
   days.computeIfPresent(r.getCompletedAt().toLocalDate(),(k,v)->v+1);
   attempts.add(new Attempt(r.getId(),r.getLesson().getId(),r.getLesson().getTitle(),r.getLesson().getSkillType(),r.getCompletedAt().toString(),r.getAccuracyPercentage(),r.getCorrectAnswers(),r.getTotalQuestions()));
  }
  int writingCount=0;
  for(var w:writing.findByUserId(user.getId())){
   if(w.getUser()==null || !Objects.equals(user.getId(),w.getUser().getId()) || w.getSubmittedAt()==null || w.getSubmittedAt().isAfter(now))continue;
   writingCount++;days.computeIfPresent(w.getSubmittedAt().toLocalDate(),(k,v)->v+1);
  }
  attempts.sort(Comparator.comparing(Attempt::date).thenComparing(Attempt::id,Comparator.nullsLast(Comparator.naturalOrder())));
  return new Statistics(summaries.summarize(user),days.entrySet().stream().map(e->new Day(e.getKey().toString(),e.getValue())).toList(),List.copyOf(attempts),writingCount,(int)days.values().stream().filter(v->v>0).count(),attempts.size()+writingCount);
 }
}
