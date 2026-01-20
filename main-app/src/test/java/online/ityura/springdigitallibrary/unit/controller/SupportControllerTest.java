package online.ityura.springdigitallibrary.unit.controller;

import online.ityura.springdigitallibrary.controller.SupportController;
import online.ityura.springdigitallibrary.dto.request.SupportRequest;
import online.ityura.springdigitallibrary.dto.response.SupportResponse;
import online.ityura.springdigitallibrary.service.TelegramService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class SupportControllerTest {
    
    @Mock
    private TelegramService telegramService;
    
    @Mock
    private Authentication authentication;
    
    @Mock
    private MultipartFile multipartFile;
    
    @InjectMocks
    private SupportController supportController;
    
    private UserDetails userDetails;
    
    @BeforeEach
    void setUp() {
        userDetails = org.springframework.security.core.userdetails.User.builder()
                .username("test@example.com")
                .password("password")
                .authorities("ROLE_USER")
                .build();
    }
    
    @Test
    void testSendMessage_Success_ShouldReturnSuccessResponse() {
        // Given
        SupportRequest request = new SupportRequest();
        request.setMessage("Test message");
        request.setTelegram("@testuser");
        
        when(authentication.getPrincipal()).thenReturn(userDetails);
        doNothing().when(telegramService).sendMessage(anyString());
        
        // When
        ResponseEntity<SupportResponse> result = supportController.sendMessage(request, authentication);
        
        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertTrue(result.getBody().getMessage().contains("успешно отправлено"));
        assertEquals("text", result.getBody().getContentType());
        verify(telegramService).sendMessage(anyString());
    }
    
    @Test
    void testSendMessage_EmptyMessage_ShouldThrowException() {
        // Given
        SupportRequest request = new SupportRequest();
        request.setMessage("");
        
        lenient().when(authentication.getPrincipal()).thenReturn(userDetails);
        
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> supportController.sendMessage(request, authentication));
        
        assertEquals("Message is required", exception.getMessage());
        verify(telegramService, never()).sendMessage(anyString());
    }
    
    @Test
    void testSendMessage_NullMessage_ShouldThrowException() {
        // Given
        SupportRequest request = new SupportRequest();
        request.setMessage(null);
        
        lenient().when(authentication.getPrincipal()).thenReturn(userDetails);
        
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> supportController.sendMessage(request, authentication));
        
        assertEquals("Message is required", exception.getMessage());
    }
    
    @Test
    void testSendMessage_WithoutTelegram_ShouldSendMessage() {
        // Given
        SupportRequest request = new SupportRequest();
        request.setMessage("Test message");
        request.setTelegram(null);
        
        when(authentication.getPrincipal()).thenReturn(userDetails);
        doNothing().when(telegramService).sendMessage(anyString());
        
        // When
        ResponseEntity<SupportResponse> result = supportController.sendMessage(request, authentication);
        
        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(telegramService).sendMessage(anyString());
    }
    
    @Test
    void testSendMessage_Unauthenticated_ShouldThrowException() {
        // Given
        SupportRequest request = new SupportRequest();
        request.setMessage("Test message");
        
        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> supportController.sendMessage(request, null));
        
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        verify(telegramService, never()).sendMessage(anyString());
    }
    
    @Test
    void testSendFile_WithImage_ShouldSendPhoto() {
        // Given
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getContentType()).thenReturn("image/png");
        lenient().when(multipartFile.getOriginalFilename()).thenReturn("test.png");
        lenient().when(multipartFile.getSize()).thenReturn(1024L);
        
        doNothing().when(telegramService).sendPhoto(any(MultipartFile.class), anyString());
        
        // When
        ResponseEntity<SupportResponse> result = supportController.sendFile(
                multipartFile, "Test caption", null, authentication);
        
        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals("photo", result.getBody().getContentType());
        verify(telegramService).sendPhoto(any(MultipartFile.class), anyString());
        verify(telegramService, never()).sendVideo(any(MultipartFile.class), anyString());
    }
    
    @Test
    void testSendFile_WithVideo_ShouldSendVideo() {
        // Given
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getContentType()).thenReturn("video/mp4");
        lenient().when(multipartFile.getOriginalFilename()).thenReturn("test.mp4");
        lenient().when(multipartFile.getSize()).thenReturn(2048L);
        
        doNothing().when(telegramService).sendVideo(any(MultipartFile.class), anyString());
        
        // When
        ResponseEntity<SupportResponse> result = supportController.sendFile(
                multipartFile, null, null, authentication);
        
        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("video", result.getBody().getContentType());
        verify(telegramService).sendVideo(any(MultipartFile.class), anyString());
    }
    
    @Test
    void testSendFile_EmptyFile_ShouldThrowException() {
        // Given
        lenient().when(authentication.getPrincipal()).thenReturn(userDetails);
        when(multipartFile.isEmpty()).thenReturn(true);
        
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> supportController.sendFile(multipartFile, null, null, authentication));
        
        assertEquals("File is required", exception.getMessage());
        verify(telegramService, never()).sendPhoto(any(), anyString());
        verify(telegramService, never()).sendVideo(any(), anyString());
    }
    
    @Test
    void testSendFile_WithJpegExtension_ShouldSendPhoto() {
        // Given
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getContentType()).thenReturn(null);
        when(multipartFile.getOriginalFilename()).thenReturn("test.jpg");
        lenient().when(multipartFile.getSize()).thenReturn(1024L);
        
        doNothing().when(telegramService).sendPhoto(any(MultipartFile.class), anyString());
        
        // When
        ResponseEntity<SupportResponse> result = supportController.sendFile(
                multipartFile, null, null, authentication);
        
        // Then
        assertNotNull(result);
        assertEquals("photo", result.getBody().getContentType());
        verify(telegramService).sendPhoto(any(MultipartFile.class), anyString());
    }
    
    @Test
    void testSendFile_WithMp4Extension_ShouldSendVideo() {
        // Given
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getContentType()).thenReturn(null);
        when(multipartFile.getOriginalFilename()).thenReturn("test.mp4");
        lenient().when(multipartFile.getSize()).thenReturn(2048L);
        
        doNothing().when(telegramService).sendVideo(any(MultipartFile.class), anyString());
        
        // When
        ResponseEntity<SupportResponse> result = supportController.sendFile(
                multipartFile, null, null, authentication);
        
        // Then
        assertNotNull(result);
        assertEquals("video", result.getBody().getContentType());
        verify(telegramService).sendVideo(any(MultipartFile.class), anyString());
    }
    
    @Test
    void testSendFile_UnknownFileType_ShouldSendAsMessage() {
        // Given
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getContentType()).thenReturn("application/pdf");
        when(multipartFile.getOriginalFilename()).thenReturn("test.pdf");
        when(multipartFile.getSize()).thenReturn(4096L);
        
        doNothing().when(telegramService).sendMessage(anyString());
        
        // When
        ResponseEntity<SupportResponse> result = supportController.sendFile(
                multipartFile, null, null, authentication);
        
        // Then
        assertNotNull(result);
        assertEquals("document", result.getBody().getContentType());
        verify(telegramService).sendMessage(anyString());
    }
}
