package online.ityura.springdigitallibrary.unit.security;

import online.ityura.springdigitallibrary.model.Role;
import online.ityura.springdigitallibrary.model.User;
import online.ityura.springdigitallibrary.repository.UserRepository;
import online.ityura.springdigitallibrary.security.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;
    
    private User testUser;
    
    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .nickname("testuser")
                .passwordHash("encodedPassword")
                .role(Role.USER)
                .build();
    }
    
    @Test
    void testLoadUserByUsername_Success_ShouldReturnUserDetails() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        
        // When
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("test@example.com");
        
        // Then
        assertNotNull(userDetails);
        assertEquals("test@example.com", userDetails.getUsername());
        assertEquals("encodedPassword", userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    }
    
    @Test
    void testLoadUserByUsername_AdminRole_ShouldReturnAdminUserDetails() {
        // Given
        testUser.setRole(Role.ADMIN);
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(testUser));
        
        // When
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("admin@example.com");
        
        // Then
        assertNotNull(userDetails);
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }
    
    @Test
    void testLoadUserByUsername_UserNotFound_ShouldThrowException() {
        // Given
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());
        
        // When & Then
        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, 
                () -> customUserDetailsService.loadUserByUsername("nonexistent@example.com"));
        
        assertTrue(exception.getMessage().contains("User not found with email"));
    }
}

