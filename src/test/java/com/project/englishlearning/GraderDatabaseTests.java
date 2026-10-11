package com.project.englishlearning;

import com.project.englishlearning.entity.*;
import com.project.englishlearning.repository.*;
import com.project.englishlearning.service.*;
import com.project.englishlearning.dto.ExpertWritingReviewForm;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

/** Opt-in integration tests, deliberately isolated from the application's database. */
@EnabledIfEnvironmentVariable(named="RUN_GRADER_DB_TESTS",matches="true")
@SpringBootTest(properties={"spring.datasource.url=jdbc:postgresql://localhost:5432/english_grader_test_20261010","spring.jpa.show-sql=false","writing.assignment.initial-delay-ms=86400000"})
class GraderDatabaseTests {
    @Autowired UserRepository users;@Autowired CourseRepository courses;@Autowired LessonRepository lessons;
    @Autowired WritingSubmissionRepository submissions;@Autowired WritingAssignmentService assignments;@Autowired ExpertWritingService expert;
    @Autowired GraderAccountService accounts;@Autowired JdbcTemplate jdbc;@Autowired PlatformTransactionManager manager;
    @Autowired WebApplicationContext context;
    List<Long> userIds=new ArrayList<>();Long courseId,lessonId;String prefix;
    TransactionTemplate tx;
    @BeforeEach void setup() { tx=new TransactionTemplate(manager);prefix="grading_"+UUID.randomUUID().toString().replace("-","").substring(0,12); }
    User create(String suffix,String role) { User u=new User();u.setUsername(prefix+suffix);u.setEmail(prefix+suffix+"@example.test");u.setFullName("Test "+suffix);u.setRole(role);u.setStatus("ACTIVE");u.setPassword("test-hash");users.saveAndFlush(u);userIds.add(u.getId());return u; }
    List<Long> seed(int count) {
        return tx.execute(status -> {
            var student=create("student","ROLE_STUDENT");create("one","ROLE_GRADER");create("two","ROLE_GRADER");
            var c=new Course();c.setTitle(prefix);c.setIsFree(true);courses.saveAndFlush(c);courseId=c.getId();
            var l=new Lesson();l.setCourse(c);l.setTitle("Writing fixture");l.setSkillType("WRITING");l.setLessonType("MOCK_TEST");lessons.saveAndFlush(l);lessonId=l.getId();
            var ids=new ArrayList<Long>();for(int i=0;i<count;i++) { var s=new WritingSubmission();s.setUser(student);s.setLesson(l);s.setGradingMode("EXPERT");s.setStatus("WAITING_REVIEW");s.setExpertAttempt(1);s.setTask2Essay("A test essay.");s.setSubmittedAt(java.time.LocalDateTime.now().plusSeconds(i));submissions.saveAndFlush(s);ids.add(s.getId()); }return ids;
        });
    }
    @AfterEach void cleanup() {
        tx.executeWithoutResult(s -> {
            if(lessonId!=null) jdbc.update("delete from writing_submissions where lesson_id=?",lessonId);
            for(Long id:userIds) jdbc.update("delete from notifications where user_id=?",id);
            if(lessonId!=null) jdbc.update("delete from lessons where id=?",lessonId);
            if(courseId!=null) jdbc.update("delete from courses where id=?",courseId);
            for(Long id:userIds) jdbc.update("delete from users where id=?",id);
        });
    }
    @Test void concurrentDispatchNeverExceedsCapacityOrDuplicatesNotifications() throws Exception {
        var ids=seed(18);var workers=Executors.newFixedThreadPool(4);var ready=new CountDownLatch(4);var go=new CountDownLatch(1);
        try {
            var futures=new ArrayList<Future<?>>();for(int i=0;i<4;i++) futures.add(workers.submit(() -> { ready.countDown();try { go.await();assignments.dispatch(); } catch(InterruptedException e) { Thread.currentThread().interrupt();throw new RuntimeException(e); } }));
            assertTrue(ready.await(10,TimeUnit.SECONDS));go.countDown();for(var f:futures) f.get(30,TimeUnit.SECONDS);
        } finally { workers.shutdownNow(); }
        assertEquals(10,jdbc.queryForObject("select count(*) from writing_submissions where lesson_id=? and assigned_grader_id is not null",Integer.class,lessonId));
        assertEquals(8,jdbc.queryForObject("select count(*) from writing_submissions where lesson_id=? and status='WAITING_REVIEW'",Integer.class,lessonId));
        assertEquals(10,jdbc.queryForObject("select count(*) from notifications where user_id in (?,?)",Integer.class,userIds.get(1),userIds.get(2)));
        assertEquals(5,jdbc.queryForObject("select max(n) from (select count(*) n from writing_submissions where lesson_id=? group by assigned_grader_id having assigned_grader_id is not null) x",Integer.class,lessonId));
        assertNull(submissions.findById(ids.get(17)).orElseThrow().getAssignedGrader());
    }
    @Test void foreignAssignmentIsForbiddenAndCompletingOwnWorkDispatchesQueuedWork() throws Exception {
        var ids=seed(12);assignments.dispatch();var s=submissions.findById(ids.get(0)).orElseThrow();Long owner=s.getAssignedGrader().getId();
        var grader=users.findById(owner).orElseThrow();var other=users.findById(userIds.stream().filter(id -> !id.equals(userIds.get(0)) && !id.equals(owner)).findFirst().orElseThrow()).orElseThrow();
        var mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        mvc.perform(get("/grader/writing/reviews/"+s.getId()).with(user(other.getUsername()).roles("GRADER"))).andExpect(status().isForbidden());
        mvc.perform(get("/grader/writing/reviews/"+s.getId()).with(user(grader.getUsername()).roles("GRADER"))).andExpect(status().isOk());
        var form=new ExpertWritingReviewForm();form.setTask2Band(6.5);form.setTask2Feedback("Feedback");form.setTask2Suggested("Suggested text");
        var denied=assertThrows(org.springframework.web.server.ResponseStatusException.class,() -> expert.respondTasks(s.getId(),other.getUsername(),form));assertEquals(403,denied.getStatusCode().value());
        expert.startReview(s.getId(),grader.getUsername());assertEquals("IN_REVIEW",submissions.findById(s.getId()).orElseThrow().getStatus());
        expert.respondTasks(s.getId(),grader.getUsername(),form);assertEquals("REVIEWED",submissions.findById(s.getId()).orElseThrow().getStatus());
        assertNotNull(submissions.findById(ids.get(10)).orElseThrow().getAssignedGrader());
        assertEquals(5,jdbc.queryForObject("select count(*) from writing_submissions where assigned_grader_id=? and status in ('ASSIGNED','IN_REVIEW')",Integer.class,owner));
        assertEquals(1,jdbc.queryForObject("select count(*) from notifications where user_id=?",Integer.class,userIds.get(0)));
    }
    @Test void lockedGraderLosesAccessAndUnfinishedAssignmentsReturnToQueue() {
        seed(12);assignments.dispatch();Long locked=userIds.get(1);accounts.change(locked,"lock");
        assertEquals("LOCKED",users.findById(locked).orElseThrow().getStatus());
        assertEquals(0,jdbc.queryForObject("select count(*) from writing_submissions where assigned_grader_id=? and status <> 'REVIEWED'",Integer.class,locked));
        assertEquals(7,jdbc.queryForObject("select count(*) from writing_submissions where lesson_id=? and status='WAITING_REVIEW'",Integer.class,lessonId));
        accounts.change(locked,"unlock");assertEquals(2,jdbc.queryForObject("select count(*) from writing_submissions where lesson_id=? and status='WAITING_REVIEW'",Integer.class,lessonId));
    }
    @Test void adminCreatesRestrictedGraderWithHashedPasswordAndDispatchesWaitingWork() {
        var ids=seed(1);tx.executeWithoutResult(s -> { for(int i=1;i<=2;i++) { var u=users.findById(userIds.get(i)).orElseThrow();u.setStatus("LOCKED");users.save(u); } });
        accounts.create(prefix+"new",prefix+"new@example.test","New Grader","password123");
        var added=users.findByUsername(prefix+"new").orElseThrow();userIds.add(added.getId());assertEquals("ROLE_GRADER",added.getRole());assertTrue(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().matches("password123",added.getPassword()));
        assertEquals(added.getId(),submissions.findById(ids.get(0)).orElseThrow().getAssignedGrader().getId());
    }
}
