package online.ityura.springdigitallibrary.controller;

import online.ityura.springdigitallibrary.dto.response.ReviewResponse;
import online.ityura.springdigitallibrary.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
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

@RestController
@RequestMapping("/api/v1/reviews")
@Tag(name = "Пользовательские отзывы", description = "API для управления отзывами пользователя")
@RequiredArgsConstructor
public class UserReviewController {
    
    private final ReviewService reviewService;
    
    private final online.ityura.springdigitallibrary.repository.UserRepository userRepository;
    
    @Operation(
            summary = "Получить все свои отзывы",
            description = "Возвращает пагинированный список всех отзывов текущего пользователя с возможностью сортировки. " +
                    "Параметры пагинации: `page` (номер страницы, по умолчанию 0), `size` (размер страницы, по умолчанию 20), " +
                    "`sort` (сортировка, по умолчанию `createdAt,desc` - новые первыми). " +
                    "Доступные поля для сортировки: `createdAt` (дата создания), `updatedAt` (дата обновления). " +
                    "Примеры: `createdAt,desc`, `createdAt,asc`, `updatedAt,desc`."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Список отзывов получен",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Не авторизован",
                    content = @Content(mediaType = "application/json")
            )
    })
    @GetMapping("/my")
    public ResponseEntity<Page<ReviewResponse>> getMyReviews(
            @ParameterObject
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        return ResponseEntity.ok(reviewService.getMyReviews(userId, pageable));
    }
    
    private Long getCurrentUserId(Authentication authentication) {
        String email = ((UserDetails) authentication.getPrincipal()).getUsername();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"))
                .getId();
    }
}





























