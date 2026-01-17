package online.ityura.springdigitallibrary.unit.service;

import online.ityura.springdigitallibrary.dto.request.CreateReviewRequest;
import online.ityura.springdigitallibrary.dto.request.UpdateReviewRequest;
import online.ityura.springdigitallibrary.dto.response.ReviewResponse;
import online.ityura.springdigitallibrary.model.Author;
import online.ityura.springdigitallibrary.model.Book;
import online.ityura.springdigitallibrary.model.Review;
import online.ityura.springdigitallibrary.model.Role;
import online.ityura.springdigitallibrary.model.User;
import online.ityura.springdigitallibrary.repository.BookRepository;
import online.ityura.springdigitallibrary.repository.ReviewRepository;
import online.ityura.springdigitallibrary.repository.UserRepository;
import online.ityura.springdigitallibrary.service.ReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {
    
    @Mock
    private ReviewRepository reviewRepository;
    
    @Mock
    private BookRepository bookRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private ReviewService reviewService;
    
    private Author testAuthor;
    private Book testBook;
    private User testUser;
    private Review testReview;
    private CreateReviewRequest createRequest;
    private UpdateReviewRequest updateRequest;
    
    @BeforeEach
    void setUp() {
        testAuthor = Author.builder()
                .id(1L)
                .fullName("Test Author")
                .build();
        
        testBook = Book.builder()
                .id(1L)
                .title("Test Book")
                .author(testAuthor)
                .build();
        
        testUser = User.builder()
                .id(1L)
                .nickname("testuser")
                .email("test@example.com")
                .passwordHash("encodedPassword")
                .role(Role.USER)
                .build();
        
        testReview = Review.builder()
                .id(1L)
                .book(testBook)
                .user(testUser)
                .text("Great book!")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        createRequest = new CreateReviewRequest();
        createRequest.setText("Great book!");
        
        updateRequest = new UpdateReviewRequest();
        updateRequest.setText("Updated review text");
    }
    
    @Test
    void testCreateReview_Success_ShouldReturnReviewResponse() {
        // Given
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(reviewRepository.existsByBookIdAndUserId(1L, 1L)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenReturn(testReview);
        
        // When
        ReviewResponse response = reviewService.createReview(1L, 1L, createRequest);
        
        // Then
        assertNotNull(response);
        assertEquals(testReview.getId(), response.getId());
        assertEquals(testReview.getBook().getId(), response.getBookId());
        assertEquals(testReview.getUser().getId(), response.getUser().getId());
        assertEquals(testReview.getText(), response.getText());
        
        verify(bookRepository).findById(1L);
        verify(userRepository).findById(1L);
        verify(reviewRepository).existsByBookIdAndUserId(1L, 1L);
        verify(reviewRepository).save(any(Review.class));
    }
    
    @Test
    void testCreateReview_BookNotFound_ShouldThrowException() {
        // Given
        when(bookRepository.findById(999L)).thenReturn(Optional.empty());
        
        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
                () -> reviewService.createReview(999L, 1L, createRequest));
        
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Book not found"));
        
        verify(bookRepository).findById(999L);
        verify(userRepository, never()).findById(anyLong());
        verify(reviewRepository, never()).save(any(Review.class));
    }
    
    @Test
    void testCreateReview_UserNotFound_ShouldThrowException() {
        // Given
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        
        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
                () -> reviewService.createReview(1L, 999L, createRequest));
        
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getReason().contains("User not found"));
        
        verify(bookRepository).findById(1L);
        verify(userRepository).findById(999L);
        verify(reviewRepository, never()).save(any(Review.class));
    }
    
    @Test
    void testCreateReview_ReviewAlreadyExists_ShouldThrowException() {
        // Given
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(reviewRepository.existsByBookIdAndUserId(1L, 1L)).thenReturn(true);
        
        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
                () -> reviewService.createReview(1L, 1L, createRequest));
        
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Review already exists"));
        
        verify(reviewRepository, never()).save(any(Review.class));
    }
    
    @Test
    void testUpdateReview_Success_ShouldReturnUpdatedReviewResponse() {
        // Given
        Review updatedReview = Review.builder()
                .id(1L)
                .book(testBook)
                .user(testUser)
                .text("Updated review text")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        when(reviewRepository.findByBookIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(testReview));
        when(reviewRepository.save(any(Review.class))).thenReturn(updatedReview);
        
        // When
        ReviewResponse response = reviewService.updateReview(1L, 1L, updateRequest);
        
        // Then
        assertNotNull(response);
        assertEquals(updatedReview.getText(), response.getText());
        
        verify(reviewRepository).findByBookIdAndUserId(1L, 1L);
        verify(reviewRepository).save(any(Review.class));
    }
    
    @Test
    void testUpdateReview_ReviewNotFound_ShouldThrowException() {
        // Given
        when(reviewRepository.findByBookIdAndUserId(1L, 1L))
                .thenReturn(Optional.empty());
        
        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
                () -> reviewService.updateReview(1L, 1L, updateRequest));
        
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Review not found", exception.getReason());
        
        verify(reviewRepository, never()).save(any(Review.class));
    }
    
    @Test
    void testGetReviewsByBookId_ShouldReturnPageOfReviews() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Review> reviewPage = new PageImpl<>(List.of(testReview), pageable, 1);
        
        when(reviewRepository.findByBookIdOrderByCreatedAtDesc(1L, pageable))
                .thenReturn(reviewPage);
        
        // When
        Page<ReviewResponse> result = reviewService.getReviewsByBookId(1L, pageable);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        
        ReviewResponse reviewResponse = result.getContent().get(0);
        assertEquals(testReview.getId(), reviewResponse.getId());
        assertEquals(testReview.getText(), reviewResponse.getText());
        
        verify(reviewRepository).findByBookIdOrderByCreatedAtDesc(1L, pageable);
    }
    
    @Test
    void testGetMyReview_Success_ShouldReturnReviewResponse() {
        // Given
        when(reviewRepository.findByBookIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(testReview));
        
        // When
        ReviewResponse response = reviewService.getMyReview(1L, 1L);
        
        // Then
        assertNotNull(response);
        assertEquals(testReview.getId(), response.getId());
        assertEquals(testReview.getText(), response.getText());
        
        verify(reviewRepository).findByBookIdAndUserId(1L, 1L);
    }
    
    @Test
    void testGetMyReview_ReviewNotFound_ShouldThrowException() {
        // Given
        when(reviewRepository.findByBookIdAndUserId(1L, 1L))
                .thenReturn(Optional.empty());
        
        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
                () -> reviewService.getMyReview(1L, 1L));
        
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Review not found", exception.getReason());
    }
    
    @Test
    void testGetMyReviews_ShouldReturnPageOfReviews() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Review> reviewPage = new PageImpl<>(List.of(testReview), pageable, 1);
        
        when(reviewRepository.findByUserIdOrderByCreatedAtDesc(1L, pageable))
                .thenReturn(reviewPage);
        
        // When
        Page<ReviewResponse> result = reviewService.getMyReviews(1L, pageable);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        
        verify(reviewRepository).findByUserIdOrderByCreatedAtDesc(1L, pageable);
    }
}

