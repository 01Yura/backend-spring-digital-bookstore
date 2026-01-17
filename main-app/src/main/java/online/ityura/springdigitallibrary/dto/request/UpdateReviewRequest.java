package online.ityura.springdigitallibrary.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import online.ityura.springdigitallibrary.dto.BaseDto;

@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "Запрос на обновление отзыва")
public class UpdateReviewRequest extends BaseDto {
    
    @Schema(description = "Обновленный текст отзыва", example = "Обновленный отзыв: книга превзошла все ожидания!", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Review text is required")
    private String text;
}

