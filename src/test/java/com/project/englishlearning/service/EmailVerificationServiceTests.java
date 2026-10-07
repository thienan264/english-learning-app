package com.project.englishlearning.service;

import com.project.englishlearning.entity.*;
import com.project.englishlearning.repository.*;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.time.*;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmailVerificationServiceTests {
    EmailVerificationRepository records;
    UserRepository users;
    VerificationEmailSender mail;
    EmailVerificationService service;
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    final Instant now = Instant.parse("2026-10-07T08:00:00Z");
    private <T> T fake(Class<T> type) { return mock(type, withSettings().mockMaker(org.mockito.MockMakers.SUBCLASS)); }
    @BeforeEach void setup() {
        records = fake(EmailVerificationRepository.class); users = fake(UserRepository.class);
        mail = fake(VerificationEmailSender.class);
        service = new EmailVerificationService(records, users, mail, encoder, Clock.fixed(now, ZoneOffset.UTC));
        when(records.findForUpdate(anyString())).thenReturn(Optional.empty());
    }
    User user(String email) { var u = new User(); u.setEmail(email); return u; }
    EmailVerification issue() {
        assertNull(service.sendCode("student@example.com", "session-1"));
        var record = ArgumentCaptor.forClass(EmailVerification.class);
        verify(records).saveAndFlush(record.capture());
        when(records.findForUpdate("student@example.com")).thenReturn(Optional.of(record.getValue()));
        return record.getValue();
    }
    String sentCode() {
        var code = ArgumentCaptor.forClass(String.class);
        verify(mail).send(eq("student@example.com"), code.capture());
        return code.getValue();
    }
    @Test void codeIsEmailedHashedAndConsumedOnlyOnce() {
        var record = issue(); var code = sentCode();
        assertTrue(code.matches("[0-9]{6}"));
        assertNotEquals(code, record.getCodeHash());
        assertTrue(encoder.matches(code, record.getCodeHash()));
        assertEquals(now.plusSeconds(600), record.getExpiresAt());
        assertNull(service.createVerifiedUser(user("student@example.com"), code, "session-1"));
        verify(users).saveAndFlush(any(User.class));
        assertNull(record.getCodeHash());
        assertNotNull(service.createVerifiedUser(user("student@example.com"), code, "session-1"));
        verify(users, times(1)).saveAndFlush(any(User.class));
    }
    @Test void codeIsBoundToEmailAndBrowserSession() {
        issue(); var code = sentCode();
        assertNotNull(service.createVerifiedUser(user("student@example.com"), code, "another-session"));
        assertNotNull(service.createVerifiedUser(user("other@example.com"), code, "session-1"));
        verify(users, never()).saveAndFlush(any());
    }
    @Test void expiredCodeCannotCreateAccount() {
        var record = issue(); record.setExpiresAt(now);
        assertNotNull(service.createVerifiedUser(user("student@example.com"), sentCode(), "session-1"));
        verify(users, never()).saveAndFlush(any());
    }
    @Test void fiveWrongAttemptsBlockEvenTheCorrectCode() {
        var record = issue(); var code = sentCode();
        for (int i = 0; i < 5; i++) assertNotNull(service.createVerifiedUser(user("student@example.com"), "invalid", "session-1"));
        assertEquals(5, record.getAttempts());
        assertNotNull(service.createVerifiedUser(user("student@example.com"), code, "session-1"));
        verify(users, never()).saveAndFlush(any());
    }
    @Test void resendCooldownAndHourlyLimitApplyAcrossSessions() {
        var record = issue();
        assertNotNull(service.sendCode("student@example.com", "session-2"));
        verify(mail, times(1)).send(anyString(), anyString());
        record.setLastSentAt(now.minusSeconds(61)); record.setSendCount(5);
        assertNotNull(service.sendCode("student@example.com", "session-2"));
        verify(mail, times(1)).send(anyString(), anyString());
    }
    @Test void resendInvalidatesPreviousCodeAndMovesSessionBinding() {
        var record = issue(); String oldHash = record.getCodeHash();
        record.setLastSentAt(now.minusSeconds(61));
        assertNull(service.sendCode("student@example.com", "session-2"));
        assertNotEquals(oldHash, record.getCodeHash());
        var codes = ArgumentCaptor.forClass(String.class);
        verify(mail, times(2)).send(eq("student@example.com"), codes.capture());
        String newest = codes.getAllValues().get(1);
        assertNotNull(service.createVerifiedUser(user("student@example.com"), newest, "session-1"));
        assertNull(service.createVerifiedUser(user("student@example.com"), newest, "session-2"));
    }
    @Test void mailFailurePropagatesToRollBackTheTransaction() {
        doThrow(new org.springframework.mail.MailSendException("unavailable")).when(mail).send(anyString(), anyString());
        assertThrows(org.springframework.mail.MailSendException.class, () -> service.sendCode("student@example.com", "session-1"));
        verify(records, never()).save(any()); verify(users, never()).saveAndFlush(any());
    }
    @Test void competingFirstRequestCannotSendBeforePersistenceSucceeds() {
        when(records.saveAndFlush(any())).thenThrow(new org.springframework.dao.DataIntegrityViolationException("duplicate"));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
            () -> service.sendCode("student@example.com", "session-1"));
        verifyNoInteractions(mail);
    }
    @Test void invalidEmailCannotSendMail() {
        assertNotNull(service.sendCode("bad-email", "session-1"));
        verifyNoInteractions(mail, records);
    }
}
