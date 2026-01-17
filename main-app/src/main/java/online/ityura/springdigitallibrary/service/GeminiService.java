package online.ityura.springdigitallibrary.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class GeminiService {
    
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String geminiApiKey;
    
    @Autowired
    public GeminiService(
            RestTemplate restTemplate, 
            ObjectMapper objectMapper,
            @Value("${gemini.api.key}") String geminiApiKey) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.geminiApiKey = geminiApiKey;
        
        if (geminiApiKey == null || geminiApiKey.trim().isEmpty()) {
            log.warn("Gemini API key is not configured. Please set GEMINI_API_KEY environment variable or gemini.api.key property.");
        }
    }
    
    public String sendPromptAndGetResponse(String prompt) {
        try {
            // Формируем тело запроса согласно формату Gemini API
            Map<String, Object> requestBody = new HashMap<>();
            
            // Структура contents
            List<Map<String, Object>> contents = new ArrayList<>();
            Map<String, Object> content = new HashMap<>();
            
            // Структура parts
            List<Map<String, Object>> parts = new ArrayList<>();
            Map<String, Object> part = new HashMap<>();
            part.put("text", prompt);
            parts.add(part);
            
            content.put("parts", parts);
            contents.add(content);
            
            requestBody.put("contents", contents);
            
            // Устанавливаем заголовки
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-goog-api-key", geminiApiKey);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            // Отправляем POST запрос
            log.info("Sending request to Gemini API");
            ResponseEntity<String> response = restTemplate.postForEntity(
                    GEMINI_API_URL,
                    request,
                    String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Received response from Gemini API, status code: {}", response.getStatusCode());
                log.debug("Response body: {}", response.getBody());
                
                // Парсим JSON ответ
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                
                // Извлекаем текст из ответа
                return extractTextFromResponse(jsonResponse);
            } else {
                log.error("Failed to get response from Gemini API. Status: {}, Body: {}", 
                        response.getStatusCode(), response.getBody());
                throw new RuntimeException("Failed to get response from Gemini API. Status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error calling Gemini API", e);
            throw new RuntimeException("Error calling Gemini API: " + e.getMessage(), e);
        }
    }
    
    private String extractTextFromResponse(JsonNode jsonResponse) {
        try {
            // Структура ответа: candidates[0].content.parts[0].text
            JsonNode candidates = jsonResponse.get("candidates");
            if (candidates != null && candidates.isArray() && candidates.size() > 0) {
                JsonNode firstCandidate = candidates.get(0);
                JsonNode content = firstCandidate.get("content");
                if (content != null) {
                    JsonNode parts = content.get("parts");
                    if (parts != null && parts.isArray() && parts.size() > 0) {
                        JsonNode firstPart = parts.get(0);
                        JsonNode text = firstPart.get("text");
                        if (text != null) {
                            String rawText = text.asText();
                            // return cleanMarkdownFormatting(rawText);
                            return rawText;
                        }
                    }
                }
            }
            throw new RuntimeException("Could not extract text from Gemini response");
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

