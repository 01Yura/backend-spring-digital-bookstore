package online.ityura.springdigitallibrary.unit.controller;

import online.ityura.springdigitallibrary.controller.BookController;
import online.ityura.springdigitallibrary.dto.response.AuthorResponse;
import online.ityura.springdigitallibrary.dto.response.BookResponse;
import online.ityura.springdigitallibrary.repository.UserRepository;
import online.ityura.springdigitallibrary.service.BookImageService;
import online.ityura.springdigitallibrary.service.BookService;
import online.ityura.springdigitallibrary.service.KafkaProducerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.net.URI;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class BookControllerTest {
    
    @Mock
    private BookService bookService;
    
    @Mock
    private BookImageService bookImageService;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private KafkaProducerService kafkaProducerService;
    
    @Mock
    private Authentication authentication;
    
    @InjectMocks
    private BookController bookController;
    
    private MockMvc mockMvc;
    
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(bookController).build();
    }
    
    @Test
    void testGetAllBooks_UnitTest_ShouldCallService() {
        // Given
        BookResponse bookResponse = BookResponse.builder()
                .id(1L)
                .title("Test Book")
                .author(AuthorResponse.builder()
                        .id(1L)
                        .fullName("Test Author")
                        .build())
                .build();
        
        Page<BookResponse> page = new PageImpl<>(
                List.of(bookResponse),
                PageRequest.of(0, 10),
                1
        );
        
        when(bookService.getAllBooks(any(), isNull())).thenReturn(page);
        
        // When
        ResponseEntity<Page<BookResponse>> result = bookController.getAllBooks(null, PageRequest.of(0, 10));
        
        // Then
        assertNotNull(result);
        assertNotNull(result.getBody());
        assertEquals(1, result.getBody().getTotalElements());
        assertEquals(1, result.getBody().getContent().size());
        assertEquals(1L, result.getBody().getContent().get(0).getId());
    }
    
    @Test
    void testGetAllBooks_EmptyList_UnitTest_ShouldReturnEmptyPage() {
        // Given
        Page<BookResponse> emptyPage = new PageImpl<>(
                Collections.emptyList(),
                PageRequest.of(0, 10),
                0
        );
        
        when(bookService.getAllBooks(any(), isNull())).thenReturn(emptyPage);
        
        // When
        ResponseEntity<Page<BookResponse>> result = bookController.getAllBooks(null, PageRequest.of(0, 10));
        
        // Then
        assertNotNull(result);
        assertNotNull(result.getBody());
        assertEquals(0, result.getBody().getTotalElements());
        assertTrue(result.getBody().getContent().isEmpty());
    }
    
    @Test
    void testGetBookById_Success_ShouldReturn200() throws Exception {
        // Given
        BookResponse bookResponse = BookResponse.builder()
                .id(1L)
                .title("Test Book")
                .description("Test Description")
                .author(AuthorResponse.builder()
                        .id(1L)
                        .fullName("Test Author")
                        .build())
                .build();
        
        when(bookService.getBookById(1L)).thenReturn(bookResponse);
        
        // When & Then
        mockMvc.perform(get("/api/v1/books/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Test Book"))
                .andExpect(jsonPath("$.description").value("Test Description"));
    }
    
    @Test
    void testGetBookById_UnitTest_ShouldCallService() {
        // Given
        BookResponse bookResponse = BookResponse.builder()
                .id(1L)
                .title("Test Book")
                .description("Test Description")
                .author(AuthorResponse.builder()
                        .id(1L)
                        .fullName("Test Author")
                        .build())
                .build();
        
        when(bookService.getBookById(1L)).thenReturn(bookResponse);
        
        // When
        ResponseEntity<BookResponse> result = bookController.getBookById(1L, null);
        
        // Then
        assertNotNull(result);
        assertNotNull(result.getBody());
        assertEquals(1L, result.getBody().getId());
        assertEquals("Test Book", result.getBody().getTitle());
    }
    
    @Test
    void testGetBookImage_UnitTest_ShouldCallService() throws Exception {
        // Given
        Resource mockResource = new ByteArrayResource("test".getBytes()) {
            @Override
            public URI getURI() {
                try {
                    return new URI("file:///test/image.png");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
        
        when(bookImageService.getBookImage(1L)).thenReturn(mockResource);
        
        // When
        ResponseEntity<Resource> result = bookController.getBookImage(1L);
        
        // Then
        assertNotNull(result);
        assertNotNull(result.getBody());
        assertNotNull(result.getHeaders().getContentType());
    }

    @Test
    void testGetAllBookImages_Success_ShouldReturn200() throws Exception {
        // Given
        Resource mockZipResource = new ByteArrayResource("zip content".getBytes());
        
        when(bookImageService.getAllBookImagesAsZip()).thenReturn(mockZipResource);
        
        // When & Then
        mockMvc.perform(get("/api/v1/books/images/all")
                        .contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"book-images.zip\""))
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM));
        
        verify(bookImageService).getAllBookImagesAsZip();
    }

    @Test
    void testGetAllBookImages_UnitTest_ShouldCallService() {
        // Given
        Resource mockZipResource = new ByteArrayResource("zip content".getBytes());
        
        when(bookImageService.getAllBookImagesAsZip()).thenReturn(mockZipResource);
        
        // When
        ResponseEntity<Resource> result = bookController.getAllBookImages();
        
        // Then
        assertNotNull(result);
        assertEquals(200, result.getStatusCode().value());
        assertNotNull(result.getBody());
        assertNotNull(result.getHeaders().get("Content-Disposition"));
        assertTrue(result.getHeaders().get("Content-Disposition").get(0).contains("book-images.zip"));
        assertEquals(MediaType.APPLICATION_OCTET_STREAM, result.getHeaders().getContentType());
    }
}
