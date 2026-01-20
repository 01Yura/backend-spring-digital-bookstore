package online.ityura.springdigitallibrary.unit.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import online.ityura.springdigitallibrary.service.GeminiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GeminiServiceTest {
    
    @Mock
    private RestTemplate restTemplate;
    
    @Mock
    private ObjectMapper objectMapper;
    
    @InjectMocks
    private GeminiService geminiService;
    
    @BeforeEach
    void setUp() {
        // Set API key using reflection
        ReflectionTestUtils.setField(geminiService, "geminiApiKey", "test-api-key");
    }
    
    @Test
    void testSendPromptAndGetResponse_Success_ShouldReturnResponse() throws Exception {
        // Given
        String prompt = "Test prompt";
        String responseBody = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"Test response\"}]}}]}";
        ResponseEntity<String> response = ResponseEntity.ok(responseBody);
        
        JsonNode jsonNode = mock(JsonNode.class);
        JsonNode candidates = mock(JsonNode.class);
        JsonNode firstCandidate = mock(JsonNode.class);
        JsonNode content = mock(JsonNode.class);
        JsonNode parts = mock(JsonNode.class);
        JsonNode firstPart = mock(JsonNode.class);
        JsonNode text = mock(JsonNode.class);
        
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(response);
        when(objectMapper.readTree(responseBody)).thenReturn(jsonNode);
        when(jsonNode.get("candidates")).thenReturn(candidates);
        when(candidates.isArray()).thenReturn(true);
        when(candidates.size()).thenReturn(1);
        when(candidates.get(0)).thenReturn(firstCandidate);
        when(firstCandidate.get("content")).thenReturn(content);
        when(content.get("parts")).thenReturn(parts);
        when(parts.isArray()).thenReturn(true);
        when(parts.size()).thenReturn(1);
        when(parts.get(0)).thenReturn(firstPart);
        when(firstPart.get("text")).thenReturn(text);
        when(text.asText()).thenReturn("Test response");
        
        // When
        String result = geminiService.sendPromptAndGetResponse(prompt);
        
        // Then
        assertNotNull(result);
        assertEquals("Test response", result);
        verify(restTemplate).postForEntity(anyString(), any(), eq(String.class));
    }
    
    @Test
    void testSendPromptAndGetResponse_Non2xxResponse_ShouldThrowException() {
        // Given
        String prompt = "Test prompt";
        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error");
        
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(response);
        
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> geminiService.sendPromptAndGetResponse(prompt));
        
        assertTrue(exception.getMessage().contains("Failed to get response from Gemini API"));
    }
    
    @Test
    void testSendPromptAndGetResponse_Exception_ShouldThrowRuntimeException() {
        // Given
        String prompt = "Test prompt";
        
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenThrow(new RuntimeException("Network error"));
        
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> geminiService.sendPromptAndGetResponse(prompt));
        
        assertTrue(exception.getMessage().contains("Error calling Gemini API"));
    }
}
