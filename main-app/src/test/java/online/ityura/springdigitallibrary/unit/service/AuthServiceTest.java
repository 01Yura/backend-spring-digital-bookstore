package online.ityura.springdigitallibrary.unit.service;

import online.ityura.springdigitallibrary.dto.request.LoginRequest;
import online.ityura.springdigitallibrary.dto.request.RefreshTokenRequest;
import online.ityura.springdigitallibrary.dto.request.RegisterRequest;
import online.ityura.springdigitallibrary.dto.response.LoginResponse;
import online.ityura.springdigitallibrary.dto.response.RegisterResponse;
import online.ityura.springdigitallibrary.model.Role;
import online.ityura.springdigitallibrary.model.User;
import online.ityura.springdigitallibrary.repository.UserRepository;
import online.ityura.springdigitallibrary.security.JwtTokenProvider;
import online.ityura.springdigitallibrary.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    
    @Mock
    private AuthenticationManager authenticationManager;
    
    @InjectMocks
    private AuthService authService;
    
    private User testUser;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    
    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .nickname("testuser")
                .email("test@example.com")
                .passwordHash("encodedPassword")
                .role(Role.USER)
                .build();
        
        registerRequest = new RegisterRequest();
        registerRequest.setNickname("testuser");
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("Password123!");
        
        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("Password123!");
    }
    
    @Test
    void testRegister_Success_ShouldReturnRegisterResponse() {
        // Given
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        
        // When
        RegisterResponse response = authService.register(registerRequest);
        
        // Then
        assertNotNull(response);
        assertEquals(testUser.getId(), response.getUserId());
        assertEquals(testUser.getEmail(), response.getEmail());
        assertEquals(Role.USER, response.getRole());
        
        verify(userRepository).existsByEmail(registerRequest.getEmail());
        verify(passwordEncoder).encode(registerRequest.getPassword());
        verify(userRepository).save(any(User.class));
    }
    
    @Test
    void testRegister_EmailAlreadyExists_ShouldThrowException() {
        // Given
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);
        
        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
                () -> authService.register(registerRequest));
        
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("Email already exists", exception.getReason());
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    void testLogin_Success_ShouldReturnLoginResponse() {
        // Given
        String accessToken = "accessToken";
        String refreshToken = "refreshToken";
        
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByEmail(loginRequest.getEmail()))
                .thenReturn(Optional.of(testUser));
        when(jwtTokenProvider.generateToken(testUser.getEmail(), testUser.getRole().name()))
                .thenReturn(accessToken);
        when(jwtTokenProvider.generateRefreshToken(testUser.getEmail(), testUser.getRole().name()))
                .thenReturn(refreshToken);
        
        // When
        LoginResponse response = authService.login(loginRequest);
        
        // Then
        assertNotNull(response);
        assertEquals(accessToken, response.getAccessToken());
        assertEquals(refreshToken, response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findByEmail(loginRequest.getEmail());
        verify(jwtTokenProvider).generateToken(testUser.getEmail(), testUser.getRole().name());
        verify(jwtTokenProvider).generateRefreshToken(testUser.getEmail(), testUser.getRole().name());
    }
    
    @Test
    void testLogin_BadCredentials_ShouldThrowException() {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));
        
        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
                () -> authService.login(loginRequest));
        
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("Bad credentials", exception.getReason());
        verify(userRepository, never()).findByEmail(anyString());
    }
    
    @Test
    void testLogin_UserNotFound_ShouldThrowException() {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByEmail(loginRequest.getEmail()))
                .thenReturn(Optional.empty());
        
        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
                () -> authService.login(loginRequest));
        
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("User not found", exception.getReason());
    }
    
    @Test
    void testRefreshToken_Success_ShouldReturnNewTokens() {
        // Given
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("validRefreshToken");
        
        String newAccessToken = "newAccessToken";
        String newRefreshToken = "newRefreshToken";
        
        when(jwtTokenProvider.isRefreshToken("validRefreshToken")).thenReturn(true);
        when(jwtTokenProvider.extractUsername("validRefreshToken")).thenReturn(testUser.getEmail());
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(testUser.getEmail())
                .password(testUser.getPasswordHash())
                .authorities("ROLE_" + testUser.getRole().name())
                .build();
        
        when(jwtTokenProvider.validateToken("validRefreshToken", userDetails)).thenReturn(true);
        when(jwtTokenProvider.generateToken(testUser.getEmail(), testUser.getRole().name()))
                .thenReturn(newAccessToken);
        when(jwtTokenProvider.generateRefreshToken(testUser.getEmail(), testUser.getRole().name()))
                .thenReturn(newRefreshToken);
        
        // When
        LoginResponse response = authService.refreshToken(request);
        
        // Then
        assertNotNull(response);
        assertEquals(newAccessToken, response.getAccessToken());
        assertEquals(newRefreshToken, response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        
        verify(jwtTokenProvider).isRefreshToken("validRefreshToken");
        verify(jwtTokenProvider).extractUsername("validRefreshToken");
        verify(userRepository).findByEmail(testUser.getEmail());
    }
    
    @Test
    void testRefreshToken_InvalidRefreshToken_ShouldThrowException() {
        // Given
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("invalidToken");
        
        when(jwtTokenProvider.isRefreshToken("invalidToken")).thenReturn(false);
        
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> authService.refreshToken(request));
        
        assertEquals("Invalid refresh token", exception.getMessage());
        verify(userRepository, never()).findByEmail(anyString());
    }
    
    @Test
    void testRefreshToken_UserNotFound_ShouldThrowException() {
        // Given
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("validRefreshToken");
        
        when(jwtTokenProvider.isRefreshToken("validRefreshToken")).thenReturn(true);
        when(jwtTokenProvider.extractUsername("validRefreshToken")).thenReturn("nonexistent@example.com");
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());
        
        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
                () -> authService.refreshToken(request));
        
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("User not found", exception.getReason());
    }
    
    @Test
    void testRefreshToken_ExpiredToken_ShouldThrowException() {
        // Given
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("expiredRefreshToken");
        
        when(jwtTokenProvider.isRefreshToken("expiredRefreshToken")).thenReturn(true);
        when(jwtTokenProvider.extractUsername("expiredRefreshToken")).thenReturn(testUser.getEmail());
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(testUser.getEmail())
                .password(testUser.getPasswordHash())
                .authorities("ROLE_" + testUser.getRole().name())
                .build();
        
        when(jwtTokenProvider.validateToken("expiredRefreshToken", userDetails)).thenReturn(false);
        
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> authService.refreshToken(request));
        
        assertEquals("Refresh token is expired or invalid", exception.getMessage());
    }
}

