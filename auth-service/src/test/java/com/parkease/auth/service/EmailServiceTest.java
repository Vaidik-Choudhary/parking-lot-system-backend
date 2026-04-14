package com.parkease.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@parkease.com");
        ReflectionTestUtils.setField(emailService, "frontendUrl", "http://localhost:5173");
    }

    @Test
    void shouldSendPasswordResetEmailSuccessfully() {
        emailService.sendPasswordResetEmail("vaidik@test.com", "token123");

        ArgumentCaptor<SimpleMailMessage> captor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);

        verify(mailSender).send(captor.capture());

        SimpleMailMessage message = captor.getValue();

        assertEquals("noreply@parkease.com", message.getFrom());
        assertEquals("vaidik@test.com", message.getTo()[0]);
        assertEquals("ParkEase — Reset Your Password", message.getSubject());
        assertTrue(message.getText().contains("token123"));
        assertTrue(message.getText().contains("http://localhost:5173/reset-password"));
    }

    @Test
    void shouldSendWelcomeEmailSuccessfully() {
        emailService.sendWelcomeEmail("vaidik@test.com", "Vaidik");

        ArgumentCaptor<SimpleMailMessage> captor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);

        verify(mailSender).send(captor.capture());

        SimpleMailMessage message = captor.getValue();

        assertEquals("noreply@parkease.com", message.getFrom());
        assertEquals("vaidik@test.com", message.getTo()[0]);
        assertEquals("Welcome to ParkEase!", message.getSubject());
        assertTrue(message.getText().contains("Hi Vaidik"));
        assertTrue(message.getText().contains("Welcome to ParkEase"));
    }

    @Test
    void shouldHandleMailFailureGracefullyForResetEmail() {
        doThrow(new RuntimeException("SMTP error"))
                .when(mailSender)
                .send(any(SimpleMailMessage.class));

        assertDoesNotThrow(() ->
                emailService.sendPasswordResetEmail("vaidik@test.com", "token123"));
    }

    @Test
    void shouldHandleMailFailureGracefullyForWelcomeEmail() {
        doThrow(new RuntimeException("SMTP error"))
                .when(mailSender)
                .send(any(SimpleMailMessage.class));

        assertDoesNotThrow(() ->
                emailService.sendWelcomeEmail("vaidik@test.com", "Vaidik"));
    }
}