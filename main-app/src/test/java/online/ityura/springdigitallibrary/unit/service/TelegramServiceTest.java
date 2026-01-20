package online.ityura.springdigitallibrary.unit.service;

import online.ityura.springdigitallibrary.service.TelegramService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class TelegramServiceTest {
    
    @Mock
    private RestTemplate restTemplate;
    
    @Mock
    private MultipartFile multipartFile;
    
    @InjectMocks
    private TelegramService telegramService;
    
    @BeforeEach
    void setUp() {
        // Set properties using reflection
        ReflectionTestUtils.setField(telegramService, "botToken", "test-bot-token");
        ReflectionTestUtils.setField(telegramService, "chatId", "test-chat-id");
    }
    
    @Test
    void testSendMessage_Success_ShouldSendMessage() {
        // Given
        String message = "Test message";
        ResponseEntity<String> response = ResponseEntity.ok("{\"ok\":true}");
        
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(response);
        
        // When
        assertDoesNotThrow(() -> telegramService.sendMessage(message));
        
        // Then
        verify(restTemplate).postForEntity(anyString(), any(), eq(String.class));
    }
    
    @Test
    void testSendMessage_NotConfigured_ShouldThrowException() {
        // Given
        ReflectionTestUtils.setField(telegramService, "botToken", null);
        ReflectionTestUtils.setField(telegramService, "chatId", null);
        String message = "Test message";
        
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> telegramService.sendMessage(message));
        
        assertTrue(exception.getMessage().contains("Telegram bot is not configured"));
        verify(restTemplate, never()).postForEntity(anyString(), any(), any());
    }
    
    @Test
    void testSendPhoto_Success_ShouldSendPhoto() throws Exception {
        // Given
        String caption = "Test caption";
        when(multipartFile.getBytes()).thenReturn("test image".getBytes());
        lenient().when(multipartFile.getOriginalFilename()).thenReturn("test.jpg");
        lenient().when(multipartFile.getContentType()).thenReturn("image/jpeg");
        
        ResponseEntity<String> response = ResponseEntity.ok("{\"ok\":true}");
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(response);
        
        // When
        assertDoesNotThrow(() -> telegramService.sendPhoto(multipartFile, caption));
        
        // Then
        verify(restTemplate).postForEntity(anyString(), any(), eq(String.class));
    }
    
    @Test
    void testSendVideo_Success_ShouldSendVideo() throws Exception {
        // Given
        String caption = "Test caption";
        when(multipartFile.getBytes()).thenReturn("test video".getBytes());
        lenient().when(multipartFile.getOriginalFilename()).thenReturn("test.mp4");
        lenient().when(multipartFile.getContentType()).thenReturn("video/mp4");
        
        ResponseEntity<String> response = ResponseEntity.ok("{\"ok\":true}");
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(response);
        
        // When
        assertDoesNotThrow(() -> telegramService.sendVideo(multipartFile, caption));
        
        // Then
        verify(restTemplate).postForEntity(anyString(), any(), eq(String.class));
    }
    
    @Test
    void testSendPhoto_IOException_ShouldThrowException() throws Exception {
        // Given
        String caption = "Test caption";
        when(multipartFile.getBytes()).thenThrow(new java.io.IOException("IO Error"));
        
        // When & Then
        assertThrows(Exception.class,
                () -> telegramService.sendPhoto(multipartFile, caption));
    }
}
