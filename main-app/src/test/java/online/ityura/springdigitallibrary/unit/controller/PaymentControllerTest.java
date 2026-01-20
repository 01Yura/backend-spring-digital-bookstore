package online.ityura.springdigitallibrary.unit.controller;

import jakarta.servlet.http.HttpServletRequest;
import online.ityura.springdigitallibrary.controller.PaymentController;
import online.ityura.springdigitallibrary.model.Role;
import online.ityura.springdigitallibrary.model.User;
import online.ityura.springdigitallibrary.repository.UserRepository;
import online.ityura.springdigitallibrary.service.StripeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {
    
    @Mock
    private StripeService stripeService;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private Authentication authentication;
    
    @Mock
    private HttpServletRequest request;
    
    @InjectMocks
    private PaymentController paymentController;
    
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
        
        // Set webhook secret and frontend URL using reflection
        ReflectionTestUtils.setField(paymentController, "webhookSecret", "test-webhook-secret");
        ReflectionTestUtils.setField(paymentController, "frontendUrl", "http://localhost:3000");
    }
    
    @Test
    void testCreateCheckoutSession_Success_ShouldReturnCheckoutUrl() {
        // Given
        String checkoutUrl = "https://checkout.stripe.com/pay/cs_test_123";
        
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(stripeService.createCheckoutSession(1L, 1L)).thenReturn(checkoutUrl);
        
        // When
        ResponseEntity<Map<String, String>> result = paymentController.createCheckoutSession(1L, authentication);
        
        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(checkoutUrl, result.getBody().get("checkoutUrl"));
        verify(stripeService).createCheckoutSession(1L, 1L);
    }
    
    @Test
    void testCreateCheckoutSession_FreeBook_ShouldReturnMessage() {
        // Given
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(stripeService.createCheckoutSession(1L, 1L)).thenReturn(null); // Free book
        
        // When
        ResponseEntity<Map<String, String>> result = paymentController.createCheckoutSession(1L, authentication);
        
        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertTrue(result.getBody().get("message").contains("free"));
        assertNull(result.getBody().get("checkoutUrl"));
    }
    
    @Test
    void testCreateCheckoutSession_UserNotFound_ShouldThrowException() {
        // Given
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        
        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> paymentController.createCheckoutSession(1L, authentication));
        
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("User not found", exception.getReason());
        verify(stripeService, never()).createCheckoutSession(anyLong(), anyLong());
    }
    
    @Test
    void testPaymentSuccess_WithSessionId_ShouldReturnSuccessStatus() {
        // Given
        String sessionId = "cs_test_123";
        Long bookId = 1L;
        
        when(request.getHeader(HttpHeaders.ACCEPT)).thenReturn("application/json");
        when(stripeService.verifyAndCompletePaymentIfNeeded(sessionId)).thenReturn(true);
        
        // When
        ResponseEntity<?> result = paymentController.paymentSuccess(sessionId, bookId, request);
        
        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) result.getBody();
        assertNotNull(body);
        assertEquals("success", body.get("status"));
        assertTrue(body.get("message").contains("successfully"));
    }
    
    @Test
    void testPaymentSuccess_PendingPayment_ShouldReturnPendingStatus() {
        // Given
        String sessionId = "cs_test_123";
        Long bookId = 1L;
        
        // Reset mock to clear any previous stubbing
        reset(stripeService);
        
        // Set frontendUrl to empty to avoid redirects
        ReflectionTestUtils.setField(paymentController, "frontendUrl", "");
        when(request.getHeader(HttpHeaders.ACCEPT)).thenReturn("application/json");
        // Use eq() to ensure exact match and return false for pending payment
        when(stripeService.verifyAndCompletePaymentIfNeeded(eq(sessionId))).thenReturn(false);
        
        // When
        ResponseEntity<?> result = paymentController.paymentSuccess(sessionId, bookId, request);
        
        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) result.getBody();
        assertNotNull(body);
        assertEquals("pending", body.get("status"), "Expected status to be 'pending' but was: " + body.get("status"));
        assertTrue(body.get("message").contains("processed") || body.get("message").contains("processing"), 
                "Message should contain 'processed' or 'processing' but was: " + body.get("message"));
        
        // Verify the service was called with the correct sessionId
        verify(stripeService).verifyAndCompletePaymentIfNeeded(eq(sessionId));
        
        // Restore frontendUrl
        ReflectionTestUtils.setField(paymentController, "frontendUrl", "http://localhost:3000");
    }
    
    @Test
    void testPaymentSuccess_WithoutSessionId_ShouldReturnPendingStatus() {
        // Given
        Long bookId = 1L;
        
        when(request.getHeader(HttpHeaders.ACCEPT)).thenReturn("application/json");
        
        // When
        ResponseEntity<?> result = paymentController.paymentSuccess(null, bookId, request);
        
        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) result.getBody();
        assertNotNull(body);
        assertEquals("pending", body.get("status"));
        verify(stripeService, never()).verifyAndCompletePaymentIfNeeded(anyString());
    }
    
    @Test
    void testPaymentSuccess_BrowserRequest_ShouldRedirect() {
        // Given
        String sessionId = "cs_test_123";
        Long bookId = 1L;
        
        when(request.getHeader(HttpHeaders.ACCEPT)).thenReturn("text/html");
        when(stripeService.verifyAndCompletePaymentIfNeeded(sessionId)).thenReturn(true);
        
        // When
        ResponseEntity<?> result = paymentController.paymentSuccess(sessionId, bookId, request);
        
        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.FOUND, result.getStatusCode());
        assertNotNull(result.getHeaders().getLocation());
        assertTrue(result.getHeaders().getLocation().toString().contains("/books/1"));
    }
    
    @Test
    void testPaymentCancel_ShouldReturnCancelledStatus() {
        // Given
        Long bookId = 1L;
        
        when(request.getHeader(HttpHeaders.ACCEPT)).thenReturn("application/json");
        
        // When
        ResponseEntity<?> result = paymentController.paymentCancel(bookId, request);
        
        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) result.getBody();
        assertNotNull(body);
        assertEquals("cancelled", body.get("status"));
        assertTrue(body.get("message").contains("cancelled"));
    }
    
    @Test
    void testPaymentCancel_BrowserRequest_ShouldRedirect() {
        // Given
        Long bookId = 1L;
        
        when(request.getHeader(HttpHeaders.ACCEPT)).thenReturn("text/html");
        
        // When
        ResponseEntity<?> result = paymentController.paymentCancel(bookId, request);
        
        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.FOUND, result.getStatusCode());
        assertNotNull(result.getHeaders().getLocation());
        assertTrue(result.getHeaders().getLocation().toString().contains("/books/1"));
    }
}
