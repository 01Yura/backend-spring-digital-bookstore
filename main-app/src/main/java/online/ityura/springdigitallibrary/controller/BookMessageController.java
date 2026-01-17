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
import online.ityura.springdigitallibrary.dto.request.MessageRequest;
import online.ityura.springdigitallibrary.dto.response.ErrorResponse;
import online.ityura.springdigitallibrary.dto.response.MessageResponse;
import online.ityura.springdigitallibrary.service.BookMessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/books")
@Tag(name = "Сообщения читателям", description = "API для отправки сообщений читателям о книгах (Требуется авторизация)")
@SecurityRequirement(name = "Bearer Authentication")
@RequiredArgsConstructor
public class BookMessageController {

    private final BookMessageService bookMessageService;

    @Operation(
            summary = "Отправить вопрос о книге рандомному читателю (ЦЕНЗУРНО)",
            description = "Отправляет сообщение (вопрос) рандомному читателю, который уже прочитал книгу. " +
                    "Читатель отвечает с юмором, но цензурно. Сообщение приходит не сразу, так как ему надо " +
                    "время на написание ответа. Подождите 10-20 сек."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Сообщение успешно отправлено и получен ответ от читателя",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = MessageResponse.class),
                            examples = @ExampleObject(value = "{\"message\": \"some text\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Книга не найдена",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\":404,\"error\":\"BOOK_NOT_FOUND\",\"message\":\"Book not found with id: 15\",\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/books/15/message/censored\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Ошибка валидации запроса",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\":400,\"error\":\"VALIDATION_ERROR\",\"message\":\"Validation failed\",\"fieldErrors\":{\"message\":\"Message is required\"},\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/books/15/message/censored\"}")
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
                    description = "Ошибка при обращении к OpenAI API",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\":500,\"error\":\"INTERNAL_SERVER_ERROR\",\"message\":\"Error calling OpenAI API: Connection timeout\",\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/books/15/message/censored\"}")
                    )
            )
    })
    @PostMapping("/{bookId}/message/censored")
    public ResponseEntity<MessageResponse> sendMessageToReader(
            @Parameter(description = "ID книги", example = "1", required = true)
            @PathVariable Long bookId,
            @Valid @RequestBody MessageRequest messageRequest) {

        String responseText = bookMessageService.sendMessageToReader(bookId, messageRequest.getMessage());

        MessageResponse response = MessageResponse.builder()
                .message(responseText)
                .build();

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Отправить вопрос о книге книге рандомному читателю (НЕЦЕНЗУРНО)",
            description = "Отправляет сообщение (вопрос) рандомному читателю, который уже прочитал книгу. " +
                    "Он отвечает с юмором, используя нецензурную лексику и жесткие шутки. " +
                    "Сообщение приходит не сразу, так как ему надо время на написание ответа. Подождите 10-20 сек."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Сообщение успешно отправлено и получен ответ от читателя",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = MessageResponse.class),
                            examples = @ExampleObject(value = "{\"message\": \"some text\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Книга не найдена",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\":404,\"error\":\"BOOK_NOT_FOUND\",\"message\":\"Book not found with id: 15\",\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/books/15/message/uncensored\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Ошибка валидации запроса",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\":400,\"error\":\"VALIDATION_ERROR\",\"message\":\"Validation failed\",\"fieldErrors\":{\"message\":\"Message is required\"},\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/books/15/message/uncensored\"}")
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
                    description = "Ошибка при обращении к Gemini API",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\":500,\"error\":\"INTERNAL_SERVER_ERROR\",\"message\":\"Error calling Gemini API: Connection timeout\",\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/books/15/message/uncensored\"}")
                    )
            )
    })
    @PostMapping("/{bookId}/message/uncensored")
    public ResponseEntity<MessageResponse> sendMessageToReaderViaGemini(
            @Parameter(description = "ID книги", example = "1", required = true)
            @PathVariable Long bookId,
            @Valid @RequestBody MessageRequest messageRequest) {

        String responseText = bookMessageService.sendMessageToReaderViaGemini(bookId, messageRequest.getMessage());

        MessageResponse response = MessageResponse.builder()
                .message(responseText)
                .build();

        return ResponseEntity.ok(response);
    }
}

