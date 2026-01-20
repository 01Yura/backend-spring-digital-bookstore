package online.ityura.springdigitallibrary.unit.controller;

import online.ityura.springdigitallibrary.controller.AnalyticsAdminController;
import online.ityura.springdigitallibrary.dto.response.*;
import online.ityura.springdigitallibrary.model.BookAnalytics;
import online.ityura.springdigitallibrary.model.SystemAnalytics;
import online.ityura.springdigitallibrary.repository.BookAnalyticsRepository;
import online.ityura.springdigitallibrary.repository.SystemAnalyticsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsAdminControllerTest {
    
    @Mock
    private BookAnalyticsRepository bookAnalyticsRepository;
    
    @Mock
    private SystemAnalyticsRepository systemAnalyticsRepository;
    
    @InjectMocks
    private AnalyticsAdminController analyticsAdminController;
    
    private BookAnalytics testBookAnalytics;
    private SystemAnalytics testSystemAnalytics;
    
    @BeforeEach
    void setUp() {
        testBookAnalytics = BookAnalytics.builder()
                .id(1L)
                .bookId(1L)
                .bookTitle("Test Book")
                .bookGenre("FICTION")
                .viewCount(100L)
                .downloadCount(50L)
                .purchaseCount(30L)
                .reviewCount(20L)
                .ratingCount(25L)
                .averageRating(BigDecimal.valueOf(4.5))
                .totalRevenue(BigDecimal.valueOf(300.00))
                .uniqueViewers(80)
                .uniqueDownloaders(40)
                .uniquePurchasers(25)
                .aggregatedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
        
        testSystemAnalytics = SystemAnalytics.builder()
                .id(1L)
                .totalBooks(100)
                .totalUsers(500)
                .totalViews(10000L)
                .totalDownloads(5000L)
                .totalPurchases(3000L)
                .totalRevenue(BigDecimal.valueOf(30000.00))
                .totalReviews(2000L)
                .totalRatings(2500L)
                .averageRating(BigDecimal.valueOf(4.2))
                .averageReviewLength(BigDecimal.valueOf(150.5))
                .mostPopularBookId(1L)
                .mostPopularBookTitle("Test Book")
                .topGenre("FICTION")
                .topGenreBookCount(40)
                .topGenreTotalViews(4000L)
                .aggregatedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
    }
    
    @Test
    void testGetBookAnalytics_Success_ShouldReturnBookAnalytics() {
        // Given
        when(bookAnalyticsRepository.findFirstByBookIdOrderByAggregatedAtDesc(1L))
                .thenReturn(Optional.of(testBookAnalytics));
        
        // When
        ResponseEntity<BookAnalyticsResponse> result = analyticsAdminController.getBookAnalytics(1L);
        
        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(1L, result.getBody().getBookId());
        assertEquals("Test Book", result.getBody().getBookTitle());
        assertEquals(100L, result.getBody().getViewCount());
        verify(bookAnalyticsRepository).findFirstByBookIdOrderByAggregatedAtDesc(1L);
    }
    
    @Test
    void testGetBookAnalytics_NotFound_ShouldThrowException() {
        // Given
        when(bookAnalyticsRepository.findFirstByBookIdOrderByAggregatedAtDesc(999L))
                .thenReturn(Optional.empty());
        
        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> analyticsAdminController.getBookAnalytics(999L));
        
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Analytics not found for book: 999"));
    }
    
    @Test
    void testGetBookAnalyticsHistory_Success_ShouldReturnHistory() {
        // Given
        LocalDateTime start = LocalDateTime.now().minusDays(7);
        LocalDateTime end = LocalDateTime.now();
        List<BookAnalytics> history = List.of(testBookAnalytics);
        
        when(bookAnalyticsRepository.findByBookIdAndAggregatedAtBetween(1L, start, end))
                .thenReturn(history);
        
        // When
        ResponseEntity<BookAnalyticsHistoryResponse> result = analyticsAdminController
                .getBookAnalyticsHistory(1L, start, end);
        
        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(1L, result.getBody().getBookId());
        assertEquals("Test Book", result.getBody().getBookTitle());
        assertNotNull(result.getBody().getHistory());
        assertEquals(1, result.getBody().getHistory().size());
    }
    
    @Test
    void testGetBookAnalyticsHistory_WithDefaultDates_ShouldUseDefaultRange() {
        // Given
        List<BookAnalytics> history = List.of(testBookAnalytics);
        
        when(bookAnalyticsRepository.findByBookIdAndAggregatedAtBetween(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(history);
        
        // When
        ResponseEntity<BookAnalyticsHistoryResponse> result = analyticsAdminController
                .getBookAnalyticsHistory(1L, null, null);
        
        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(bookAnalyticsRepository).findByBookIdAndAggregatedAtBetween(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class));
    }
    
    @Test
    void testGetBookAnalyticsHistory_EmptyHistory_ShouldThrowException() {
        // Given
        when(bookAnalyticsRepository.findByBookIdAndAggregatedAtBetween(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        
        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> analyticsAdminController.getBookAnalyticsHistory(1L, null, null));
        
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getReason().contains("No analytics history found"));
    }
    
    @Test
    void testGetSystemOverview_Success_ShouldReturnSystemAnalytics() {
        // Given
        when(systemAnalyticsRepository.findFirstByOrderByAggregatedAtDesc())
                .thenReturn(Optional.of(testSystemAnalytics));
        
        // When
        ResponseEntity<SystemAnalyticsResponse> result = analyticsAdminController.getSystemOverview();
        
        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(100, result.getBody().getTotalBooks());
        assertEquals(500, result.getBody().getTotalUsers());
        assertEquals(10000L, result.getBody().getTotalViews());
        verify(systemAnalyticsRepository).findFirstByOrderByAggregatedAtDesc();
    }
    
    @Test
    void testGetSystemOverview_NotFound_ShouldThrowException() {
        // Given
        when(systemAnalyticsRepository.findFirstByOrderByAggregatedAtDesc())
                .thenReturn(Optional.empty());
        
        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> analyticsAdminController.getSystemOverview());
        
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("System analytics not found", exception.getReason());
    }
    
    @Test
    void testGetPopularBooks_Success_ShouldReturnPopularBooks() {
        // Given
        List<BookAnalytics> allBooks = List.of(testBookAnalytics);
        
        when(bookAnalyticsRepository.findLatestForAllBooks())
                .thenReturn(allBooks);
        
        // When
        ResponseEntity<PopularBooksResponse> result = analyticsAdminController.getPopularBooks(10, "views");
        
        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertNotNull(result.getBody().getBooks());
        assertEquals("views", result.getBody().getSortBy());
        verify(bookAnalyticsRepository).findLatestForAllBooks();
    }
    
    @Test
    void testGetPopularBooks_WithDownloadsSort_ShouldSortByDownloads() {
        // Given
        List<BookAnalytics> allBooks = List.of(testBookAnalytics);
        
        when(bookAnalyticsRepository.findLatestForAllBooks())
                .thenReturn(allBooks);
        
        // When
        ResponseEntity<PopularBooksResponse> result = analyticsAdminController.getPopularBooks(10, "downloads");
        
        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("downloads", result.getBody().getSortBy());
    }
    
    @Test
    void testGetPopularBooks_WithPurchasesSort_ShouldSortByPurchases() {
        // Given
        List<BookAnalytics> allBooks = List.of(testBookAnalytics);
        
        when(bookAnalyticsRepository.findLatestForAllBooks())
                .thenReturn(allBooks);
        
        // When
        ResponseEntity<PopularBooksResponse> result = analyticsAdminController.getPopularBooks(10, "purchases");
        
        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("purchases", result.getBody().getSortBy());
    }
    
    @Test
    void testGetPopularBooks_WithRevenueSort_ShouldSortByRevenue() {
        // Given
        List<BookAnalytics> allBooks = List.of(testBookAnalytics);
        
        when(bookAnalyticsRepository.findLatestForAllBooks())
                .thenReturn(allBooks);
        
        // When
        ResponseEntity<PopularBooksResponse> result = analyticsAdminController.getPopularBooks(10, "revenue");
        
        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("revenue", result.getBody().getSortBy());
    }
    
    @Test
    void testGetPopularBooks_WithLimit_ShouldLimitResults() {
        // Given
        BookAnalytics analytics2 = BookAnalytics.builder()
                .bookId(2L)
                .bookTitle("Book 2")
                .viewCount(50L)
                .build();
        
        List<BookAnalytics> allBooks = List.of(testBookAnalytics, analytics2);
        
        when(bookAnalyticsRepository.findLatestForAllBooks())
                .thenReturn(allBooks);
        
        // When
        ResponseEntity<PopularBooksResponse> result = analyticsAdminController.getPopularBooks(1, "views");
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.getBody().getBooks().size());
    }
    
    @Test
    void testGetSystemOverviewHistory_Success_ShouldReturnHistory() {
        // Given
        LocalDateTime start = LocalDateTime.now().minusDays(7);
        LocalDateTime end = LocalDateTime.now();
        List<SystemAnalytics> history = List.of(testSystemAnalytics);
        
        when(systemAnalyticsRepository.findByAggregatedAtBetween(start, end))
                .thenReturn(history);
        
        // When
        ResponseEntity<SystemAnalyticsHistoryResponse> result = analyticsAdminController
                .getSystemOverviewHistory(start, end);
        
        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertNotNull(result.getBody().getHistory());
        assertEquals(1, result.getBody().getHistory().size());
    }
    
    @Test
    void testGetSystemOverviewHistory_WithDefaultDates_ShouldUseDefaultRange() {
        // Given
        List<SystemAnalytics> history = List.of(testSystemAnalytics);
        
        when(systemAnalyticsRepository.findByAggregatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(history);
        
        // When
        ResponseEntity<SystemAnalyticsHistoryResponse> result = analyticsAdminController
                .getSystemOverviewHistory(null, null);
        
        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(systemAnalyticsRepository).findByAggregatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class));
    }
    
    @Test
    void testGetSystemOverviewHistory_EmptyHistory_ShouldReturnEmptyList() {
        // Given
        when(systemAnalyticsRepository.findByAggregatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        
        // When
        ResponseEntity<SystemAnalyticsHistoryResponse> result = analyticsAdminController
                .getSystemOverviewHistory(null, null);
        
        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertTrue(result.getBody().getHistory().isEmpty());
    }
}
