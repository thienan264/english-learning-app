package com.project.englishlearning.service;

import com.project.englishlearning.entity.*;
import com.project.englishlearning.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ExpertWritingService {
    private final WritingSubmissionRepository submissions;
    private final UserRepository users;
    private final WritingExamRepository exams;
    private final NotificationRepository notifications;
    private final CourseAccessService access;
    public ExpertWritingService(WritingSubmissionRepository submissions, UserRepository users,
            WritingExamRepository exams, NotificationRepository notifications, CourseAccessService access) {
        this.submissions=submissions;this.users=users;this.exams=exams;this.notifications=notifications;this.access=access;
    }
    public record Availability(boolean eligible, int used, boolean waiting) {
        public boolean isCanSubmit() { return eligible && used < 2 && !waiting; }
        public String getLabel() {
            if(waiting) return "Đang chờ phản hồi lần " + used;
            if(used >= 2) return "Đã dùng hết 2 lần chấm chuyên gia";
            return "Gửi chuyên gia chấm · Lần " + (used+1) + "/2";
        }
    }
    public Availability availability(User user, Lesson lesson) {
        Course course=course(lesson);
        boolean eligible=user != null && course != null && Boolean.FALSE.equals(course.getIsFree()) && access.canLearn(user,course);
        List<WritingSubmission> history=user == null ? List.of() : submissions.findByUserIdAndLessonIdAndGradingModeOrderBySubmittedAtAsc(user.getId(),lesson.getId(),"EXPERT");
        return new Availability(eligible,history.size(),history.stream().anyMatch(s -> !"REVIEWED".equals(s.getStatus())));
    }
    private Course course(Lesson lesson) { return lesson.getModule() != null ? lesson.getModule().getCourse() : lesson.getCourse(); }
    private void requireWriting(Lesson lesson) {
        if(!"WRITING".equals(lesson.getSkillType()) || !"MOCK_TEST".equals(lesson.getLessonType()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Bài học này không phải bài Writing.");
    }
    private String text(String value, int limit) {
        String result=value == null ? "" : value.strip();
        if(result.length()>limit) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Nội dung quá dài.");
        return result;
    }
    @Transactional
    public WritingSubmission submit(User user, Lesson lesson, String task1, String task2, String legacy) {
        users.lockForWriting(user.getId()).orElseThrow(); // Serializes quota checks across tabs/requests.
        requireWriting(lesson);
        Availability state=availability(user,lesson);
        if(!state.eligible()) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Chấm chuyên gia dành cho học viên có quyền học khóa trả phí.");
        if(!state.isCanSubmit()) throw new ResponseStatusException(HttpStatus.CONFLICT,state.getLabel());
        task1=text(task1,20000);task2=text(task2,20000);legacy=text(legacy,20000);
        WritingExam exam=exams.findByLessonId(lesson.getId()).orElse(null);
        if(exam == null && legacy.isEmpty()) legacy=task1 + (task2.isEmpty() ? "" : "\n\n"+task2);
        if((exam == null && legacy.isBlank()) || (exam != null && task1.isBlank() && task2.isBlank()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Vui lòng viết bài trước khi nộp.");
        WritingSubmission s=new WritingSubmission();s.setUser(user);s.setLesson(lesson);s.setWritingExam(exam);
        s.setGradingMode("EXPERT");s.setExpertAttempt(state.used()+1);s.setStatus("WAITING_REVIEW");
        s.setTask1Essay(task1);s.setTask2Essay(task2);s.setSubmissionText(exam == null ? legacy : task1+"\n---\n"+task2);
        s.setPromptSnapshot(exam == null ? lesson.getContent() :
            "TASK 1:\n"+(exam.getTask1() == null ? "" : exam.getTask1().getInstruction())+
            "\n\nTASK 2:\n"+(exam.getTask2() == null ? "" : exam.getTask2().getInstruction()));
        return submissions.saveAndFlush(s);
    }
    public record Annotation(int task, int start, int end, String original, String correction, String explanation) {}
    public record TaskReview(Double band, String feedback, String corrections, String suggested) {}
    public record Review(TaskReview task1, TaskReview task2, List<Annotation> annotations) {}
    private final com.fasterxml.jackson.databind.ObjectMapper mapper=new com.fasterxml.jackson.databind.ObjectMapper();
    public Review review(WritingSubmission submission) {
        if(submission.getExpertReviewJson() == null) return null;
        try { return mapper.readValue(submission.getExpertReviewJson(),Review.class); }
        catch(com.fasterxml.jackson.core.JsonProcessingException ex) { throw new IllegalStateException("Không đọc được phản hồi Writing",ex); }
    }
    private void validateBand(Double band) {
        if(band == null || !Double.isFinite(band) || band<0 || band>9 || band*2 != Math.rint(band*2))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Band phải từ 0 đến 9, bước 0.5.");
    }
    private TaskReview taskReview(String essay, Double band, String feedback, String corrections, String suggested) {
        if(essay.isBlank()) return null;
        validateBand(band);feedback=text(feedback,20000);corrections=text(corrections,20000);suggested=text(suggested,40000);
        if(feedback.isBlank() || suggested.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Điền điểm, nhận xét và bài gợi ý cho từng Task đã nộp.");
        return new TaskReview(band,feedback,corrections,suggested);
    }
    private List<Annotation> annotations(String json, WritingSubmission submission) {
        if(json == null || json.isBlank()) return List.of();
        if(json.length()>200000) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Quá nhiều nội dung đánh dấu.");
        List<Annotation> parsed;
        try { parsed=mapper.readValue(json,mapper.getTypeFactory().constructCollectionType(List.class,Annotation.class)); }
        catch(com.fasterxml.jackson.core.JsonProcessingException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Đánh dấu lỗi không hợp lệ."); }
        if(parsed == null || parsed.size()>100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Tối đa 100 lỗi được đánh dấu.");
        var cleaned=new java.util.ArrayList<Annotation>();
        for(Annotation a:parsed) {
            if(a == null || (a.task()!=1 && a.task()!=2)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Task đánh dấu không hợp lệ.");
            String essay=a.task()==1 ? submission.getExpertTask1Text() : submission.getExpertTask2Text();
            if(a.start()<0 || a.end()<=a.start() || a.end()>essay.length() || !essay.substring(a.start(),a.end()).equals(a.original()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Đoạn đánh dấu không khớp bài viết gốc.");
            if(cleaned.stream().anyMatch(b -> b.task()==a.task() && a.start()<b.end() && a.end()>b.start()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Các đoạn đánh dấu không được chồng lên nhau.");
            String correction=text(a.correction(),5000),explanation=text(a.explanation(),5000);
            if(correction.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Nhập câu sửa cho đoạn đã đánh dấu.");
            cleaned.add(new Annotation(a.task(),a.start(),a.end(),a.original(),correction,explanation));
        }
        cleaned.sort(java.util.Comparator.comparingInt(Annotation::task).thenComparingInt(Annotation::start));
        return cleaned;
    }
    @Transactional
    public void respondTasks(Long id, String reviewer, com.project.englishlearning.dto.ExpertWritingReviewForm form) {
        WritingSubmission s=pending(id);
        TaskReview t1=taskReview(s.getExpertTask1Text(),form.getTask1Band(),form.getTask1Feedback(),form.getTask1Corrections(),form.getTask1Suggested());
        TaskReview t2=taskReview(s.getExpertTask2Text(),form.getTask2Band(),form.getTask2Feedback(),form.getTask2Corrections(),form.getTask2Suggested());
        if(t1 == null && t2 == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Bài nộp không có nội dung.");
        Review review=new Review(t1,t2,annotations(form.getAnnotationsJson(),s));
        try { s.setExpertReviewJson(mapper.writeValueAsString(review)); }
        catch(com.fasterxml.jackson.core.JsonProcessingException ex) { throw new IllegalStateException(ex); }
        s.setTask1Overall(t1 == null ? null : t1.band());s.setTask2Overall(t2 == null ? null : t2.band());
        double overall=t1 == null ? t2.band() : (t2 == null ? t1.band() : Math.round((t1.band()+2*t2.band())/3*2)/2.0);
        s.setBandScore(overall);
        finish(s,reviewer);
    }
    private WritingSubmission pending(Long id) {
        WritingSubmission s=submissions.lockForReview(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if(!"EXPERT".equals(s.getGradingMode())) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        if(!"WAITING_REVIEW".equals(s.getStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT,"Bài này đã được phản hồi.");
        return s;
    }
    private void finish(WritingSubmission s, String reviewer) {
        s.setReviewedBy(reviewer);s.setEvaluatedAt(LocalDateTime.now());s.setStatus("REVIEWED");submissions.save(s);
        Notification n=new Notification();n.setUser(s.getUser());n.setTitle("Đã có phản hồi Writing từ chuyên gia");
        n.setMessage(s.getLesson().getTitle()+" · Lần "+s.getExpertAttempt()+" · Band "+s.getBandScore());
        n.setUrl("/lessons/"+s.getLesson().getId()+"/writing/result/"+s.getId());notifications.save(n);
    }
}
