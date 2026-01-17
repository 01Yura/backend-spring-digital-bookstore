package online.ityura.springdigitallibrary.unit.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import online.ityura.springdigitallibrary.service.OpenAIService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OpenAIServiceTest {
    
    @Mock
    private RestTemplate restTemplate;
    
    @Mock
    private ObjectMapper objectMapper;
    
    @InjectMocks
    private OpenAIService openAIService;
    
    private String testApiKey = "test-api-key";
    private String testPrompt = "Test prompt";
    private ObjectMapper realObjectMapper;
    
    @BeforeEach
    void setUp() {
        realObjectMapper = new ObjectMapper();
        // Устанавливаем API ключ через рефлексию
        ReflectionTestUtils.setField(openAIService, "openaiApiKey", testApiKey);
    }
    
    @Test
    void testSendPromptAndGetResponse_CompletedStatus_ShouldReturnText() throws Exception {
        // Given
        String responseId = "response-123";
        String responseJson = """
                {
                    "id": "%s",
                    "status": "completed",
                    "output": [
                        {
                            "content": [
                                {
                                    "text": "This is a test response"
                                }
                            ]
                        }
                    ]
                }
                """.formatted(responseId);
        
        JsonNode jsonNode = realObjectMapper.readTree(responseJson);
        ResponseEntity<String> responseEntity = new ResponseEntity<>(responseJson, HttpStatus.OK);
        
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(responseEntity);
        when(objectMapper.readTree(responseJson)).thenReturn(jsonNode);
        
        // When
        String result = openAIService.sendPromptAndGetResponse(testPrompt);
        
        // Then
        assertNotNull(result);
        assertTrue(result.contains("test response") || result.contains("This is a test response"));
        
        verify(restTemplate).postForEntity(anyString(), any(HttpEntity.class), eq(String.class));
    }
    
    @Test
    void testSendPromptAndGetResponse_RequestContainsCorrectData() throws Exception {
        // Given
        String responseJson = """
                {
                    "id": "response-123",
                    "status": "completed",
                    "output": [
                        {
                            "content": [
                                {
                                    "text": "Response text"
                                }
                            ]
                        }
                    ]
                }
                """;
        
        JsonNode jsonNode = realObjectMapper.readTree(responseJson);
        ResponseEntity<String> responseEntity = new ResponseEntity<>(responseJson, HttpStatus.OK);
        
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(responseEntity);
        when(objectMapper.readTree(responseJson)).thenReturn(jsonNode);
        
        @SuppressWarnings("unchecked")
        ArgumentCaptor<HttpEntity<Map<String, Object>>> entityCaptor = 
                ArgumentCaptor.forClass((Class<HttpEntity<Map<String, Object>>>) (Class<?>) HttpEntity.class);
        
        // When
        openAIService.sendPromptAndGetResponse(testPrompt);
        
        // Then
        verify(restTemplate).postForEntity(anyString(), entityCaptor.capture(), eq(String.class));
        
        HttpEntity<?> capturedEntity = entityCaptor.getValue();
        assertNotNull(capturedEntity.getBody());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> requestBody = (Map<String, Object>) capturedEntity.getBody();
        assertEquals("gpt-5.1", requestBody.get("model"));
        assertEquals(testPrompt, requestBody.get("input"));
        
        // Проверяем заголовки
        assertNotNull(capturedEntity.getHeaders().get("Authorization"));
        assertTrue(capturedEntity.getHeaders().getFirst("Authorization").contains(testApiKey));
    }
    
    @Test
    void testSendPromptAndGetResponse_NonCompletedStatus_ShouldPoll() throws Exception {
        // Given
        String responseId = "response-123";
        String initialResponseJson = """
                {
                    "id": "%s",
                    "status": "processing"
                }
                """.formatted(responseId);
        
        String completedResponseJson = """
                {
                    "id": "%s",
                    "status": "completed",
                    "output": [
                        {
                            "content": [
                                {
                                    "text": "Final response"
                                }
                            ]
                        }
                    ]
                }
                """.formatted(responseId);
        
        JsonNode initialJsonNode = realObjectMapper.readTree(initialResponseJson);
        JsonNode completedJsonNode = realObjectMapper.readTree(completedResponseJson);
        
        ResponseEntity<String> initialResponse = new ResponseEntity<>(initialResponseJson, HttpStatus.OK);
        ResponseEntity<String> completedResponse = new ResponseEntity<>(completedResponseJson, HttpStatus.OK);
        
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(initialResponse);
        when(objectMapper.readTree(initialResponseJson)).thenReturn(initialJsonNode);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(completedResponse);
        when(objectMapper.readTree(completedResponseJson)).thenReturn(completedJsonNode);
        
        // When
        String result = openAIService.sendPromptAndGetResponse(testPrompt);
        
        // Then
        assertNotNull(result);
        verify(restTemplate).postForEntity(anyString(), any(HttpEntity.class), eq(String.class));
        verify(restTemplate, atLeastOnce()).exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class));
    }
    
    @Test
    void testSendPromptAndGetResponse_Timeout_ShouldThrowException() {
        // Given
        ResourceAccessException timeoutException = new ResourceAccessException("Connection timeout");
        
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenThrow(timeoutException);
        
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> openAIService.sendPromptAndGetResponse(testPrompt));
        
        assertTrue(exception.getMessage().contains("timeout") || 
                   exception.getMessage().contains("OpenAI API request timeout"));
        
        verify(restTemplate).postForEntity(anyString(), any(HttpEntity.class), eq(String.class));
    }
    
    @Test
    void testSendPromptAndGetResponse_Non2xxStatus_ShouldThrowException() {
        // Given
        ResponseEntity<String> errorResponse = new ResponseEntity<>("Error", HttpStatus.INTERNAL_SERVER_ERROR);
        
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(errorResponse);
        
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> openAIService.sendPromptAndGetResponse(testPrompt));
        
        assertTrue(exception.getMessage().contains("Failed to get response from OpenAI API"));
        
        verify(restTemplate).postForEntity(anyString(), any(HttpEntity.class), eq(String.class));
    }
    
    @Test
    void testSendPromptAndGetResponse_MissingIdField_ShouldThrowException() throws Exception {
        // Given
        String responseJson = """
                {
                    "status": "completed",
                    "output": []
                }
                """;
        
        JsonNode jsonNode = realObjectMapper.readTree(responseJson);
        ResponseEntity<String> responseEntity = new ResponseEntity<>(responseJson, HttpStatus.OK);
        
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(responseEntity);
        when(objectMapper.readTree(responseJson)).thenReturn(jsonNode);
        
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> openAIService.sendPromptAndGetResponse(testPrompt));
        
        assertTrue(exception.getMessage().contains("does not contain 'id' field"));
    }
    
    @Test
    void testSendPromptAndGetResponse_MissingStatusField_ShouldThrowException() throws Exception {
        // Given
        String responseJson = """
                {
                    "id": "response-123"
                }
                """;
        
        JsonNode jsonNode = realObjectMapper.readTree(responseJson);
        ResponseEntity<String> responseEntity = new ResponseEntity<>(responseJson, HttpStatus.OK);
        
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(responseEntity);
        when(objectMapper.readTree(responseJson)).thenReturn(jsonNode);
        
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> openAIService.sendPromptAndGetResponse(testPrompt));
        
        assertTrue(exception.getMessage().contains("does not contain 'status' field"));
    }
    
    @Test
    void testSendPromptAndGetResponse_EmptyResponseBody_ShouldThrowException() {
        // Given
        ResponseEntity<String> emptyResponse = ResponseEntity.ok().build();
        
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(emptyResponse);
        
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> openAIService.sendPromptAndGetResponse(testPrompt));
        
        assertTrue(exception.getMessage().contains("Failed to get response from OpenAI API"));
    }
    
    @Test
    void testSendPromptAndGetResponse_InterruptedException_ShouldHandleGracefully() {
        // Given
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenAnswer(invocation -> {
                    Thread.currentThread().interrupt();
                    Thread.sleep(100); // Это вызовет InterruptedException
                    return null;
                });
        
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> openAIService.sendPromptAndGetResponse(testPrompt));
        
        assertTrue(exception.getMessage().contains("interrupted") || 
                   exception.getMessage().contains("Request interrupted"));
        
        // Проверяем, что флаг прерывания был сброшен
        assertTrue(Thread.interrupted());
    }
}

