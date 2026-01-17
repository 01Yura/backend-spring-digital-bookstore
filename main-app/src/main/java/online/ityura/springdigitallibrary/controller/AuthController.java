package online.ityura.springdigitallibrary.controller;

import online.ityura.springdigitallibrary.dto.request.ForgotPasswordRequest;
import online.ityura.springdigitallibrary.dto.request.LoginRequest;
import online.ityura.springdigitallibrary.dto.request.RefreshTokenRequest;
import online.ityura.springdigitallibrary.dto.request.RegisterRequest;
import online.ityura.springdigitallibrary.dto.request.ResendVerificationRequest;
import online.ityura.springdigitallibrary.dto.request.ResetPasswordRequest;
import online.ityura.springdigitallibrary.dto.response.ErrorResponse;
import online.ityura.springdigitallibrary.dto.response.LoginResponse;
import online.ityura.springdigitallibrary.dto.response.MessageResponse;
import online.ityura.springdigitallibrary.dto.response.RegisterResponse;
import online.ityura.springdigitallibrary.service.AuthService;
import online.ityura.springdigitallibrary.service.EmailVerificationService;
import online.ityura.springdigitallibrary.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Аутентификация", description = "API для регистрации и входа в систему (JWT access token и refresh токены)")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;
    private final PasswordResetService passwordResetService;
    
    @Operation(
            summary = "Регистрация нового пользователя",
            description = "Создает нового пользователя с ролью USER. После регистрации на указанный email будет отправлено письмо с ссылкой для подтверждения. " +
                    "Пользователь должен подтвердить email перед входом в систему. JWT токены НЕ выдаются при регистрации."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Пользователь успешно зарегистрирован",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = RegisterResponse.class),
                            examples = @ExampleObject(value = "{\"userId\":1,\"email\":\"john@example.com\",\"role\":\"USER\",\"message\":\"Registration successful! Please check your email and click the verification link to activate your account.\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Неверный формат данных",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\":400,\"error\":\"VALIDATION_ERROR\",\"message\":\"Validation failed\",\"fieldErrors\":{\"password\":\"Password must contain at least one digit, one lower case, one upper case, one special character, no spaces, and be at least 8 characters long\",\"nickname\":\"Nickname must contain only letters, digits, dashes, underscores, and dots. Spaces and other special characters are not allowed\",\"email\":\"Email should be valid\"},\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/auth/register\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Email уже существует",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\":409,\"error\":\"EMAIL_ALREADY_EXISTS\",\"message\":\"Email already exists\",\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/auth/register\"}")
                    )
            )
    })
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @Operation(
            summary = "Вход в систему",
            description = "Аутентифицирует пользователя по email и паролю. Возвращает access токен (5 минут) и refresh токен (24 часа)."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Успешный вход",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = LoginResponse.class),
                            examples = @ExampleObject(value = "{\"accessToken\":\"eyJhbGciOiJIUzI1NiJ9...\",\"refreshToken\":\"eyJhbGciOiJIUzI1NiJ9...\",\"tokenType\":\"Bearer\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Неверные учетные данные",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\":401,\"error\":\"UNAUTHORIZED\",\"message\":\"Bad credentials\",\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/auth/login\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Неверный формат данных",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\":400,\"error\":\"VALIDATION_ERROR\",\"message\":\"Validation failed\",\"fieldErrors\":{\"email\":\"Email should be valid\",\"password\":\"Password is required\"},\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/auth/login\"}")
                    )
            )
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
    
    @Operation(
            summary = "Обновить access токен",
            description = "Обновляет access токен используя refresh токен. Возвращает новую пару access и refresh токенов. " +
                    "Refresh токен действителен 24 часа."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Токены успешно обновлены",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = LoginResponse.class),
                            examples = @ExampleObject(value = "{\"accessToken\":\"eyJhbGciOiJIUzI1NiJ9...\",\"refreshToken\":\"eyJhbGciOiJIUzI1NiJ9...\",\"tokenType\":\"Bearer\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Неверный формат данных",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\":400,\"error\":\"VALIDATION_ERROR\",\"message\":\"Validation failed\",\"fieldErrors\":{\"refreshToken\":\"Refresh token is required\"},\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/auth/refresh\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Refresh токен недействителен или истек",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\":401,\"error\":\"UNAUTHORIZED\",\"message\":\"Refresh token is expired or invalid\",\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/auth/refresh\"}")
                    )
            )
    })
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        LoginResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }
    
    @Operation(
            summary = "Верификация email",
            description = "Подтверждает email пользователя по токену верификации, полученному в письме."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Email успешно верифицирован",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = MessageResponse.class),
                            examples = @ExampleObject(value = "{\"message\":\"Email successfully verified\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Токен не найден",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\":404,\"error\":\"NOT_FOUND\",\"message\":\"Verification token not found\",\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/auth/verify-email\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "410",
                    description = "Токен истек или уже использован",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\":410,\"error\":\"GONE\",\"message\":\"Verification token has expired\",\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/auth/verify-email\"}")
                    )
            )
    })
    @GetMapping("/verify-email")
    public ResponseEntity<MessageResponse> verifyEmail(@RequestParam String token) {
        emailVerificationService.verifyEmail(token);
        return ResponseEntity.ok(MessageResponse.builder()
                .message("Email successfully verified")
                .build());
    }
    
    @Operation(
            summary = "Повторная отправка письма верификации",
            description = "Отправляет новое письмо с токеном верификации на указанный email. " +
                    "Доступно только для неверифицированных пользователей."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Письмо успешно отправлено",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = MessageResponse.class),
                            examples = @ExampleObject(value = "{\"message\":\"Verification email sent successfully\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Пользователь не найден",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\":404,\"error\":\"NOT_FOUND\",\"message\":\"User not found\",\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/auth/resend-verification\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Email уже верифицирован",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\":400,\"error\":\"BAD_REQUEST\",\"message\":\"Email is already verified\",\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/auth/resend-verification\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Неверный формат данных",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\":400,\"error\":\"VALIDATION_ERROR\",\"message\":\"Validation failed\",\"fieldErrors\":{\"email\":\"Email should be valid\"},\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/auth/resend-verification\"}")
                    )
            )
    })
    @PostMapping("/resend-verification")
    public ResponseEntity<MessageResponse> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        emailVerificationService.resendVerificationEmail(request.getEmail());
        return ResponseEntity.ok(MessageResponse.builder()
                .message("Verification email sent successfully")
                .build());
    }
    
    @Operation(
            summary = "Запрос на восстановление пароля",
            description = "Отправляет письмо с ссылкой для восстановления пароля на указанный email. " +
                    "Всегда возвращает 200 OK, даже если email не найден (security best practice)."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Запрос обработан (письмо отправлено, если email существует)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = MessageResponse.class),
                            examples = @ExampleObject(value = "{\"message\":\"If the email exists, a password reset link has been sent\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Неверный формат данных",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\":400,\"error\":\"VALIDATION_ERROR\",\"message\":\"Validation failed\",\"fieldErrors\":{\"email\":\"Email should be valid\"},\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/auth/forgot-password\"}")
                    )
            )
    })
    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.generatePasswordResetToken(request.getEmail());
        return ResponseEntity.ok(MessageResponse.builder()
                .message("If the email exists, a password reset link has been sent")
                .build());
    }
    
    @Operation(
            summary = "Проверка токена восстановления пароля",
            description = "Проверяет валидность токена восстановления пароля. Используется при переходе по ссылке из письма. " +
                    "Не сбрасывает пароль, только проверяет токен."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Токен валиден",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = MessageResponse.class),
                            examples = @ExampleObject(value = "{\"message\":\"Token is valid\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Токен не найден",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\":404,\"error\":\"NOT_FOUND\",\"message\":\"Password reset token not found\",\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/auth/reset-password\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "410",
                    description = "Токен истек или уже использован",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\":410,\"error\":\"GONE\",\"message\":\"Password reset token has expired\",\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/auth/reset-password\"}")
                    )
            )
    })
    @GetMapping("/reset-password")
    public ResponseEntity<MessageResponse> validateResetToken(@RequestParam String token) {
        passwordResetService.validateToken(token);
        return ResponseEntity.ok(MessageResponse.builder()
                .message("Token is valid")
                .build());
    }
    
    @Operation(
            summary = "Сброс пароля",
            description = "Сбрасывает пароль пользователя по токену восстановления, полученному в письме."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Пароль успешно сброшен",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = MessageResponse.class),
                            examples = @ExampleObject(value = "{\"message\":\"Password has been reset successfully\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Неверный формат данных",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\":400,\"error\":\"VALIDATION_ERROR\",\"message\":\"Validation failed\",\"fieldErrors\":{\"newPassword\":\"Password must contain at least one digit, one lower case, one upper case, one special character, no spaces, and be at least 8 characters long\"},\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/auth/reset-password\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Токен не найден",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\":404,\"error\":\"NOT_FOUND\",\"message\":\"Password reset token not found\",\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/auth/reset-password\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "410",
                    description = "Токен истек или уже использован",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\":410,\"error\":\"GONE\",\"message\":\"Password reset token has expired\",\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/auth/reset-password\"}")
                    )
            )
    })
    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(MessageResponse.builder()
                .message("Password has been reset successfully")
                .build());
    }
}

