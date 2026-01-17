package online.ityura.springdigitallibrary.unit.service;

import online.ityura.springdigitallibrary.dto.request.CreateRatingRequest;
import online.ityura.springdigitallibrary.dto.request.UpdateRatingRequest;
import online.ityura.springdigitallibrary.dto.response.RatingResponse;
import online.ityura.springdigitallibrary.model.Author;
import online.ityura.springdigitallibrary.model.Book;
import online.ityura.springdigitallibrary.model.Rating;
import online.ityura.springdigitallibrary.model.Role;
import online.ityura.springdigitallibrary.model.User;
import online.ityura.springdigitallibrary.repository.BookRepository;
import online.ityura.springdigitallibrary.repository.RatingRepository;
import online.ityura.springdigitallibrary.repository.UserRepository;
import online.ityura.springdigitallibrary.service.RatingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RatingServiceTest {
    
    @Mock
    private RatingRepository ratingRepository;
    
    @Mock
    private BookRepository bookRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private RatingService ratingService;
    
    private Author testAuthor;
    private Book testBook;
    private User testUser;
    private Rating testRating;
    private CreateRatingRequest createRequest;
    private UpdateRatingRequest updateRequest;
    
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
                .ratingAvg(BigDecimal.ZERO)
                .ratingCount(0)
                .build();
        
        testUser = User.builder()
                .id(1L)
                .nickname("testuser")
                .email("test@example.com")
                .passwordHash("encodedPassword")
                .role(Role.USER)
                .build();
        
        testRating = Rating.builder()
                .id(1L)
                .book(testBook)
                .user(testUser)
                .value((short) 5)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        createRequest = new CreateRatingRequest();
        createRequest.setValue((short) 5);
        
        updateRequest = new UpdateRatingRequest();
        updateRequest.setValue((short) 4);
    }
    
    @Test
    void testCreateRating_Success_ShouldReturnRatingResponse() {
        // Given
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(ratingRepository.existsByBookIdAndUserId(1L, 1L)).thenReturn(false);
        when(ratingRepository.save(any(Rating.class))).thenReturn(testRating);
        when(ratingRepository.calculateAverageRating(1L)).thenReturn(5.0);
        when(ratingRepository.countByBookId(1L)).thenReturn(1L);
        when(bookRepository.save(any(Book.class))).thenReturn(testBook);
        
        // When
        RatingResponse response = ratingService.createRating(1L, 1L, createRequest);
        
        // Then
        assertNotNull(response);
        assertEquals(testRating.getId(), response.getId());
        assertEquals(testRating.getBook().getId(), response.getBookId());
        assertEquals(testRating.getUser().getId(), response.getUserId());
        assertEquals(testRating.getValue(), response.getValue());
        
        verify(bookRepository, times(2)).findById(1L); // Called in createRating and updateBookRating
        verify(userRepository).findById(1L);
        verify(ratingRepository).existsByBookIdAndUserId(1L, 1L);
        verify(ratingRepository).save(any(Rating.class));
        verify(ratingRepository).calculateAverageRating(1L);
        verify(ratingRepository).countByBookId(1L);
        verify(bookRepository).save(any(Book.class));
    }
    
    @Test
    void testCreateRating_BookNotFound_ShouldThrowException() {
        // Given
        when(bookRepository.findById(999L)).thenReturn(Optional.empty());
        
        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
                () -> ratingService.createRating(999L, 1L, createRequest));
        
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Book not found"));
        
        verify(bookRepository).findById(999L);
        verify(userRepository, never()).findById(anyLong());
        verify(ratingRepository, never()).save(any(Rating.class));
    }
    
    @Test
    void testCreateRating_UserNotFound_ShouldThrowException() {
        // Given
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        
        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
                () -> ratingService.createRating(1L, 999L, createRequest));
        
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getReason().contains("User not found"));
        
        verify(bookRepository).findById(1L);
        verify(userRepository).findById(999L);
        verify(ratingRepository, never()).save(any(Rating.class));
    }
    
    @Test
    void testCreateRating_RatingAlreadyExists_ShouldThrowException() {
        // Given
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(ratingRepository.existsByBookIdAndUserId(1L, 1L)).thenReturn(true);
        
        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
                () -> ratingService.createRating(1L, 1L, createRequest));
        
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Rating already exists"));
        
        verify(ratingRepository, never()).save(any(Rating.class));
    }
    
    @Test
    void testUpdateRating_Success_ShouldReturnUpdatedRatingResponse() {
        // Given
        Rating updatedRating = Rating.builder()
                .id(1L)
                .book(testBook)
                .user(testUser)
                .value((short) 4)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        when(ratingRepository.findByBookIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(testRating));
        when(ratingRepository.save(any(Rating.class))).thenReturn(updatedRating);
        when(ratingRepository.calculateAverageRating(1L)).thenReturn(4.5);
        when(ratingRepository.countByBookId(1L)).thenReturn(1L);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(bookRepository.save(any(Book.class))).thenReturn(testBook);
        
        // When
        RatingResponse response = ratingService.updateRating(1L, 1L, updateRequest);
        
        // Then
        assertNotNull(response);
        assertEquals(updatedRating.getValue(), response.getValue());
        
        verify(ratingRepository).findByBookIdAndUserId(1L, 1L);
        verify(ratingRepository).save(any(Rating.class));
        verify(ratingRepository).calculateAverageRating(1L);
        verify(bookRepository).save(any(Book.class));
    }
    
    @Test
    void testUpdateRating_RatingNotFound_ShouldThrowException() {
        // Given
        when(ratingRepository.findByBookIdAndUserId(1L, 1L))
                .thenReturn(Optional.empty());
        
        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
                () -> ratingService.updateRating(1L, 1L, updateRequest));
        
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Rating not found", exception.getReason());
        
        verify(ratingRepository, never()).save(any(Rating.class));
    }
    
    @Test
    void testCreateRating_ShouldUpdateBookRating() {
        // Given
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(ratingRepository.existsByBookIdAndUserId(1L, 1L)).thenReturn(false);
        when(ratingRepository.save(any(Rating.class))).thenReturn(testRating);
        when(ratingRepository.calculateAverageRating(1L)).thenReturn(4.75);
        when(ratingRepository.countByBookId(1L)).thenReturn(2L);
        when(bookRepository.save(any(Book.class))).thenReturn(testBook);
        
        // When
        ratingService.createRating(1L, 1L, createRequest);
        
        // Then
        verify(bookRepository).save(argThat(book -> 
                book.getRatingAvg().equals(BigDecimal.valueOf(4.75).setScale(2)) &&
                book.getRatingCount() == 2
        ));
    }
}

