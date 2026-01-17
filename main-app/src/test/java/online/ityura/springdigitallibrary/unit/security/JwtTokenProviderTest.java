package online.ityura.springdigitallibrary.unit.security;

import online.ityura.springdigitallibrary.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {
    
    private JwtTokenProvider jwtTokenProvider;
    private static final String SECRET = "my-secret-key-for-jwt-generation-must-be-at-least-256-bits-long-for-security";
    private static final Long EXPIRATION = 3000000L; // 50 minutes
    private static final Long REFRESH_EXPIRATION = 86400000L; // 24 hours
    
    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "secret", SECRET);
        ReflectionTestUtils.setField(jwtTokenProvider, "expiration", EXPIRATION);
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshExpiration", REFRESH_EXPIRATION);
    }
    
    @Test
    void testGenerateToken_ShouldCreateValidToken() {
        // Given
        String username = "test@example.com";
        String role = "USER";
        
        // When
        String token = jwtTokenProvider.generateToken(username, role);
        
        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }
    
    @Test
    void testGenerateRefreshToken_ShouldCreateValidToken() {
        // Given
        String username = "test@example.com";
        String role = "USER";
        
        // When
        String refreshToken = jwtTokenProvider.generateRefreshToken(username, role);
        
        // Then
        assertNotNull(refreshToken);
        assertFalse(refreshToken.isEmpty());
    }
    
    @Test
    void testExtractUsername_ShouldReturnCorrectUsername() {
        // Given
        String username = "test@example.com";
        String role = "USER";
        String token = jwtTokenProvider.generateToken(username, role);
        
        // When
        String extractedUsername = jwtTokenProvider.extractUsername(token);
        
        // Then
        assertEquals(username, extractedUsername);
    }
    
    @Test
    void testIsRefreshToken_WithRefreshToken_ShouldReturnTrue() {
        // Given
        String username = "test@example.com";
        String role = "USER";
        String refreshToken = jwtTokenProvider.generateRefreshToken(username, role);
        
        // When
        boolean isRefresh = jwtTokenProvider.isRefreshToken(refreshToken);
        
        // Then
        assertTrue(isRefresh);
    }
    
    @Test
    void testIsRefreshToken_WithAccessToken_ShouldReturnFalse() {
        // Given
        String username = "test@example.com";
        String role = "USER";
        String accessToken = jwtTokenProvider.generateToken(username, role);
        
        // When
        boolean isRefresh = jwtTokenProvider.isRefreshToken(accessToken);
        
        // Then
        assertFalse(isRefresh);
    }
    
    @Test
    void testValidateToken_ValidToken_ShouldReturnTrue() {
        // Given
        String username = "test@example.com";
        String role = "USER";
        String token = jwtTokenProvider.generateToken(username, role);
        UserDetails userDetails = User.builder()
                .username(username)
                .password("password")
                .authorities("ROLE_" + role)
                .build();
        
        // When
        boolean isValid = jwtTokenProvider.validateToken(token, userDetails);
        
        // Then
        assertTrue(isValid);
    }
    
    @Test
    void testValidateToken_InvalidUsername_ShouldReturnFalse() {
        // Given
        String username = "test@example.com";
        String role = "USER";
        String token = jwtTokenProvider.generateToken(username, role);
        UserDetails userDetails = User.builder()
                .username("different@example.com")
                .password("password")
                .authorities("ROLE_" + role)
                .build();
        
        // When
        boolean isValid = jwtTokenProvider.validateToken(token, userDetails);
        
        // Then
        assertFalse(isValid);
    }
    
    @Test
    void testGetRoleFromToken_ShouldReturnCorrectRole() {
        // Given
        String username = "test@example.com";
        String role = "ADMIN";
        String token = jwtTokenProvider.generateToken(username, role);
        
        // When
        String extractedRole = jwtTokenProvider.getRoleFromToken(token);
        
        // Then
        assertEquals(role, extractedRole);
    }
    
    @Test
    void testExtractExpiration_ShouldReturnFutureDate() {
        // Given
        String username = "test@example.com";
        String role = "USER";
        String token = jwtTokenProvider.generateToken(username, role);
        
        // When
        Date expiration = jwtTokenProvider.extractExpiration(token);
        
        // Then
        assertNotNull(expiration);
        assertTrue(expiration.after(new Date()));
    }
    
    @Test
    void testGenerateToken_WithUserDetails_ShouldCreateValidToken() {
        // Given
        UserDetails userDetails = User.builder()
                .username("test@example.com")
                .password("password")
                .authorities("ROLE_USER")
                .build();
        
        // When
        String token = jwtTokenProvider.generateToken(userDetails);
        
        // Then
        assertNotNull(token);
        String extractedUsername = jwtTokenProvider.extractUsername(token);
        assertEquals(userDetails.getUsername(), extractedUsername);
    }
    
    @Test
    void testIsRefreshToken_InvalidToken_ShouldReturnFalse() {
        // Given
        String invalidToken = "invalid.token.here";
        
        // When
        boolean isRefresh = jwtTokenProvider.isRefreshToken(invalidToken);
        
        // Then
        assertFalse(isRefresh);
    }
    
    @Test
    void testValidateToken_ExpiredToken_ShouldThrowException() throws InterruptedException {
        // Given - Create a token with very short expiration
        JwtTokenProvider shortExpirationProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(shortExpirationProvider, "secret", SECRET);
        ReflectionTestUtils.setField(shortExpirationProvider, "expiration", 100L); // 100ms
        
        String username = "test@example.com";
        String role = "USER";
        String token = shortExpirationProvider.generateToken(username, role);
        
        // Wait for token to expire
        Thread.sleep(200);
        
        UserDetails userDetails = User.builder()
                .username(username)
                .password("password")
                .authorities("ROLE_" + role)
                .build();
        
        // When & Then - validateToken will throw exception when trying to extract username from expired token
        // because extractAllClaims throws ExpiredJwtException for expired tokens
        assertThrows(Exception.class, () -> {
            shortExpirationProvider.validateToken(token, userDetails);
        });
    }
}

