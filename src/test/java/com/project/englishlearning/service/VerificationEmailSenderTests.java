package com.project.englishlearning.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class VerificationEmailSenderTests {
    @Test void sendsCodeToRequestedRecipientFromConfiguredAddress() {
        JavaMailSender mailer = mock(JavaMailSender.class, withSettings().mockMaker(org.mockito.MockMakers.SUBCLASS));
        var factory = new StaticListableBeanFactory(java.util.Map.of("mailer", mailer));
        var sender = new VerificationEmailSender(factory.getBeanProvider(JavaMailSender.class),
            "phamanmap2004@gmail.com", "test-credential");
        sender.send("student@example.com", "012345");
        var message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailer).send(message.capture());
        assertEquals("phamanmap2004@gmail.com", message.getValue().getFrom());
        assertArrayEquals(new String[]{"student@example.com"}, message.getValue().getTo());
        assertTrue(message.getValue().getText().contains("012345"));
        assertTrue(message.getValue().getText().contains("10 phút"));
    }
    @Test void missingPasswordDoesNotAttemptSmtp() {
        JavaMailSender mailer = mock(JavaMailSender.class, withSettings().mockMaker(org.mockito.MockMakers.SUBCLASS));
        var factory = new StaticListableBeanFactory(java.util.Map.of("mailer", mailer));
        var sender = new VerificationEmailSender(factory.getBeanProvider(JavaMailSender.class),
            "phamanmap2004@gmail.com", "");
        assertThrows(IllegalStateException.class, () -> sender.send("student@example.com", "012345"));
        verifyNoInteractions(mailer);
    }
}
