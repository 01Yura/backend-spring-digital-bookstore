package online.ityura.springdigitallibrary.unit.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import online.ityura.springdigitallibrary.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {
    
    @Mock
    private JavaMailSender mailSender;
    
    @Mock
    private MimeMessage mimeMessage;
    
    @InjectMocks
    private EmailService emailService;
    
    @BeforeEach
    void setUp() {
        // Set properties using reflection
        ReflectionTestUtils.setField(emailService, "baseUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@test.com");
    }
    
    @Test
    void testSendVerificationEmail_Success_ShouldSendEmail() throws MessagingException {
        // Given
        String toEmail = "test@example.com";
        String token = "verification-token-123";
        
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));
        
        // When
        emailService.sendVerificationEmail(toEmail, token);
        
        // Then
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(any(MimeMessage.class));
    }
    
    @Test
    void testSendVerificationEmail_MessagingException_ShouldThrowRuntimeException() throws MessagingException {
        // Given
        String toEmail = "test@example.com";
        String token = "verification-token-123";
        
        // MimeMessageHelper can throw MessagingException, so we simulate it by making createMimeMessage throw
        // But since we can't easily mock MimeMessageHelper, we'll test that the method handles exceptions
        // by using doAnswer to simulate an exception during message creation
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doAnswer(invocation -> {
            throw new MessagingException("Failed to send");
        }).when(mailSender).send(any(MimeMessage.class));
        
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> emailService.sendVerificationEmail(toEmail, token));
        
        assertTrue(exception.getMessage().contains("Failed to send verification email"));
        verify(mailSender).createMimeMessage();
    }
    
    @Test
    void testSendPasswordResetEmail_Success_ShouldSendEmail() throws MessagingException {
        // Given
        String toEmail = "test@example.com";
        String token = "reset-token-123";
        
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));
        
        // When
        emailService.sendPasswordResetEmail(toEmail, token);
        
        // Then
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(any(MimeMessage.class));
    }
    
    @Test
    void testSendPasswordResetEmail_MessagingException_ShouldThrowRuntimeException() throws MessagingException {
        // Given
        String toEmail = "test@example.com";
        String token = "reset-token-123";
        
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doAnswer(invocation -> {
            throw new MessagingException("Failed to send");
        }).when(mailSender).send(any(MimeMessage.class));
        
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> emailService.sendPasswordResetEmail(toEmail, token));
        
        assertTrue(exception.getMessage().contains("Failed to send password reset email"));
        verify(mailSender).createMimeMessage();
    }
}
