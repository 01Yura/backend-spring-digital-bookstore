package online.ityura.springdigitallibrary.unit.security;

import online.ityura.springdigitallibrary.security.Md5PasswordEncoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class Md5PasswordEncoderTest {
    
    private Md5PasswordEncoder passwordEncoder;
    
    @BeforeEach
    void setUp() {
        passwordEncoder = new Md5PasswordEncoder();
    }
    
    @Test
    void testEncode_ShouldReturnMd5Hash() {
        // Given
        String rawPassword = "testPassword123";
        
        // When
        String encoded = passwordEncoder.encode(rawPassword);
        
        // Then
        assertNotNull(encoded);
        assertEquals(32, encoded.length()); // MD5 hash is always 32 hex characters
        assertNotEquals(rawPassword, encoded);
    }
    
    @Test
    void testEncode_EmptyPassword_ShouldReturnHash() {
        // Given
        String rawPassword = "";
        
        // When
        String encoded = passwordEncoder.encode(rawPassword);
        
        // Then
        assertNotNull(encoded);
        assertEquals(32, encoded.length());
    }
    
    @Test
    void testEncode_SpecialCharacters_ShouldReturnHash() {
        // Given
        String rawPassword = "!@#$%^&*()";
        
        // When
        String encoded = passwordEncoder.encode(rawPassword);
        
        // Then
        assertNotNull(encoded);
        assertEquals(32, encoded.length());
    }
    
    @Test
    void testEncode_ConsistentEncoding_ShouldReturnSameHash() {
        // Given
        String rawPassword = "samePassword";
        
        // When
        String encoded1 = passwordEncoder.encode(rawPassword);
        String encoded2 = passwordEncoder.encode(rawPassword);
        
        // Then
        assertEquals(encoded1, encoded2);
    }
    
    @Test
    void testMatches_CorrectPassword_ShouldReturnTrue() {
        // Given
        String rawPassword = "testPassword123";
        String encodedPassword = passwordEncoder.encode(rawPassword);
        
        // When
        boolean result = passwordEncoder.matches(rawPassword, encodedPassword);
        
        // Then
        assertTrue(result);
    }
    
    @Test
    void testMatches_IncorrectPassword_ShouldReturnFalse() {
        // Given
        String rawPassword = "testPassword123";
        String wrongPassword = "wrongPassword";
        String encodedPassword = passwordEncoder.encode(rawPassword);
        
        // When
        boolean result = passwordEncoder.matches(wrongPassword, encodedPassword);
        
        // Then
        assertFalse(result);
    }
    
    @Test
    void testMatches_EmptyPassword_ShouldReturnTrue() {
        // Given
        String rawPassword = "";
        String encodedPassword = passwordEncoder.encode(rawPassword);
        
        // When
        boolean result = passwordEncoder.matches(rawPassword, encodedPassword);
        
        // Then
        assertTrue(result);
    }
    
    @Test
    void testMatches_EmptyPasswordWithNonEmptyHash_ShouldReturnFalse() {
        // Given
        String rawPassword = "";
        String encodedPassword = passwordEncoder.encode("nonEmpty");
        
        // When
        boolean result = passwordEncoder.matches(rawPassword, encodedPassword);
        
        // Then
        assertFalse(result);
    }
    
    @Test
    void testMatches_UnicodeCharacters_ShouldWork() {
        // Given
        String rawPassword = "пароль123";
        String encodedPassword = passwordEncoder.encode(rawPassword);
        
        // When
        boolean result = passwordEncoder.matches(rawPassword, encodedPassword);
        
        // Then
        assertTrue(result);
    }
    
    @Test
    void testEncode_LongPassword_ShouldReturnHash() {
        // Given
        String rawPassword = "a".repeat(1000);
        
        // When
        String encoded = passwordEncoder.encode(rawPassword);
        
        // Then
        assertNotNull(encoded);
        assertEquals(32, encoded.length());
    }
}
