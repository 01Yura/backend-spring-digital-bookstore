package online.ityura.springdigitallibrary.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import online.ityura.springdigitallibrary.dto.response.AdminUserResponse;
import online.ityura.springdigitallibrary.dto.response.ErrorResponse;
import online.ityura.springdigitallibrary.model.Role;
import online.ityura.springdigitallibrary.model.User;
import online.ityura.springdigitallibrary.repository.EmailVerificationTokenRepository;
import online.ityura.springdigitallibrary.repository.PasswordResetTokenRepository;
import online.ityura.springdigitallibrary.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/users")
@Tag(name = "Администрирование пользователей", description = "API для управления пользователями (требуется роль ADMIN)")
@SecurityRequirement(name = "Bearer Authentication")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    @Operation(
            summary = "Получить список всех пользователей",
            description = "Возвращает список всех зарегистрированных пользователей системы. " +
                    "Включает информацию о пользователе: ID, никнейм, email, роль и дату создания. " +
                    "Требуется роль ADMIN."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Список пользователей успешно получен",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = AdminUserResponse.class)),
                            examples = @ExampleObject(
                                    value = """
                                            [
                                              {
                                                "id": 1,
                                                "nickname": "admin",
                                                "email": "admin@gmail.com",
                                                "role": "ADMIN",
                                                "createdAt": "2025-12-21T19:09:33.964107"
                                              },
                                              {
                                                "id": 2,
                                                "nickname": "crackMyPassword",
                                                "email": "crackMyPassword@gmail.com",
                                                "role": "ADMIN",
                                                "createdAt": "2025-12-21T19:09:34.116402"
                                              }
                                            ]
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Недостаточно прав (требуется роль ADMIN)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    value = "{\"status\":403,\"error\":\"ACCESS_DENIED\",\"message\":\"Insufficient permissions (ADMIN role required)\",\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/admin/users\"}"
                            )
                    )
            )
    })
    @GetMapping
    public ResponseEntity<List<AdminUserResponse>> getAllUsers() {
        List<User> users = userRepository.findAll();
        List<AdminUserResponse> responses = users.stream()
                .map(user -> AdminUserResponse.builder()
                        .id(user.getId())
                        .nickname(user.getNickname())
                        .email(user.getEmail())
                        .role(user.getRole())
                        .createdAt(user.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @Operation(
            summary = "Удалить пользователя",
            description = "Удаляет пользователя из системы по его ID. " +
                    "При удалении также удаляются все связанные данные: отзывы, рейтинги, покупки, " +
                    "токены верификации email и токены сброса пароля пользователя. " +
                    "Нельзя удалить пользователя с ролью ADMIN. " +
                    "Требуется роль ADMIN."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "Пользователь успешно удален"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Пользователь не найден",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    value = "{\"status\":404,\"error\":\"USER_NOT_FOUND\",\"message\":\"User not found with id: 1\",\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/admin/users/1\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Недостаточно прав или попытка удалить администратора",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Insufficient permissions",
                                            value = "{\"status\":403,\"error\":\"ACCESS_DENIED\",\"message\":\"Insufficient permissions (ADMIN role required)\",\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/admin/users/1\"}"
                                    ),
                                    @ExampleObject(
                                            name = "Cannot delete admin",
                                            value = "{\"status\":403,\"error\":\"CANNOT_DELETE_ADMIN\",\"message\":\"Cannot delete user with ADMIN role\",\"timestamp\":\"2025-12-17T13:20:00Z\",\"path\":\"/api/v1/admin/users/1\"}"
                                    )
                            }
                    )
            ),
    })
    @DeleteMapping("/{userId}")
    @Transactional
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "ID пользователя", example = "1", required = true)
            @PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "User not found with id: " + userId));
        
        if (user.getRole() == Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Cannot delete user with ADMIN role");
        }
        
        // Удаляем токены верификации email и сброса пароля перед удалением пользователя
        emailVerificationTokenRepository.deleteByUser(user);
        passwordResetTokenRepository.deleteByUser(user);
        
        userRepository.delete(user);
        return ResponseEntity.noContent().build();
    }
}

