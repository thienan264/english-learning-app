package com.project.englishlearning.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "email_verifications")
public class EmailVerification {
    @Id @Column(length = 100)
    private String email;
    private String codeHash;
    private String sessionHash;
    private Instant expiresAt;
    private Instant lastSentAt;
    private Instant windowStart;
    private int sendCount;
    private int attempts;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getCodeHash() { return codeHash; }
    public void setCodeHash(String value) { codeHash = value; }
    public String getSessionHash() { return sessionHash; }
    public void setSessionHash(String value) { sessionHash = value; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant value) { expiresAt = value; }
    public Instant getLastSentAt() { return lastSentAt; }
    public void setLastSentAt(Instant value) { lastSentAt = value; }
    public Instant getWindowStart() { return windowStart; }
    public void setWindowStart(Instant value) { windowStart = value; }
    public int getSendCount() { return sendCount; }
    public void setSendCount(int value) { sendCount = value; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int value) { attempts = value; }
}
