package online.ityura.springdigitallibrary.unit.controller;

import online.ityura.springdigitallibrary.controller.AdminBookController;
import online.ityura.springdigitallibrary.dto.request.CreateBookRequest;
import online.ityura.springdigitallibrary.dto.request.PutBookRequest;
import online.ityura.springdigitallibrary.dto.request.UpdateBookRequest;
import online.ityura.springdigitallibrary.dto.response.AuthorResponse;
import online.ityura.springdigitallibrary.dto.response.BookResponse;
import online.ityura.springdigitallibrary.dto.response.MessageResponse;
import online.ityura.springdigitallibrary.service.AdminBookService;
import online.ityura.springdigitallibrary.service.BookImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminBookControllerTest {
    
    @Mock
    private AdminBookService adminBookService;
    
    @Mock
    private BookImageService bookImageService;
    
    @InjectMocks
    private AdminBookController adminBookController;
    
    private CreateBookRequest createRequest;
    private PutBookRequest putRequest;
    private UpdateBookRequest updateRequest;
    private BookResponse bookResponse;
    
    @BeforeEach
    void setUp() {
        createRequest = new CreateBookRequest();
        createRequest.setTitle("Test Book");
        createRequest.setAuthorName("Test Author");
        createRequest.setDescription("Test Description");
        
        putRequest = new PutBookRequest();
        putRequest.setTitle("Updated Book");
        putRequest.setAuthorName("Updated Author");
        putRequest.setDescription("Updated Description");
        
        updateRequest = new UpdateBookRequest();
        updateRequest.setTitle("Patched Book");
        
        bookResponse = BookResponse.builder()
                .id(1L)
                .title("Test Book")
                .description("Test Description")
                .author(AuthorResponse.builder()
                        .id(1L)
                        .fullName("Test Author")
                        .build())
                .build();
    }
    
    @Test
    void testCreateBook_Success_ShouldReturn201() {
        // Given
        when(adminBookService.createBook(any(CreateBookRequest.class))).thenReturn(bookResponse);
        
        // When
        ResponseEntity<BookResponse> response = adminBookController.createBook(createRequest);
        
        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getId());
        assertEquals("Test Book", response.getBody().getTitle());
    }
    
    @Test
    void testUpdateBook_Success_ShouldReturn200() {
        // Given
        BookResponse updatedResponse = BookResponse.builder()
                .id(1L)
                .title("Updated Book")
                .description("Updated Description")
                .author(AuthorResponse.builder()
                        .id(1L)
                        .fullName("Updated Author")
                        .build())
                .build();
        
        when(adminBookService.updateBook(anyLong(), any(PutBookRequest.class))).thenReturn(updatedResponse);
        
        // When
        ResponseEntity<BookResponse> response = adminBookController.updateBook(1L, putRequest);
        
        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Updated Book", response.getBody().getTitle());
    }
    
    @Test
    void testPatchBook_Success_ShouldReturn200() {
        // Given
        BookResponse patchedResponse = BookResponse.builder()
                .id(1L)
                .title("Patched Book")
                .author(AuthorResponse.builder()
                        .id(1L)
                        .fullName("Test Author")
                        .build())
                .build();
        
        when(adminBookService.patchBook(anyLong(), any(UpdateBookRequest.class), isNull(), isNull()))
                .thenReturn(patchedResponse);
        
        // When
        ResponseEntity<BookResponse> response = adminBookController.patchBook(1L, updateRequest);
        
        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Patched Book", response.getBody().getTitle());
    }
    
    @Test
    void testDeleteBook_Success_ShouldReturn204() {
        // Given
        doNothing().when(adminBookService).deleteBook(anyLong());
        
        // When
        ResponseEntity<Void> response = adminBookController.deleteBook(1L);
        
        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verify(adminBookService).deleteBook(1L);
    }
    
    @Test
    void testUploadBookImage_Success_ShouldReturn200() {
        // Given
        MultipartFile mockFile = mock(MultipartFile.class);
        when(bookImageService.uploadBookImage(anyLong(), any(MultipartFile.class))).thenReturn("image.jpg");
        
        // When
        ResponseEntity<MessageResponse> response = adminBookController.uploadBookImage(1L, mockFile);
        
        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Image uploaded successfully", response.getBody().getMessage());
        verify(bookImageService).uploadBookImage(1L, mockFile);
    }
}

