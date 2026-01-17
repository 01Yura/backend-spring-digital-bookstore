package online.ityura.springdigitallibrary.unit.service;

import online.ityura.springdigitallibrary.dto.request.CreateBookRequest;
import online.ityura.springdigitallibrary.dto.request.PutBookRequest;
import online.ityura.springdigitallibrary.dto.request.UpdateBookRequest;
import online.ityura.springdigitallibrary.model.Author;
import online.ityura.springdigitallibrary.model.Book;
import online.ityura.springdigitallibrary.model.Genre;
import online.ityura.springdigitallibrary.repository.AuthorRepository;
import online.ityura.springdigitallibrary.repository.BookRepository;
import online.ityura.springdigitallibrary.repository.ReviewRepository;
import online.ityura.springdigitallibrary.service.AdminBookService;
import online.ityura.springdigitallibrary.service.BookImageService;
import online.ityura.springdigitallibrary.service.BookFileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminBookServiceTest {
    
    @Mock
    private BookRepository bookRepository;
    
    @Mock
    private AuthorRepository authorRepository;
    
    @Mock
    private ReviewRepository reviewRepository;
    
    @Mock
    private BookImageService bookImageService;
    
    @Mock
    private BookFileService bookFileService;
    
    @InjectMocks
    private AdminBookService adminBookService;
    
    private Author testAuthor;
    private Book testBook;
    private CreateBookRequest createRequest;
    private PutBookRequest putRequest;
    private UpdateBookRequest updateRequest;
    
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
                .deletionLocked(false)
                .ratingAvg(BigDecimal.ZERO)
                .ratingCount(0)
                .build();
        
        createRequest = new CreateBookRequest();
        createRequest.setTitle("New Book");
        createRequest.setAuthorName("Test Author");
        createRequest.setDescription("New Description");
        createRequest.setPublishedYear(2024);
        createRequest.setGenre(Genre.SCIENCE_FICTION);
        
        putRequest = new PutBookRequest();
        putRequest.setTitle("Updated Book");
        putRequest.setAuthorName("Updated Author");
        putRequest.setDescription("Updated Description");
        putRequest.setPublishedYear(2025);
        putRequest.setGenre(Genre.MYSTERY);
        
        updateRequest = new UpdateBookRequest();
        updateRequest.setTitle("Patched Book");
    }
    
    @Test
    void testCreateBook_Success_WithExistingAuthor() {
        // Given
        when(authorRepository.findByFullName("Test Author")).thenReturn(Optional.of(testAuthor));
        when(bookRepository.existsByTitleAndAuthorId("New Book", 1L)).thenReturn(false);
        when(bookRepository.save(any(Book.class))).thenReturn(testBook);
        
        // When
        var response = adminBookService.createBook(createRequest);
        
        // Then
        assertNotNull(response);
        verify(authorRepository).findByFullName("Test Author");
        verify(bookRepository).existsByTitleAndAuthorId("New Book", 1L);
        verify(bookRepository).save(any(Book.class));
    }
    
    @Test
    void testCreateBook_Success_WithNewAuthor() {
        // Given
        when(authorRepository.findByFullName("New Author")).thenReturn(Optional.empty());
        when(authorRepository.save(any(Author.class))).thenReturn(testAuthor);
        when(bookRepository.existsByTitleAndAuthorId(anyString(), anyLong())).thenReturn(false);
        when(bookRepository.save(any(Book.class))).thenReturn(testBook);
        
        createRequest.setAuthorName("New Author");
        
        // When
        var response = adminBookService.createBook(createRequest);
        
        // Then
        assertNotNull(response);
        verify(authorRepository).findByFullName("New Author");
        verify(authorRepository).save(any(Author.class));
        verify(bookRepository).save(any(Book.class));
    }
    
    @Test
    void testCreateBook_BookAlreadyExists_ShouldThrowException() {
        // Given
        when(authorRepository.findByFullName("Test Author")).thenReturn(Optional.of(testAuthor));
        when(bookRepository.existsByTitleAndAuthorId("New Book", 1L)).thenReturn(true);
        
        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
                () -> adminBookService.createBook(createRequest));
        
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertTrue(exception.getReason().contains("already exists"));
        verify(bookRepository, never()).save(any(Book.class));
    }
    
    @Test
    void testUpdateBook_Success_ShouldReturnUpdatedBook() {
        // Given
        when(bookRepository.findByIdWithAuthor(1L)).thenReturn(Optional.of(testBook));
        when(authorRepository.findByFullName("Updated Author")).thenReturn(Optional.of(testAuthor));
        when(bookRepository.existsByTitleAndAuthorId("Updated Book", 1L)).thenReturn(false);
        when(bookRepository.save(any(Book.class))).thenReturn(testBook);
        
        // When
        var response = adminBookService.updateBook(1L, putRequest);
        
        // Then
        assertNotNull(response);
        verify(bookRepository).findByIdWithAuthor(1L);
        verify(bookRepository).save(any(Book.class));
    }
    
    @Test
    void testUpdateBook_BookNotFound_ShouldThrowException() {
        // Given
        when(bookRepository.findByIdWithAuthor(999L)).thenReturn(Optional.empty());
        
        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
                () -> adminBookService.updateBook(999L, putRequest));
        
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Book not found"));
    }
    
    @Test
    void testPatchBook_Success_ShouldReturnPatchedBook() {
        // Given
        when(bookRepository.findByIdWithAuthor(1L)).thenReturn(Optional.of(testBook));
        when(bookRepository.save(any(Book.class))).thenReturn(testBook);
        
        // When
        var response = adminBookService.patchBook(1L, updateRequest, null, null);
        
        // Then
        assertNotNull(response);
        verify(bookRepository).findByIdWithAuthor(1L);
        verify(bookRepository).save(any(Book.class));
    }
    
    @Test
    void testDeleteBook_Success() {
        // Given
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(reviewRepository.countByBookId(1L)).thenReturn(0L);
        doNothing().when(bookRepository).delete(any(Book.class));
        
        // When
        adminBookService.deleteBook(1L);
        
        // Then
        verify(bookRepository).findById(1L);
        verify(reviewRepository).countByBookId(1L);
        verify(bookRepository).delete(any(Book.class));
    }
    
    @Test
    void testDeleteBook_DeletionLocked_ShouldThrowException() {
        // Given
        testBook.setDeletionLocked(true);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        
        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
                () -> adminBookService.deleteBook(1L));
        
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertTrue(exception.getReason().contains("deletion is locked"));
        verify(bookRepository, never()).delete(any(Book.class));
    }
    
    @Test
    void testDeleteBook_HasReviews_ShouldThrowException() {
        // Given
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(reviewRepository.countByBookId(1L)).thenReturn(5L);
        
        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
                () -> adminBookService.deleteBook(1L));
        
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertTrue(exception.getReason().contains("has reviews"));
        verify(bookRepository, never()).delete(any(Book.class));
    }
}

