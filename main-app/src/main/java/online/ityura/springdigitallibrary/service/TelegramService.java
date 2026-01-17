package online.ityura.springdigitallibrary.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@Service
public class TelegramService {
    
    private static final String TELEGRAM_BOT_API_URL = "https://api.telegram.org/bot";
    
    private final RestTemplate restTemplate;
    private final String botToken;
    private final String chatId;
    
    @Autowired
    public TelegramService(
            RestTemplate restTemplate,
            @Value("${telegram.bot.token}") String botToken,
            @Value("${telegram.bot.chat-id}") String chatId) {
        this.restTemplate = restTemplate;
        this.botToken = botToken;
        this.chatId = chatId;
        
        if (botToken == null || botToken.trim().isEmpty()) {
            log.warn("Telegram bot token is not configured. Please set telegram.bot.token property.");
        }
        if (chatId == null || chatId.trim().isEmpty()) {
            log.warn("Telegram chat ID is not configured. Please set telegram.bot.chat-id property.");
        }
    }
    
    /**
     * Отправляет текстовое сообщение в Telegram
     */
    public void sendMessage(String message) {
        if (botToken == null || botToken.trim().isEmpty() || chatId == null || chatId.trim().isEmpty()) {
            throw new RuntimeException("Telegram bot is not configured. Please set telegram.bot.token and telegram.bot.chat-id properties.");
        }
        
        try {
            String url = TELEGRAM_BOT_API_URL + botToken + "/sendMessage";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            String requestBody = String.format(
                "{\"chat_id\":\"%s\",\"text\":\"%s\"}",
                chatId,
                escapeJson(message)
            );
            
            HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
            
            log.info("Sending message to Telegram bot");
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Message successfully sent to Telegram");
            } else {
                log.error("Failed to send message to Telegram. Status: {}, Body: {}", 
                        response.getStatusCode(), response.getBody());
                throw new RuntimeException("Failed to send message to Telegram. Status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error sending message to Telegram", e);
            throw new RuntimeException("Error sending message to Telegram: " + e.getMessage(), e);
        }
    }
    
    /**
     * Отправляет изображение в Telegram
     */
    public void sendPhoto(MultipartFile photo, String caption) {
        if (botToken == null || botToken.trim().isEmpty() || chatId == null || chatId.trim().isEmpty()) {
            throw new RuntimeException("Telegram bot is not configured. Please set telegram.bot.token and telegram.bot.chat-id properties.");
        }
        
        try {
            String url = TELEGRAM_BOT_API_URL + botToken + "/sendPhoto";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("chat_id", chatId);
            
            if (caption != null && !caption.trim().isEmpty()) {
                body.add("caption", caption);
            }
            
            ByteArrayResource resource = new ByteArrayResource(photo.getBytes()) {
                @Override
                public String getFilename() {
                    return photo.getOriginalFilename();
                }
            };
            body.add("photo", resource);
            
            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
            
            log.info("Sending photo to Telegram bot");
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Photo successfully sent to Telegram");
            } else {
                log.error("Failed to send photo to Telegram. Status: {}, Body: {}", 
                        response.getStatusCode(), response.getBody());
                throw new RuntimeException("Failed to send photo to Telegram. Status: " + response.getStatusCode());
            }
        } catch (IOException e) {
            log.error("Error reading photo file", e);
            throw new RuntimeException("Error reading photo file: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error sending photo to Telegram", e);
            throw new RuntimeException("Error sending photo to Telegram: " + e.getMessage(), e);
        }
    }
    
    /**
     * Отправляет видео в Telegram
     */
    public void sendVideo(MultipartFile video, String caption) {
        if (botToken == null || botToken.trim().isEmpty() || chatId == null || chatId.trim().isEmpty()) {
            throw new RuntimeException("Telegram bot is not configured. Please set telegram.bot.token and telegram.bot.chat-id properties.");
        }
        
        try {
            String url = TELEGRAM_BOT_API_URL + botToken + "/sendVideo";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("chat_id", chatId);
            
            if (caption != null && !caption.trim().isEmpty()) {
                body.add("caption", caption);
            }
            
            ByteArrayResource resource = new ByteArrayResource(video.getBytes()) {
                @Override
                public String getFilename() {
                    return video.getOriginalFilename();
                }
            };
            body.add("video", resource);
            
            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
            
            log.info("Sending video to Telegram bot");
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Video successfully sent to Telegram");
            } else {
                log.error("Failed to send video to Telegram. Status: {}, Body: {}", 
                        response.getStatusCode(), response.getBody());
                throw new RuntimeException("Failed to send video to Telegram. Status: " + response.getStatusCode());
            }
        } catch (IOException e) {
            log.error("Error reading video file", e);
            throw new RuntimeException("Error reading video file: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error sending video to Telegram", e);
            throw new RuntimeException("Error sending video to Telegram: " + e.getMessage(), e);
        }
    }
    
    /**
     * Экранирует специальные символы для JSON
     */
    private String escapeJson(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
