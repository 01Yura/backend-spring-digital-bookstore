package online.ityura.springdigitallibrary.unit.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import online.ityura.springdigitallibrary.controller.AuthController;
import online.ityura.springdigitallibrary.dto.request.LoginRequest;
import online.ityura.springdigitallibrary.dto.request.RefreshTokenRequest;
import online.ityura.springdigitallibrary.dto.request.RegisterRequest;
import online.ityura.springdigitallibrary.dto.response.LoginResponse;
import online.ityura.springdigitallibrary.dto.response.RegisterResponse;
import online.ityura.springdigitallibrary.model.Role;
import online.ityura.springdigitallibrary.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {
    
    @Mock
    private AuthService authService;
    
    @InjectMocks
    private AuthController authController;
    
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    
    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
        objectMapper = new ObjectMapper();
    }
    
    @Test
    void testRegister_Success_ShouldReturn201() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setNickname("testuser");
        request.setEmail("test@example.com");
        request.setPassword("Password123!");
        
        RegisterResponse response = RegisterResponse.builder()
                .userId(1L)
                .email("test@example.com")
                .role(Role.USER)
                .build();
        
        when(authService.register(any(RegisterRequest.class))).thenReturn(response);
        
        // When & Then
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(1L))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }
    
    @Test
    void testLogin_Success_ShouldReturn200() throws Exception {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("Password123!");
        
        LoginResponse response = LoginResponse.builder()
                .accessToken("accessToken")
                .refreshToken("refreshToken")
                .tokenType("Bearer")
                .build();
        
        when(authService.login(any(LoginRequest.class))).thenReturn(response);
        
        // When & Then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("accessToken"))
                .andExpect(jsonPath("$.refreshToken").value("refreshToken"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }
    
    @Test
    void testRefreshToken_Success_ShouldReturn200() throws Exception {
        // Given
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("refreshToken");
        
        LoginResponse response = LoginResponse.builder()
                .accessToken("newAccessToken")
                .refreshToken("newRefreshToken")
                .tokenType("Bearer")
                .build();
        
        when(authService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(response);
        
        // When & Then
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("newAccessToken"))
                .andExpect(jsonPath("$.refreshToken").value("newRefreshToken"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }
    
    @Test
    void testRegister_UnitTest_ShouldCallService() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setNickname("testuser");
        request.setEmail("test@example.com");
        request.setPassword("Password123!");
        
        RegisterResponse response = RegisterResponse.builder()
                .userId(1L)
                .email("test@example.com")
                .role(Role.USER)
                .build();
        
        when(authService.register(any(RegisterRequest.class))).thenReturn(response);
        
        // When
        ResponseEntity<RegisterResponse> result = authController.register(request);
        
        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(1L, result.getBody().getUserId());
        assertEquals("test@example.com", result.getBody().getEmail());
        assertEquals(Role.USER, result.getBody().getRole());
    }
}
