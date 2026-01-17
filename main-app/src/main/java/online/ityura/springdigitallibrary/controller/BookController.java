package online.ityura.springdigitallibrary.controller;

import online.ityura.springdigitallibrary.dto.event.BookViewEvent;
import online.ityura.springdigitallibrary.dto.response.BookResponse;
import online.ityura.springdigitallibrary.dto.response.ErrorResponse;
import online.ityura.springdigitallibrary.model.Genre;
import online.ityura.springdigitallibrary.repository.UserRepository;
import online.ityura.springdigitallibrary.service.BookImageService;
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
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/books")
@Tag(name = "Книги", description = "API для работы с книгами (доступно без авторизации)")
@SecurityRequirement(name = "Bearer Authentication")
@RequiredArgsConstructor
public class BookController {
    
    private final BookService bookService;
    
    private final BookImageService bookImageService;
    
    @Autowired(required = false)
    private KafkaProducerService kafkaProducerService;
    
    private final UserRepository userRepository;
    
    @Operation(
            summary = "Получить список книг",
            description = "Возвращает пагинированный список всех книг с возможностью сортировки и фильтрации по жанру. " +
                    "Параметры пагинации: `page` (номер страницы, по умолчанию 0), `size` (размер страницы, по умолчанию 10), " +
                    "`sort` (сортировка, по умолчанию `title,asc`). " +
                    "Доступные поля для сортировки: `title` (название), `author.fullName` (автор), `ratingAvg` (рейтинг), " +
                    "`genre` (жанр), `createdAt` (дата добавления), `publishedYear` (год публикации), `updatedAt` (дата обновления). " +
                    "Параметр фильтрации: `genre` (опциональный, фильтрует книги по жанру). " +
                    "Примеры: `title,asc`, `ratingAvg,desc`, `author.fullName,asc`, `genre,asc`, `createdAt,desc`. " +
                    "Пример с фильтрацией: `?genre=FICTION&page=0&size=10`."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Список книг успешно получен",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class))
            )
    })
    @SecurityRequirements
    @GetMapping
    public ResponseEntity<Page<BookResponse>> getAllBooks(
            @Parameter(description = "Жанр для фильтрации книг (опционально)", example = "FICTION")
            @RequestParam(required = false) Genre genre,
            @ParameterObject
            @PageableDefault(size = 10, sort = "title", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(bookService.getAllBooks(pageable, genre));
    }
    
    @Operation(
            summary = "Получить детальную информацию о книге",
            description = "Возвращает полную информацию о книге по её ID, включая автора, рейтинг и наличие файла. " +
                    "Доступно без авторизации."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Информация о книге получена",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = BookResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Книга не найдена",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\":404,\"error\":\"BOOK_NOT_FOUND\",\"message\":\"Book not found with id: 1\",\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/books/1\"}")
                    )
            )
    })
    @SecurityRequirements
    @GetMapping("/{bookId}")
    public ResponseEntity<BookResponse> getBookById(
            @Parameter(description = "ID книги", example = "1", required = true)
            @PathVariable Long bookId,
            Authentication authentication) {
        log.info("📖 GET /api/v1/books/{} - Request received", bookId);
        BookResponse book = bookService.getBookById(bookId);
        
        // Отправка события просмотра книги
        Long userId = getCurrentUserIdOrNull(authentication);
        String eventId = UUID.randomUUID().toString();
        log.info("📝 Creating BookViewEvent - bookId: {}, userId: {}, eventId: {}", bookId, userId, eventId);
        
        BookViewEvent event = BookViewEvent.builder()
                .eventId(eventId)
                .eventType("BOOK_VIEW")
                .timestamp(LocalDateTime.now())
                .bookId(bookId)
                .userId(userId)
                .bookTitle(book.getTitle())
                .bookGenre(book.getGenre() != null ? book.getGenre().name() : null)
                .build();
        
        if (kafkaProducerService != null) {
            kafkaProducerService.sendBookViewEvent(event);
        }
        
        log.info("✅ GET /api/v1/books/{} - Response sent", bookId);
        return ResponseEntity.ok(book);
    }
    
    private Long getCurrentUserIdOrNull(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetails userDetails)) {
            return null;
        }
        try {
            String email = userDetails.getUsername();
            return userRepository.findByEmail(email)
                    .map(user -> user.getId())
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
    
    @Operation(
            summary = "Получить изображение книги",
            description = "Возвращает изображение книги по её ID. " +
                    "Доступно без авторизации."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Изображение успешно получено",
                    content = @Content(mediaType = "image/png, image/jpeg, image/jpg")
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Книга или изображение не найдены",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\":404,\"error\":\"NOT_FOUND\",\"message\":\"Image not found for book id: 1\",\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/books/1/image\"}")
                    )
            )
    })
    @SecurityRequirements
    @GetMapping("/{bookId}/image")
    public ResponseEntity<Resource> getBookImage(
            @Parameter(description = "ID книги", example = "1", required = true)
            @PathVariable Long bookId) {
        Resource resource = bookImageService.getBookImage(bookId);
        
        // Определяем MediaType на основе расширения файла
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        try {
            String imagePath = resource.getURI().getPath();
            String extension = "";
            if (imagePath != null && imagePath.contains(".")) {
                extension = imagePath.substring(imagePath.lastIndexOf(".") + 1).toLowerCase();
            }
            
            switch (extension) {
                case "png":
                    mediaType = MediaType.IMAGE_PNG;
                    break;
                case "jpg":
                case "jpeg":
                    mediaType = MediaType.IMAGE_JPEG;
                    break;
                case "gif":
                    mediaType = MediaType.IMAGE_GIF;
                    break;
                case "webp":
                    mediaType = MediaType.parseMediaType("image/webp");
                    break;
                default:
                    mediaType = MediaType.APPLICATION_OCTET_STREAM;
            }
        } catch (Exception e) {
            // Если не удалось определить тип, используем по умолчанию
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }
        
        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(resource);
    }
    
    @Operation(
            summary = "Получить все изображения книг в ZIP архиве",
            description = "Возвращает ZIP архив со всеми изображениями книг, у которых есть изображение. " +
                    "Каждое изображение в архиве имеет имя в формате: {bookId}_{originalFileName}. " +
                    "Доступно без авторизации."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "ZIP архив успешно создан",
                    content = @Content(mediaType = "application/zip")
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Не найдено книг с изображениями",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\":404,\"error\":\"NOT_FOUND\",\"message\":\"No books with images found\",\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/books/images/all\"}")
                    )
            )
    })
    @SecurityRequirements
    @GetMapping("/images/all")
    public ResponseEntity<Resource> getAllBookImages() {
        Resource resource = bookImageService.getAllBookImagesAsZip();
        
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"book-images.zip\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}

