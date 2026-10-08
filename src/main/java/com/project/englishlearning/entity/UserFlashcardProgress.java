package com.project.englishlearning.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_flashcard_progress", uniqueConstraints = @UniqueConstraint(name="uq_user_flashcard_progress_pair", columnNames={"user_id","flashcard_id"}))
public class UserFlashcardProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flashcard_id", nullable = false)
    private Flashcard flashcard;

    @Column(name = "is_flipped")
    private Boolean isFlipped = false;

    @Column(name = "flipped_at")
    private LocalDateTime flippedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Flashcard getFlashcard() { return flashcard; }
    public void setFlashcard(Flashcard flashcard) { this.flashcard = flashcard; }
    public Boolean getIsFlipped() { return isFlipped; }
    public void setIsFlipped(Boolean isFlipped) { this.isFlipped = isFlipped; }
    public LocalDateTime getFlippedAt() { return flippedAt; }
    public void setFlippedAt(LocalDateTime flippedAt) { this.flippedAt = flippedAt; }
}
