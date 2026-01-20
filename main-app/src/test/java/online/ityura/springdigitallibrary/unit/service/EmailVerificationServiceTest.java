package online.ityura.springdigitallibrary.unit.service;

import online.ityura.springdigitallibrary.model.EmailVerificationToken;
import online.ityura.springdigitallibrary.model.Role;
import online.ityura.springdigitallibrary.model.User;
import online.ityura.springdigitallibrary.repository.EmailVerificationTokenRepository;
import online.ityura.springdigitallibrary.repository.UserRepository;
import online.ityura.springdigitallibrary.service.EmailService;
import online.ityura.springdigitallibrary.service.EmailVerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {
    
    @Mock
    private EmailVerificationTokenRepository tokenRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private EmailService emailService;
    
    @InjectMocks
    private EmailVerificationService emailVerificationService;
    
    private User testUser;
    private EmailVerificationToken testToken;
    
    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .nickname("testuser")
                .email("test@example.com")
                .passwordHash("encodedPassword")
                .role(Role.USER)
                .isVerified(false)
                .build();
        
        testToken = EmailVerificationToken.builder()
                .id(1L)
                .token("test-token-123")
                .user(testUser)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .used(false)
                .build();
        
        // Set token expiration hours using reflection
        ReflectionTestUtils.setField(emailVerificationService, "tokenExpirationHours", 24);
    }
    
    @Test
    void testGenerateVerificationToken_Success_ShouldGenerateToken() {
        // Given
        doNothing().when(tokenRepository).deleteByUser(testUser);
        when(tokenRepository.save(any(EmailVerificationToken.class))).thenReturn(testToken);
        
        // When
        String token = emailVerificationService.generateVerificationToken(testUser);
        
        // Then
        assertNotNull(token);
        verify(tokenRepository).deleteByUser(testUser);
        verify(tokenRepository).save(any(EmailVerificationToken.class));
    }
    
    @Test
    void testVerifyEmail_Success_ShouldVerifyUser() {
        // Given
        when(tokenRepository.findByToken("test-token-123")).thenReturn(Optional.of(testToken));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(tokenRepository.save(any(EmailVerificationToken.class))).thenReturn(testToken);
        
        // When
        emailVerificationService.verifyEmail("test-token-123");
        
        // Then
        assertTrue(testUser.getIsVerified());
        assertTrue(testToken.getUsed());
        verify(userRepository).save(testUser);
        verify(tokenRepository).save(testToken);
    }
    
    @Test
    void testVerifyEmail_TokenNotFound_ShouldThrowException() {
        // Given
        when(tokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());
        
        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> emailVerificationService.verifyEmail("invalid-token"));
        
        assertEquals(404, exception.getStatusCode().value());
        assertEquals("Verification token not found", exception.getReason());
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    void testVerifyEmail_TokenAlreadyUsed_ShouldThrowException() {
        // Given
        testToken.setUsed(true);
        when(tokenRepository.findByToken("test-token-123")).thenReturn(Optional.of(testToken));
        
        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> emailVerificationService.verifyEmail("test-token-123"));
        
        assertEquals(410, exception.getStatusCode().value());
        assertEquals("Verification token has already been used", exception.getReason());
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    void testVerifyEmail_TokenExpired_ShouldThrowException() {
        // Given
        testToken.setExpiresAt(LocalDateTime.now().minusHours(1));
        when(tokenRepository.findByToken("test-token-123")).thenReturn(Optional.of(testToken));
        
        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> emailVerificationService.verifyEmail("test-token-123"));
        
        assertEquals(410, exception.getStatusCode().value());
        assertEquals("Verification token has expired", exception.getReason());
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    void testResendVerificationEmail_Success_ShouldResendEmail() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        doNothing().when(tokenRepository).deleteByUser(testUser);
        when(tokenRepository.save(any(EmailVerificationToken.class))).thenReturn(testToken);
        doNothing().when(emailService).sendVerificationEmail(eq("test@example.com"), anyString());
        
        // When
        emailVerificationService.resendVerificationEmail("test@example.com");
        
        // Then
        verify(userRepository).findByEmail("test@example.com");
        verify(tokenRepository).deleteByUser(testUser);
        verify(tokenRepository).save(any(EmailVerificationToken.class));
        verify(emailService).sendVerificationEmail(eq("test@example.com"), anyString());
    }
    
    @Test
    void testResendVerificationEmail_UserNotFound_ShouldThrowException() {
        // Given
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());
        
        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> emailVerificationService.resendVerificationEmail("nonexistent@example.com"));
        
        assertEquals(404, exception.getStatusCode().value());
        assertEquals("User not found", exception.getReason());
        verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
    }
    
    @Test
    void testResendVerificationEmail_AlreadyVerified_ShouldThrowException() {
        // Given
        testUser.setIsVerified(true);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        
        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> emailVerificationService.resendVerificationEmail("test@example.com"));
        
        assertEquals(400, exception.getStatusCode().value());
        assertEquals("Email is already verified", exception.getReason());
        verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
    }
}
