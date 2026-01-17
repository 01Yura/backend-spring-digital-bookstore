package online.ityura.springdigitallibrary.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import online.ityura.springdigitallibrary.dto.BaseDto;
import online.ityura.springdigitallibrary.model.Role;

@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Ответ на регистрацию нового пользователя")
public class RegisterResponse extends BaseDto {
    
    @Schema(description = "ID пользователя", example = "1")
    private Long userId;
    
    @Schema(description = "Email пользователя", example = "user@example.com")
    private String email;
    
    @Schema(description = "Роль пользователя", example = "USER")
    private Role role;
    
    @Schema(description = "Сообщение с инструкцией о необходимости подтвердить email", 
            example = "Registration successful! Please check your email and click the verification link to activate your account.")
    private String message;
}

