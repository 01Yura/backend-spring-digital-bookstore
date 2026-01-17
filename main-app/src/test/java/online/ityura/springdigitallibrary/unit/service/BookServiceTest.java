package online.ityura.springdigitallibrary.unit.service;

import online.ityura.springdigitallibrary.dto.response.BookResponse;
import online.ityura.springdigitallibrary.model.Author;
import online.ityura.springdigitallibrary.model.Book;
import online.ityura.springdigitallibrary.model.Genre;
import online.ityura.springdigitallibrary.model.Review;
import online.ityura.springdigitallibrary.model.Role;
import online.ityura.springdigitallibrary.model.User;
import online.ityura.springdigitallibrary.repository.BookRepository;
import online.ityura.springdigitallibrary.repository.ReviewRepository;
import online.ityura.springdigitallibrary.service.BookService;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {
    
    @Mock
    private BookRepository bookRepository;
    
    @Mock
    private ReviewRepository reviewRepository;
    
    @InjectMocks
    private BookService bookService;
    
    private Author testAuthor;
    private Book testBook;
    private User testUser;
    private Review testReview;
    
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
                .description("Test Description")
                .publishedYear(2023)
                .genre(Genre.FICTION)
                .ratingAvg(BigDecimal.valueOf(4.5))
                .ratingCount(10)
                .imagePath("/images/test.jpg")
                .pdfPath("/pdf/test.pdf")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
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
    }
    
    @Test
    void testGetAllBooks_ShouldReturnPageOfBooks() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Book> bookPage = new PageImpl<>(List.of(testBook), pageable, 1);
        
        when(bookRepository.findAll(pageable)).thenReturn(bookPage);
        when(reviewRepository.findByBookIdIn(anyList())).thenReturn(Collections.emptyList());
        
        // When
        Page<BookResponse> result = bookService.getAllBooks(pageable, null);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        
        BookResponse bookResponse = result.getContent().get(0);
        assertEquals(testBook.getId(), bookResponse.getId());
        assertEquals(testBook.getTitle(), bookResponse.getTitle());
        assertEquals(testBook.getDescription(), bookResponse.getDescription());
        assertTrue(bookResponse.getHasFile());
        
        verify(bookRepository).findAll(pageable);
        verify(reviewRepository).findByBookIdIn(anyList());
    }
    
    @Test
    void testGetAllBooks_WithReviews_ShouldIncludeReviews() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Book> bookPage = new PageImpl<>(List.of(testBook), pageable, 1);
        
        when(bookRepository.findAll(pageable)).thenReturn(bookPage);
        when(reviewRepository.findByBookIdIn(anyList())).thenReturn(List.of(testReview));
        
        // When
        Page<BookResponse> result = bookService.getAllBooks(pageable, null);
        
        // Then
        assertNotNull(result);
        BookResponse bookResponse = result.getContent().get(0);
        assertNotNull(bookResponse.getReviews());
        assertEquals(1, bookResponse.getReviews().size());
        
        verify(bookRepository).findAll(pageable);
        verify(reviewRepository).findByBookIdIn(anyList());
    }
    
    @Test
    void testGetAllBooks_EmptyPage_ShouldReturnEmptyPage() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Book> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        
        when(bookRepository.findAll(pageable)).thenReturn(emptyPage);
        
        // When
        Page<BookResponse> result = bookService.getAllBooks(pageable, null);
        
        // Then
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        
        verify(bookRepository).findAll(pageable);
        verify(reviewRepository, never()).findByBookIdIn(anyList());
    }
    
    @Test
    void testGetBookById_ShouldReturnBookResponse() {
        // Given
        when(bookRepository.findByIdWithAuthor(1L)).thenReturn(Optional.of(testBook));
        when(reviewRepository.findByBookIdWithUserOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(testReview));
        
        // When
        BookResponse result = bookService.getBookById(1L);
        
        // Then
        assertNotNull(result);
        assertEquals(testBook.getId(), result.getId());
        assertEquals(testBook.getTitle(), result.getTitle());
        assertEquals(testBook.getDescription(), result.getDescription());
        assertEquals(testBook.getPublishedYear(), result.getPublishedYear());
        assertEquals(testBook.getGenre(), result.getGenre());
        assertTrue(result.getHasFile());
        assertNotNull(result.getReviews());
        assertEquals(1, result.getReviews().size());
        
        verify(bookRepository).findByIdWithAuthor(1L);
        verify(reviewRepository).findByBookIdWithUserOrderByCreatedAtDesc(1L);
    }
    
    @Test
    void testGetBookById_BookNotFound_ShouldThrowException() {
        // Given
        when(bookRepository.findByIdWithAuthor(999L)).thenReturn(Optional.empty());
        
        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
                () -> bookService.getBookById(999L));
        
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Book not found"));
        
        verify(bookRepository).findByIdWithAuthor(999L);
        verify(reviewRepository, never()).findByBookIdWithUserOrderByCreatedAtDesc(anyLong());
    }
    
    @Test
    void testGetBookById_BookWithoutPdf_ShouldReturnHasFileFalse() {
        // Given
        Book bookWithoutPdf = Book.builder()
                .id(2L)
                .title("Book Without PDF")
                .author(testAuthor)
                .pdfPath(null)
                .build();
        
        when(bookRepository.findByIdWithAuthor(2L)).thenReturn(Optional.of(bookWithoutPdf));
        when(reviewRepository.findByBookIdWithUserOrderByCreatedAtDesc(2L))
                .thenReturn(Collections.emptyList());
        
        // When
        BookResponse result = bookService.getBookById(2L);
        
        // Then
        assertNotNull(result);
        assertFalse(result.getHasFile());
    }
    
    @Test
    void testGetBookById_BookWithEmptyPdfPath_ShouldReturnHasFileFalse() {
        // Given
        Book bookWithEmptyPdf = Book.builder()
                .id(3L)
                .title("Book With Empty PDF")
                .author(testAuthor)
                .pdfPath("")
                .build();
        
        when(bookRepository.findByIdWithAuthor(3L)).thenReturn(Optional.of(bookWithEmptyPdf));
        when(reviewRepository.findByBookIdWithUserOrderByCreatedAtDesc(3L))
                .thenReturn(Collections.emptyList());
        
        // When
        BookResponse result = bookService.getBookById(3L);
        
        // Then
        assertNotNull(result);
        assertFalse(result.getHasFile());
    }
}

