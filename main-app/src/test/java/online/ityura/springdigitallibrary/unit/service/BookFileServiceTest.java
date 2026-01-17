package online.ityura.springdigitallibrary.unit.service;

import online.ityura.springdigitallibrary.model.Book;
import online.ityura.springdigitallibrary.repository.BookRepository;
import online.ityura.springdigitallibrary.service.BookFileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookFileServiceTest {
    
    @Mock
    private BookRepository bookRepository;
    
    @InjectMocks
    private BookFileService bookFileService;
    
    private Book testBook;
    
    @BeforeEach
    void setUp() {
        testBook = Book.builder()
                .id(1L)
                .title("Test Book")
                .pdfPath("test/path/to/book.pdf")
                .build();
    }
    
    @Test
    void testDownloadBookFile_BookNotFound_ShouldThrowException() {
        // Given
        when(bookRepository.findById(999L)).thenReturn(Optional.empty());
        
        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
                () -> bookFileService.downloadBookFile(999L, 1L));
        
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Book not found"));
    }
    
    @Test
    void testDownloadBookFile_NoPdfPath_ShouldThrowException() {
        // Given
        testBook.setPdfPath(null);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        
        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
                () -> bookFileService.downloadBookFile(1L, 1L));
        
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getReason().contains("PDF file not found"));
    }
    
    @Test
    void testDownloadBookFile_EmptyPdfPath_ShouldThrowException() {
        // Given
        testBook.setPdfPath("");
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        
        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
                () -> bookFileService.downloadBookFile(1L, 1L));
        
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getReason().contains("PDF file not found"));
    }
    
    @Test
    void testGetOriginalFilename_BookNotFound_ShouldThrowException() {
        // Given
        when(bookRepository.findById(999L)).thenReturn(Optional.empty());
        
        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
                () -> bookFileService.getOriginalFilename(999L));
        
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Book not found"));
    }
    
    @Test
    void testGetOriginalFilename_NoPdfPath_ShouldThrowException() {
        // Given
        testBook.setPdfPath(null);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        
        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
                () -> bookFileService.getOriginalFilename(1L));
        
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getReason().contains("PDF file not found"));
    }
}

