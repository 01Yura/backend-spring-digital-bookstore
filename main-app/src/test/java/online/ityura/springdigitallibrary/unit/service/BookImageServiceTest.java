package online.ityura.springdigitallibrary.unit.service;

import online.ityura.springdigitallibrary.model.Book;
import online.ityura.springdigitallibrary.repository.BookRepository;
import online.ityura.springdigitallibrary.service.BookImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookImageServiceTest {
    
    @Mock
    private BookRepository bookRepository;
    
    @InjectMocks
    private BookImageService bookImageService;
    
    private Book testBook;
    private MultipartFile mockFile;
    
    @BeforeEach
    void setUp() {
        testBook = Book.builder()
                .id(1L)
                .title("Test Book")
                .build();
        
        mockFile = mock(MultipartFile.class);
        
        // Устанавливаем storagePath через ReflectionTestUtils
        ReflectionTestUtils.setField(bookImageService, "storagePath", "test/storage/path");
    }
    
    @Test
    void testUploadBookImage_BookNotFound_ShouldThrowException() {
        // Given
        when(bookRepository.findById(999L)).thenReturn(Optional.empty());
        
        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
                () -> bookImageService.uploadBookImage(999L, mockFile));
        
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Book not found"));
    }
    
    @Test
    void testUploadBookImage_EmptyFile_ShouldThrowException() {
        // Given
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(mockFile.isEmpty()).thenReturn(true);
        
        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
                () -> bookImageService.uploadBookImage(1L, mockFile));
        
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Image file is required"));
    }
    
    @Test
    void testUploadBookImage_NullFile_ShouldThrowException() {
        // Given
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        
        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
                () -> bookImageService.uploadBookImage(1L, null));
        
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Image file is required"));
    }
    
    @Test
    void testGetBookImage_BookNotFound_ShouldThrowException() {
        // Given
        when(bookRepository.findById(999L)).thenReturn(Optional.empty());
        
        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
                () -> bookImageService.getBookImage(999L));
        
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Book not found"));
    }
}

