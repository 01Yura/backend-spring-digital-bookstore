package online.ityura.springdigitallibrary.unit.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import online.ityura.springdigitallibrary.controller.BookMessageController;
import online.ityura.springdigitallibrary.dto.request.MessageRequest;
import online.ityura.springdigitallibrary.dto.response.MessageResponse;
import online.ityura.springdigitallibrary.service.BookMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class BookMessageControllerTest {
    
    @Mock
    private BookMessageService bookMessageService;
    
    @InjectMocks
    private BookMessageController bookMessageController;
    
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(bookMessageController).build();
        objectMapper = new ObjectMapper();
    }
    
    @Test
    void testSendMessageToReader_Success_ShouldReturn200() throws Exception {
        // Given
        Long bookId = 1L;
        String message = "Что вам понравилось в этой книге?";
        String serviceResponse = "Мне очень понравился сюжет и персонажи!";
        
        MessageRequest request = new MessageRequest();
        request.setMessage(message);
        
        when(bookMessageService.sendMessageToReader(bookId, message))
                .thenReturn(serviceResponse);
        
        // When & Then
        mockMvc.perform(post("/api/v1/books/{bookId}/message/censored", bookId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(serviceResponse));
        
        verify(bookMessageService).sendMessageToReader(bookId, message);
    }
    
    @Test
    void testSendMessageToReader_UnitTest_ShouldCallService() {
        // Given
        Long bookId = 1L;
        String message = "Ваш вопрос о книге";
        String serviceResponse = "Ответ от читателя";
        
        MessageRequest request = new MessageRequest();
        request.setMessage(message);
        
        when(bookMessageService.sendMessageToReader(bookId, message))
                .thenReturn(serviceResponse);
        
        // When
        ResponseEntity<MessageResponse> result = bookMessageController.sendMessageToReader(bookId, request);
        
        // Then
        assertNotNull(result);
        assertEquals(200, result.getStatusCode().value());
        assertNotNull(result.getBody());
        assertEquals(serviceResponse, result.getBody().getMessage());
        
        verify(bookMessageService).sendMessageToReader(bookId, message);
    }
    
    @Test
    void testSendMessageToReader_EmptyMessage_ShouldReturn400() throws Exception {
        // Given
        Long bookId = 1L;
        MessageRequest request = new MessageRequest();
        request.setMessage(""); // Пустое сообщение
        
        // When & Then
        mockMvc.perform(post("/api/v1/books/{bookId}/message/censored", bookId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        
        verify(bookMessageService, never()).sendMessageToReader(anyLong(), anyString());
    }
    
    @Test
    void testSendMessageToReader_NullMessage_ShouldReturn400() throws Exception {
        // Given
        Long bookId = 1L;
        MessageRequest request = new MessageRequest();
        request.setMessage(null); // null сообщение
        
        // When & Then
        mockMvc.perform(post("/api/v1/books/{bookId}/message/censored", bookId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        
        verify(bookMessageService, never()).sendMessageToReader(anyLong(), anyString());
    }
    
    @Test
    void testSendMessageToReader_BlankMessage_ShouldReturn400() throws Exception {
        // Given
        Long bookId = 1L;
        MessageRequest request = new MessageRequest();
        request.setMessage("   "); // Только пробелы
        
        // When & Then
        mockMvc.perform(post("/api/v1/books/{bookId}/message/censored", bookId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        
        verify(bookMessageService, never()).sendMessageToReader(anyLong(), anyString());
    }
    
    @Test
    void testSendMessageToReader_ServiceThrowsException_ShouldPropagate() {
        // Given
        Long bookId = 1L;
        String message = "Вопрос";
        MessageRequest request = new MessageRequest();
        request.setMessage(message);
        
        RuntimeException serviceException = new RuntimeException("Service error");
        when(bookMessageService.sendMessageToReader(bookId, message))
                .thenThrow(serviceException);
        
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> bookMessageController.sendMessageToReader(bookId, request));
        
        assertEquals("Service error", exception.getMessage());
        verify(bookMessageService).sendMessageToReader(bookId, message);
    }
    
    @Test
    void testSendMessageToReader_ValidMessage_ShouldBuildResponseCorrectly() {
        // Given
        Long bookId = 1L;
        String message = "Что вы думаете о главном герое?";
        String serviceResponse = "Очень интересный персонаж с глубокой психологией.";
        
        MessageRequest request = new MessageRequest();
        request.setMessage(message);
        
        when(bookMessageService.sendMessageToReader(bookId, message))
                .thenReturn(serviceResponse);
        
        // When
        ResponseEntity<MessageResponse> result = bookMessageController.sendMessageToReader(bookId, request);
        
        // Then
        assertNotNull(result);
        assertNotNull(result.getBody());
        MessageResponse response = result.getBody();
        assertEquals(serviceResponse, response.getMessage());
    }

    // Тесты для эндпоинта /message/uncensored
    @Test
    void testSendMessageToReaderViaGemini_Success_ShouldReturn200() throws Exception {
        // Given
        Long bookId = 1L;
        String message = "Что вы думаете о книге?";
        String serviceResponse = "Книга просто огонь!";
        
        MessageRequest request = new MessageRequest();
        request.setMessage(message);
        
        when(bookMessageService.sendMessageToReaderViaGemini(bookId, message))
                .thenReturn(serviceResponse);
        
        // When & Then
        mockMvc.perform(post("/api/v1/books/{bookId}/message/uncensored", bookId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(serviceResponse));
        
        verify(bookMessageService).sendMessageToReaderViaGemini(bookId, message);
    }

    @Test
    void testSendMessageToReaderViaGemini_UnitTest_ShouldCallService() {
        // Given
        Long bookId = 1L;
        String message = "Ваш вопрос о книге";
        String serviceResponse = "Ответ от читателя (нецензурно)";
        
        MessageRequest request = new MessageRequest();
        request.setMessage(message);
        
        when(bookMessageService.sendMessageToReaderViaGemini(bookId, message))
                .thenReturn(serviceResponse);
        
        // When
        ResponseEntity<MessageResponse> result = bookMessageController.sendMessageToReaderViaGemini(bookId, request);
        
        // Then
        assertNotNull(result);
        assertEquals(200, result.getStatusCode().value());
        assertNotNull(result.getBody());
        assertEquals(serviceResponse, result.getBody().getMessage());
        
        verify(bookMessageService).sendMessageToReaderViaGemini(bookId, message);
    }

    @Test
    void testSendMessageToReaderViaGemini_EmptyMessage_ShouldReturn400() throws Exception {
        // Given
        Long bookId = 1L;
        MessageRequest request = new MessageRequest();
        request.setMessage(""); // Пустое сообщение
        
        // When & Then
        mockMvc.perform(post("/api/v1/books/{bookId}/message/uncensored", bookId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        
        verify(bookMessageService, never()).sendMessageToReaderViaGemini(anyLong(), anyString());
    }

    @Test
    void testSendMessageToReaderViaGemini_ServiceThrowsException_ShouldPropagate() {
        // Given
        Long bookId = 1L;
        String message = "Вопрос";
        MessageRequest request = new MessageRequest();
        request.setMessage(message);
        
        RuntimeException serviceException = new RuntimeException("Gemini API error");
        when(bookMessageService.sendMessageToReaderViaGemini(bookId, message))
                .thenThrow(serviceException);
        
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> bookMessageController.sendMessageToReaderViaGemini(bookId, request));
        
        assertEquals("Gemini API error", exception.getMessage());
        verify(bookMessageService).sendMessageToReaderViaGemini(bookId, message);
    }
}

