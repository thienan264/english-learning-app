package com.project.englishlearning.service;

import com.fasterxml.jackson.databind.*;
import com.project.englishlearning.dto.LearningSummary;
import com.project.englishlearning.entity.*;
import com.project.englishlearning.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class LearningSummaryService {
    private final TestResultRepository tests;
    private final UserCourseEnrollmentRepository enrollments;
    private final UserLessonProgressRepository progress;
    private final ObjectMapper mapper = new ObjectMapper();
    private static final Set<String> LEVELS = Set.of("BEGINNER", "INTERMEDIATE", "ADVANCED");
    private static final Map<String,String> TAGS = Map.of(
            "DETAIL","Thông tin chi tiết", "MAIN_IDEA","Ý chính", "PARAPHRASE","Diễn đạt tương đương",
            "INFERENCE","Suy luận", "AUTHOR_ATTITUDE","Quan điểm và thái độ", "ARGUMENT","Lập luận",
            "CORRECTION","Thông tin được sửa lại", "SPEAKER_MATCH","Ý kiến từng người nói",
            "TRUE_FALSE_NOT_GIVEN","Đúng / sai / không được đề cập");

    public LearningSummaryService(TestResultRepository tests, UserCourseEnrollmentRepository enrollments,
                                  UserLessonProgressRepository progress) {
        this.tests=tests; this.enrollments=enrollments; this.progress=progress;
    }
    private record Answer(String level,String tag,boolean correct) {}
    private record Attempt(TestResult result,String skill,String role,List<Answer> answers) {}
    private static class Count {
        int total,correct;
        void add(Answer answer) { total++; if(answer.correct()) correct++; }
        double percentage() { return total==0 ? 0 : Math.round(correct*1000.0/total)/10.0; }
        boolean placementPass() { return total>=3 && correct*3>=total*2; }
    }

    @Transactional(readOnly=true)
    public LearningSummary summarize(User user) {
        LocalDateTime now=LocalDateTime.now();
        List<TestResult> rows=new ArrayList<>(tests.findByUserIdOrderByCompletedAtDesc(user.getId()));
        rows.sort(Comparator.comparing(TestResult::getCompletedAt,Comparator.nullsLast(Comparator.reverseOrder())));
        Set<Long> seenLessons=new HashSet<>(); List<Attempt> attempts=new ArrayList<>();
        int excluded=0,legacy=0;
        for(TestResult result:rows) {
            Lesson lesson=result.getLesson();
            // Never infer capabilities from another account, malformed/empty data or stale attempts.
            if(result.getUser()==null || !user.getId().equals(result.getUser().getId()) || lesson==null
                    || lesson.getId()==null || !Set.of("READING","LISTENING").contains(Objects.toString(lesson.getSkillType(),""))
                    || result.getCompletedAt()==null || result.getCompletedAt().isBefore(now.minusDays(90))
                    || result.getCompletedAt().isAfter(now.plusMinutes(5)) || result.getTotalQuestions()==null
                    || result.getTotalQuestions()<=0 || !seenLessons.add(lesson.getId())) { excluded++; continue; }
            List<Answer> answers=new ArrayList<>();
            try {
                JsonNode details=mapper.readTree(result.getDetailedResultJson()==null?"":result.getDetailedResultJson());
                if(details==null || !details.isArray()) { excluded++; continue; }
                Set<String> seenQuestions=new HashSet<>();
                for(JsonNode item:details) {
                    String id=item.path("questionId").asText("");
                    if(!id.isEmpty() && !seenQuestions.add(id)) continue;
                    String tag=item.path("competencyTag").asText("");
                    String level=item.path("learningLevel").asText("");
                    // A missing snapshot stays unknown; do not backfill it from an edited exam.
                    if(!TAGS.containsKey(tag) || !LEVELS.contains(level) || !item.path("isCorrect").isBoolean()) { legacy++; continue; }
                    answers.add(new Answer(level,tag,item.get("isCorrect").asBoolean()));
                }
                if(answers.isEmpty()) { excluded++; continue; }
                attempts.add(new Attempt(result,lesson.getSkillType(),lesson.getAssessmentRole(),answers));
            } catch(Exception malformed) { excluded++; }
        }
        List<LearningSummary.Skill> skills=List.of(skill("READING","Reading",attempts),skill("LISTENING","Listening",attempts));
        List<LearningSummary.Course> courses=enrollments.findByUserId(user.getId()).stream()
                .filter(e->e.getUser()!=null && user.getId().equals(e.getUser().getId()) && e.getCourse()!=null)
                .map(e->{
                    String status="REVOKED".equals(e.getStatus())?"REVOKED":
                            e.getExpiresAt()!=null && !e.getExpiresAt().isAfter(now)?"EXPIRED":"AVAILABLE";
                    return new LearningSummary.Course(e.getCourse().getId(),e.getCourse().getTitle(),status,
                            switch(status) { case "REVOKED"->"Đã thu hồi";case "EXPIRED"->"Đã hết hạn";default->"Đang có quyền học"; },
                            e.getCompletionPercentage()==null?0:Math.max(0,Math.min(100,e.getCompletionPercentage())));
                }).toList();
        int completed=(int)progress.findByUserId(user.getId()).stream()
                .filter(p->p.getUser()!=null && user.getId().equals(p.getUser().getId()) && "COMPLETED".equals(p.getStatus())).count();
        return new LearningSummary(user.getLearningGoal(),skills,courses,completed,excluded,legacy,now);
    }

    private LearningSummary.Skill skill(String code,String label,List<Attempt> attempts) {
        List<Attempt> relevant=attempts.stream().filter(a->code.equals(a.skill())).toList();
        Count overall=new Count(); Map<String,Count> counts=new TreeMap<>();
        for(Attempt attempt:relevant) for(Answer answer:attempt.answers()) {
            overall.add(answer); counts.computeIfAbsent(answer.level()+"|"+answer.tag(),ignored->new Count()).add(answer);
        }
        List<LearningSummary.Competency> competencies=counts.entrySet().stream().map(entry->{
            Count count=entry.getValue(); String status=count.total<5?"MORE_DATA":count.percentage()<70?"REVIEW":"ON_TRACK";
            String[] key=entry.getKey().split("\\|");
            return new LearningSummary.Competency(key[1],TAGS.get(key[1]),key[0],displayLevel(key[0]),count.total,count.correct,count.percentage(),status,
                    switch(status){case "MORE_DATA"->"Cần thêm dữ liệu";case "REVIEW"->"Nên ôn lại";default->"Đang làm tốt";});
        }).sorted(Comparator.comparingInt((LearningSummary.Competency c)->"REVIEW".equals(c.status())?0:1)
                .thenComparingDouble(LearningSummary.Competency::accuracy)).toList();
        String level=null,basis="Chưa có bài đầu vào hoặc cuối chặng đủ dữ liệu. Bài luyện chưa dùng để kết luận mức học.";
        Attempt evidence=null;
        // The newest complete assessment is the basis, never the highest historical score.
        for(Attempt candidate:relevant) {
            if("PLACEMENT".equals(candidate.role())) {
                Map<String,Count> byLevel=new HashMap<>();
                for(Answer answer:candidate.answers()) byLevel.computeIfAbsent(answer.level(),ignored->new Count()).add(answer);
                Count beginner=byLevel.getOrDefault("BEGINNER",new Count());
                Count intermediate=byLevel.getOrDefault("INTERMEDIATE",new Count());
                Count advanced=byLevel.getOrDefault("ADVANCED",new Count());
                if(beginner.total<3 || intermediate.total<3 || advanced.total<3) continue;
                evidence=candidate;
                if((!beginner.placementPass() && (intermediate.placementPass() || advanced.placementPass()))
                        || (!intermediate.placementPass() && advanced.placementPass())) {
                    basis="Kết quả giữa các mức chưa nhất quán. Nên làm bài đánh giá bổ sung trước khi chọn mức học.";
                } else {
                    level=!beginner.placementPass()?"BEGINNER":!intermediate.placementPass()?"INTERMEDIATE":"ADVANCED";
                    basis="Bài đầu vào gần nhất: Beginner "+beginner.correct+"/"+beginner.total+", Intermediate "+intermediate.correct+"/"+intermediate.total+", Advanced "+advanced.correct+"/"+advanced.total+". Đây là mức học đề xuất, không phải chứng nhận trình độ.";
                }
                break;
            }
            if("FINAL".equals(candidate.role()) && candidate.answers().size()>=6
                    && candidate.answers().size()==candidate.result().getTotalQuestions()) {
                Set<String> levels=new HashSet<>();Count count=new Count();
                for(Answer answer:candidate.answers()) { levels.add(answer.level()); count.add(answer); }
                if(levels.size()!=1) continue;
                evidence=candidate;String assessed=levels.iterator().next();
                Double configured=candidate.result().getLesson().getPassScore();
                double threshold=configured==null || configured<=0?8.0:configured;
                boolean passed=count.correct*10.0/count.total>=threshold;
                level=passed?switch(assessed){case "BEGINNER"->"INTERMEDIATE";default->"ADVANCED";}:assessed;
                basis="Bài cuối chặng "+displayLevel(assessed)+": "+count.correct+"/"+count.total+" câu đúng. "+
                        (passed?"Đạt ngưỡng "+threshold+"/10; mức học tiếp theo được đề xuất là "+displayLevel(level)+".":"Chưa đạt ngưỡng "+threshold+"/10; đề xuất ôn lại chặng hiện tại.");
                break;
            }
        }
        return new LearningSummary.Skill(code,label,level,displayLevel(level),basis,
                evidence==null?null:evidence.result().getLesson().getId(),evidence==null?null:evidence.result().getId(),
                overall.total,overall.correct,overall.percentage(),competencies);
    }
    private static String displayLevel(String level) {
        return level==null?"Chưa đủ dữ liệu":switch(level){case "BEGINNER"->"Beginner";case "INTERMEDIATE"->"Intermediate";default->"Advanced";};
    }
}
