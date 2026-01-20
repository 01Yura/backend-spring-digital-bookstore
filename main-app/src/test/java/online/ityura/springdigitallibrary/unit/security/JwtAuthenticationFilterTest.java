package online.ityura.springdigitallibrary.unit.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import online.ityura.springdigitallibrary.model.Role;
import online.ityura.springdigitallibrary.model.User;
import online.ityura.springdigitallibrary.repository.UserRepository;
import online.ityura.springdigitallibrary.security.JwtAuthenticationFilter;
import online.ityura.springdigitallibrary.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {
    
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    
    @Mock
    private UserDetailsService userDetailsService;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private HttpServletRequest request;
    
    @Mock
    private HttpServletResponse response;
    
    @Mock
    private FilterChain filterChain;
    
    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }
    
    @Test
    void testDoFilterInternal_SwaggerEndpoint_ShouldSkipAuthentication() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/swagger-ui/index.html");
        
        // When - используем ReflectionTestUtils для вызова protected метода
        ReflectionTestUtils.invokeMethod(jwtAuthenticationFilter, "doFilterInternal", request, response, filterChain);
        
        // Then
        verify(filterChain).doFilter(request, response);
        verify(jwtTokenProvider, never()).extractUsername(anyString());
    }
    
    @Test
    void testDoFilterInternal_PublicBooksEndpoint_ShouldSkipAuthentication() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/books");
        
        // When - используем ReflectionTestUtils для вызова protected метода
        ReflectionTestUtils.invokeMethod(jwtAuthenticationFilter, "doFilterInternal", request, response, filterChain);
        
        // Then
        verify(filterChain).doFilter(request, response);
        verify(jwtTokenProvider, never()).extractUsername(anyString());
    }
    
    @Test
    void testDoFilterInternal_BookImageEndpoint_ShouldSkipAuthentication() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/books/1/image");
        
        // When - используем ReflectionTestUtils для вызова protected метода
        ReflectionTestUtils.invokeMethod(jwtAuthenticationFilter, "doFilterInternal", request, response, filterChain);
        
        // Then
        verify(filterChain).doFilter(request, response);
        verify(jwtTokenProvider, never()).extractUsername(anyString());
    }
    
    @Test
    void testDoFilterInternal_ValidToken_ShouldSetAuthentication() throws ServletException, IOException {
        // Given
        String token = "validToken";
        String username = "test@example.com";
        String role = "USER";
        
        User testUser = User.builder()
                .id(1L)
                .email(username)
                .nickname("testuser")
                .passwordHash("password")
                .role(Role.USER)
                .isVerified(true)
                .build();
        
        when(request.getRequestURI()).thenReturn("/api/v1/books/1/download");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtTokenProvider.extractUsername(token)).thenReturn(username);
        
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(username)
                .password("password")
                .authorities("ROLE_" + role)
                .build();
        
        when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
        when(jwtTokenProvider.validateToken(token, userDetails)).thenReturn(true);
        when(jwtTokenProvider.getRoleFromToken(token)).thenReturn(role);
        when(userRepository.findByEmail(username)).thenReturn(java.util.Optional.of(testUser));
        
        // When - используем ReflectionTestUtils для вызова protected метода
        ReflectionTestUtils.invokeMethod(jwtAuthenticationFilter, "doFilterInternal", request, response, filterChain);
        
        // Then
        verify(filterChain).doFilter(request, response);
        verify(jwtTokenProvider).extractUsername(token);
        verify(jwtTokenProvider).validateToken(token, userDetails);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }
    
    @Test
    void testDoFilterInternal_InvalidToken_ShouldNotSetAuthentication() throws ServletException, IOException {
        // Given
        String token = "invalidToken";
        
        when(request.getRequestURI()).thenReturn("/api/v1/books/1/download");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtTokenProvider.extractUsername(token)).thenThrow(new RuntimeException("Invalid token"));
        
        // When - используем ReflectionTestUtils для вызова protected метода
        ReflectionTestUtils.invokeMethod(jwtAuthenticationFilter, "doFilterInternal", request, response, filterChain);
        
        // Then
        verify(filterChain).doFilter(request, response);
        verify(jwtTokenProvider).extractUsername(token);
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
    
    @Test
    void testDoFilterInternal_NoAuthHeader_ShouldNotSetAuthentication() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/books/1/download");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn(null);
        
        // When - используем ReflectionTestUtils для вызова protected метода
        ReflectionTestUtils.invokeMethod(jwtAuthenticationFilter, "doFilterInternal", request, response, filterChain);
        
        // Then
        verify(filterChain).doFilter(request, response);
        verify(jwtTokenProvider, never()).extractUsername(anyString());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
    
    @Test
    void testDoFilterInternal_InvalidAuthHeaderFormat_ShouldNotSetAuthentication() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/books/1/download");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn("InvalidFormat token");
        
        // When - используем ReflectionTestUtils для вызова protected метода
        ReflectionTestUtils.invokeMethod(jwtAuthenticationFilter, "doFilterInternal", request, response, filterChain);
        
        // Then
        verify(filterChain).doFilter(request, response);
        verify(jwtTokenProvider, never()).extractUsername(anyString());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}

