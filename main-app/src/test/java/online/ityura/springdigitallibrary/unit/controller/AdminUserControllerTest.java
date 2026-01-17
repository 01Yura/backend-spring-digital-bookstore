package online.ityura.springdigitallibrary.unit.controller;

import online.ityura.springdigitallibrary.controller.AdminUserController;
import online.ityura.springdigitallibrary.dto.response.AdminUserResponse;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private AdminUserController adminUserController;
    
    private MockMvc mockMvc;
    private User testUser;
    private User testAdmin;
    
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminUserController).build();
        
        testUser = User.builder()
                .id(1L)
                .nickname("testuser")
                .email("test@example.com")
                .passwordHash("encodedPassword")
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .build();
        
        testAdmin = User.builder()
                .id(2L)
                .nickname("admin")
                .email("admin@example.com")
                .passwordHash("encodedPassword")
                .role(Role.ADMIN)
                .createdAt(LocalDateTime.now())
                .build();
    }
    
    @Test
    void testGetAllUsers_Success_ShouldReturn200() throws Exception {
        // Given
        List<User> users = Arrays.asList(testUser, testAdmin);
        when(userRepository.findAll()).thenReturn(users);
        
        // When & Then
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].nickname").value("testuser"))
                .andExpect(jsonPath("$[0].email").value("test@example.com"))
                .andExpect(jsonPath("$[0].role").value("USER"))
                .andExpect(jsonPath("$[1].id").value(2L))
                .andExpect(jsonPath("$[1].role").value("ADMIN"));
        
        verify(userRepository).findAll();
    }
    
    @Test
    void testGetAllUsers_UnitTest_ShouldReturnListOfUsers() {
        // Given
        List<User> users = Arrays.asList(testUser, testAdmin);
        when(userRepository.findAll()).thenReturn(users);
        
        // When
        ResponseEntity<List<AdminUserResponse>> result = adminUserController.getAllUsers();
        
        // Then
        assertNotNull(result);
        assertEquals(200, result.getStatusCode().value());
        assertNotNull(result.getBody());
        assertEquals(2, result.getBody().size());
        
        AdminUserResponse firstUser = result.getBody().get(0);
        assertEquals(testUser.getId(), firstUser.getId());
        assertEquals(testUser.getNickname(), firstUser.getNickname());
        assertEquals(testUser.getEmail(), firstUser.getEmail());
        assertEquals(testUser.getRole(), firstUser.getRole());
        assertEquals(testUser.getCreatedAt(), firstUser.getCreatedAt());
        
        verify(userRepository).findAll();
    }
    
    @Test
    void testGetAllUsers_EmptyList_ShouldReturnEmptyList() {
        // Given
        when(userRepository.findAll()).thenReturn(Collections.emptyList());
        
        // When
        ResponseEntity<List<AdminUserResponse>> result = adminUserController.getAllUsers();
        
        // Then
        assertNotNull(result);
        assertEquals(200, result.getStatusCode().value());
        assertNotNull(result.getBody());
        assertTrue(result.getBody().isEmpty());
        
        verify(userRepository).findAll();
    }
    
    @Test
    void testDeleteUser_Success_ShouldReturn204() throws Exception {
        // Given
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        doNothing().when(userRepository).delete(testUser);
        
        // When & Then
        mockMvc.perform(delete("/api/v1/admin/users/{userId}", userId))
                .andExpect(status().isNoContent());
        
        verify(userRepository).findById(userId);
        verify(userRepository).delete(testUser);
    }
    
    @Test
    void testDeleteUser_UnitTest_ShouldReturnNoContent() {
        // Given
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        doNothing().when(userRepository).delete(testUser);
        
        // When
        ResponseEntity<Void> result = adminUserController.deleteUser(userId);
        
        // Then
        assertNotNull(result);
        assertEquals(204, result.getStatusCode().value());
        assertNull(result.getBody());
        
        verify(userRepository).findById(userId);
        verify(userRepository).delete(testUser);
    }
    
    @Test
    void testDeleteUser_UserNotFound_ShouldThrow404() {
        // Given
        Long userId = 999L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        
        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
                () -> adminUserController.deleteUser(userId));
        
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getReason().contains("User not found with id: " + userId));
        
        verify(userRepository).findById(userId);
        verify(userRepository, never()).delete(any(User.class));
    }
    
    @Test
    void testDeleteUser_UserNotFound_ShouldReturn404() throws Exception {
        // Given
        Long userId = 999L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        
        // When & Then
        mockMvc.perform(delete("/api/v1/admin/users/{userId}", userId))
                .andExpect(status().isNotFound());
        
        verify(userRepository).findById(userId);
        verify(userRepository, never()).delete(any(User.class));
    }
    
    @Test
    void testDeleteUser_AdminUser_ShouldThrow403() {
        // Given
        Long adminId = 2L;
        when(userRepository.findById(adminId)).thenReturn(Optional.of(testAdmin));
        
        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
                () -> adminUserController.deleteUser(adminId));
        
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Cannot delete user with ADMIN role"));
        
        verify(userRepository).findById(adminId);
        verify(userRepository, never()).delete(any(User.class));
    }
    
    @Test
    void testDeleteUser_AdminUser_ShouldReturn403() throws Exception {
        // Given
        Long adminId = 2L;
        when(userRepository.findById(adminId)).thenReturn(Optional.of(testAdmin));
        
        // When & Then
        mockMvc.perform(delete("/api/v1/admin/users/{userId}", adminId))
                .andExpect(status().isForbidden());
        
        verify(userRepository).findById(adminId);
        verify(userRepository, never()).delete(any(User.class));
    }
    
    @Test
    void testDeleteUser_UserWithPurchases_ShouldDeleteSuccessfully() {
        // Given
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        doNothing().when(userRepository).delete(testUser);
        
        // When
        ResponseEntity<Void> result = adminUserController.deleteUser(userId);
        
        // Then
        assertNotNull(result);
        assertEquals(204, result.getStatusCode().value());
        assertNull(result.getBody());
        
        verify(userRepository).findById(userId);
        verify(userRepository).delete(testUser);
    }
    
    @Test
    void testDeleteUser_UserWithPurchases_ShouldReturn204() throws Exception {
        // Given
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        doNothing().when(userRepository).delete(testUser);
        
        // When & Then
        mockMvc.perform(delete("/api/v1/admin/users/{userId}", userId))
                .andExpect(status().isNoContent());
        
        verify(userRepository).findById(userId);
        verify(userRepository).delete(testUser);
    }
    
    @Test
    void testGetAllUsers_MapsAllFieldsCorrectly() {
        // Given
        User userWithAllFields = User.builder()
                .id(3L)
                .nickname("fulluser")
                .email("full@example.com")
                .passwordHash("hash")
                .role(Role.USER)
                .createdAt(LocalDateTime.of(2023, 1, 1, 12, 0))
                .build();
        
        when(userRepository.findAll()).thenReturn(Collections.singletonList(userWithAllFields));
        
        // When
        ResponseEntity<List<AdminUserResponse>> result = adminUserController.getAllUsers();
        
        // Then
        assertNotNull(result.getBody());
        assertEquals(1, result.getBody().size());
        
        AdminUserResponse response = result.getBody().get(0);
        assertEquals(userWithAllFields.getId(), response.getId());
        assertEquals(userWithAllFields.getNickname(), response.getNickname());
        assertEquals(userWithAllFields.getEmail(), response.getEmail());
        assertEquals(userWithAllFields.getRole(), response.getRole());
        assertEquals(userWithAllFields.getCreatedAt(), response.getCreatedAt());
    }
}

