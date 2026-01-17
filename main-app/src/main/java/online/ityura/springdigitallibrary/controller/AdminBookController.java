package online.ityura.springdigitallibrary.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.ityura.springdigitallibrary.dto.request.CreateBookRequest;
import online.ityura.springdigitallibrary.dto.request.PutBookRequest;
import online.ityura.springdigitallibrary.dto.request.UpdateBookRequest;
import online.ityura.springdigitallibrary.dto.response.BookResponse;
import online.ityura.springdigitallibrary.dto.response.ErrorResponse;
import online.ityura.springdigitallibrary.dto.response.MessageResponse;
import online.ityura.springdigitallibrary.service.AdminBookService;
import online.ityura.springdigitallibrary.service.BookFileService;
import online.ityura.springdigitallibrary.service.BookImageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/admin/books")
@Tag(name = "Администрирование книг", description = "API для управления книгами (требуется роль ADMIN)")
@SecurityRequirement(name = "Bearer Authentication")
@RequiredArgsConstructor
public class AdminBookController {

    private final AdminBookService adminBookService;

    private final BookImageService bookImageService;
    
    private final BookFileService bookFileService;

    @Operation(
            summary = "Создать новую книгу",
            description = "Создает новую книгу в каталоге. Автор будет создан автоматически, если его еще нет. " +
                    "Книга должна быть уникальна по паре (title, author). Требуется роль ADMIN."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Книга успешно создана",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = BookResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Неверный формат данных или несуществующий жанр",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Validation error",
                                            value = "{\"status\":400,\"error\":\"VALIDATION_ERROR\",\"message\":\"Validation failed\",\"fieldErrors\":{\"title\":\"Title is required\",\"authorName\":\"Author name is required\",\"publishedYear\":\"Published year must be at least 1000\"},\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/admin/books\"}"
                                    ),
                                    @ExampleObject(
                                            name = "Invalid genre",
                                            value = "{\"status\":400,\"error\":\"INVALID_GENRE\",\"message\":\"Invalid genre: INVALID_GENRE_VALUE. Please use one of the valid genre values.\",\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/admin/books\"}"
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Недостаточно прав (требуется роль ADMIN)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\":403,\"error\":\"ACCESS_DENIED\",\"message\":\"Insufficient permissions (ADMIN role required)\",\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/admin/books\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Книга с таким названием и автором уже существует",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\":409,\"error\":\"BOOK_ALREADY_EXISTS\",\"message\":\"Book with this title and author already exists\",\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/admin/books\"}")
                    )
            )
    })
    @PostMapping
    public ResponseEntity<BookResponse> createBook(@Valid @RequestBody CreateBookRequest request) {
        BookResponse response = adminBookService.createBook(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Полностью заменить всю инфу о книге",
            description = "Полностью заменяет информацию о существующей книге согласно REST стандартам. " +
                    "Все поля обязательны. Если переданы не все поля, возвращается ошибка 400. " +
                    "PUT заменяет весь ресурс целиком, в отличие от PATCH который делает частичное обновление. " +
                    "При изменении title или author проверяется уникальность комбинации title + author (у разных авторов могут быть книги с одинаковым названием). " +
                    "Обновление запрещено, если deletion_locked = true (административная блокировка, установленная в БД) - возвращает 403. " +
                    "Требуется роль ADMIN."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Книга успешно обновлена",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = BookResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Неверный формат данных",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Validation error",
                                    value = "{\"status\":400,\"error\":\"VALIDATION_ERROR\",\"message\":\"Validation failed\",\"fieldErrors\":{\"title\":\"Title is required\",\"publishedYear\":\"Published year must be between 1000-9999\"},\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/admin/books/1\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Книга не найдена",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\":404,\"error\":\"BOOK_NOT_FOUND\",\"message\":\"Book not found with id: 1\",\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/admin/books/1\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Недостаточно прав",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\":403,\"error\":\"ACCESS_DENIED\",\"message\":\"Insufficient permissions (ADMIN role required)\",\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/admin/books/1\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Конфликт уникальности",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\":409,\"error\":\"BOOK_ALREADY_EXISTS\",\"message\":\"Book with this title and author already exists\",\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/admin/books/1\"}")
                    )
            )
    })
    @PutMapping("/{bookId}")
    public ResponseEntity<BookResponse> updateBook(
            @Parameter(description = "ID книги", example = "1", required = true)
            @PathVariable Long bookId,
            @Valid @RequestBody PutBookRequest request) {
        BookResponse response = adminBookService.updateBook(bookId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Частично обновить информацию о книге",
            description = "Частично обновляет информацию о существующей книге. Все поля опциональны. " +
                    "При изменении title или author проверяется уникальность комбинации title + author (у разных авторов могут быть книги с одинаковым названием). " +
                    "Для обновления изображения используйте PATCH с multipart/form-data. " +
                    "Обновление запрещено, если deletion_locked = true (административная блокировка, установленная в БД) - возвращает 403. " +
                    "Требуется роль ADMIN."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Книга успешно обновлена",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = BookResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Неверный формат данных",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Validation error",
                                    value = "{\"status\":400,\"error\":\"VALIDATION_ERROR\",\"message\":\"Validation failed\",\"fieldErrors\":{\"publishedYear\":\"Published year must be between 1000-9999\"},\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/admin/books/1\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Книга не найдена",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\":404,\"error\":\"BOOK_NOT_FOUND\",\"message\":\"Book not found with id: 1\",\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/admin/books/1\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Недостаточно прав или обновление запрещено администратором (deletion_locked = true в БД)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Insufficient permissions",
                                            value = "{\"status\":403,\"error\":\"ACCESS_DENIED\",\"message\":\"Insufficient permissions (ADMIN role required)\",\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/admin/books/1\"}"
                                    ),
                                    @ExampleObject(
                                            name = "Modification locked",
                                            value = "{\"status\":403,\"error\":\"ACCESS_DENIED\",\"message\":\"Cannot update book: modification is locked\",\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/admin/books/1\"}"
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Конфликт уникальности",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\":409,\"error\":\"BOOK_ALREADY_EXISTS\",\"message\":\"Book with this title and author already exists\",\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/admin/books/1\"}")
                    )
            )
    })
    @PatchMapping(value = "/{bookId}", consumes = "application/json")
    public ResponseEntity<BookResponse> patchBook(
            @Parameter(description = "ID книги", example = "1", required = true)
            @PathVariable Long bookId,
            @Valid @RequestBody UpdateBookRequest request) {
        BookResponse response = adminBookService.patchBook(bookId, request, null, null);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Частично обновить информацию о книге с изображением и/или PDF (multipart/form-data)",
            description = "Частично обновляет информацию о существующей книге и/или изображение и/или PDF файл. " +
                    "Все поля опциональны. Можно обновить только поля, только изображение, только PDF, или любую комбинацию. " +
                    "Можно обновить любое поле, включая автора (authorName). " +
                    "При изменении title или author проверяется уникальность комбинации title + author (у разных авторов могут быть книги с одинаковым названием). " +
                    "Изображение должно быть в формате multipart/form-data, размером не более 5MB. " +
                    "PDF файл должен быть в формате application/pdf. " +
                    "Обновление запрещено, если deletion_locked = true (административная блокировка, установленная в БД) - возвращает 403. " +
                    "Требуется роль ADMIN."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Книга успешно обновлена",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = BookResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Неверный формат данных, файл слишком большой или неверный формат файла",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Validation error",
                                            value = "{\"status\":400,\"error\":\"VALIDATION_ERROR\",\"message\":\"Validation failed\",\"fieldErrors\":{\"publishedYear\":\"Published year must be between 1000-9999\"},\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/admin/books/1\"}"
                                    ),
                                    @ExampleObject(
                                            name = "Invalid PDF format",
                                            value = "{\"status\":400,\"error\":\"BAD_REQUEST\",\"message\":\"File must be a PDF (application/pdf)\",\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/admin/books/1\"}"
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Книга не найдена",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\":404,\"error\":\"BOOK_NOT_FOUND\",\"message\":\"Book not found with id: 1\",\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/admin/books/1\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Недостаточно прав или обновление запрещено администратором (deletion_locked = true в БД)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Insufficient permissions",
                                            value = "{\"status\":403,\"error\":\"ACCESS_DENIED\",\"message\":\"Insufficient permissions (ADMIN role required)\",\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/admin/books/1\"}"
                                    ),
                                    @ExampleObject(
                                            name = "Modification locked",
                                            value = "{\"status\":403,\"error\":\"ACCESS_DENIED\",\"message\":\"Cannot update book: modification is locked\",\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/admin/books/1\"}"
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Конфликт уникальности",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\":409,\"error\":\"BOOK_ALREADY_EXISTS\",\"message\":\"Book with this title and author already exists\",\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/admin/books/1\"}")
                    )
            )
    })
    @PatchMapping(value = "/{bookId}", consumes = "multipart/form-data")
    public ResponseEntity<BookResponse> patchBookWithImage(
            @Parameter(description = "ID книги", example = "1", required = true)
            @PathVariable Long bookId,
            @Parameter(description = "Название книги", required = false)
            @RequestParam(value = "title", required = false) String title,
            @Parameter(description = "Полное имя автора (можно изменить автора, при изменении проверяется уникальность комбинации title + author)", required = false)
            @RequestParam(value = "authorName", required = false) String authorName,
            @Parameter(description = "Описание книги", required = false)
            @RequestParam(value = "description", required = false) String description,
            @Parameter(description = "Год публикации", required = false)
            @RequestParam(value = "publishedYear", required = false) Integer publishedYear,
            @Parameter(description = "Жанр книги", required = false)
            @RequestParam(value = "genre", required = false) String genre,
            @Parameter(description = "Цена книги в USD", required = false)
            @RequestParam(value = "price", required = false) java.math.BigDecimal price,
            @Parameter(description = "Процент скидки", required = false)
            @RequestParam(value = "discountPercent", required = false) java.math.BigDecimal discountPercent,
            @Parameter(description = "Файл изображения", required = false)
            @RequestParam(value = "image", required = false) MultipartFile image,
            @Parameter(description = "PDF файл", required = false)
            @RequestParam(value = "pdf", required = false) MultipartFile pdf) {

        // Создаем UpdateBookRequest из параметров
        UpdateBookRequest request = new UpdateBookRequest();
        request.setTitle(title);
        request.setAuthorName(authorName);
        request.setDescription(description);
        request.setPublishedYear(publishedYear);
        request.setPrice(price);
        request.setDiscountPercent(discountPercent);
        if (genre != null && !genre.isEmpty()) {
            try {
                request.setGenre(online.ityura.springdigitallibrary.model.Genre.valueOf(genre.toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid genre: " + genre);
            }
        }

        BookResponse response = adminBookService.patchBook(bookId, request, image, pdf);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Удалить книгу",
            description = "Удаляет книгу из каталога. Удаление запрещено, если: " +
                    "1) deletion_locked = true (административная блокировка, установленная в БД) - возвращает 403, " +
                    "2) есть связанные отзывы - возвращает 409. Требуется роль ADMIN."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Книга успешно удалена"),
            @ApiResponse(
                    responseCode = "403",
                    description = "Удаление запрещено администратором (deletion_locked = true в БД)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\":403,\"error\":\"ACCESS_DENIED\",\"message\":\"Cannot delete book: deletion is locked\",\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/admin/books/1\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Удаление запрещено (есть связанные отзывы)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\":409,\"error\":\"BOOK_HAS_REVIEWS\",\"message\":\"Cannot delete book: it has reviews\",\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/admin/books/1\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Книга не найдена",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\":404,\"error\":\"BOOK_NOT_FOUND\",\"message\":\"Book not found with id: 1\",\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/admin/books/1\"}")
                    )
            )
    })
    @DeleteMapping("/{bookId}")
    public ResponseEntity<Void> deleteBook(
            @Parameter(description = "ID книги", example = "1", required = true)
            @PathVariable Long bookId) {
        adminBookService.deleteBook(bookId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Загрузить изображение для книги",
            description = "Загружает изображение для указанной книги. " +
                    "Изображение должно быть в формате multipart/form-data, размером не более 5MB. " +
                    "Имя файла в хранилище будет сгенерировано на основе названия книги (пробелы заменяются на _). " +
                    "Загрузка запрещена, если deletion_locked = true (административная блокировка, установленная в БД) - возвращает 403. " +
                    "Требуется роль ADMIN."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Изображение успешно загружено",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = MessageResponse.class),
                            examples = @ExampleObject(value = "{\"message\":\"Image uploaded successfully\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Неверный формат данных или файл слишком большой",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\":400,\"error\":\"VALIDATION_ERROR\",\"message\":\"Invalid file format or file too large\",\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/admin/books/1/image\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Книга не найдена",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\":404,\"error\":\"BOOK_NOT_FOUND\",\"message\":\"Book not found with id: 1\",\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/admin/books/1/image\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Недостаточно прав (требуется роль ADMIN) или загрузка запрещена администратором (deletion_locked = true в БД)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Insufficient permissions",
                                            value = "{\"status\":403,\"error\":\"ACCESS_DENIED\",\"message\":\"Insufficient permissions (ADMIN role required)\",\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/admin/books/1/image\"}"
                                    ),
                                    @ExampleObject(
                                            name = "Modification locked",
                                            value = "{\"status\":403,\"error\":\"ACCESS_DENIED\",\"message\":\"Cannot upload image: book modification is locked\",\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/admin/books/1/image\"}"
                                    )
                            }
                    )
            )
    })
    @PostMapping(value = "/{bookId}/image", consumes = "multipart/form-data")
    public ResponseEntity<MessageResponse> uploadBookImage(
            @Parameter(description = "ID книги", example = "1", required = true)
            @PathVariable Long bookId,
            @Parameter(description = "Файл изображения", required = true)
            @RequestParam("file") MultipartFile file) {
        bookImageService.uploadBookImage(bookId, file);
        MessageResponse response = MessageResponse.builder()
                .message("Image uploaded successfully")
                .build();
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Загрузить PDF файл для книги",
            description = "Загружает PDF файл для указанной книги. " +
                    "PDF файл должен быть в формате multipart/form-data. " +
                    "Имя файла в хранилище будет сгенерировано на основе названия книги (пробелы заменяются на _). " +
                    "Если у книги уже есть PDF файл, старый файл будет переименован с timestamp, а новый сохранится с тем же именем. " +
                    "Загрузка запрещена, если deletion_locked = true (административная блокировка, установленная в БД) - возвращает 403. " +
                    "Требуется роль ADMIN."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "PDF файл успешно загружен",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = MessageResponse.class),
                            examples = @ExampleObject(value = "{\"message\":\"PDF uploaded successfully\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Неверный формат данных (файл не является PDF) или файл пустой",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Invalid file format",
                                            value = "{\"status\":400,\"error\":\"BAD_REQUEST\",\"message\":\"File must be a PDF (application/pdf)\",\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/admin/books/1/pdf\"}"
                                    ),
                                    @ExampleObject(
                                            name = "Empty file",
                                            value = "{\"status\":400,\"error\":\"BAD_REQUEST\",\"message\":\"PDF file is required\",\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/admin/books/1/pdf\"}"
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Книга не найдена",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\":404,\"error\":\"BOOK_NOT_FOUND\",\"message\":\"Book not found with id: 1\",\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/admin/books/1/pdf\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Недостаточно прав (требуется роль ADMIN) или загрузка запрещена администратором (deletion_locked = true в БД)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Insufficient permissions",
                                            value = "{\"status\":403,\"error\":\"ACCESS_DENIED\",\"message\":\"Insufficient permissions (ADMIN role required)\",\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/admin/books/1/pdf\"}"
                                    ),
                                    @ExampleObject(
                                            name = "Modification locked",
                                            value = "{\"status\":403,\"error\":\"ACCESS_DENIED\",\"message\":\"Cannot upload PDF: book modification is locked\",\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/admin/books/1/pdf\"}"
                                    )
                            }
                    )
            )
    })
    @PostMapping(value = "/{bookId}/pdf", consumes = "multipart/form-data")
    public ResponseEntity<MessageResponse> uploadBookPdf(
            @Parameter(description = "ID книги", example = "1", required = true)
            @PathVariable Long bookId,
            @Parameter(description = "PDF файл", required = true)
            @RequestParam("file") MultipartFile file) {
        bookFileService.uploadBookPdf(bookId, file);
        MessageResponse response = MessageResponse.builder()
                .message("PDF uploaded successfully")
                .build();
        return ResponseEntity.ok(response);
    }
    
}

