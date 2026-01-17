package online.ityura.springdigitallibrary.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.ityura.springdigitallibrary.dto.request.SupportRequest;
import online.ityura.springdigitallibrary.dto.response.ErrorResponse;
import online.ityura.springdigitallibrary.dto.response.SupportResponse;
import online.ityura.springdigitallibrary.service.TelegramService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/support")
@Tag(name = "Служба поддержки", description = "API для отправки сообщений, изображений и видео в службу поддержки")
@SecurityRequirement(name = "Bearer Authentication")
@RequiredArgsConstructor
public class SupportController {

    private final TelegramService telegramService;

    @Operation(
            summary = "Отправить сообщение в службу поддержки",
            description = "Отправляет текстовое сообщение в службу поддержки. " +
                    "Сообщение будет доставлено администратору."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Сообщение успешно отправлено",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SupportResponse.class),
                            examples = @ExampleObject(value = "{\"message\":\"Сообщение успешно отправлено в службу поддержки\",\"contentType\":\"text\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Ошибка валидации запроса",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\":400,\"error\":\"VALIDATION_ERROR\",\"message\":\"Validation failed\",\"fieldErrors\":{\"message\":\"Message must not exceed 1000 characters\"},\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/support\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Не авторизован",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"message\":\"Unauthorized\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Ошибка при отправке сообщения",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\":500,\"error\":\"INTERNAL_SERVER_ERROR\",\"message\":\"Error sending message to Telegram: Bot token is not configured\",\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/support\"}")
                    )
            )
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SupportResponse> sendMessage(
            @Valid @org.springframework.web.bind.annotation.RequestBody SupportRequest request,
            Authentication authentication) {
        
        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            throw new IllegalArgumentException("Message is required");
        }
        
        String userEmail = getUserEmail(authentication);
        String formattedMessage = formatMessageWithContactInfo(request.getMessage(), userEmail, request.getTelegram());
        
        telegramService.sendMessage(formattedMessage);
        
        SupportResponse response = SupportResponse.builder()
                .message("Сообщение успешно отправлено в службу поддержки, мы ответим вам по email, указанном при регистрации или в Telegram")
                .contentType("text")
                .build();
        
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Отправить файл (изображение или видео) в службу поддержки",
            description = "Отправляет файл (изображение или видео) в службу поддержки. " +
                    "Можно также добавить текстовое сообщение (caption). " +
                    "Поддерживаемые форматы изображений: JPG, PNG, GIF. " +
                    "Поддерживаемые форматы видео: MP4, MOV, AVI."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Файл успешно отправлен",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SupportResponse.class),
                            examples = @ExampleObject(value = "{\"message\":\"Файл успешно отправлен в службу поддержки\",\"contentType\":\"photo\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Ошибка валидации запроса (файл не предоставлен или неверный формат)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\":400,\"error\":\"BAD_REQUEST\",\"message\":\"File is required\",\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/support/file\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Не авторизован",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"message\":\"Unauthorized\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Ошибка при отправке файла",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\":500,\"error\":\"INTERNAL_SERVER_ERROR\",\"message\":\"Error sending file to Telegram: Bot token is not configured\",\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/support/file\"}")
                    )
            )
    })
    @PostMapping(value = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SupportResponse> sendFile(
            @RequestParam(value = "file", required = true) MultipartFile file,
            @RequestParam(value = "message", required = false) String message,
            @RequestParam(value = "telegram", required = false) String telegram,
            Authentication authentication) {
        
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }
        
        String userEmail = getUserEmail(authentication);
        String formattedMessage = formatMessageWithContactInfo(message, userEmail, telegram);
        
        String contentType = file.getContentType();
        String contentTypeStr = "file";
        
        if (contentType != null) {
            if (contentType.startsWith("image/")) {
                telegramService.sendPhoto(file, formattedMessage);
                contentTypeStr = "photo";
            } else if (contentType.startsWith("video/")) {
                telegramService.sendVideo(file, formattedMessage);
                contentTypeStr = "video";
            } else {
                // Если это не изображение и не видео, отправляем как документ через sendMessage
                // Для простоты отправляем сообщение с информацией о файле
                String fileInfo = String.format("Получен файл: %s (тип: %s, размер: %d байт)", 
                        file.getOriginalFilename(), contentType, file.getSize());
                String fullMessage = formattedMessage != null && !formattedMessage.trim().isEmpty() 
                        ? formattedMessage + "\n\n" + fileInfo 
                        : fileInfo;
                telegramService.sendMessage(fullMessage);
                contentTypeStr = "document";
            }
        } else {
            // Если тип не определен, пытаемся определить по расширению
            String filename = file.getOriginalFilename();
            if (filename != null) {
                String lowerFilename = filename.toLowerCase();
                if (lowerFilename.endsWith(".jpg") || lowerFilename.endsWith(".jpeg") || 
                    lowerFilename.endsWith(".png") || lowerFilename.endsWith(".gif")) {
                    telegramService.sendPhoto(file, formattedMessage);
                    contentTypeStr = "photo";
                } else if (lowerFilename.endsWith(".mp4") || lowerFilename.endsWith(".mov") || 
                          lowerFilename.endsWith(".avi")) {
                    telegramService.sendVideo(file, formattedMessage);
                    contentTypeStr = "video";
                } else {
                    String fileInfo = String.format("Получен файл: %s (размер: %d байт)", 
                            filename, file.getSize());
                    String fullMessage = formattedMessage != null && !formattedMessage.trim().isEmpty() 
                            ? formattedMessage + "\n\n" + fileInfo 
                            : fileInfo;
                    telegramService.sendMessage(fullMessage);
                    contentTypeStr = "document";
                }
            } else {
                throw new IllegalArgumentException("Cannot determine file type");
            }
        }
        
        SupportResponse response = SupportResponse.builder()
                .message("Файл успешно отправлен в службу поддержки, мы ответим вам по email, указанном при регистрации или в Telegram")
                .contentType(contentTypeStr)
                .build();
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Получает email пользователя из Authentication
     */
    private String getUserEmail(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetails userDetails)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated");
        }
        return userDetails.getUsername();
    }
    
    /**
     * Форматирует сообщение с добавлением контактной информации (email и telegram)
     */
    private String formatMessageWithContactInfo(String message, String userEmail, String telegram) {
        StringBuilder formatted = new StringBuilder();
        
        if (message != null && !message.trim().isEmpty()) {
            formatted.append(message);
        }
        
        formatted.append("\n\n--- Контактная информация ---\n");
        formatted.append("Email: ").append(userEmail);
        
        if (telegram != null && !telegram.trim().isEmpty()) {
            // Убираем пробелы и проверяем, начинается ли с @
            String telegramUsername = telegram.trim();
            if (!telegramUsername.startsWith("@")) {
                telegramUsername = "@" + telegramUsername;
            }
            formatted.append("\nTelegram: ").append(telegramUsername);
        }
        
        return formatted.toString();
    }
}
