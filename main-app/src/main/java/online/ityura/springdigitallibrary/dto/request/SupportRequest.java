package online.ityura.springdigitallibrary.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import online.ityura.springdigitallibrary.dto.BaseDto;

@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Запрос в службу поддержки")
public class SupportRequest extends BaseDto {
    
    @Schema(description = "Сообщение о проблеме или баге (опционально, если отправляется только файл)", 
            example = "Обнаружен баг: при нажатии на кнопку приложение крашится")
    @Size(max = 1000, message = "Message must not exceed 1000 characters")
    private String message;
    
    @Schema(description = "Telegram аккаунт пользователя для обратной связи (опционально)", 
            example = "@username")
    @Size(max = 100, message = "Telegram must not exceed 100 characters")
    private String telegram;
}
