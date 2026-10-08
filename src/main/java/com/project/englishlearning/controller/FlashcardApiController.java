package com.project.englishlearning.controller;

import com.project.englishlearning.entity.Flashcard;
import com.project.englishlearning.entity.Notification;
import com.project.englishlearning.entity.User;
import com.project.englishlearning.entity.UserFlashcardProgress;
import com.project.englishlearning.repository.FlashcardRepository;
import com.project.englishlearning.repository.NotificationRepository;
import com.project.englishlearning.repository.UserFlashcardProgressRepository;
import com.project.englishlearning.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/flashcards")
public class FlashcardApiController {

    private final UserFlashcardProgressRepository progressRepository;
    private final FlashcardRepository flashcardRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    public FlashcardApiController(UserFlashcardProgressRepository progressRepository,
                                  FlashcardRepository flashcardRepository,
                                  UserRepository userRepository,
                                  NotificationRepository notificationRepository) {
        this.progressRepository = progressRepository;
        this.flashcardRepository = flashcardRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
    }

    @org.springframework.transaction.annotation.Transactional
    @PostMapping("/{flashcardId}/flip")
    public ResponseEntity<?> flipFlashcard(@PathVariable Long flashcardId, Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return ResponseEntity.status(401).build();

        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        userRepository.lockForWriting(user.getId());
        Flashcard flashcard = flashcardRepository.findById(flashcardId).orElseThrow();

        UserFlashcardProgress progress = progressRepository.findByUserIdAndFlashcardId(user.getId(), flashcardId)
                .orElseGet(() -> {
                    UserFlashcardProgress newProg = new UserFlashcardProgress();
                    newProg.setUser(user);
                    newProg.setFlashcard(flashcard);
                    return newProg;
                });

        if (!Boolean.TRUE.equals(progress.getIsFlipped())) {
            progress.setIsFlipped(true);
            progress.setFlippedAt(LocalDateTime.now());
            progressRepository.save(progress);
        }

        Long courseId = flashcard.getCourse().getId();
        long totalFlashcards = flashcardRepository.countByCourseId(courseId);
        long flippedCount = progressRepository.countByUserIdAndFlashcardCourseIdAndIsFlippedTrue(user.getId(), courseId);

        boolean allCompleted = (flippedCount >= totalFlashcards && totalFlashcards > 0);
        boolean notificationCreated = false;

        if (allCompleted) {
            // Check if notification already exists to avoid duplicate
            String notifUrl = "/courses/" + courseId + "/vocabulary-quiz";
            boolean exists = notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                    .anyMatch(n -> notifUrl.equals(n.getUrl()));
            
            if (!exists) {
                Notification notif = new Notification();
                notif.setUser(user);
                notif.setTitle("Đã mở khóa Kiểm tra Từ vựng!");
                notif.setMessage("Bạn đã học xong toàn bộ từ vựng của khóa học " + flashcard.getCourse().getTitle() + ".");
                notif.setUrl(notifUrl);
                notificationRepository.save(notif);
                notificationCreated = true;
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("allCompleted", allCompleted);
        response.put("notificationCreated", notificationCreated);
        response.put("total", totalFlashcards);
        response.put("flipped", flippedCount);

        return ResponseEntity.ok(response);
    }
}
