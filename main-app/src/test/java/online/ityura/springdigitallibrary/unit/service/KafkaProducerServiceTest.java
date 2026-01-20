package online.ityura.springdigitallibrary.unit.service;

import online.ityura.springdigitallibrary.dto.event.*;
import online.ityura.springdigitallibrary.service.KafkaProducerService;
import org.apache.kafka.common.errors.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaProducerServiceTest {
    
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    @InjectMocks
    private KafkaProducerService kafkaProducerService;
    
    private BookViewEvent bookViewEvent;
    private BookDownloadEvent bookDownloadEvent;
    private BookPurchaseEvent bookPurchaseEvent;
    private BookReviewEvent bookReviewEvent;
    private BookRatingEvent bookRatingEvent;
    
    @BeforeEach
    void setUp() throws Exception {
        // Reset kafkaAvailable to true before each test
        Field kafkaAvailableField = KafkaProducerService.class.getDeclaredField("kafkaAvailable");
        kafkaAvailableField.setAccessible(true);
        AtomicBoolean kafkaAvailable = (AtomicBoolean) kafkaAvailableField.get(null);
        kafkaAvailable.set(true);
        
        bookViewEvent = BookViewEvent.builder()
                .eventId("event-1")
                .eventType("BOOK_VIEW")
                .timestamp(LocalDateTime.now())
                .bookId(1L)
                .userId(100L)
                .bookTitle("Test Book")
                .bookGenre("FICTION")
                .build();
        
        bookDownloadEvent = BookDownloadEvent.builder()
                .eventId("event-2")
                .eventType("BOOK_DOWNLOAD")
                .timestamp(LocalDateTime.now())
                .bookId(1L)
                .userId(100L)
                .bookTitle("Test Book")
                .bookPrice(10.0)
                .isFree(false)
                .build();
        
        bookPurchaseEvent = BookPurchaseEvent.builder()
                .eventId("event-3")
                .eventType("BOOK_PURCHASE")
                .timestamp(LocalDateTime.now())
                .bookId(1L)
                .userId(100L)
                .bookTitle("Test Book")
                .amountPaid(10.0)
                .originalPrice(15.0)
                .discountPercent(33.33)
                .stripeSessionId("session_123")
                .build();
        
        bookReviewEvent = BookReviewEvent.builder()
                .eventId("event-4")
                .eventType("BOOK_REVIEW")
                .timestamp(LocalDateTime.now())
                .bookId(1L)
                .userId(100L)
                .reviewId(1L)
                .action("CREATED")
                .reviewLength(10)
                .build();
        
        bookRatingEvent = BookRatingEvent.builder()
                .eventId("event-5")
                .eventType("BOOK_RATING")
                .timestamp(LocalDateTime.now())
                .bookId(1L)
                .userId(100L)
                .ratingId(1L)
                .ratingValue((short) 5)
                .action("CREATED")
                .build();
    }
    
    @Test
    void testSendBookViewEvent_Success_ShouldSendEvent() {
        // Given
        @SuppressWarnings("unchecked")
        SendResult<String, Object> sendResult = mock(SendResult.class);
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);
        
        when(kafkaTemplate.send(eq("book.views"), eq("1"), any(BookViewEvent.class)))
                .thenReturn(future);
        
        // When
        kafkaProducerService.sendBookViewEvent(bookViewEvent);
        
        // Then
        verify(kafkaTemplate).send(eq("book.views"), eq("1"), any(BookViewEvent.class));
    }
    
    @Test
    void testSendBookViewEvent_TimeoutException_ShouldHandleGracefully() {
        // Given
        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        future.completeExceptionally(new TimeoutException("Kafka timeout"));
        
        when(kafkaTemplate.send(eq("book.views"), eq("1"), any(BookViewEvent.class)))
                .thenReturn(future);
        
        // When - should not throw exception
        try {
            kafkaProducerService.sendBookViewEvent(bookViewEvent);
        } catch (Exception e) {
            fail("Should not throw exception");
        }
        
        // Then
        verify(kafkaTemplate).send(eq("book.views"), eq("1"), any(BookViewEvent.class));
    }
    
    @Test
    void testSendBookDownloadEvent_Success_ShouldSendEvent() {
        // Given
        @SuppressWarnings("unchecked")
        SendResult<String, Object> sendResult = mock(SendResult.class);
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);
        
        when(kafkaTemplate.send(eq("book.downloads"), eq("100"), any(BookDownloadEvent.class)))
                .thenReturn(future);
        
        // When
        kafkaProducerService.sendBookDownloadEvent(bookDownloadEvent);
        
        // Then
        verify(kafkaTemplate).send(eq("book.downloads"), eq("100"), any(BookDownloadEvent.class));
    }
    
    @Test
    void testSendBookPurchaseEvent_Success_ShouldSendEvent() {
        // Given
        @SuppressWarnings("unchecked")
        SendResult<String, Object> sendResult = mock(SendResult.class);
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);
        
        when(kafkaTemplate.send(eq("book.purchases"), eq("100"), any(BookPurchaseEvent.class)))
                .thenReturn(future);
        
        // When
        kafkaProducerService.sendBookPurchaseEvent(bookPurchaseEvent);
        
        // Then
        verify(kafkaTemplate).send(eq("book.purchases"), eq("100"), any(BookPurchaseEvent.class));
    }
    
    @Test
    void testSendBookReviewEvent_Success_ShouldSendEvent() {
        // Given
        @SuppressWarnings("unchecked")
        SendResult<String, Object> sendResult = mock(SendResult.class);
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);
        
        when(kafkaTemplate.send(eq("book.reviews"), eq("1"), any(BookReviewEvent.class)))
                .thenReturn(future);
        
        // When
        kafkaProducerService.sendBookReviewEvent(bookReviewEvent);
        
        // Then
        verify(kafkaTemplate).send(eq("book.reviews"), eq("1"), any(BookReviewEvent.class));
    }
    
    @Test
    void testSendBookRatingEvent_Success_ShouldSendEvent() {
        // Given
        @SuppressWarnings("unchecked")
        SendResult<String, Object> sendResult = mock(SendResult.class);
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);
        
        when(kafkaTemplate.send(eq("book.ratings"), eq("1"), any(BookRatingEvent.class)))
                .thenReturn(future);
        
        // When
        kafkaProducerService.sendBookRatingEvent(bookRatingEvent);
        
        // Then
        verify(kafkaTemplate).send(eq("book.ratings"), eq("1"), any(BookRatingEvent.class));
    }
    
    @Test
    void testSendBookViewEvent_Exception_ShouldHandleGracefully() {
        // Given
        when(kafkaTemplate.send(eq("book.views"), eq("1"), any(BookViewEvent.class)))
                .thenThrow(new RuntimeException("Kafka error"));
        
        // When - should not throw exception
        try {
            kafkaProducerService.sendBookViewEvent(bookViewEvent);
        } catch (Exception e) {
            fail("Should not throw exception");
        }
        
        // Then
        verify(kafkaTemplate).send(eq("book.views"), eq("1"), any(BookViewEvent.class));
    }
}
