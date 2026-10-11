package com.project.englishlearning.service;

import com.project.englishlearning.entity.*;
import com.project.englishlearning.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.LocalDateTime;
import java.util.*;

/** One database transaction lock serializes dispatch across requests and application instances. */
@Service
public class WritingAssignmentService {
    public static final int CAPACITY = 5;
    public static final List<String> OPEN = List.of("WAITING_REVIEW", "ASSIGNED", "IN_REVIEW");
    private final WritingSubmissionRepository submissions;
    private final UserRepository users;
    private final NotificationRepository notifications;
    private final JdbcTemplate jdbc;
    public WritingAssignmentService(WritingSubmissionRepository submissions, UserRepository users,
            NotificationRepository notifications, JdbcTemplate jdbc) {
        this.submissions=submissions;this.users=users;this.notifications=notifications;this.jdbc=jdbc;
    }
    public void lockDispatch() {
        jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<Void>) connection -> {
            try(var statement=connection.prepareStatement("select pg_advisory_xact_lock(81472026)")) { statement.execute(); }
            return null;
        });
    }
    @Transactional
    public void dispatch() {
        lockDispatch();
        List<User> graders=users.findByRoleAndStatusOrderByIdAsc("ROLE_GRADER","ACTIVE");
        Map<Long,Integer> load=new HashMap<>();
        Map<Long,LocalDateTime> last=new HashMap<>();
        for(User g:graders) {
            load.put(g.getId(),0);
            // Historical last allocation is retained after review, preventing low-volume starvation.
            last.put(g.getId(),submissions.findByAssignedGraderIdAndGradingModeOrderBySubmittedAtAsc(g.getId(),"EXPERT").stream()
                .map(WritingSubmission::getAssignedAt).filter(Objects::nonNull).max(LocalDateTime::compareTo).orElse(LocalDateTime.MIN));
        }
        var open=submissions.findByGradingModeAndStatusInOrderBySubmittedAtAscIdAsc("EXPERT",OPEN);
        for(var s:open) {
            User assigned=s.getAssignedGrader();
            if(assigned!=null && (!load.containsKey(assigned.getId()) || Objects.equals(assigned.getId(),s.getUser().getId()))) {
                s.setAssignedGrader(null);s.setAssignedAt(null);s.setReviewStartedAt(null);s.setStatus("WAITING_REVIEW");submissions.save(s);
            } else if(assigned!=null) load.merge(assigned.getId(),1,Integer::sum);
        }
        for(var s:open) {
            if(s.getAssignedGrader()!=null) continue;
            User chosen=graders.stream().filter(g -> load.get(g.getId())<CAPACITY && !Objects.equals(g.getId(),s.getUser().getId()))
                .min(Comparator.comparingInt((User g) -> load.get(g.getId())).thenComparing(g -> last.get(g.getId())).thenComparing(User::getId)).orElse(null);
            if(chosen==null) continue;
            var now=LocalDateTime.now();
            s.setAssignedGrader(chosen);s.setAssignedAt(now);s.setStatus("ASSIGNED");submissions.save(s);
            load.merge(chosen.getId(),1,Integer::sum);last.put(chosen.getId(),now);
            Notification n=new Notification();n.setUser(chosen);n.setTitle("Bạn được phân công chấm bài Writing");
            n.setMessage(s.getUser().getFullName()+" · "+s.getLesson().getTitle()+" · Lần "+s.getExpertAttempt());
            n.setUrl("/grader/writing/reviews/"+s.getId());notifications.save(n);
        }
    }
}
