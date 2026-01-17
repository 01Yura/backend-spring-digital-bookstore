package online.ityura.springdigitallibrary.controller;

import online.ityura.springdigitallibrary.dto.event.BookReviewEvent;
import online.ityura.springdigitallibrary.dto.request.CreateReviewRequest;
import online.ityura.springdigitallibrary.dto.request.UpdateReviewRequest;
import online.ityura.springdigitallibrary.dto.response.MessageResponse;
import online.ityura.springdigitallibrary.dto.response.ReviewResponse;
import online.ityura.springdigitallibrary.dto.response.ValidationErrorResponse;
import online.ityura.springdigitallibrary.service.KafkaProducerService;
import online.ityura.springdigitallibrary.service.ReviewService;
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
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/books/{bookId}/reviews")
@Tag(name = "Отзывы", description = "API для управления отзывами на книги")
@RequiredArgsConstructor
public class ReviewController {
    
    private final ReviewService reviewService;
    
    private final online.ityura.springdigitallibrary.repository.UserRepository userRepository;
    
    @Autowired(required = false)
    private KafkaProducerService kafkaProducerService;
    
    @Operation(
            summary = "Создать отзыв на книгу",
            description = "Создает отзыв текущего пользователя на указанную книгу. " +
                    "Один пользователь может оставить только один отзыв на книгу."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Отзыв успешно создан",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ReviewResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Неверный формат данных",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ValidationErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Книга не найдена",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = MessageResponse.class),
                            examples = @ExampleObject(value = "{\"message\":\"Book not found with id: 1\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Отзыв уже существует для этой книги",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = MessageResponse.class),
                            examples = @ExampleObject(value = "{\"message\":\"Review already exists for this book\"}")
                    )
            )
    })
    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(
            @Parameter(description = "ID книги", example = "1", required = true)
            @PathVariable Long bookId,
            @Valid @RequestBody CreateReviewRequest request,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        ReviewResponse response = reviewService.createReview(bookId, userId, request);
        
        // Отправка события создания отзыва
        BookReviewEvent event = BookReviewEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("BOOK_REVIEW")
                .timestamp(LocalDateTime.now())
                .bookId(bookId)
                .userId(userId)
                .reviewId(response.getId())
                .action("CREATED")
                .reviewLength(request.getText() != null ? request.getText().length() : 0)
                .build();
        
        if (kafkaProducerService != null) {
            kafkaProducerService.sendBookReviewEvent(event);
        }
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @Operation(
            summary = "Обновить свой отзыв",
            description = "Обновляет текст отзыва текущего пользователя на указанную книгу."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Отзыв успешно обновлен",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ReviewResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Неверный формат данных",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ValidationErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Отзыв не найден",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = MessageResponse.class),
                            examples = @ExampleObject(value = "{\"message\":\"Review not found\"}")
                    )
            )
    })
    @PutMapping("/my")
    public ResponseEntity<ReviewResponse> updateMyReview(
            @Parameter(description = "ID книги", example = "1", required = true)
            @PathVariable Long bookId,
            @Valid @RequestBody UpdateReviewRequest request,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        ReviewResponse response = reviewService.updateReview(bookId, userId, request);
        
        // Отправка события обновления отзыва
        BookReviewEvent event = BookReviewEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("BOOK_REVIEW")
                .timestamp(LocalDateTime.now())
                .bookId(bookId)
                .userId(userId)
                .reviewId(response.getId())
                .action("UPDATED")
                .reviewLength(request.getText() != null ? request.getText().length() : 0)
                .build();
        
        if (kafkaProducerService != null) {
            kafkaProducerService.sendBookReviewEvent(event);
        }
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(
            summary = "Получить список отзывов на книгу (доступно без авторизации)",
            description = "Возвращает пагинированный список всех отзывов на указанную книгу с возможностью сортировки. " +
                    "Параметры пагинации: `page` (номер страницы, по умолчанию 0), `size` (размер страницы, по умолчанию 20), " +
                    "`sort` (сортировка, по умолчанию `createdAt,desc` - новые первыми). " +
                    "Доступные поля для сортировки: `createdAt` (дата создания), `updatedAt` (дата обновления). " +
                    "Примеры: `createdAt,desc`, `createdAt,asc`, `updatedAt,desc`."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Список отзывов получен",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Книга не найдена или отзывы отсутствуют",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = MessageResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Book not found",
                                            value = "{\"message\":\"Book not found with id: 1\"}",
                                            summary = "Книга не найдена"
                                    ),
                                    @ExampleObject(
                                            name = "No reviews found",
                                            value = "{\"message\":\"No reviews found for book with id: 1\"}",
                                            summary = "Отзывы отсутствуют"
                                    )
                            }
                    )
            )
    })
    @GetMapping
    public ResponseEntity<Page<ReviewResponse>> getReviews(
            @Parameter(description = "ID книги", example = "1", required = true)
            @PathVariable Long bookId,
            @ParameterObject
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(reviewService.getReviewsByBookId(bookId, pageable));
    }
    
    private Long getCurrentUserId(Authentication authentication) {
        String email = ((UserDetails) authentication.getPrincipal()).getUsername();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"))
                .getId();
    }
}

