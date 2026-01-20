package online.ityura.springdigitallibrary.unit.service;

import online.ityura.springdigitallibrary.model.*;
import online.ityura.springdigitallibrary.repository.BookRepository;
import online.ityura.springdigitallibrary.repository.PurchaseRepository;
import online.ityura.springdigitallibrary.repository.UserRepository;
import online.ityura.springdigitallibrary.service.KafkaProducerService;
import online.ityura.springdigitallibrary.service.StripeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StripeServiceTest {
    
    @Mock
    private BookRepository bookRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PurchaseRepository purchaseRepository;
    
    @Mock
    private KafkaProducerService kafkaProducerService;
    
    @InjectMocks
    private StripeService stripeService;
    
    private User testUser;
    private Book testBook;
    private Purchase testPurchase;
    
    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .nickname("testuser")
                .email("test@example.com")
                .passwordHash("encodedPassword")
                .role(Role.USER)
                .build();
        
        testBook = Book.builder()
                .id(1L)
                .title("Test Book")
                .description("Test Description")
                .price(BigDecimal.valueOf(10.00))
                .discountPercent(BigDecimal.ZERO)
                .build();
        
        testPurchase = Purchase.builder()
                .id(1L)
                .user(testUser)
                .book(testBook)
                .stripePaymentIntentId("session_123")
                .amountPaid(BigDecimal.valueOf(10.00))
                .status(PurchaseStatus.PENDING)
                .build();
        
        // Set properties using reflection
        ReflectionTestUtils.setField(stripeService, "stripeSecretKey", "test-secret-key");
        ReflectionTestUtils.setField(stripeService, "successUrl", "http://localhost/success");
        ReflectionTestUtils.setField(stripeService, "cancelUrl", "http://localhost/cancel");
        ReflectionTestUtils.setField(stripeService, "webhookWaitTimeoutMs", 0L);
    }
    
    @Test
    void testIsBookPurchased_BookPurchased_ShouldReturnTrue() {
        // Given
        when(purchaseRepository.existsByUserIdAndBookIdAndStatus(1L, 1L, PurchaseStatus.COMPLETED))
                .thenReturn(true);
        
        // When
        boolean result = stripeService.isBookPurchased(1L, 1L);
        
        // Then
        assertTrue(result);
        verify(purchaseRepository).existsByUserIdAndBookIdAndStatus(1L, 1L, PurchaseStatus.COMPLETED);
    }
    
    @Test
    void testIsBookPurchased_BookNotPurchased_ShouldReturnFalse() {
        // Given
        when(purchaseRepository.existsByUserIdAndBookIdAndStatus(1L, 1L, PurchaseStatus.COMPLETED))
                .thenReturn(false);
        
        // When
        boolean result = stripeService.isBookPurchased(1L, 1L);
        
        // Then
        assertFalse(result);
    }
    
    @Test
    void testHandlePaymentSuccess_Success_ShouldUpdatePurchaseStatus() {
        // Given
        when(purchaseRepository.findByStripePaymentIntentId("session_123"))
                .thenReturn(Optional.of(testPurchase));
        when(purchaseRepository.save(any(Purchase.class))).thenReturn(testPurchase);
        doNothing().when(purchaseRepository).flush();
        doNothing().when(kafkaProducerService).sendBookPurchaseEvent(any());
        
        // When
        stripeService.handlePaymentSuccess("session_123");
        
        // Then
        assertEquals(PurchaseStatus.COMPLETED, testPurchase.getStatus());
        verify(purchaseRepository).save(testPurchase);
        verify(purchaseRepository).flush();
        verify(kafkaProducerService).sendBookPurchaseEvent(any());
    }
    
    @Test
    void testHandlePaymentSuccess_PurchaseNotFound_ShouldThrowException() {
        // Given
        when(purchaseRepository.findByStripePaymentIntentId("invalid_session"))
                .thenReturn(Optional.empty());
        
        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> stripeService.handlePaymentSuccess("invalid_session"));
        
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Purchase not found"));
        verify(purchaseRepository, never()).save(any(Purchase.class));
    }
    
    @Test
    void testHandlePaymentFailure_Success_ShouldUpdatePurchaseStatus() {
        // Given
        when(purchaseRepository.findByStripePaymentIntentId("session_123"))
                .thenReturn(Optional.of(testPurchase));
        when(purchaseRepository.save(any(Purchase.class))).thenReturn(testPurchase);
        
        // When
        stripeService.handlePaymentFailure("session_123");
        
        // Then
        assertEquals(PurchaseStatus.FAILED, testPurchase.getStatus());
        verify(purchaseRepository).save(testPurchase);
    }
    
    @Test
    void testHandlePaymentFailure_PurchaseNotFound_ShouldNotThrowException() {
        // Given
        when(purchaseRepository.findByStripePaymentIntentId("invalid_session"))
                .thenReturn(Optional.empty());
        
        // When - should not throw exception
        assertDoesNotThrow(() -> stripeService.handlePaymentFailure("invalid_session"));
        
        // Then
        verify(purchaseRepository, never()).save(any(Purchase.class));
    }
    
    @Test
    void testHandlePaymentSuccess_WithoutKafka_ShouldNotThrowException() {
        // Given
        ReflectionTestUtils.setField(stripeService, "kafkaProducerService", null);
        when(purchaseRepository.findByStripePaymentIntentId("session_123"))
                .thenReturn(Optional.of(testPurchase));
        when(purchaseRepository.save(any(Purchase.class))).thenReturn(testPurchase);
        doNothing().when(purchaseRepository).flush();
        
        // When - should not throw exception even without Kafka
        assertDoesNotThrow(() -> stripeService.handlePaymentSuccess("session_123"));
        
        // Then
        assertEquals(PurchaseStatus.COMPLETED, testPurchase.getStatus());
        verify(purchaseRepository).save(testPurchase);
    }
}
