package com.project.englishlearning;

import com.project.englishlearning.entity.*;
import com.project.englishlearning.repository.*;
import com.project.englishlearning.service.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.jupiter.api.*;
import java.util.*;
import java.time.LocalDateTime;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class WritingAssignmentTests {
    static <T> T fake(Class<T> type) { return mock(type,withSettings().mockMaker(org.mockito.MockMakers.SUBCLASS)); }
    WritingSubmissionRepository submissions=fake(WritingSubmissionRepository.class);
    UserRepository users=fake(UserRepository.class);
    NotificationRepository notifications=fake(NotificationRepository.class);
    WritingAssignmentService service=new WritingAssignmentService(submissions,users,notifications,fake(JdbcTemplate.class));
    User user(long id) { var u=new User();u.setId(id);u.setUsername("user"+id);u.setFullName("Name "+id);u.setRole("ROLE_GRADER");u.setStatus("ACTIVE");return u; }
    WritingSubmission row(long id,User assigned) { var s=new WritingSubmission();s.setId(id);s.setUser(user(99));var l=new Lesson();l.setTitle("Writing");s.setLesson(l);s.setExpertAttempt(1);s.setStatus(assigned==null ? "WAITING_REVIEW" : "ASSIGNED");s.setAssignedGrader(assigned);return s; }
    void queue(List<User> graders,List<WritingSubmission> rows) { when(users.findByRoleAndStatusOrderByIdAsc("ROLE_GRADER","ACTIVE")).thenReturn(graders);when(submissions.findByGradingModeAndStatusInOrderBySubmittedAtAscIdAsc("EXPERT",WritingAssignmentService.OPEN)).thenReturn(rows); }
    @Test void manySubmissionsBalanceAtCapacityAndExcessRemainQueued() {
        var a=user(1);var b=user(2);var rows=new ArrayList<WritingSubmission>();for(int i=1;i<=13;i++) rows.add(row(i,null));queue(List.of(a,b),rows);service.dispatch();
        assertEquals(5,rows.stream().filter(s -> s.getAssignedGrader()==a).count());assertEquals(5,rows.stream().filter(s -> s.getAssignedGrader()==b).count());assertEquals(3,rows.stream().filter(s -> s.getAssignedGrader()==null).count());verify(notifications,times(10)).save(any());assertEquals(a,rows.get(0).getAssignedGrader());assertNull(rows.get(12).getAssignedGrader());
    }
    @Test void equalEmptyLoadsRotateUsingLastHistoricalAssignment() {
        var a=user(1);var b=user(2);var old=row(10,a);old.setAssignedAt(LocalDateTime.now());when(submissions.findByAssignedGraderIdAndGradingModeOrderBySubmittedAtAsc(1L,"EXPERT")).thenReturn(List.of(old));var next=row(11,null);queue(List.of(a,b),List.of(next));service.dispatch();assertEquals(b,next.getAssignedGrader());
    }
    @Test void leastBusyReceivesNextAndDispatchDoesNotDuplicateNotifications() {
        var a=user(1);var b=user(2);var one=row(1,a);var two=row(2,a);var three=row(3,b);var next=row(4,null);queue(List.of(a,b),List.of(one,two,three,next));service.dispatch();assertEquals(b,next.getAssignedGrader());service.dispatch();verify(notifications,times(1)).save(any());
    }
    @Test void lockedOrRevokedGraderUnfinishedWorkIsReallocated() {
        var former=user(1);former.setStatus("LOCKED");var active=user(2);var s=row(1,former);s.setStatus("IN_REVIEW");s.setReviewStartedAt(LocalDateTime.now());queue(List.of(active),List.of(s));service.dispatch();assertEquals(active,s.getAssignedGrader());assertEquals("ASSIGNED",s.getStatus());assertNull(s.getReviewStartedAt());verify(notifications).save(any());
    }
    @Test void noGraderPreservesQueueWithoutNotification() { var s=row(1,null);queue(List.of(),List.of(s));service.dispatch();assertNull(s.getAssignedGrader());assertEquals("WAITING_REVIEW",s.getStatus());verifyNoInteractions(notifications); }
    @Test void graderCannotBeAllocatedTheirOwnSubmission() { var a=user(1);var s=row(1,null);s.setUser(a);queue(List.of(a),List.of(s));service.dispatch();assertNull(s.getAssignedGrader());verifyNoInteractions(notifications); }
    @Test void completingWorkFreesSlotForOldestQueuedSubmission() {
        var a=user(1);var rows=new ArrayList<WritingSubmission>();for(int i=0;i<5;i++) rows.add(row(i,a));var next=row(6,null);rows.add(next);queue(List.of(a),rows);service.dispatch();assertNull(next.getAssignedGrader());rows.remove(0);service.dispatch();assertEquals(a,next.getAssignedGrader());verify(notifications).save(any());
    }
}
