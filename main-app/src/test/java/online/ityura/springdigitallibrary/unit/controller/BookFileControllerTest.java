package online.ityura.springdigitallibrary.unit.controller;

import online.ityura.springdigitallibrary.controller.BookFileController;
import online.ityura.springdigitallibrary.model.Role;
import online.ityura.springdigitallibrary.model.User;
import online.ityura.springdigitallibrary.repository.UserRepository;
import online.ityura.springdigitallibrary.service.BookFileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookFileControllerTest {
    
    @Mock
    private BookFileService bookFileService;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private Authentication authentication;
    
    @Mock
    private UserDetails userDetails;
    
    @InjectMocks
    private BookFileController bookFileController;
    
    private User testUser;
    
    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .nickname("testuser")
                .email("test@example.com")
                .passwordHash("encodedPassword")
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .build();
    }
    
    @Test
    void testDownloadBook_Success_ShouldReturn200() {
        // Given
        Resource mockResource = new ByteArrayResource("test pdf content".getBytes());
        String filename = "test-book.pdf";
        
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(bookFileService.downloadBookFile(1L, 1L)).thenReturn(mockResource);
        when(bookFileService.getOriginalFilename(1L)).thenReturn(filename);
        
        // When
        ResponseEntity<Resource> response = bookFileController.downloadBook(1L, authentication);
        
        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));
        assertTrue(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).contains(filename));
    }
}

