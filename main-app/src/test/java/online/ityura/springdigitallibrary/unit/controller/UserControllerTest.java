package online.ityura.springdigitallibrary.unit.controller;

import online.ityura.springdigitallibrary.controller.UserController;
import online.ityura.springdigitallibrary.dto.response.UserInfoResponse;
import online.ityura.springdigitallibrary.model.Role;
import online.ityura.springdigitallibrary.model.User;
import online.ityura.springdigitallibrary.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private Authentication authentication;
    
    @InjectMocks
    private UserController userController;
    
    private User testUser;
    private UserDetails userDetails;
    
    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .nickname("testuser")
                .email("test@example.com")
                .passwordHash("encodedPassword")
                .role(Role.USER)
                .build();
        
        userDetails = org.springframework.security.core.userdetails.User.builder()
                .username("test@example.com")
                .password("password")
                .authorities("ROLE_USER")
                .build();
    }
    
    @Test
    void testGetCurrentUser_Success_ShouldReturnUserInfo() {
        // Given
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        
        // When
        ResponseEntity<UserInfoResponse> result = userController.getCurrentUser(authentication);
        
        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(1L, result.getBody().getId());
        assertEquals("testuser", result.getBody().getNickname());
        assertEquals("test@example.com", result.getBody().getEmail());
        assertEquals(Role.USER, result.getBody().getRole());
        
        verify(userRepository).findByEmail("test@example.com");
    }
    
    @Test
    void testGetCurrentUser_AuthenticationNull_ShouldThrowUnauthorized() {
        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> userController.getCurrentUser(null));
        
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("User is not authenticated", exception.getReason());
        verify(userRepository, never()).findByEmail(anyString());
    }
    
    @Test
    void testGetCurrentUser_InvalidPrincipal_ShouldThrowUnauthorized() {
        // Given
        when(authentication.getPrincipal()).thenReturn("invalid principal");
        
        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> userController.getCurrentUser(authentication));
        
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("User is not authenticated", exception.getReason());
        verify(userRepository, never()).findByEmail(anyString());
    }
    
    @Test
    void testGetCurrentUser_UserNotFound_ShouldThrowNotFound() {
        // Given
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        
        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> userController.getCurrentUser(authentication));
        
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("User not found", exception.getReason());
        verify(userRepository).findByEmail("test@example.com");
    }
    
    @Test
    void testGetCurrentUser_WithAdminRole_ShouldReturnAdminRole() {
        // Given
        User adminUser = User.builder()
                .id(2L)
                .nickname("admin")
                .email("admin@example.com")
                .passwordHash("encodedPassword")
                .role(Role.ADMIN)
                .build();
        
        UserDetails adminUserDetails = org.springframework.security.core.userdetails.User.builder()
                .username("admin@example.com")
                .password("password")
                .authorities("ROLE_ADMIN")
                .build();
        
        when(authentication.getPrincipal()).thenReturn(adminUserDetails);
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(adminUser));
        
        // When
        ResponseEntity<UserInfoResponse> result = userController.getCurrentUser(authentication);
        
        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(Role.ADMIN, result.getBody().getRole());
    }
}
