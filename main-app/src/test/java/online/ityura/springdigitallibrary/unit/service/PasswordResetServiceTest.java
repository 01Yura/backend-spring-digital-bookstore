package online.ityura.springdigitallibrary.unit.service;

import online.ityura.springdigitallibrary.model.PasswordResetToken;
import online.ityura.springdigitallibrary.model.Role;
import online.ityura.springdigitallibrary.model.User;
import online.ityura.springdigitallibrary.repository.PasswordResetTokenRepository;
import online.ityura.springdigitallibrary.repository.UserRepository;
import online.ityura.springdigitallibrary.service.EmailService;
import online.ityura.springdigitallibrary.service.PasswordResetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {
    
    @Mock
    private PasswordResetTokenRepository tokenRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private EmailService emailService;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @InjectMocks
    private PasswordResetService passwordResetService;
    
    private User testUser;
    private PasswordResetToken testToken;
    
    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .nickname("testuser")
                .email("test@example.com")
                .passwordHash("oldEncodedPassword")
                .role(Role.USER)
                .build();
        
        testToken = PasswordResetToken.builder()
                .id(1L)
                .token("reset-token-123")
                .user(testUser)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .used(false)
                .build();
        
        // Set token expiration hours using reflection
        ReflectionTestUtils.setField(passwordResetService, "tokenExpirationHours", 1);
    }
    
    @Test
    void testGeneratePasswordResetToken_Success_ShouldGenerateToken() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        doNothing().when(tokenRepository).deleteByUser(testUser);
        when(tokenRepository.save(any(PasswordResetToken.class))).thenReturn(testToken);
        doNothing().when(emailService).sendPasswordResetEmail(eq("test@example.com"), anyString());
        
        // When
        passwordResetService.generatePasswordResetToken("test@example.com");
        
        // Then
        verify(userRepository).findByEmail("test@example.com");
        verify(tokenRepository).deleteByUser(testUser);
        verify(tokenRepository).save(any(PasswordResetToken.class));
        verify(emailService).sendPasswordResetEmail(eq("test@example.com"), anyString());
    }
    
    @Test
    void testGeneratePasswordResetToken_UserNotFound_ShouldNotThrowException() {
        // Given
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());
        
        // When - should not throw exception (security: don't reveal if email exists)
        assertDoesNotThrow(() -> passwordResetService.generatePasswordResetToken("nonexistent@example.com"));
        
        // Then
        verify(userRepository).findByEmail("nonexistent@example.com");
        verify(tokenRepository, never()).deleteByUser(any());
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
    }
    
    @Test
    void testResetPassword_Success_ShouldResetPassword() {
        // Given
        String newPassword = "NewPassword123!";
        String encodedPassword = "newEncodedPassword";
        
        when(tokenRepository.findByToken("reset-token-123")).thenReturn(Optional.of(testToken));
        when(passwordEncoder.encode(newPassword)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(tokenRepository.save(any(PasswordResetToken.class))).thenReturn(testToken);
        
        // When
        passwordResetService.resetPassword("reset-token-123", newPassword);
        
        // Then
        assertEquals(encodedPassword, testUser.getPasswordHash());
        assertTrue(testToken.getUsed());
        verify(passwordEncoder).encode(newPassword);
        verify(userRepository).save(testUser);
        verify(tokenRepository).save(testToken);
    }
    
    @Test
    void testResetPassword_TokenNotFound_ShouldThrowException() {
        // Given
        when(tokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());
        
        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> passwordResetService.resetPassword("invalid-token", "NewPassword123!"));
        
        assertEquals(404, exception.getStatusCode().value());
        assertEquals("Password reset token not found", exception.getReason());
        verify(passwordEncoder, never()).encode(anyString());
    }
    
    @Test
    void testResetPassword_TokenAlreadyUsed_ShouldThrowException() {
        // Given
        testToken.setUsed(true);
        when(tokenRepository.findByToken("reset-token-123")).thenReturn(Optional.of(testToken));
        
        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> passwordResetService.resetPassword("reset-token-123", "NewPassword123!"));
        
        assertEquals(410, exception.getStatusCode().value());
        assertEquals("Password reset token has already been used", exception.getReason());
        verify(passwordEncoder, never()).encode(anyString());
    }
    
    @Test
    void testResetPassword_TokenExpired_ShouldThrowException() {
        // Given
        testToken.setExpiresAt(LocalDateTime.now().minusHours(1));
        when(tokenRepository.findByToken("reset-token-123")).thenReturn(Optional.of(testToken));
        
        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> passwordResetService.resetPassword("reset-token-123", "NewPassword123!"));
        
        assertEquals(410, exception.getStatusCode().value());
        assertEquals("Password reset token has expired", exception.getReason());
        verify(passwordEncoder, never()).encode(anyString());
    }
    
    @Test
    void testValidateToken_Success_ShouldNotThrowException() {
        // Given
        when(tokenRepository.findByToken("reset-token-123")).thenReturn(Optional.of(testToken));
        
        // When & Then
        assertDoesNotThrow(() -> passwordResetService.validateToken("reset-token-123"));
    }
    
    @Test
    void testValidateToken_TokenNotFound_ShouldThrowException() {
        // Given
        when(tokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());
        
        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> passwordResetService.validateToken("invalid-token"));
        
        assertEquals(404, exception.getStatusCode().value());
        assertEquals("Password reset token not found", exception.getReason());
    }
    
    @Test
    void testValidateToken_TokenExpired_ShouldThrowException() {
        // Given
        testToken.setExpiresAt(LocalDateTime.now().minusHours(1));
        when(tokenRepository.findByToken("reset-token-123")).thenReturn(Optional.of(testToken));
        
        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> passwordResetService.validateToken("reset-token-123"));
        
        assertEquals(410, exception.getStatusCode().value());
        assertEquals("Password reset token has expired", exception.getReason());
    }
}
