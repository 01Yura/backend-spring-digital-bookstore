package online.ityura.springdigitallibrary.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import online.ityura.springdigitallibrary.dto.BaseDto;
import online.ityura.springdigitallibrary.testinfra.generators.RegexGen;

@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "Запрос на регистрацию нового пользователя")
public class RegisterRequest extends BaseDto {
    
    @Schema(description = "Никнейм пользователя", example = "john_doe", requiredMode = Schema.RequiredMode.REQUIRED, minLength = 3, maxLength = 50)
    @NotBlank(message = "Nickname is required")
    @Size(min = 3, max = 50, message = "Nickname must be between 3 and 50 characters")
    @Pattern(regexp = "^[\\w\\.\\-]+$", message = "Nickname must contain only letters, digits, dashes, underscores, and dots. Spaces and other special characters are not allowed")
//    @RegexGen(value = "^[A-Za-z0-9]{3,50}$")
    private String nickname;
    
    @Schema(description = "Email пользователя", example = "john@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
//    @RegexGen(value = "[a-z]{5,8}@[a-z]{5,8}\\.com")
    private String email;
    
    @Schema(description = "Пароль пользователя", example = "Password123!", requiredMode = Schema.RequiredMode.REQUIRED, minLength = 8)
    @NotBlank(message = "Password is required")
    @Pattern(
            regexp = "^(?=\\S+$)(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&+=]).{8,}$",
            message = "Password must contain at least one digit, one lower case, one upper case, one special character, no spaces, and be at least 8 characters long"
    )
//    @RegexGen(value = "^[A-Z]{3,5}[a-z]{3,5}[0-9]{3,5}[!@#$%^&]{3,5}$")
    private String password;
}

