package online.ityura.springdigitallibrary.controller;

import online.ityura.springdigitallibrary.dto.event.BookDownloadEvent;
import online.ityura.springdigitallibrary.dto.response.BookResponse;
import online.ityura.springdigitallibrary.dto.response.ErrorResponse;
import online.ityura.springdigitallibrary.repository.UserRepository;
import online.ityura.springdigitallibrary.service.BookFileService;
import online.ityura.springdigitallibrary.service.BookService;
import online.ityura.springdigitallibrary.service.KafkaProducerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/books/{bookId}")
@Tag(name = "Файлы книг", description = "API для скачивания PDF файлов книг")
@SecurityRequirement(name = "Bearer Authentication")
@RequiredArgsConstructor
public class BookFileController {
    
    private final BookFileService bookFileService;
    private final UserRepository userRepository;
    private final BookService bookService;
    @Autowired(required = false)
    private KafkaProducerService kafkaProducerService;
    
    @Operation(
            summary = "Скачать PDF файл книги",
            description = "Скачивает PDF файл указанной книги. Файл возвращается с заголовком Content-Disposition для загрузки."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "PDF файл успешно загружен",
                    content = @Content(mediaType = "application/pdf")
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Книга или файл не найдены",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Книга не найдена",
                                            value = "{\"status\":404,\"error\":\"BOOK_NOT_FOUND\",\"message\":\"Book not found with id: 1000\",\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/books/1000/download\"}"
                                    ),
                                    @ExampleObject(
                                            name = "PDF файл не найден",
                                            value = "{\"status\":404,\"error\":\"NOT_FOUND\",\"message\":\"PDF file not found for book id: 1\",\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/books/1/download\"}"
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Не авторизован",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\":401,\"error\":\"UNAUTHORIZED\",\"message\":\"Unauthorized\",\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/books/1/download\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "402",
                    description = "Требуется оплата",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\":402,\"error\":\"PAYMENT_REQUIRED\",\"message\":\"Book must be purchased before download. Please complete payment first.\",\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/books/1/download\"}")
                    )
            )
    })
    @GetMapping("/download")
    public ResponseEntity<Resource> downloadBook(
            @Parameter(description = "ID книги", example = "1", required = true)
            @PathVariable Long bookId,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        Resource resource = bookFileService.downloadBookFile(bookId, userId);
        String filename = bookFileService.getOriginalFilename(bookId);
        
        // Получаем информацию о книге для события
        BookResponse book = bookService.getBookById(bookId);
        
        // Отправка события скачивания книги
        BookDownloadEvent event = BookDownloadEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("BOOK_DOWNLOAD")
                .timestamp(LocalDateTime.now())
                .bookId(bookId)
                .userId(userId)
                .bookTitle(book.getTitle())
                .bookPrice(book.getPrice() != null ? book.getPrice().doubleValue() : 0.0)
                .isFree(book.getPrice() == null || book.getPrice().compareTo(java.math.BigDecimal.ZERO) == 0)
                .build();
        
        if (kafkaProducerService != null) {
            kafkaProducerService.sendBookDownloadEvent(event);
        }
        
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                .replace("+", "%20");
        
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                        "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encodedFilename)
                .body(resource);
    }
    
    private Long getCurrentUserId(Authentication authentication) {
        String email = ((UserDetails) authentication.getPrincipal()).getUsername();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "User not found"))
                .getId();
    }
}

