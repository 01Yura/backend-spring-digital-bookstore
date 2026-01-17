package online.ityura.springdigitallibrary.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
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
@Schema(description = "Запрос с сообщением для читателя")
public class MessageRequest extends BaseDto {
    
    @Schema(description = "Сообщение с вопросом о книге", example = "Что вам больше всего понравилось в этой книге?", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Message is required")
    @Size(max = 256, message = "Message must not exceed 256 characters")
    private String message;
}

