package online.ityura.springdigitallibrary.testinfra.requests.clients;

import lombok.AllArgsConstructor;
import lombok.Getter;
import online.ityura.springdigitallibrary.dto.BaseDto;
import online.ityura.springdigitallibrary.dto.request.*;
import online.ityura.springdigitallibrary.dto.response.*;

@Getter
@AllArgsConstructor
public enum Endpoint {
    // Auth endpoints
    AUTH_REGISTER("/auth/register", RegisterRequest.class, RegisterResponse.class),
    AUTH_LOGIN("/auth/login", LoginRequest.class, LoginResponse.class),
    AUTH_REFRESH("/auth/refresh", RefreshTokenRequest.class, LoginResponse.class),
    AUTH_RESEND_VERIFICATION("/auth/resend-verification", ResendVerificationRequest.class, MessageResponse.class),
    AUTH_FORGOT_PASSWORD("/auth/forgot-password", ForgotPasswordRequest.class, MessageResponse.class),
    AUTH_RESET_PASSWORD("/auth/reset-password", ResetPasswordRequest.class, MessageResponse.class),
    
    // User endpoints
    USERS_ME("/users/me", BaseDto.class, UserInfoResponse.class),
    
    // Book endpoints
    BOOKS("/books", BaseDto.class, BookResponse.class),
    BOOKS_BY_ID("/books/{id}", BaseDto.class, BookResponse.class),
    
    // Rating endpoints
    BOOKS_RATINGS("/books/{id}/ratings", CreateRatingRequest.class, RatingResponse.class),
    BOOKS_RATINGS_MY("/books/{id}/ratings/my", UpdateRatingRequest.class, RatingResponse.class),
    
    // Review endpoints
    BOOKS_REVIEWS("/books/{id}/reviews", CreateReviewRequest.class, ReviewResponse.class),
    BOOKS_REVIEWS_MY("/books/{id}/reviews/my", UpdateReviewRequest.class, ReviewResponse.class),
    BOOKS_REVIEWS_LIST("/books/{id}/reviews", BaseDto.class, ReviewResponse.class),
    REVIEWS_MY("/reviews/my", BaseDto.class, ReviewResponse.class),
    
    // Book message endpoints
    BOOKS_MESSAGE_CENSORED("/books/{id}/message/censored", MessageRequest.class, MessageResponse.class),
    BOOKS_MESSAGE_UNCENSORED("/books/{id}/message/uncensored", MessageRequest.class, MessageResponse.class),
    
    // Support endpoints
    SUPPORT("/support", SupportRequest.class, SupportResponse.class),
    
    // Admin Book endpoints
    ADMIN_BOOKS("/admin/books", CreateBookRequest.class, BookResponse.class),
    ADMIN_BOOKS_BY_ID("/admin/books/{id}", PutBookRequest.class, BookResponse.class),
    ADMIN_BOOKS_PATCH("/admin/books/{id}", UpdateBookRequest.class, BookResponse.class),
    
    // Admin User endpoints
    ADMIN_USERS("/admin/users", BaseDto.class, AdminUserResponse.class),
    
    // Admin Analytics endpoints
    ADMIN_ANALYTICS_BOOKS("/admin/analytics/books/{id}", BaseDto.class, BaseDto.class),
    ADMIN_ANALYTICS_BOOKS_HISTORY("/admin/analytics/books/{id}/history", BaseDto.class, BaseDto.class),
    ADMIN_ANALYTICS_OVERVIEW("/admin/analytics/overview", BaseDto.class, BaseDto.class),
    ADMIN_ANALYTICS_POPULAR("/admin/analytics/popular", BaseDto.class, BaseDto.class),
    ADMIN_ANALYTICS_OVERVIEW_HISTORY("/admin/analytics/overview/history", BaseDto.class, BaseDto.class);

    private final String relativePath;
    private final Class<? extends BaseDto> requestDto;
    private final Class<? extends BaseDto> responseDto;
    
    /**
     * Заменяет path variables в пути эндпоинта на реальные значения
     * @param pathVariables пары "имя переменной" -> "значение"
     * @return путь с замененными переменными
     */
    public String getPath(Object... pathVariables) {
        String path = this.relativePath;
        if (pathVariables.length > 0) {
            // Заменяем {id} на первое значение, если оно есть
            if (path.contains("{id}") && pathVariables.length > 0) {
                path = path.replace("{id}", String.valueOf(pathVariables[0]));
            }
        }
        return path;
    }
}
