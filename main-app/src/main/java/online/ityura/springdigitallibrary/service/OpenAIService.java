package online.ityura.springdigitallibrary.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class OpenAIService {
    
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/responses";
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String openaiApiKey;
    
    @Autowired
    public OpenAIService(
            RestTemplate restTemplate, 
            ObjectMapper objectMapper,
            @Value("${openai.api.key}") String openaiApiKey) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.openaiApiKey = openaiApiKey;
        
        if (openaiApiKey == null || openaiApiKey.trim().isEmpty()) {
            log.warn("OpenAI API key is not configured. Please set OPENAI_API_KEY environment variable or openai.api.key property.");
        }
    }
    
    public String sendPromptAndGetResponse(String prompt) {
        try {
            // Создаем запрос
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-5.1");
            requestBody.put("input", prompt);
            
            // Устанавливаем заголовки
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openaiApiKey);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            // Отправляем POST запрос
            log.info("Sending request to OpenAI API");
            ResponseEntity<String> response;
            try {
                response = restTemplate.postForEntity(
                        OPENAI_API_URL,
                        request,
                        String.class
                );
            } catch (org.springframework.web.client.ResourceAccessException e) {
                // Если произошел timeout, возможно API все еще обрабатывает запрос
                // В этом случае нужно использовать другой подход - получить response ID из логов или использовать другой метод
                log.warn("Request timeout, but API might still be processing. Error: {}", e.getMessage());
                throw new RuntimeException("OpenAI API request timeout. The request might still be processing. Please check OpenAI logs for response ID.", e);
            }
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Received response from OpenAI API, status code: {}", response.getStatusCode());
                log.debug("Response body: {}", response.getBody());
                
                // Парсим JSON ответ
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                
                // Проверяем наличие ID ответа
                JsonNode idNode = jsonResponse.get("id");
                if (idNode == null) {
                    log.error("Response does not contain 'id' field. Full response: {}", response.getBody());
                    throw new RuntimeException("OpenAI API response does not contain 'id' field");
                }
                
                String responseId = idNode.asText();
                log.info("OpenAI response ID: {}", responseId);
                
                // Проверяем статус ответа
                JsonNode statusNode = jsonResponse.get("status");
                if (statusNode == null) {
                    log.error("Response does not contain 'status' field. Full response: {}", response.getBody());
                    throw new RuntimeException("OpenAI API response does not contain 'status' field");
                }
                
                String status = statusNode.asText();
                log.info("OpenAI response status: {}", status);
                
                // Если статус "completed", извлекаем текст сразу
                if ("completed".equals(status)) {
                    log.info("Response is already completed, extracting text");
                    return extractTextFromResponse(jsonResponse);
                }
                
                // Если статус не "completed", начинаем polling
                log.info("Response status is '{}', starting polling for completion", status);
                // Небольшая задержка перед первым polling запросом
                Thread.sleep(5000); // 5 секунд
                return pollForCompletion(responseId);
            } else {
                log.error("Failed to get response from OpenAI API. Status: {}, Body: {}", 
                        response.getStatusCode(), response.getBody());
                throw new RuntimeException("Failed to get response from OpenAI API. Status: " + response.getStatusCode());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread interrupted while waiting for OpenAI response", e);
            throw new RuntimeException("Request interrupted", e);
        } catch (Exception e) {
            log.error("Error calling OpenAI API", e);
            throw new RuntimeException("Error calling OpenAI API: " + e.getMessage(), e);
        }
    }
    
    private String pollForCompletion(String responseId) {
        return pollForCompletion(responseId, 0);
    }
    
    private String pollForCompletion(String responseId, int attempt) {
        final int MAX_ATTEMPTS = 20; // Максимум 20 попыток (около 200 секунд)
        
        if (attempt >= MAX_ATTEMPTS) {
            throw new RuntimeException("OpenAI response polling timeout after " + MAX_ATTEMPTS + " attempts");
        }
        
        try {
            // Создаем запрос для получения статуса
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(openaiApiKey);
            HttpEntity<Void> request = new HttpEntity<>(headers);
            
            // Делаем GET запрос для проверки статуса
            String getUrl = OPENAI_API_URL + "/" + responseId;
            log.info("Polling OpenAI response, attempt: {}/{}, URL: {}", attempt + 1, MAX_ATTEMPTS, getUrl);
            
            ResponseEntity<String> response = restTemplate.exchange(
                    getUrl,
                    HttpMethod.GET,
                    request,
                    String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.debug("Polling response body: {}", response.getBody());
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                
                JsonNode statusNode = jsonResponse.get("status");
                if (statusNode == null) {
                    log.error("Polling response does not contain 'status' field. Response: {}", response.getBody());
                    throw new RuntimeException("Polling response does not contain 'status' field");
                }
                
                String status = statusNode.asText();
                log.info("OpenAI response status: {}, attempt: {}/{}", status, attempt + 1, MAX_ATTEMPTS);
                
                if ("completed".equals(status)) {
                    log.info("Response completed, extracting text");
                    return extractTextFromResponse(jsonResponse);
                } else {
                    // Если еще не готово, ждем еще
                    int waitTime = 10000; // 10 секунд по умолчанию
                    // Если статус "processing" или подобный, можно подождать чуть дольше
                    if (attempt < 5) {
                        waitTime = 15000; // Первые 5 попыток ждем 15 секунд
                    }
                    log.info("Response not ready yet, waiting {} seconds before next attempt", waitTime / 1000);
                    Thread.sleep(waitTime);
                    return pollForCompletion(responseId, attempt + 1);
                }
            }
            
            log.error("Failed to poll OpenAI response status. Status code: {}, Body: {}", 
                    response.getStatusCode(), response.getBody());
            throw new RuntimeException("Failed to poll OpenAI response status. Status code: " + response.getStatusCode());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Polling interrupted", e);
            throw new RuntimeException("Polling interrupted", e);
        } catch (Exception e) {
            log.error("Error polling OpenAI response, attempt: {}/{}", attempt + 1, MAX_ATTEMPTS, e);
            throw new RuntimeException("Error polling OpenAI response: " + e.getMessage(), e);
        }
    }
    
    private String extractTextFromResponse(JsonNode jsonResponse) {
        try {
            JsonNode output = jsonResponse.get("output");
            if (output != null && output.isArray() && output.size() > 0) {
                JsonNode firstOutput = output.get(0);
                JsonNode content = firstOutput.get("content");
                if (content != null && content.isArray() && content.size() > 0) {
                    JsonNode firstContent = content.get(0);
                    JsonNode text = firstContent.get("text");
                    if (text != null) {
                        String rawText = text.asText();
                        // return cleanMarkdownFormatting(rawText);
                        return rawText;
                    }
                }
            }
            throw new RuntimeException("Could not extract text from OpenAI response");
        } catch (Exception e) {
            log.error("Error extracting text from response", e);
            throw new RuntimeException("Error extracting text from response: " + e.getMessage(), e);
        }
    }
    
    /**
     * Убирает Markdown-форматирование из текста для более читаемого отображения
     */
    private String cleanMarkdownFormatting(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        String cleaned = text;
        
        // Убираем экранированные кавычки
        // Используем простой replace для надежности - обрабатываем все возможные варианты
        // В Java строке: "\\\"" означает литеральную строку из двух символов: \ и "
        cleaned = cleaned.replace("\\\"", "\"");  // Заменяем \" на "
        cleaned = cleaned.replace("\\'", "'");    // Заменяем \' на '
        
        // Обрабатываем случаи с множественным экранированием (если есть)
        // Заменяем все оставшиеся обратные слэши перед кавычками через регулярное выражение
        cleaned = cleaned.replaceAll("\\\\+(\"|')", "$1");
        
        // Убираем жирный текст (**текст** или __текст__)
        cleaned = cleaned.replaceAll("\\*\\*(.*?)\\*\\*", "$1");
        cleaned = cleaned.replaceAll("__(.*?)__", "$1");
        
        // Убираем курсив (*текст* или _текст_), но только если это не часть жирного текста
        // Сначала убираем курсив, который не является частью жирного
        cleaned = cleaned.replaceAll("(?<!\\*)\\*(?!\\*)(.*?)(?<!\\*)\\*(?!\\*)", "$1");
        cleaned = cleaned.replaceAll("(?<!_)_(?!_)(.*?)(?<!_)_(?!_)", "$1");
        
        // Убираем заголовки (### Заголовок -> Заголовок) - используем MULTILINE режим
        cleaned = cleaned.replaceAll("(?m)^#{1,6}\\s+", "");
        
        // Убираем символы для списков (- пункт или * пункт) в начале строки
        cleaned = cleaned.replaceAll("(?m)^[-*+]\\s+", "");
        
        // Убираем символы для цитат (> текст) в начале строки
        cleaned = cleaned.replaceAll("(?m)^>\\s+", "");
        
        // Убираем горизонтальные разделители (--- или ***) - целые строки
        cleaned = cleaned.replaceAll("(?m)^[-*]{3,}\\s*$", "");
        
        // Заменяем все переносы строк на пробелы
        cleaned = cleaned.replaceAll("\\n+", " ");
        
        // Убираем множественные пробелы (но сохраняем один пробел)
        cleaned = cleaned.replaceAll(" {2,}", " ");
        
        return cleaned.trim();
    }
}

