package com.project.englishlearning.service;

import com.project.englishlearning.entity.EmailVerification;
import com.project.englishlearning.entity.User;
import com.project.englishlearning.repository.EmailVerificationRepository;
import com.project.englishlearning.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.HexFormat;

@Service
public class EmailVerificationService {
    private final EmailVerificationRepository verifications;
    private final UserRepository users;
    private final VerificationEmailSender mail;
    private final PasswordEncoder encoder;
    private final SecureRandom random = new SecureRandom();
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public EmailVerificationService(EmailVerificationRepository verifications, UserRepository users,
            VerificationEmailSender mail, PasswordEncoder encoder) {
        this(verifications, users, mail, encoder, Clock.systemUTC());
    }
    EmailVerificationService(EmailVerificationRepository verifications, UserRepository users,
            VerificationEmailSender mail, PasswordEncoder encoder, Clock clock) {
        this.verifications = verifications;
        this.users = users;
        this.mail = mail;
        this.encoder = encoder;
        this.clock = clock;
    }
    public static String normalize(String email) {
        return email == null ? "" : email.strip().toLowerCase(java.util.Locale.ROOT);
    }
    public static boolean validEmail(String email) {
        return email.length() <= 100 && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }
    private static String sessionHash(String sessionId) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(sessionId.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }

    @Transactional
    public String sendCode(String email, String sessionId) {
        email = normalize(email);
        if (!validEmail(email)) return "Vui lòng nhập địa chỉ email hợp lệ.";
        if (users.existsByEmailIgnoreCase(email)) return "Email này đã được đăng ký. Vui lòng đăng nhập.";
        Instant now = clock.instant();
        EmailVerification verification = verifications.findForUpdate(email).orElseGet(EmailVerification::new);
        if (verification.getLastSentAt() != null && now.isBefore(verification.getLastSentAt().plusSeconds(60)))
            return "Vui lòng chờ 60 giây giữa các lần gửi mã.";
        if (verification.getWindowStart() == null || !now.isBefore(verification.getWindowStart().plusSeconds(3600))) {
            verification.setWindowStart(now);
            verification.setSendCount(0);
        }
        if (verification.getSendCount() >= 5) return "Bạn đã gửi mã quá nhiều lần. Vui lòng thử lại sau một giờ.";
        String code = String.format(java.util.Locale.ROOT, "%06d", random.nextInt(1_000_000));
        verification.setEmail(email);
        verification.setCodeHash(encoder.encode(code));
        verification.setSessionHash(sessionHash(sessionId));
        verification.setExpiresAt(now.plusSeconds(600));
        verification.setLastSentAt(now);
        verification.setSendCount(verification.getSendCount() + 1);
        verification.setAttempts(0);
        // Flush before sending so competing first requests cannot both email a code.
        // A mail failure rolls this transaction back, preserving any previous code.
        verifications.saveAndFlush(verification);
        mail.send(email, code);
        return null;
    }

    /** Return an error without throwing so failed attempts are committed. */
    @Transactional
    public String createVerifiedUser(User user, String code, String sessionId) {
        EmailVerification verification = verifications.findForUpdate(user.getEmail()).orElse(null);
        if (verification == null || verification.getCodeHash() == null ||
                !sessionHash(sessionId).equals(verification.getSessionHash()))
            return "Hãy gửi mã xác nhận tới email của bạn trước khi đăng ký.";
        if (!clock.instant().isBefore(verification.getExpiresAt())) return "Mã đã hết hạn. Vui lòng gửi mã mới.";
        if (verification.getAttempts() >= 5) return "Bạn đã nhập sai mã 5 lần. Vui lòng gửi mã mới.";
        if (code == null || !code.matches("[0-9]{6}") || !encoder.matches(code, verification.getCodeHash())) {
            verification.setAttempts(verification.getAttempts() + 1);
            verifications.save(verification);
            return "Mã xác nhận không đúng. Vui lòng kiểm tra email và nhập lại.";
        }
        users.saveAndFlush(user);
        verification.setCodeHash(null);
        verifications.save(verification);
        return null;
    }
}
