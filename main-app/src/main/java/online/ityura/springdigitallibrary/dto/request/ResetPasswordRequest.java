package online.ityura.springdigitallibrary.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import online.ityura.springdigitallibrary.dto.BaseDto;

@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "Запрос на сброс пароля")
public class ResetPasswordRequest extends BaseDto {
    
    @Schema(description = "Токен восстановления пароля", example = "550e8400-e29b-41d4-a716-446655440000", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Token is required")
    private String token;
    
    @Schema(description = "Новый пароль", example = "NewPassword123!", requiredMode = Schema.RequiredMode.REQUIRED, minLength = 8)
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    @Pattern(
            regexp = "^(?=\\S+$)(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&+=]).{8,}$",
            message = "Password must contain at least one digit, one lower case, one upper case, one special character, no spaces, and be at least 8 characters long"
    )
    private String newPassword;
}
